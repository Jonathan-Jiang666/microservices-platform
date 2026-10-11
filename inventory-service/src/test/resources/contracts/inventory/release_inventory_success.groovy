import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "release inventory successfully"

    request {
        method POST()
        url "/api/inventory/release"
        headers {
            contentType(applicationJson())
            header("idempotencyKey", "release-order-contract-001-sku-contract-001")
        }
        body(
                orderNo: "order-contract-001",
                skuId: "sku-contract-001",
                releaseQuantity: 10
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
                reservedQuantity: 0,
                availableQuantity: 100,
                createdAt: "2026-09-16T10:00:00",
                updatedAt: "2026-09-16T10:00:00"
        )
    }
}