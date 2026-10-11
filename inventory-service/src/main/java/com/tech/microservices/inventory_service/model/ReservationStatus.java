package com.tech.microservices.inventory_service.model;

public enum ReservationStatus {
    RESERVED,  //已经锁定库存
    CONFIRMED, //已经确认消耗库存
    RELEASED, //释放
    EXPIRED,  //已过期
    FAILED   //预定失败
}
