package com.tech.microservices.order_service.repository;

import com.tech.microservices.order_service.model.IdempotencyKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IdempotencyKeysRepository extends JpaRepository<IdempotencyKeys, Long>, JpaSpecificationExecutor<IdempotencyKeys> {

    Optional<IdempotencyKeys> findByIdempotencyKey(String idempotencyKey);
}
