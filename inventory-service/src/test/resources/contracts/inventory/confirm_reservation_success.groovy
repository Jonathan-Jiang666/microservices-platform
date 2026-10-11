import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "confirm inventory successfully"

    request {
        method POST()
        url "/api/inventory/confirm"
        headers {
            contentType(applicationJson())
            header("idempotencyKey", "confirm-order-contract-001-sku-contract-001")
        }
        body(
                orderNo: "order-contract-001",
                skuId: "sku-contract-001",
                confirmQuantity: 10
        )
    }

    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body(
                skuId: "sku-contract-001",
                onHandQuantity: 90,
                reservedQuantity: 0,
                availableQuantity: 90,
                createdAt: "2026-09-16T10:00:00",
                updatedAt: "2026-09-16T10:00:00"
        )
    }
}