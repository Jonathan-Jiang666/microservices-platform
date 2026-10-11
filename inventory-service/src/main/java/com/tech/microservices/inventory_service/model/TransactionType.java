package com.tech.microservices.inventory_service.model;

//方法	                    ReservationStatus	                    TransactionType	                        InventoryEventType	             数量变化
//createInventory()	          无	                                INITIAL_STOCK	                        INVENTORY_CREATED	              onHand +，reserved = 0
//stockIn()	                  无	                                STOCK_IN	                            INVENTORY_STOCKED_IN	          onHand +，available +
//reserveInventory()	    RESERVED	                            RESERVE	                                INVENTORY_RESERVED	              reserved +，available -
//releaseInventory()	    RESERVED -> RELEASED	                RELEASE	                                INVENTORY_RESERVATION_RELEASED	  reserved -，available +
//confirmReservation()	    RESERVED -> CONFIRMED	                SALE	                                INVENTORY_RESERVATION_CONFIRMED	  onHand -，reserved -，available 不变
//expireReservation()	    RESERVED -> EXPIRED	                    EXPIRE 或 RELEASE	                    INVENTORY_RESERVATION_EXPIRED	  reserved -，available +
public enum TransactionType
{
    INITIAL_STOCK,
    STOCK_IN,
    RESERVE,
    RELEASE,
    SALE,
    RETURN,
    ADJUSTMENT,
    EXPIRE
}



