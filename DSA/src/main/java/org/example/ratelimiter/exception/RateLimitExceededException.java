package org.example.ratelimiter.exception;

/**
 * Exception thrown when the rate limit is exceeded.
 * 
 * <p>This exception can be caught at the application layer to return appropriate 
 * HTTP status codes (like 429 Too Many Requests) to the client.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
