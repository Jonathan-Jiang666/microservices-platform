package com.tech.microservices.inventory_service.repository;

import com.tech.microservices.inventory_service.model.IdempotencyKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeys, Long>, JpaSpecificationExecutor<IdempotencyKeys> {
    Optional<IdempotencyKeys> findByIdempotencyKey(String idempotencyKey);
}
