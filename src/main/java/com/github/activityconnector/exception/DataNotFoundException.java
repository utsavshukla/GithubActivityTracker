package com.github.activityconnector.exception;

/**
 * Custom exception for data not found scenarios
 */
public class DataNotFoundException extends RuntimeException {
    
    public DataNotFoundException(String message) {
        super(message);
    }
    
    public DataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
