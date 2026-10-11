package com.tech.microservices.order_service.service;

import com.tech.microservices.order_service.client.InventoryClient;
import com.tech.microservices.order_service.dto.*;

import com.tech.microservices.order_service.repository.OrderRepository;

import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import com.tech.microservices.order_service.exception.DuplicateIdempotencyKeyException;
import com.tech.microservices.order_service.exception.DuplicateOrderException;
import com.tech.microservices.order_service.exception.InsufficientInventoryException;
import com.tech.microservices.order_service.exception.InvalidOrderStatusTransitionException;
import com.tech.microservices.order_service.exception.OrderNotFoundException;
import com.tech.microservices.order_service.exception.ReservationNotFoundException;
import com.tech.microservices.order_service.integration.common.event.EventEnvelope;
import com.tech.microservices.order_service.integration.inventory.command.InventoryCommandType;
import com.tech.microservices.order_service.integration.inventory.command.InventoryReleaseReason;
import com.tech.microservices.order_service.integration.inventory.command.InventoryReleaseRequestCommand;
import com.tech.microservices.order_service.integration.inventory.command.InventoryReleaseRequestItem;
import com.tech.microservices.order_service.integration.inventory.command.InventoryReservationRequestCommand;
import com.tech.microservices.order_service.integration.inventory.command.InventoryReservationRequestItem;
import com.tech.microservices.order_service.model.IdempotencyKeyStatus;
import com.tech.microservices.order_service.model.IdempotencyKeys;
import com.tech.microservices.order_service.model.IdempotencyOperation;
import com.tech.microservices.order_service.model.Order;
import com.tech.microservices.order_service.model.OrderEventType;
import com.tech.microservices.order_service.model.OrderItems;
import com.tech.microservices.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import feign.FeignException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyKeysService idempotencyKeysService;
    private final OutboxEventsService outboxEventsService;
    private final ObjectMapper objectMapper;
    private final InventoryClient inventoryClient;

    /*
     * Legacy reference: synchronous Inventory Feign flow
     * 旧版同步实现，仅用于学习对照，不会被执行。
     *
     * 调用链：Order Service -> InventoryClient -> Inventory Service。
     * 创建订单时会同步预定每个 SKU；库存不足会立即返回冲突，
     * 部分 SKU 已预定后发生异常时，还需要逐项调用 releaseInventory() 补偿。
     *
     * 这种方式能立即得到库存结果，但订单与库存不属于同一个数据库事务：
     * Inventory 不可用会直接阻塞订单创建，补偿失败也可能造成跨服务数据不一致。
     *
     * 当前实现已改为 Saga + Transactional Outbox + Kafka：
     * createOrder() 只保存 INVENTORY_PENDING 订单和库存预定命令，
     * Inventory 异步处理后，再由 Order Consumer 回写最终状态。
     *
     * -------------------- Legacy implementation --------------------
    //   @Transactional
    // public OrderResponse createOrder(String idempotencyKey, OrderRequest orderRequest) {

    //     if (idempotencyKeysService.findByIdempotencyKey(idempotencyKey).isPresent()) {
    //         throw new DuplicateIdempotencyKeyException("Idempotency key already exists before creating order");
    //     }

    //     if (orderRepository.findByOrderNumber(orderRequest.orderNumber()).isPresent()) {
    //         throw new DuplicateOrderException("Order already exists with orderNumber: " + orderRequest.orderNumber());
    //     }

    //     // 1. 根据 OrderRequest body 生成 requestHash,这个过程封装在startProcess（）方法里  / Generate requestHash from the request body.
    //     // 2. 使用 Idempotency-Key 查询 idempotency_keys 表 这个过程封装在startProcess（）方法里 / Check idempotency_keys by Idempotency-Key.
    //     // 3. 如果 key 已存在，当前版本先直接拒绝，这个过程封装在startProcess（）方法里/ If the key already exists, reject the request for now.
    //     // throw new RuntimeException("Idempotency key already exists before creating order");
    //     // 4. 创建 idempotency_keys 记录，状态为 PROCESSING 这个过程封装在startProcess（）方法里/ Insert an idempotency record with PROCESSING status.
    //     IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
    //             IdempotencyOperation.CREATE_ORDER,
    //             "ORDER",
    //             null,
    //             orderRequest
    //     );
    //     IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

    //     // Reserve inventory for each order item
    //     List<OrderItemRequest> reservedItems = new ArrayList<>();
    //     try {
    //         for (OrderItemRequest item : orderRequest.orderItems()) {
    //             InventoryResponse inventoryResponse = inventoryClient.reserveInventory(
    //                     "reserve-" + orderRequest.orderNumber() + "-" + item.skuId(),
    //                     new InventoryReservationRequest(
    //                             orderRequest.orderNumber(),
    //                             item.skuId(),
    //                             item.quantity().longValue()
    //                     )
    //             );
    //             reservedItems.add(item);

    //         }
    //         // 5. 创建 Order 主表对象。/ Build the Order entity for the orders table.
    //         Order order = Order.builder()
    //                 .orderNumber(orderRequest.orderNumber())
    //                 .userId(orderRequest.userId())
    //                 .orderStatus(OrderStatus.INVENTORY_RESERVED)
    //                 .currency(orderRequest.currency())
    //                 .idempotencyKey(idempotencyKey)
    //                 .note(orderRequest.note())
    //                 .billingAddress(orderRequest.billingAddress())
    //                 .shippingAddress(orderRequest.shippingAddress())
    //                 .recipientName(orderRequest.recipientName())
    //                 .recipientEmail(orderRequest.recipientEmail())
    //                 .recipientPhone(orderRequest.recipientPhone())
    //                 .build();

    //         // 6. 将 OrderItemRequest 转换成 OrderItems / Convert each OrderItemRequest into an OrderItems entity
    //         List<OrderItems> orderItems = orderRequest.orderItems().stream()
    //                 .map(item2 -> toOrderItem(item2, order))
    //                 .toList();

    //         // 7. 设置 Order 和 OrderItems 的双向关系 / Link order_items back to the parent Order.
    //         order.setOrderItems(orderItems);

    //         // 8. 根据 item.subtotal 汇总 totalAmount / Calculate order totalAmount from item subtotals.
    //         BigDecimal totalAmount = orderItems.stream()
    //                 .map(OrderItems::getSubtotal)
    //                 .reduce(BigDecimal.ZERO, BigDecimal::add);
    //         order.setTotalAmount(totalAmount);

    //         // 9. 保存 Order aggregate。
    //         //     *    Save the Order aggregate.
    //         //     *    Because Order.orderItems uses CascadeType.ALL,
    //         //     *    orderRepository.save(order) will also save order_items.
    //         Order saveOrder = orderRepository.save(order);

    //         // Transform payload by saved order
    //         String payload = buildOrderCreatedPayload(saveOrder, OrderEventType.ORDER_CREATED);

    //         // 10. 创建 ORDER_CREATED outbox event && inventory reservation event
    //         //     *     Create an ORDER_CREATED outbox event.
    //         //     *     The event is stored as PENDING and will be published later by OutboxPublisher.
    //         outboxEventsService.createOrderCreatedEvent(saveOrder, payload);
       
    //         String inventoryReservationPayload = buildInventoryReservationRequestedPayload(saveOrder);
    //         outboxEventsService.createInventoryReservationRequestedEvent(saveOrder, inventoryReservationPayload);

    //         //11. 将 Order 转换成 OrderResponse，并序列化 responseBody
    //         // Convert the saved Order to OrderResponse and serialize it as responseBody.
    //         OrderResponse orderResponse = toResponse(saveOrder);

    //         // Transform to json by order response
    //         String responseBody = toJson(orderResponse);

    //         // 12. 更新 idempotency_keys 状态为 COMPLETED。
    //         // Mark the idempotency record as COMPLETED.
    //         idempotencyKeysService.markCompleted(
    //                 idempotencyKeys,
    //                 saveOrder.getOrderNumber(),
    //                 201,
    //                 IdempotencyKeyStatus.COMPLETED.name());

    //         log.info("Order {} created successfully", order.getId());

    //         // Return saved order response
    //         return toResponse(saveOrder);
        
    //     } 
    //     catch (FeignException.Conflict e) {
    //       log.error("Feign conflict error occurred while creating order", e);
    //       for (OrderItemRequest item : reservedItems) {
    //             inventoryClient.releaseInventory(
    //             "release-" + orderRequest.orderNumber() + "-" + item.skuId(),
    //             new InventoryReleaseRequest(
    //                     orderRequest.orderNumber(),
    //                     item.skuId(),
    //                     item.quantity().longValue()
    //             )
    //            );
    //      }
    //       throw new InsufficientInventoryException("Inventory insufficient");
    //     }
        
    //     catch (Exception e) {
    //         for (OrderItemRequest item : reservedItems) {
    //             inventoryClient.releaseInventory(
    //                     "release-" + orderRequest.orderNumber() + "-" + item.skuId(),
    //                     new InventoryReleaseRequest(
    //                             orderRequest.orderNumber(),
    //                             item.skuId(),
    //                             item.quantity().longValue()
    //                     )
    //             );
    //         }
    //         throw e;
    //     }
    // }
    */


    /**
     * 创建订单，并通过 Saga + Transactional Outbox 异步请求库存预定。
     *
     * 本地事务会同时保存 INVENTORY_PENDING 订单、订单明细、幂等结果，
     * 以及 ORDER_CREATED 和 INVENTORY_RESERVATION_REQUESTED 两条 Outbox 记录。
     * 任一步骤失败时，本地数据全部回滚。
     *
     * 本方法不直接调用 InventoryClient，也不等待库存服务返回结果。
     * Inventory Service 消费 Kafka 命令后执行预定，Order Consumer 再根据
     * 结果将订单更新为 INVENTORY_RESERVED 或 INVENTORY_FAILED。
     * 因此方法返回成功只表示订单及库存预定命令已成功保存。
     *
     * @param idempotencyKey HTTP 请求的幂等键
     * @param orderRequest 创建订单请求
     * @return 状态为 INVENTORY_PENDING 的订单
     */
    @Transactional 
    public OrderResponse createOrder(String idempotencyKey, OrderRequest orderRequest)
    {
        // Idempotency process 幂等检查
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(
                idempotencyKey,
                new IdempotencyRequestContext(
                        IdempotencyOperation.CREATE_ORDER,
                        "ORDER",
                        null,
                        orderRequest
                )
        );

        // 1. check if the order already exists 检查订单是否已经存在
        if (orderRepository.findByOrderNumber(orderRequest.orderNumber()).isPresent()) {
            throw new DuplicateOrderException("Order already exists with orderNumber: " + orderRequest.orderNumber());
        }

        // 2. create the order entity 创建订单实体
        Order order = Order.builder()
            .orderNumber(orderRequest.orderNumber())
            .userId(orderRequest.userId())
            .currency(orderRequest.currency())
            .idempotencyKey(idempotencyKey)
            .note(orderRequest.note())
            .shippingAddress(orderRequest.shippingAddress())
            .billingAddress(orderRequest.billingAddress())
            .recipientName(orderRequest.recipientName())
            .recipientEmail(orderRequest.recipientEmail())
            .recipientPhone(orderRequest.recipientPhone())
            .orderStatus(OrderStatus.INVENTORY_PENDING)  //新订单一定是 pending
            .build();

         // 3. convert order items
         // 创建订单明细
         List<OrderItems> orderItems = orderRequest.orderItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
                    return OrderItems.builder()
                            .order(order)
                            .skuId(item.skuId())
                            .skuName(item.skuName())
                            .unitPrice(item.unitPrice())
                            .quantity(item.quantity())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();
        order.setOrderItems(orderItems);

        // 4. calculate total amount 计算总金额
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItems::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 5. save the order 保存订单
        Order savedOrder = orderRepository.save(order);



        // 6. create outbox event 创建订单创建事件
        String outboxEvent = buildOrderCreatedPayload(savedOrder);
        outboxEventsService.createOrderCreatedEvent(savedOrder, outboxEvent);

        // 7. create inventory reservation event 创建库存预留事件
        String inventoryReservationEvent = buildInventoryReservationRequestedPayload(savedOrder);
        outboxEventsService.createInventoryReservationRequestedEvent(savedOrder, inventoryReservationEvent);

        // 8. mark idempotency key as completed 标记幂等键为已完成
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedOrder.getOrderNumber(),
                201,
                IdempotencyKeyStatus.COMPLETED.name()
        );

        return toResponse(savedOrder);
    }


    // Create an OrderCreatedEvent by Order 创建订单事件
    private String buildOrderCreatedPayload(Order order)
    {
        OrderCreatedEvent oce = new OrderCreatedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getRecipientName(),
                order.getRecipientEmail(),
                order.getRecipientPhone(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getOrderStatus(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemCreatedEvent(
                                item.getSkuId(),
                                item.getSkuName(),
                                item.getUnitPrice(),
                                item.getQuantity(),
                                item.getSubtotal()
                        ))
                        .toList()
        );
        return this.buildEventEnvelope(OrderEventType.ORDER_CREATED.name(), order.getOrderNumber(), oce);
    }



    // Get all orders by pagination and sorting by controller transmit Pageable and parameters
    public Page<OrderSummaryResponse> getAllOrders(Pageable pageable) {
        // Settle the page size to a maximum of 100
        int pageSize = Math.min(pageable.getPageSize(), 100);

        // Create a safe Pageable object that handles invalid page numbers and sorts
        Pageable safePageable = PageRequest.of(
               pageable.getPageNumber(),
               pageSize, pageable.getSort()
        );
        Page<Order> orders = orderRepository.findAll(safePageable);
        return orders.map( this::toSummaryResponse);
    }

    // Get Order and then transfer By ID
    public OrderResponse getOrderById(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            //throw new RuntimeException("Order not found");
            throw new OrderNotFoundException(orderNumber);
        }
        return toResponse(order);
    }


    // Update Order Basic Info
    @Transactional
    public OrderResponse updateOrderBasicInfo(String idempotencyKey, String orderNumber, UpdateOrderBasicRequest request)
    {
        // Idempotency process
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.UPDATE_ORDER_BASIC_INFO,
                "ORDER",
                orderNumber,
                request
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);


        // check the order status
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));

        if( !canUpdateBasicOrderInfo(order.getOrderStatus()))
        {
            throw new InvalidOrderStatusTransitionException("Order cannot be updated in its current status");
        }

        // set the new values
        updateIfPresent( request.billingAddress(), order::setBillingAddress);
        updateIfPresent(request.recipientEmail(), order:: setRecipientEmail);
        updateIfPresent(request.recipientName(), order:: setRecipientName);
        updateIfPresent(request.recipientPhone(), order:: setRecipientPhone);
        updateIfPresent(request.shippingAddress(), order:: setShippingAddress);
        updateIfPresent(request.note(), order:: setNote);

        Order savedOrder = orderRepository.save(order);

        String payload = buildOrderBasicInfoChangedPayload(savedOrder);
        outboxEventsService.createOrderBasicInfoChangedEvent(savedOrder, payload);

        OrderResponse response = toResponse(savedOrder);

        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                orderNumber,
                204,
                null
        );

       return response;
    }


    //1.@Transactional
    //2.startProcess
    //3.findByOrderNumber
    //4.canUpdateOrderItem
    //5.把 request.orderItems 转成 OrderItems
    //6.clear 旧 items 然后addAll 新 items
    //7.重新计算 totalAmount
    //8. save order
    //9. 写 ORDER_ITEMS_CHANGED outbox
    //10. markCompleted
    //11. return OrderResponse
    @Transactional
    public OrderResponse updateOrderItems(String idempotencyKey, String orderNumber, UpdateOrderItemsRequest itemsRequest)
    {
        // Idempotency process
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.UPDATE_ORDER_ITEMS,
                "ORDER",
                orderNumber,
                itemsRequest
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);


        // check the order status
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));

        if( !canUpdateOrderItem(order.getOrderStatus()))
        {
            throw new InvalidOrderStatusTransitionException("Order cannot be updated in its current status");
        }

        // 将 OrderItemRequest 转换成 OrderItems / Convert each OrderItemRequest into an OrderItems entity
        List<OrderItems> orderItems = itemsRequest.orderItems().stream()
                .map(item -> toOrderItem(item, order))
                .toList();

        // 重新计算总价，因为订单物品项目变了 / Recalculate the total amount cause the order items have been changed
        BigDecimal totalAmount = orderItems.stream()
                        .map(OrderItems::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 清理旧的items / clear old items and add new items
        order.getOrderItems().clear();
        // 添加新的 items / add new items
        order.getOrderItems().addAll(orderItems);

        Order savedOrder = orderRepository.save(order);


        // 要写生成新的payload和添加新的outbox event / Create a new payload and add a new outbox event
        String changedItem = this.buildOrderItemChangedPayload(savedOrder);
        this.outboxEventsService.createOrderItemsChangedEvent(savedOrder, changedItem);

        OrderResponse response = toResponse(savedOrder);

        // 写入outbox / Create an ORDER_ITEMS_CHANGED outbox event.
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                orderNumber,
                200,
                toJson(response)
        );

        return response;
    }

    public String buildOrderItemChangedPayload(Order order)
    {
        OrderItemsChangedEvent event = new OrderItemsChangedEvent(
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemCreatedEvent(
                                item.getSkuId(),
                                item.getSkuName(),
                                item.getUnitPrice(),
                                item.getQuantity(),
                                item.getSubtotal()
                        ))
                        .toList(),
                order.getUpdatedAt()
        );
        return this.buildEventEnvelope(OrderEventType.ORDER_ITEMS_CHANGED.name(), order.getOrderNumber(),event);
    }

    // Build an InventoryReservationRequestedEvent payload for outbox
    // 为 outbox 构建 InventoryReservationRequestedEvent 负载
    public String buildOrderBasicInfoChangedPayload(Order order)
    {
        OrderBasicInfoChangedEvent event = new OrderBasicInfoChangedEvent(
                order.getOrderNumber(),
                order.getUserId(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getNote(),
                order.getRecipientName(),
                order.getRecipientEmail(),
                order.getRecipientPhone(),
                order.getUpdatedAt()
        );

        return this.buildEventEnvelope(OrderEventType.ORDER_BASIC_INFO_CHANGED.name(), order.getOrderNumber(), event);
    }

    // Check if the order status allows updating basic order info
    // 检查订单状态是否允许更新基本信息，主要是收件人和收件地址 在不影响库存或者支付前都可以修改
    public boolean canUpdateBasicOrderInfo(OrderStatus currentStatus)
    {
        return switch (currentStatus)
        {
            case 
            CREATED, 
            INVENTORY_PENDING, 
            INVENTORY_RESERVED -> true;
            

            case 
            CANCELLATION_PENDING,
            CANCELLATION_FAILED,
            PAYMENT_PENDING, 
            PAYMENT_FAILED,
            INVENTORY_FAILED,
            PAID,
            CANCELLATION_COMPLETED,
            CONFIRMED , 
            COMPLETED, 
            CANCELLED, 
            EXPIRED,
            REFUNDED -> false;
            default -> throw new IllegalArgumentException("Unexpected value: " + currentStatus);
        };
    }

    // Check if the order status allows updating order items
    // 检查订单状态是否允许更新订单物品
    public boolean canUpdateOrderItem(OrderStatus currentStatus)
    {
        return switch (currentStatus) {
            case CREATED -> true;
            case INVENTORY_PENDING, 
            INVENTORY_RESERVED, 
            CANCELLATION_PENDING,
            CANCELLATION_FAILED,
            CANCELLATION_COMPLETED,
            PAYMENT_PENDING,  
            PAYMENT_FAILED, 
            INVENTORY_FAILED, 
            PAID, 
            CONFIRMED, 
            COMPLETED, 
            CANCELLED, 
            EXPIRED, 
            REFUNDED -> false;
            default -> throw new IllegalArgumentException("Unexpected value: " + currentStatus);
        };

    }

    /**
     * 取消订单，并释放该订单已经预定的库存。
     *
     * 当前实现会先完成订单存在性、请求幂等性和状态转换校验，随后通过
     * InventoryClient 逐项同步释放库存。全部释放成功后，将订单更新为
     * CANCELLED，并在本地事务中保存 ORDER_CANCELLED 和
     * INVENTORY_RELEASE_REQUESTED 两条 Outbox 记录。
     *
     * 重复取消已经处于 CANCELLED 状态的订单时，本方法直接按成功处理。
     * 如果订单状态不允许取消，或者对应的库存预定不存在，则抛出业务异常，
     * 本地订单状态和 Outbox 记录不会提交。
     *
     * @param idempotencyKey HTTP 请求的幂等键
     * @param orderNumber 要取消的订单号
     * @throws OrderNotFoundException 订单不存在时抛出
     * @throws InvalidOrderStatusTransitionException 当前订单状态不允许取消时抛出
     * @throws ReservationNotFoundException 对应的库存预定不存在时抛出
     */
    @Transactional
    public void cancelOrder(String idempotencyKey, String orderNumber) {

        // 1. find the order 查询需要取消的订单
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));

        // 2. start idempotency process 开始取消订单的幂等处理
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.CANCEL_ORDER,
                "ORDER",
                orderNumber,
                null
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

        // 3. get the current order status 获取订单当前状态
        OrderStatus currentStatus = order.getOrderStatus();

        // 4. handle repeated cancellation 已取消的订单按幂等成功处理
        if(currentStatus == OrderStatus.CANCELLED)
        {
            idempotencyKeysService.markCompleted(
                    idempotencyKeys,
                    orderNumber,
                    204,
                    null
            );
            return;
        }

        // 5. validate whether the order can be cancelled 校验当前状态是否允许取消
        if (!canCancel(order.getOrderStatus())) {
            throw new InvalidOrderStatusTransitionException("Order cannot be cancelled in its current status");
        }


        //原来我犯的错误是直接使用Fiegn 同步释放了库存 然后又实用了异步释放 属于重复释放了
        // 6. release reserved inventory for every order item 逐项释放订单预定库存
        // order.getOrderItems().forEach(item -> {
        //     try{

        //          inventoryClient.releaseInventory(
        //             "release-" + order.getOrderNumber() + "-" + item.getSkuId(),
        //             new InventoryReleaseRequest(
        //                     order.getOrderNumber(),
        //                     item.getSkuId(),
        //                     Long.valueOf(item.getQuantity())
        //             )
        //     );
        //     }catch(FeignException.NotFound e)
        //     {
        //         log.error("Failed to release inventory for order {} item {}: {}", order.getOrderNumber(), item.getSkuId(), e.getMessage());
        //         throw new ReservationNotFoundException("Failed to release inventory for order " + order.getOrderNumber() + " item " + item.getSkuId() + ": " + e.getMessage());
        //     }
           
        // });

        // 7. update and save the cancelled order 更新并保存取消后的订单
        // order.setOrderStatus(OrderStatus.CANCELLED);
        // Order savedOrder = orderRepository.save(order);


        order.setOrderStatus(OrderStatus.CANCELLATION_PENDING);
        Order savedOrder = orderRepository.save(order);


        // 8. create order cancellation requested event 创建订单取消的状态变化事件
        String cancellationRequestedPayload = buildOrderStatusChangedPayload( 
        savedOrder,
        currentStatus,
        OrderStatus.CANCELLATION_PENDING,
        OrderEventType.ORDER_CANCELLATION_REQUESTED);

        outboxEventsService.createOrderStatusEvent(
        savedOrder,
        OrderEventType.ORDER_CANCELLATION_REQUESTED,
        cancellationRequestedPayload);

        // // Update idempotency_keys status to COMPLETED
        // 原来流程直接标记幂等键为已完成，但现在改为在创建库存释放事件后再标记为已完成
        // idempotencyKeysService.markCompleted(
        //         idempotencyKeys,
        //         savedOrder.getOrderNumber(),
        //         204,
        //         "Canceled successfully"
        // );
        // log.info("Order {} cancelled successfully", savedOrder.getId());


        // 9. create the inventory release command outbox event 创建库存释放命令事件
        String inventoryReleaseEvent = buildInventoryReleaseRequestedPayload(savedOrder, InventoryReleaseReason.ORDER_CANCELLATION);
        outboxEventsService.createInventoryReleaseRequestedEvent(savedOrder, inventoryReleaseEvent);

        // 10. mark the idempotency key as completed 标记幂等键为已完成，204 表示成功但无响应体
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedOrder.getOrderNumber(),
                204,
                IdempotencyKeyStatus.COMPLETED.name()
        );
    }




    // Update order status to confirmed and confirm inventory reservation
    @Transactional
    public OrderResponse updateOrderStatus(String idempotencyKey, String orderNumber, OrderStatus newStatus, OrderEventType eventType) {

        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.UPDATE_ORDER_STATUS,
                "ORDER",
                orderNumber,
                newStatus
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == newStatus) {
            idempotencyKeysService.markCompleted(
                    idempotencyKeys,
                    orderNumber,
                    204,
                    null
            );
            return toResponse(order);
        }

        if(!canTransition(currentStatus, newStatus))
        {
            //throw new RuntimeException("Order status transition not allowed");
            throw new InvalidOrderStatusTransitionException("Order status transition not allowed");
        }
        order.setOrderStatus(newStatus);
        Order saveOrder = orderRepository.save(order);

        String payload = buildOrderStatusChangedPayload(saveOrder, currentStatus, newStatus , eventType);

        outboxEventsService.createOrderStatusEvent(saveOrder, eventType, payload);
        //outboxEventsService.createOrderStatusChangedEvent(saveOrder, payload);
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                saveOrder.getOrderNumber(),
                204,
                null
                );

        log.info("Order: {} status changed successfully", saveOrder.getOrderNumber());
        return toResponse(saveOrder);
    }
    

    @Transactional
    public void confirmPaidOrderInventory(String idempotencyKey, String orderNumber) {

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.CONFIRM_ORDER_PAYMENT,
                "ORDER",
                orderNumber,
                null
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);
       
        OrderStatus currentStatus = order.getOrderStatus();
        
        if (currentStatus == OrderStatus.CONFIRMED) {
            idempotencyKeysService.markCompleted(
                    idempotencyKeys,
                    orderNumber,
                    204,
                    null
            );
            return;
        }

        if (!canTransition(currentStatus, OrderStatus.CONFIRMED)) {
            throw new InvalidOrderStatusTransitionException("Order cannot be confirmed from status: " + currentStatus);
        }

          order.getOrderItems().forEach(item -> {
            inventoryClient.confirmReservation(
                    "confirm-" + order.getOrderNumber() + "-" + item.getSkuId(),
                    new InventoryConfirmRequest(
                            order.getOrderNumber(),
                            item.getSkuId(),
                            Long.valueOf(item.getQuantity())
                    )
            );
        });
        order.setOrderStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        // Create OrderStatusChangedEvent to outbox_events
        String payload = buildOrderStatusChangedPayload(savedOrder, currentStatus, OrderStatus.CONFIRMED, OrderEventType.ORDER_CONFIRMED);
        outboxEventsService.createOrderConfirmedEvent(savedOrder, payload);

        // Update idempotency_keys status to COMPLETED
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedOrder.getOrderNumber(),
                204,
                "Confirmed successfully"
        );

        log.info("Order {} confirmed successfully", savedOrder.getId());
    }

    
    @Transactional
    public void expireOrder(String idempotencyKey, String orderNumber)
    {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));
        IdempotencyRequestContext idempotencyRequestContext = new IdempotencyRequestContext(
                IdempotencyOperation.EXPIRY_ORDER,
                "ORDER",
                orderNumber,
                null
        );
        IdempotencyKeys idempotencyKeys = idempotencyKeysService.startProcess(idempotencyKey, idempotencyRequestContext);
       
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == OrderStatus.EXPIRED) {
            idempotencyKeysService.markCompleted(
                    idempotencyKeys,
                    orderNumber,
                    204,
                    null
            );
            return;
        }

        if (!canCancel(order.getOrderStatus())) {
            throw new InvalidOrderStatusTransitionException("Order cannot be expired in its current status:"+ currentStatus);
        }

        order.getOrderItems().forEach(item -> {
            inventoryClient.returnInventory(
                    "expire-" + order.getOrderNumber() + "-" + item.getSkuId(),
                    new InventoryReservationExpiredRequest(
                            order.getOrderNumber(),
                            item.getSkuId(),
                            Long.valueOf(item.getQuantity())
                    )
            );
        });
        order.setOrderStatus(OrderStatus.EXPIRED);
        Order savedOrder = orderRepository.save(order);
        
        // Create OrderStatusChangedEvent to outbox_events
        String payload = buildOrderStatusChangedPayload(savedOrder, currentStatus, OrderStatus.EXPIRED, OrderEventType.ORDER_EXPIRED);
        outboxEventsService.createOrderExpiredEvent(savedOrder, payload);

        // Update idempotency_keys status to COMPLETED
        idempotencyKeysService.markCompleted(
                idempotencyKeys,
                savedOrder.getOrderNumber(),
                204,
                "Order expired successfully"
        );

        log.info("Order {} expired successfully", savedOrder.getId());

    }




    public void updateOrderBasicInfo(String orderNumber, OrderRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
           // throw new RuntimeException("Order not found");
            throw new OrderNotFoundException(orderNumber);
        }
    }



    // Get Orders By User ID
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    // Get Orders By Status
    public List<OrderResponse> getOrdersByStatus(String status) {
        return orderRepository.findByOrderStatus(status).stream().map(this::toResponse).toList();
    }


    // Get Orders By Currency
    public List<OrderResponse> getOrdersByCurrency(String currency) {
        return orderRepository.findByCurrency(currency).stream().map(this::toResponse).toList();
    }

    // Get Orders By CreatedAt with pagination and sorting
    public Page<OrderSummaryResponse> getOrderByCreatedAt(LocalDateTime from, LocalDateTime to, Pageable pageable) {
         return orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to, pageable).map(this::toSummaryResponse);
    }



    // Convert OrderItemRequest to OrderItems
    public OrderItems toOrderItem(OrderItemRequest orderItemRequest, Order order)
    {
        // Calculate subtotal for one order item
        BigDecimal subtotal = orderItemRequest.unitPrice().multiply(BigDecimal.valueOf(orderItemRequest.quantity()));

        return OrderItems.builder()
                .order(order)
                .skuId(orderItemRequest.skuId())
                .skuName(orderItemRequest.skuName())
                .unitPrice(orderItemRequest.unitPrice())
                .quantity(orderItemRequest.quantity())
                .subtotal(subtotal)
                .build();
    }

    // Convert Order to OrderResponse, including OrderItems
    private OrderResponse toResponse(Order order)
    {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getSkuId(),
                        item.getSkuName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                )).toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getVersion(),
                order.getNote(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getRecipientName(),
                order.getRecipientEmail(),
                order.getRecipientPhone(),
                items
        );
    }

    // Convert Order to OrderSummaryResponse, without OrderItems
    private OrderSummaryResponse toSummaryResponse(Order order)
    {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getVersion(),
                order.getNote(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getRecipientName(),
                order.getRecipientEmail(),
                order.getRecipientPhone()
        );
    }


    // Put object to JSON
    private String toJson(Object value) {
        try{
          return objectMapper.writeValueAsString(value);
        }catch(Exception e)
        {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    // Determine the current state and the target state, and identify state changes permitted by the operation
    private boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus)
    {
         return switch (currentStatus)
         {
             case CREATED -> targetStatus == OrderStatus.CANCELLED || targetStatus == OrderStatus.INVENTORY_PENDING;
             case INVENTORY_PENDING -> targetStatus == OrderStatus.INVENTORY_RESERVED || targetStatus == OrderStatus.INVENTORY_FAILED || targetStatus == OrderStatus.CANCELLED;
             case INVENTORY_RESERVED -> targetStatus == OrderStatus.PAYMENT_PENDING || targetStatus == OrderStatus.CANCELLED;
             case CANCELLATION_PENDING -> targetStatus == OrderStatus.CANCELLED || targetStatus == OrderStatus.CANCELLATION_FAILED;
             case CANCELLATION_FAILED -> targetStatus == OrderStatus.CANCELLED;
             case CANCELLATION_COMPLETED -> targetStatus == OrderStatus.CANCELLED;
             case PAYMENT_PENDING -> targetStatus == OrderStatus.PAID || targetStatus == OrderStatus.CANCELLED;
             case PAID -> targetStatus == OrderStatus.CONFIRMED;
             case CONFIRMED -> targetStatus == OrderStatus.COMPLETED;
             case INVENTORY_FAILED -> targetStatus == OrderStatus.CANCELLED;
             case PAYMENT_FAILED -> targetStatus == OrderStatus.CANCELLED;
             case COMPLETED, CANCELLED, EXPIRED, REFUNDED -> false;
         };

    }

    // Determine whether the current status allows cancellation
    private boolean canCancel(OrderStatus currentStatus)
    {
        return switch (currentStatus)
        {
            case CANCELLATION_PENDING,
            CANCELLATION_FAILED,
            CANCELLATION_COMPLETED -> true;

            case INVENTORY_RESERVED, 
            PAYMENT_PENDING, 
            PAYMENT_FAILED,
            INVENTORY_FAILED -> true;

            case
            CREATED,
            INVENTORY_PENDING, 
            PAID, 
            CONFIRMED,
            COMPLETED, 
            CANCELLED,
            EXPIRED, 
            REFUNDED -> false;
        };
    }

    // Create OrderStatusChangedEvent by Order and will used for payload json
    // 创建订单状态变更事件的负载
    private String buildOrderStatusChangedPayload(Order order, 
        OrderStatus oldStatus, 
        OrderStatus newStatus, 
        OrderEventType eventType)
    {
        try {
            OrderStatusChangedEvent oce = new OrderStatusChangedEvent(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getUserId(),
                    oldStatus,
                    newStatus,
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        return buildEventEnvelope(eventType.name(), order.getOrderNumber(), oce);
        }catch(Exception e)
        {
            throw new RuntimeException("Failed to build OrderStatusChangedEvent", e);
        }
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
  

    // Normal method to build event envelope for any event type, aggregateId and data
    // 构建事件信封的通用方法，用于任何事件类型、聚合ID和数据
    private <T> String buildEventEnvelope(
        String eventType, 
        String aggregateId,
        T data )
    {
        EventEnvelope<T> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                1,
                "ORDER",
                aggregateId,
                LocalDateTime.now(),
                data
        );
        return toJson(envelope);

    }

    // Build InventoryReservationRequestedEvent by Order and will used for payload json
    // 构建库存预留请求事件的负载
    private String buildInventoryReservationRequestedPayload(Order order)
    {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException(
            "Order items cannot be empty when requesting inventory reservation"
         );
        }
        List<InventoryReservationRequestItem> items = 
        order.getOrderItems().stream()
            .map(item -> new InventoryReservationRequestItem(
                item.getSkuId(),
                (long)item.getQuantity()
            ))
            .toList();
            
        InventoryReservationRequestCommand event = 
        new InventoryReservationRequestCommand(
            order.getOrderNumber(),
            items
        );

        return buildEventEnvelope(InventoryCommandType.INVENTORY_RESERVATION_REQUESTED.name(), order.getOrderNumber(), event);
    }

    // Build InventoryReleaseRequestedEvent by Order and will used for payload json
    // 构建库存释放请求事件的负载
    private String buildInventoryReleaseRequestedPayload(Order order,
        InventoryReleaseReason reason
    )
    {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException(
            "Order items cannot be empty when requesting inventory release"
         );
        }
        List<InventoryReleaseRequestItem> items = 
        order.getOrderItems().stream()
            .map(item -> new InventoryReleaseRequestItem(
                item.getSkuId(),
                (long)item.getQuantity()
            ))
            .toList();
            
        InventoryReleaseRequestCommand command = 
        new InventoryReleaseRequestCommand(
            order.getOrderNumber(),
            reason,
            items
        );

        return buildEventEnvelope(InventoryCommandType.INVENTORY_RELEASE_REQUESTED.name(), 
        order.getOrderNumber(), 
        command);
    }

}
