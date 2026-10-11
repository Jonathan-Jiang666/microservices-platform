package com.tech.microservices.inventory_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleInventoryNotFoundException(InventoryNotFoundException ex) {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                404,
//                "INVENTORY_NOT_FOUND",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(404).body(errorResponse);
        return build(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateInventoryException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateInventoryException(DuplicateInventoryException ex)
    {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                409,
//                "DUPLICATE_INVENTORY",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(409).body(errorResponse);
        return build(HttpStatus.CONFLICT, "DUPLICATE_INVENTORY", ex.getMessage());
    }


    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientInventoryException(InsufficientInventoryException ex) {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                409,
//                "INSUFFICIENT_INVENTORY",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(409).body(errorResponse);
        return build(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY", ex.getMessage());
    }


    @ExceptionHandler(DuplicateReservationException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateReservationException(DuplicateReservationException ex) {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                409,
//                "DUPLICATE_RESERVATION",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(409).body(errorResponse);
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESERVATION", ex.getMessage());
    }

    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateIdempotencyKeyException(DuplicateIdempotencyKeyException ex)
    {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                409,
//                "DUPLICATE_IDEMPOTENCY_KEY",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(409).body(errorResponse);
        return build(HttpStatus.CONFLICT, "DUPLICATE_IDEMPOTENCY_KEY", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyConflictException(IdempotencyConflictException ex)
    {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                409,
//                "IDEMPOTENCY_CONFLICT",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(409).body(errorResponse);
        return build(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", ex.getMessage());
    }


    @ExceptionHandler(InvalidInventoryQuantityException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInventoryQuantityException(InvalidInventoryQuantityException ex)
    {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                400,
//                "INVALID_INVENTORY_QUANTITY",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(400).body(errorResponse);
        return build(HttpStatus.BAD_REQUEST, "INVALID_INVENTORY_QUANTITY", ex.getMessage());
    }



    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationNotFoundException(ReservationNotFoundException ex)
    {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                404,
//                "RESERVATION_NOT_FOUND",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(404).body(errorResponse);
        return build(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", ex.getMessage());
    }


    @ExceptionHandler(InvalidReservationStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidReservationStatusException(InvalidReservationStatusException ex)
    {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                409,
//                "INVALID_RESERVATION_STATUS",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(409).body(errorResponse);
        return build(HttpStatus.CONFLICT, "INVALID_RESERVATION_STATUS", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(
//                500,
//                "INTERNAL_SERVER_ERROR",
//                ex.getMessage(),
//                LocalDateTime.now()
//        );
//        return ResponseEntity.status(500).body(errorResponse);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(status.value(), code, message, LocalDateTime.now()));
    }
}
