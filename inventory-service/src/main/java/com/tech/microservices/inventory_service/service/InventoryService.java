package com.tech.microservices.inventory_service.service;

import com.tech.microservices.inventory_service.dto.*;
import com.tech.microservices.inventory_service.exception.*;
import com.tech.microservices.inventory_service.model.*;
import com.tech.microservices.inventory_service.repository.InventoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import com.tech.microservices.inventory_service.dto.event.item.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final ObjectMapper objectMapper;
    private final InventoryRepository inventoryRepository;
    private final IdempotencyKeyService idempotencyKeysService;
    private final InventoryReservationService inventoryReservationService;
    private final InventoryTransactionService inventoryTransactionService;
    private final OutboxEventService outboxEventService;

    @Value("${inventory.reservation.expiration-minutes:1440}")
    private long reservationExpirationMinutes;


    public InventoryResponse getInventoryBySkuId(String skuId) {
        Inventory inventory = this.inventoryRepository.findBySkuId(skuId);
        if (inventory == null) {
            throw new InventoryNotFoundException("Inventory not found for SKU: " + skuId);
        }
        return this.bulidInventoryResponse(inventory);
    }


    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), 100);

        // Create a safe Pageable object that handles invalid page numbers and sorts
        Pageable safePageable = PageRequest.of(
               pageable.getPageNumber(),
               pageSize, pageable.getSort()
        );
        Page<Inventory> inventories = inventoryRepository.findAll(safePageable);
        return inventories.map( this::bulidInventoryResponse);
    }

    public InventoryResponse getInventoryBySkuIdAndOrderNo(String skuId, String orderNo) {
        // InventoryReservation inventoryReservation = this.inventoryReservationService.findBySkuIdAndOrderNo(skuId, orderNo);
        // Inventory inventory = inventoryReservation.getInventory();
        // return this.bulidInventoryResponse(inventory);
         // 1. 先确认这笔 skuId + orderNo 的 reservation 存在
        this.inventoryReservationService.findBySkuIdAndOrderNo(skuId, orderNo);

        // 2. 再直接查 inventory 主表
        Inventory inventory = this.inventoryRepository.findBySkuId(skuId);
        if (inventory == null) 
        {
            throw new InventoryNotFoundException("Inventory not found for SKU: " + skuId);
        }
        // 3. 返回 inventory response
        return this.bulidInventoryResponse(inventory);
    }

    public Page<InventoryReservationResponse> getAllReservation(String skuId, Pageable pageable) {
        return this.inventoryReservationService.getAllReservation(skuId, pageable);
    }

    public Page<InventoryReservationResponse> getAllReservationByOrderNo(String orderNo, Pageable pageable) {
        return this.inventoryReservationService.getAllReservationByOrderNo(orderNo, pageable);
    }

    public Page<InventoryTransactionResponse> getAllTransaction(String skuId, Pageable pageable) {
        return this.inventoryTransactionService.getAllTransaction(skuId, pageable);
    }

    public Page<InventoryTransactionResponse> getAllTransactionByOrderNo(String orderNo, Pageable pageable) {
        return this.inventoryTransactionService.getAllTransactionByOrderNo(orderNo, pageable);
    }



    /**
     *Creates an initial inventory record for a SKU.
     *  *
     *  * <p>This method creates the inventory aggregate, initializes reserved quantity
     *  * to zero, records an initial stock transaction, writes an inventory-created
     *  * outbox event, and completes the idempotency record.</p>
     *  *
     *  * @param idempotencyKey unique key used to prevent duplicate create requests
     *  * @param inventoryRequest request containing skuId and initial on-hand quantity
     *  * @return created inventory snapshot
     *  * @throws DuplicateInventoryException if inventory already exists for the SKU
     *  * @throws InvalidInventoryQuantityException if request data is invalid
     */
    @Transactional
    public InventoryResponse createInventory(String idempotencyKey, InventoryRequest inventoryRequest) {

        // 1. validate request
        if(inventoryRequest == null)
        {
            throw new InvalidInventoryQuantityException("Inventory request is required");
        }

        if(inventoryRequest.skuId() == null || inventoryRequest.skuId().isBlank())
        {
            throw new InvalidInventoryQuantityException("SKU ID is required");
        }

        if(inventoryRequest.onHandQuantity() == null || inventoryRequest.onHandQuantity() <= 0  )
        {
            throw new InvalidInventoryQuantityException("On Hand Quantity must be greater than 0");
        }

        if(idempotencyKey == null || idempotencyKey.isBlank())
        {
            throw new InvalidInventoryQuantityException("Idempotency Key is required");
        }

        // 2. start idempotency first
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.CREATE_INVENTORY,
                "INVENTORY",
                null,
                inventoryRequest
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

        // 3. business duplicate check
        if( inventoryRepository.existsBySkuId(inventoryRequest.skuId())) {
            throw new DuplicateInventoryException("Inventory already exists");
        }


         // 4. save inventory
        Inventory inventory = new Inventory();
        inventory.setSkuId(inventoryRequest.skuId());
        inventory.setOnHandQuantity(inventoryRequest.onHandQuantity());
        inventory.setReservedQuantity(0L);
        Inventory savedInventory = this.inventoryRepository.save(inventory);

        //  save inventory transaction
        InventoryTransaction newTransaction = InventoryTransaction.builder()
                .transactionNo(this.generateTransactionNo())
                .inventory(savedInventory)
                .skuId(inventoryRequest.skuId())
                .transactionType(TransactionType.INITIAL_STOCK)
                .quantity(inventoryRequest.onHandQuantity())
                .quantityBefore(0L)
                .quantityAfter(inventoryRequest.onHandQuantity())
                .referenceId(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
        this.inventoryTransactionService.saveNewInventoryTransaction(newTransaction);

        // 6. build response
        InventoryResponse inventoryResponse = this.bulidInventoryResponse(savedInventory);

        // 7. write outbox event
        String payload = this.buildInventoryCreatePayload(savedInventory, InventoryItemEventType.INVENTORY_CREATED.name());
        outboxEventService.createInventoryCreatedEvent(savedInventory, payload);

       // 8. mark idempotency completed
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedInventory.getSkuId(),
                201,
                toJson(inventoryResponse)
        );
        log.info("Inventory created successfully, the inventory id is {}", savedInventory.getId());

        // 9. return response
        return inventoryResponse;
    }


    /**
     * Replenishes inventory for an existing SKU.
     *  *
     *  * <p>This method is idempotent. It locks the inventory row, increases
     *  * on-hand quantity, records an inventory transaction, writes an outbox event,
     *  * and stores the final response in the idempotency record.</p>
     *  *
     *  * @param idempotencyKey unique key used to prevent duplicate stock-in requests
     *  * @param stockInRequest request containing skuId and stock-in quantity
     *  * @return updated inventory snapshot after stock-in
     *  * @throws InventoryNotFoundException if inventory does not exist for the SKU
     *  * @throws InvalidInventoryQuantityException if request data is invalid
     *
     */
    @Transactional
    public InventoryResponse stockIn(String idempotencyKey, StockInRequest stockInRequest) {

        // 1. start idempotency first
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.STOCK_IN,
                "INVENTORY",
                null,
                stockInRequest
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

        // 2. business duplicate check , lock the inventory use pessimistic lock
        Inventory inventory = this.inventoryRepository.findBySkuIdForUpdate(
                stockInRequest.skuId()).orElseThrow(
                () -> new InventoryNotFoundException("Inventory not found")
        );

        Long quantityBefore = inventory.getOnHandQuantity();
        Long quantityAfter = quantityBefore + stockInRequest.quantity();
        // 3.set stock in and save
        inventory.setOnHandQuantity(inventory.getOnHandQuantity() + stockInRequest.quantity());
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory savedInventory = this.inventoryRepository.save(inventory);

        //  4.save inventory transaction
        InventoryTransaction newTransaction = InventoryTransaction.builder()
                .transactionNo(this.generateTransactionNo())
                .inventory(savedInventory)
                .skuId(stockInRequest.skuId())
                .transactionType(TransactionType.STOCK_IN)
                .quantity(stockInRequest.quantity())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceId(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
        this.inventoryTransactionService.saveNewInventoryTransaction(newTransaction);

        // 5. build response
        InventoryResponse inventoryResponse = this.bulidInventoryResponse(savedInventory);

        // 6. write outbox event
        String palyload = this.buildStockInPayload(savedInventory, stockInRequest.quantity(), quantityBefore, quantityAfter,InventoryItemEventType.INVENTORY_STOCKED_IN.name());
        this.outboxEventService.createStockInEvent(savedInventory, palyload);

        // 7. mark idempotency completed
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedInventory.getSkuId(),
                200,
                toJson(inventoryResponse)
        );
        // 8. return response
        return inventoryResponse;
    }


    /**
     * Reserves inventory for an order.
     *
     * <p>This method is idempotent. It locks the inventory row, checks available
     * quantity, increases reserved quantity, creates a reservation record, records
     * an inventory transaction, writes an inventory-reserved outbox event, stores
     * the final response in the idempotency record, and returns the updated
     * inventory snapshot.</p>
     *
     * @param idempotencyKey unique key used to prevent duplicate reserve requests
     * @param inventoryRequest request containing order number, SKU id, and reserve quantity
     * @return updated inventory snapshot after reservation
     * @throws InventoryNotFoundException if inventory does not exist for the SKU
     * @throws InsufficientInventoryException if available quantity is less than requested quantity
     * @throws InvalidInventoryQuantityException if request data or idempotency key is invalid
     */
    @Transactional
    public InventoryResponse reserveInventory(String idempotencyKey, InventoryReservationRequest inventoryRequest)
    {

        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.RESERVE_INVENTORY,
                "INVENTORY",
                null,
                inventoryRequest
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

        // 1. business duplicate check , lock the inventory use pessimistic lock
        Inventory inventory = this.inventoryRepository.findBySkuIdForUpdate(
                inventoryRequest.skuId()).orElseThrow(
                () -> new InventoryNotFoundException("Inventory not found")
        );

        //原来库存数量
        Long onHandQuantity = inventory.getOnHandQuantity();
        //原来预定数量
        Long reservedBeforeQuantity = inventory.getReservedQuantity();
        //此次预定数量
        Long reservedQuantity = inventoryRequest.reservedQuantity();
        //原来可用库存
        Long availableQuantityBefore = onHandQuantity - reservedBeforeQuantity;
        if (availableQuantityBefore < reservedQuantity) {
            throw new InsufficientInventoryException("Inventory insufficient");
        }

        //此次预定之后的预定总数量 = 原来预定数量 + 此次预定数量
        Long reservedAfterQuantity = reservedBeforeQuantity + reservedQuantity;
        //预定之后可用数量 = 原来总数量 - 原来预定数量 - 此次预定数量
        Long availableQuantityAfter = onHandQuantity - reservedAfterQuantity;

        inventory.setReservedQuantity(reservedAfterQuantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory savedInventory = this.inventoryRepository.save(inventory);

        String transactionNo = this.generateTransactionNo();
        String reservationNo = this.generateReservationNo();

        // 4. save reservation
        InventoryReservation newReservation = InventoryReservation.builder()
                .reservationNo(reservationNo)
                .orderNo(inventoryRequest.orderNo())
                .skuId(savedInventory.getSkuId())
                .quantity(reservedQuantity)  //此次预定数量
                .status(ReservationStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .inventory(savedInventory)
                .expiresAt(LocalDateTime.now().plusMinutes(reservationExpirationMinutes))//设置过期时间
                .updatedAt(LocalDateTime.now())
                .build();
        InventoryReservation savedReservation = this.inventoryReservationService.saveNewInventoryReservation(newReservation);


        //record reserve inventory
        //  5.save inventory
        InventoryTransaction newTransaction = InventoryTransaction.builder()
                .transactionNo(transactionNo)
                .inventory(savedInventory)
                .reservedId(savedReservation.getId())
                .skuId(inventoryRequest.skuId())
                .orderNo(inventoryRequest.orderNo())
                .reservationNo(reservationNo)
                .transactionType(TransactionType.RESERVE)
                .quantity(reservedQuantity)  //此次预定数量
                .quantityBefore(reservedBeforeQuantity) //此次预定前的预定数量
                .quantityAfter(reservedAfterQuantity)  //此次预定后的预定数量
                .referenceId(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
        InventoryTransaction savedTransaction = this.inventoryTransactionService.saveNewInventoryTransaction(newTransaction);



        //outbox event
        String payload = this.buildReservationEvent(
                savedInventory,
                savedReservation,
                savedTransaction,
                reservedBeforeQuantity,  //此次预定前的预定数量
                reservedAfterQuantity,
                availableQuantityBefore,
                availableQuantityAfter,
                InventoryItemEventType.INVENTORY_RESERVED.name()
        );
        this.outboxEventService.createReservededInventoryEvent(savedInventory, payload);

        InventoryResponse inventoryResponse = this.bulidInventoryResponse(savedInventory);
        // 7. mark idempotency completed /complete idempotency
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedInventory.getSkuId(),
                200,
                toJson(inventoryResponse)
        );
        // 8. return response
        return inventoryResponse;
    }


    @Transactional
    public InventoryResponse releaseInventory(String idempotencyKey, InventoryReleaseRequest request)
    {
       // 1.startProcess 幂等 / idempotency Key process
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.RELEASE_RESERVATION ,
                "INVENTORY",
                null,
                request
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

       //2.findBySkuIdForUpdate 先锁 inventory
        Inventory inventory = inventoryRepository.findBySkuIdForUpdate(request.skuId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        //3. 再查 reservation
       InventoryReservation inventoryReservation = this.inventoryReservationService.findBySkuIdAndOrderNo(request.skuId(), request.orderNo());

       //4. 校验 reservation.status == RESERVED
       if( inventoryReservation.getStatus() != ReservationStatus.RESERVED)
       {
           throw new InsufficientInventoryException("The reservation is not in reserved status");
       }



        //5. 校验 releaseQuantity == reservation.quantity
       if(! request.releaseQuantity().equals( inventoryReservation.getQuantity()))  // 这里最重要！！因为涉及到整笔预定释放 所以要做判断 是不是整笔数量一致性
       {
           throw new InvalidReservationStatusException("The original reserved quantity is not equal to release quantity");
       }

       //6. 校验 inventory.reservedQuantity >= releaseQuantity
        if (inventory.getReservedQuantity() < request.releaseQuantity()) {
            throw new InvalidReservationStatusException("Reserved quantity is not enough to release");
        }

        Long reservedQuantityBefore = inventory.getReservedQuantity();
        Long reservedQuantityAfter = reservedQuantityBefore - request.releaseQuantity();

        Long availableQuantityBefore = inventory.getOnHandQuantity() - reservedQuantityBefore;
        Long availableQuantityAfter = inventory.getOnHandQuantity() - reservedQuantityAfter;

        //8. 更新 inventory.reservedQuantity
       inventory.setReservedQuantity(reservedQuantityAfter);
       inventory.setUpdatedAt(LocalDateTime.now());
       Inventory savedInventory = this.inventoryRepository.save(inventory);

        //9. 更新 reservation.status = RELEASED
        inventoryReservation.setStatus(ReservationStatus.RELEASED);
        inventoryReservation.setUpdatedAt(LocalDateTime.now());
        InventoryReservation savedReservation = this.inventoryReservationService.saveNewInventoryReservation(inventoryReservation);

        //10. 新增 RELEASE transaction
       String transactionNo = this.generateTransactionNo();
       //create transaction release
        InventoryTransaction inventoryTransaction = InventoryTransaction.builder()
                .inventory(savedInventory)
                .transactionNo(transactionNo)
                .transactionType(TransactionType.RELEASE)
                .createdAt(LocalDateTime.now())
                .orderNo(request.orderNo())
                .quantity(request.releaseQuantity())
                .quantityAfter(reservedQuantityAfter)
                .quantityBefore(reservedQuantityBefore)
                .referenceId(idempotencyKey)
                .reservationNo(inventoryReservation.getReservationNo())
                .skuId(request.skuId())
                .reservedId(savedReservation.getId())
                .build();
        InventoryTransaction savedTransaction = this.inventoryTransactionService.saveNewInventoryTransaction(inventoryTransaction);


        //12. 写 release outbox
        String payload = this.buildInventoryReleasedEvent(
                savedInventory,
                savedReservation,
                savedTransaction,
                reservedQuantityBefore,
                reservedQuantityAfter,
                availableQuantityBefore,
                availableQuantityAfter,
                InventoryItemEventType.INVENTORY_RESERVATION_RELEASED.name()
        );
        this.outboxEventService.createInventoryReleaseEvent(savedInventory, payload);

        //13. markCompleted
        InventoryResponse inventoryResponse = this.bulidInventoryResponse(savedInventory);
        //remark completed
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedInventory.getSkuId(),
                200,
                toJson(inventoryResponse)
        );
        //14. return InventoryResponse
        return inventoryResponse;
    }


    @Transactional
    public InventoryResponse confirmReservation(String idempotencyKey, InventoryConfirmRequest request ) {

        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.CONFIRM_RESERVATION,
                "INVENTORY",
                null,
                request
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);
        //idempotency process

        //grab inventory by skuId and use persistic lock to lock the main table
        Inventory inventory = this.inventoryRepository.
                findBySkuIdForUpdate(request.
                        skuId()).
                orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

        InventoryReservation inventoryReservation = this.inventoryReservationService.findBySkuIdAndOrderNo(request.skuId(), request.orderNo());
        
        //this.inventoryReservationRepository.findBySkuIdAndOrderNo(request.skuId(), request.orderNo()).orElseThrow(() -> new ReservationNotFoundException(request.skuId(), request.orderNo()));

        //grab reservation by skuId and orderNo
        // check reservation.status == RESERVED
        if (inventoryReservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InsufficientInventoryException("The reservation is not in reserved status");
        }

        if (!inventoryReservation.getQuantity().equals(request.confirmQuantity()))
        {
            throw new InsufficientInventoryException("The confirm quantity is not equals original reserved quantity");
        }

        if (inventory.getReservedQuantity() < request.confirmQuantity()) {
            throw new InvalidReservationStatusException("Reserved quantity is not enough to confirm");
        }

        if (inventory.getOnHandQuantity() < request.confirmQuantity()) {
            throw new InsufficientInventoryException("On hand quantity is not enough to confirm");
        }

        //calculate the rest of confirm quantity
        Long confirmQuantity = request.confirmQuantity();

        Long onHandBefore = inventory.getOnHandQuantity();
        Long reservedBefore = inventory.getReservedQuantity();
        Long availableBefore = onHandBefore - reservedBefore;

        Long onHandAfter = onHandBefore - confirmQuantity;
        Long reservedAfter = reservedBefore - confirmQuantity;
        Long availableAfter = onHandAfter - reservedAfter;

        //update inventory.onHandQuantity
        //update inventory.reservedQuantity
        //update inventory.updatedAt = now()
        inventory.setOnHandQuantity(onHandAfter);
        inventory.setReservedQuantity(reservedAfter);
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory savedInventory = this.inventoryRepository.save(inventory);




        //update reservation.status = CONFIRMED
        //update reservation.updatedAt = now()
        inventoryReservation.setStatus(ReservationStatus.CONFIRMED);
        inventoryReservation.setUpdatedAt(LocalDateTime.now());
        InventoryReservation savedInventoryReservation = this.inventoryReservationService.saveNewInventoryReservation(inventoryReservation);
        //this.inventoryReservationRepository.save(inventoryReservation);



        // create new transaction number
        String transanctionNo = this.generateTransactionNo();

        //create new transaction record for this confirm process
        InventoryTransaction newTransantion = InventoryTransaction.builder()
                .transactionNo(transanctionNo)
                .reservationNo(inventoryReservation.getReservationNo())
                .inventory(inventory)
                .createdAt(LocalDateTime.now())
                .orderNo(inventoryReservation.getOrderNo())
                .quantity(confirmQuantity)
                .quantityBefore(onHandBefore)
                .quantityAfter(onHandAfter)
                .referenceId(idempotencyKey)
                .reservedId(inventoryReservation.getId())
                .transactionType(TransactionType.SALE)
                .skuId(request.skuId())
                .build();

        InventoryTransaction savedTransation = this.inventoryTransactionService.saveNewInventoryTransaction(newTransantion);



        //create InventoryConfirmedEvent
        //write outbox event
        String payload = this.createInventoryConfirmedEvent(
                savedInventory,
                savedInventoryReservation,
                savedTransation,
                 onHandBefore,
                 onHandAfter,
                 reservedBefore,
                 reservedAfter,
                 availableBefore,
                 availableAfter,
                 InventoryItemEventType.INVENTORY_RESERVATION_CONFIRMED.name()
        );
        this.outboxEventService.createInventoryConfirmedEvent(savedInventory, payload);

        //mark idempotency completed
        InventoryResponse inventoryResponse = this.bulidInventoryResponse(savedInventory);
        // 7. mark idempotency completed /complete idempotency
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedInventory.getSkuId(),
                200,
                toJson(inventoryResponse)
        );
        // 8. return response
        return inventoryResponse;
    }


    @Transactional
    public InventoryResponse expireReservation(String idempotencyKey, InventoryReservationExpiredRequest request )
    {
        //Idempotency key relevant process
         IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.EXPIRE_RESERVATION,
                "INVENTORY",
                null,
                request
        );
        
        // Start idempotency process
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);
        

        //Fetch inventory by skuId and use pessimistic lock to lock the main table
        //Used pessimistic lock to lock the main table
        Inventory inventory = this.inventoryRepository.
                findBySkuIdForUpdate(request.
                        skuId()).
                orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));


        //Used orderNo and skuId to find the reservation record
        //Fetch reservation by skuId and orderNo
        InventoryReservation inventoryReservation = this.inventoryReservationService.findBySkuIdAndOrderNo(request.skuId(), request.orderNo());
    

        //check reservation.status == RESERVED
        if (inventoryReservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InsufficientInventoryException("The reservation is not in reserved status");
        }

        //check reservation.expiresAt < now()
        if (inventoryReservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidReservationStatusException("The reservation has not expired yet");
        }

        //update reservation.status = EXPIRED
        //update reservation.updatedAt = now()
        inventoryReservation.setStatus(ReservationStatus.EXPIRED);
        inventoryReservation.setUpdatedAt(LocalDateTime.now());
        InventoryReservation savedReservation = this.inventoryReservationService.saveNewInventoryReservation(inventoryReservation);

        //update inventory.reservedQuantity = inventory.reservedQuantity - reservation.quantity
        Long expiredQuantity = inventoryReservation.getQuantity();
        Long availableQuantityBefore = inventory.getOnHandQuantity() - inventory.getReservedQuantity();
        Long availableQuantityAfter = availableQuantityBefore + expiredQuantity;
        Long reservedQuantityBefore = inventory.getReservedQuantity();
        Long reservedQuantityAfter = reservedQuantityBefore - expiredQuantity;
        inventory.setReservedQuantity(reservedQuantityAfter);
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory savedInventory = this.inventoryRepository.save(inventory);

    

        //create new transaction record for this expired process
        String transactionNo = this.generateTransactionNo();
        InventoryTransaction newTransaction = InventoryTransaction.builder()
                .transactionNo(transactionNo)
                .reservationNo(inventoryReservation.getReservationNo())
                .inventory(inventory)
                .createdAt(LocalDateTime.now())
                .quantity(expiredQuantity)
                .quantityBefore(reservedQuantityBefore)
                .quantityAfter(reservedQuantityAfter)
                .referenceId(idempotencyKey)
                .reservedId(inventoryReservation.getId())
                .transactionType(TransactionType.EXPIRE)
                .skuId(request.skuId())
                .orderNo(request.orderNo())
                .build();
        InventoryTransaction savedTransaction = this.inventoryTransactionService.saveNewInventoryTransaction(newTransaction);

        //write outbox event, create InventoryReservationExpiredEvent
        String payload = this.createInventoryReservationExpiredEvent(
                savedInventory,
                savedReservation,
                savedTransaction,
                reservedQuantityBefore,
                reservedQuantityAfter,
                availableQuantityBefore,
                availableQuantityAfter,
                InventoryItemEventType.INVENTORY_RESERVATION_EXPIRED.name()
        );
        this.outboxEventService.createInventoryReservationExpiredEvent(savedInventory, payload);
        
        
        //mark idempotency completed
        InventoryResponse inventoryResponse = this.bulidInventoryResponse(savedInventory);
        // 7. mark idempotency completed /complete idempotency
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedInventory.getSkuId(),
                200,
                toJson(inventoryResponse)
        );

       return inventoryResponse;

    }



    public String createInventoryReservationExpiredEvent(Inventory inventory,
                                                InventoryReservation inventoryReservation,
                                                InventoryTransaction inventoryTransaction,
                                                Long reservedBefore,
                                                Long reservedAfter,
                                                Long availableBefore,
                                                Long availableAfter,
                                                String eventType)
    {

        InventoryReservationExpiredEvent inventoryReservationExpiredEvent = new InventoryReservationExpiredEvent(
                inventory.getId(),
                inventory.getSkuId(),
                inventoryReservation.getOrderNo(),
                inventoryReservation.getReservationNo(),
                inventoryTransaction.getTransactionNo(),
                eventType,
                inventoryTransaction.getQuantity(),
                reservedBefore,
                reservedAfter,
                availableBefore,
                availableAfter,
                LocalDateTime.now()
        );
        return toJson(inventoryReservationExpiredEvent);
    }
    

    public String createInventoryConfirmedEvent(Inventory inventory,
                                                InventoryReservation inventoryReservation,
                                                InventoryTransaction inventoryTransaction,
                                                Long onHandBefore,
                                                Long onHandAfter,
                                                Long reservedBefore,
                                                Long reservedAfter,
                                                Long availableBefore,
                                                Long availableAfter,
                                                String eventType)
    {

        InventoryReservationConfirmedEvent inventoryComfirmedEvent = new InventoryReservationConfirmedEvent(
                inventory.getId(),
                inventory.getSkuId(),
                inventoryReservation.getOrderNo(),
                inventoryReservation.getReservationNo(),
                inventoryTransaction.getTransactionNo(),
                inventoryTransaction.getQuantity(),
                onHandBefore,
                onHandAfter,
                reservedBefore,
                reservedAfter,
                availableBefore,
                availableAfter,
                LocalDateTime.now()
        );
        return toJson(inventoryComfirmedEvent);
    }


    public InventoryResponse bulidInventoryResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getSkuId(),
                inventory.getOnHandQuantity(),
                inventory.getReservedQuantity(),
                inventory.getOnHandQuantity() - inventory.getReservedQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

    private InventoryReservationResponse toReservationResponse(InventoryReservation inventoryReservation)
    {
        return new InventoryReservationResponse(
                inventoryReservation.getReservationNo(),
                inventoryReservation.getOrderNo(),
                inventoryReservation.getSkuId(),
                inventoryReservation.getQuantity(),
                inventoryReservation.getStatus(),
                inventoryReservation.getExpiresAt(),
                inventoryReservation.getCreatedAt(),
                inventoryReservation.getUpdatedAt()
        );
    }

    private InventoryTransactionResponse toInventoryTransactionResponse(InventoryTransaction inventoryTransaction)
    {
        return new InventoryTransactionResponse(
                inventoryTransaction.getTransactionNo(),
                inventoryTransaction.getReservedId(),
                inventoryTransaction.getSkuId(),
                inventoryTransaction.getOrderNo(),
                inventoryTransaction.getReservationNo(),
                inventoryTransaction.getTransactionType(),
                inventoryTransaction.getQuantity(),
                inventoryTransaction.getQuantityBefore(),
                inventoryTransaction.getQuantityAfter(),
                inventoryTransaction.getReferenceId(),
                inventoryTransaction.getCreatedAt()
        );
    }


    private String buildInventoryReleasedEvent(Inventory savedInventory,
                                               InventoryReservation savedInventoryReservation,
                                               InventoryTransaction savedInventoryTransaction,
                                               Long reservedQuantityBefore,
                                               Long reservedQuantityAfter,
                                               Long availableQuantityBefore,
                                               Long availableQuantityAfter,
                                               String eventType)
    {
        InventoryReservationReleasedEvent releaseEvent = new InventoryReservationReleasedEvent(
                savedInventory.getId(),
                savedInventoryReservation.getOrderNo(),
                savedInventory.getSkuId(),
                savedInventoryTransaction.getTransactionNo(),
                savedInventoryReservation.getReservationNo(),
                eventType,
                savedInventoryReservation.getQuantity(),
                reservedQuantityBefore,
                reservedQuantityAfter,
                availableQuantityBefore,
                availableQuantityAfter,
                LocalDateTime.now()
        );

        return toJson(releaseEvent);
    }

    // Build payload for InventoryCreatedEvent
    private String buildInventoryCreatePayload(Inventory inventory, String eventType)
    {
        InventoryCreatedEvent inventoryCreatedEvent = new InventoryCreatedEvent(
                inventory.getId(),
                eventType,
                inventory.getSkuId(),
                inventory.getOnHandQuantity(),
                inventory.getReservedQuantity(),
                inventory.getCreatedAt()
        );
        return toJson(inventoryCreatedEvent);
    }

    private String buildReservationEvent(Inventory inventory,
                                         InventoryReservation inventoryReservation,
                                         InventoryTransaction inventoryTransaction,
                                         Long reservedQuantityBefore,
                                         Long reservedQuantityAfter,
                                         Long availableQuantityBefore,
                                         Long availableQuantityAfter,
                                         String eventType)
    {
        InventoryReservedEvent inventoryReservedEvent = new InventoryReservedEvent(
               inventory.getId(),
               inventory.getSkuId(),
               inventoryReservation.getOrderNo(),
               inventoryReservation.getReservationNo(),
               inventoryTransaction.getTransactionNo(),
               eventType,
               inventoryReservation.getQuantity(),
               reservedQuantityBefore,
               reservedQuantityAfter,
               availableQuantityBefore,
               availableQuantityAfter,
               inventoryReservation.getExpiresAt(),
               LocalDateTime.now()
       );
       return toJson(inventoryReservedEvent);
    }

    private String buildStockInPayload(Inventory inventory, Long stockInQuantity, Long quantityBefore, Long quantityAfter,String eventType)
    {
        StockInEvent event = new StockInEvent(
                inventory.getId(),
                inventory.getSkuId(),
                eventType,
                stockInQuantity,
                quantityBefore,
                quantityAfter,
                inventory.getCreatedAt()
        );

        return toJson(event);
    }


    private String toJson(Object object)
    {
        return objectMapper.writeValueAsString(object);
    }

    private String generateTransactionNo()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateReservationNo()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
