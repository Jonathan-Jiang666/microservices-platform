package com.tech.microservices.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle MethodArgumentNotValidException 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return build(HttpStatus.BAD_REQUEST, "METHOD_ARGUMENT_NOT_VALID", ex.getMessage());
    }

    // Handle OrderNotFoundException 404
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFoundException(OrderNotFoundException ex) {
       return build(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage());
    }

    // Handle ReservationNotFoundException 404
    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationNotFoundException(ReservationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", ex.getMessage());
    }

    // Handle DuplicateOrderException 409
    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateOrderException(DuplicateOrderException ex)
    {
        return build(HttpStatus.CONFLICT, "DUPLICATE_ORDER", ex.getMessage());
    }

    // Handle DuplicateIdempotentRequestException 409
    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateIdempotencyKeyException(DuplicateIdempotencyKeyException ex) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_IDEMPOTENT_KEY", ex.getMessage());
    }

    // Handle InvalidOrderStatusTransitionException 409
    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStatusTransitionException(InvalidOrderStatusTransitionException e) {
        return build(HttpStatus.CONFLICT, "INVALID_ORDER_STATUS_TRANSITION", e.getMessage());
    }

    // Handle InsufficientInventoryException 409
    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientInventoryException(InsufficientInventoryException ex) {
        return build(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY", ex.getMessage());
    }

    // Handle all other exceptions 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(
            new ApiErrorResponse(status.value(), code, message, LocalDateTime.now())
    );
}
}