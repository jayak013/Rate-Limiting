package org.example.ratelimiter;

/**
 * Interface for Rate Limiter implementations.
 * A rate limiter controls the rate of traffic sent or received by a network interface or service.
 * It is used to prevent resource exhaustion, protect against DDoS attacks, and manage service quotas.
 */
public interface RateLimiter {
    /**
     * Attempts to acquire a single permit.
     * 
     * @return {@code true} if a permit was acquired (i.e., the request is allowed), 
     *         {@code false} otherwise.
     */
    boolean allow();

    /**
     * Attempts to acquire multiple permits.
     * Useful for operations that consume different amounts of resources.
     * 
     * @param permits number of permits to acquire. Must be positive.
     * @return {@code true} if the permits were acquired, {@code false} otherwise.
     */
    boolean allow(int permits);
}
