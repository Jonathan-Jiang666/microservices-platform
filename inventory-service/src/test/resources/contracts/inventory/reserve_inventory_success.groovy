import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "reserve inventory successfully"

    request {
        method POST()
        url "/api/inventory/reserve"
        headers {
            contentType(applicationJson())
            header("idempotencyKey", "reserve-order-contract-001-sku-contract-001")
        }
        body(
                orderNo: "order-contract-001",
                skuId: "sku-contract-001",
                reservedQuantity: 10
        )
    }

    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body(
                skuId: "sku-contract-001",
                onHandQuantity: 100,
                reservedQuantity: 10,
                availableQuantity: 90,
                createdAt: "2026-09-15T10:00:00",
                updatedAt: "2026-09-15T10:00:00"
        )
    }
}
