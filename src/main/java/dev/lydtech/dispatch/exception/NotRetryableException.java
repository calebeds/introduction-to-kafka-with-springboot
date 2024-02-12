package dev.lydtech.dispatch.exception;

public class NotRetryableException extends RuntimeException {
    public NotRetryableException(Exception e) {
        super(e);
    }
}
