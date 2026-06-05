package com.p5store.exception;
public class InvalidDiscountException extends RuntimeException {
    public InvalidDiscountException(String code) {
        super("Discount code '%s' is invalid, expired, or does not meet minimum order requirements.".formatted(code));
    }
}
