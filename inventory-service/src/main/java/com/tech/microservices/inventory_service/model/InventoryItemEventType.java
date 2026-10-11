package com.tech.microservices.inventory_service.model;

//表示单个 SKU 的库存事实，只属于 Inventory，不涉及订单级别； 
// 例如：库存创建、入库、预定、释放、确认等
// SKU: Stock Keeping Unit, it belongs to the inventory system
// Example: inventory creation, stock-in, reservation, release, confirmation, etc.
public enum InventoryItemEventType {
    // 库存创建 inventory created
    INVENTORY_CREATED,
    // 入库 inventory stocked in
    INVENTORY_STOCKED_IN,

    // 库存预定 inventory reserved
    INVENTORY_RESERVED,
    // 库存预定释放 inventory reservation released
    INVENTORY_RESERVATION_RELEASED,
    
    // 库存预定确认 inventory reservation confirmed
    INVENTORY_RESERVATION_CONFIRMED,
    // 库存预定过期 inventory reservation expired
    INVENTORY_RESERVATION_EXPIRED

}
