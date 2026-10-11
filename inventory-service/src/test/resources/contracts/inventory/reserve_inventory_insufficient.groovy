import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Inventory insufficient"

    request {
        method POST()
        url "/api/inventory/reserve"
        headers {
            contentType(applicationJson())
            header("idempotencyKey", 
            "reserve-order-insufficient-001-sku-contract-001")
        }
        body(
            orderNo: "order-insufficient-001",
            skuId: "sku-contract-001",
            reservedQuantity: 100
        )
    }

    response {
        status CONFLICT()
        headers {
            contentType(applicationJson())
        }
        body(
            "status": 409,
            "code": "INSUFFICIENT_INVENTORY",
            "message": "Inventory insufficient",
            "timestamp": $(consumer("2026-09-16T23:00:00"), producer(regex(".+")))
        )
    }
}