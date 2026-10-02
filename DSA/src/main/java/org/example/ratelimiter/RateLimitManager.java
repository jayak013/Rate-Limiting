package org.example.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * RateLimitManager handles rate limiting for multiple clients/users.
 * It provides a way to apply rate limits per-client (e.g., by IP address or API Key).
 * 
 * <p>Concurrency:
 * It uses a {@link java.util.concurrent.ConcurrentHashMap} to store client-specific 
 * {@link RateLimiter} instances, making it thread-safe for multi-threaded applications.
 * 
 * <p>Production Considerations:
 * In a distributed environment, this local map should be replaced with a distributed 
 * store like Redis to ensure that rate limits are enforced across all server instances.
 */
public class RateLimitManager {
    private final Map<String, RateLimiter> clientLimiters = new ConcurrentHashMap<>();
    private final RateLimiterFactory.Algorithm algorithm;
    private final long limit;
    private final long rateOrWindow;

    /**
     * Creates a manager that will use the same configuration for every client.
     */
    public RateLimitManager(RateLimiterFactory.Algorithm algorithm, long limit, long rateOrWindow) {
        this.algorithm = algorithm;
        this.limit = limit;
        this.rateOrWindow = rateOrWindow;
    }

    /**
     * Checks if a request from a specific client is allowed.
     * @param clientId The unique identifier for the client (e.g., IP, API Key, User ID)
     * @return true if allowed, false if rate limited
     */
    public boolean allow(String clientId) {
        // Retrieve or create a rate limiter for this specific client
        RateLimiter limiter = clientLimiters.computeIfAbsent(clientId, 
            key -> RateLimiterFactory.createRateLimiter(algorithm, limit, rateOrWindow));
        
        // Check if the client's request is permitted
        return limiter.allow();
    }

    /**
     * Manually get or create a limiter for a client for more complex scenarios.
     */
    public RateLimiter getLimiter(String clientId) {
        return clientLimiters.computeIfAbsent(clientId, 
            key -> RateLimiterFactory.createRateLimiter(algorithm, limit, rateOrWindow));
    }
}
