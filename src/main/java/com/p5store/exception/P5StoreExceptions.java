package com.p5store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ── Resource Not Found ──────────────────────────────────────────
@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

// ── Duplicate Resource ──────────────────────────────────────────
@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}

// ── Bad Request ─────────────────────────────────────────────────
@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

// ── Insufficient Stock ──────────────────────────────────────────
@ResponseStatus(HttpStatus.CONFLICT)
class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String variantId, int available, int requested) {
        super("Variant %s has only %d units but %d were requested".formatted(variantId, available, requested));
    }
}

// ── Discount invalid ────────────────────────────────────────────
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidDiscountException extends RuntimeException {
    public InvalidDiscountException(String code) {
        super("Discount code '%s' is invalid, expired, or does not meet minimum order requirements.".formatted(code));
    }
}
