# microservices-platform

An e-commerce microservices project built with Spring Boot and Spring Cloud Gateway, featuring product, order, and inventory services with Kafka messaging, transactional outbox, and idempotent processing.

## Services

Each service keeps its own Maven project and can be built and run independently.

| Directory | Service | Local HTTP port |
| --- | --- | --- |
| `product-service-1` | Product and SKU management | `8080` |
| `order-service-1` | Orders and inventory integration | `8081` |
| `inventory-service` | Inventory and reservations | `8082` |
| `api-gateway-1` | HTTP gateway | `9000` |

The product service uses MongoDB. Order and inventory services use MySQL and Kafka. Service configuration, SQL migrations, and Docker Compose files are kept in their respective directories.

## Development

Open `microservices-platform.code-workspace` to load all four services in VS Code.

Use Java 21 and the Maven wrapper in the service you are working on. For example:

```bash
cd api-gateway-1
./mvnw spring-boot:run
```

The existing application configuration contains local development credentials. Review those settings before using this repository in another environment.

## Project Status

This project is under active development. Current work focuses on order and inventory message consumers, failure handling, and integration testing. Gateway routing is at an early stage, and Keycloak authentication is planned.

The initial source import preserves the service code, tests, migrations, Maven wrappers, and Docker configuration. Generated build output, IDE state, local database data, and local tooling logs are excluded.
