package com.tech.microservices.inventory_service.service;


import com.tech.microservices.inventory_service.exception.DuplicateIdempotencyKeyException;
import com.tech.microservices.inventory_service.model.IdempotencyKeyStatus;
import com.tech.microservices.inventory_service.model.IdempotencyKeys;
import com.tech.microservices.inventory_service.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyKeyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    // Find by IdempotencyKey
    public Optional<IdempotencyKeys> findByIdempotencyKey(String idempotencyKey) {
        return idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
    }

    // Create new IdempotencyKey
    public IdempotencyKeys saveIdempotencyKey(IdempotencyKeys idempotencyKeys) {
        return idempotencyKeyRepository.save(idempotencyKeys);

    }

    // Create new IdempotencyKey with status PROCESSING
    public IdempotencyKeys createProcessing(String idempotencyKey, String requestHash) {
        IdempotencyKeys idempotencyKeys = IdempotencyKeys.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .resourceType("INVENTORY-SERVICE")
                .status(IdempotencyKeyStatus.PROCESSING.toString())
                .build();
        return idempotencyKeyRepository.save(idempotencyKeys);
    }

    // Mark IdempotencyKey as COMPLETED
    public void markCompleted(IdempotencyKeys key, String resourceId, Integer responseStatus, String responseBody)
    {
        key.setResourceId(resourceId);
        key.setResponseStatus(responseStatus);
        key.setResponseBody(responseBody);
        key.setStatus(IdempotencyKeyStatus.COMPLETED.toString());
        idempotencyKeyRepository.save(key);
    }

    // Get hash from request body
    public String hashRequest(Object requestPayload) {
        try {
            String requestBody = objectMapper.writeValueAsString(requestPayload);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash request", e);
        }
    }

    // Start processing an idempotency key
    public IdempotencyKeys startProcess(String idempotencyKey, Object requestPayload)
    {
        String hash = hashRequest(requestPayload);
        Optional<IdempotencyKeys> existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent())
        {
            throw new DuplicateIdempotencyKeyException("Idempotency key already exists before creating order");
        }

        IdempotencyKeys idempotencyKeys = this.createProcessing(idempotencyKey, hash);
        return idempotencyKeys;
    }
}
