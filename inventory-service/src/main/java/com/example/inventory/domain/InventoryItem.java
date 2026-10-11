package com.example.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class InventoryItem {

    @Id
    private String sku;

    private int quantity;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduce(int amount) {
        this.quantity = Math.max(0, this.quantity - amount);
    }
}
