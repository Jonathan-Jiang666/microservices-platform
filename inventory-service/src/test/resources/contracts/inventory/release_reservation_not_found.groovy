import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Reservation not found : skuId = sku-contract-001, orderNo= order-not-found-001"

    request {
        method POST()
        url "/api/inventory/release"
        headers {
            contentType(applicationJson())
            header("idempotencyKey",
                    "release-order-not-found-001-sku-contract-001"
            )
        }
        body(
            orderNo: "order-not-found-001",
            skuId: "sku-contract-001",
            releaseQuantity: 100
        )
    }

    response {
        status NOT_FOUND()
        headers {
            contentType(applicationJson())
        }
        body(
            "status": 404,
            "code": "RESERVATION_NOT_FOUND",
            "message": "Reservation not found :skuId = sku-contract-001 , orderNo = order-not-found-001",
            "timestamp": $(consumer("2026-09-16T23:00:00"), producer(regex(".+")))
        )
    }
}