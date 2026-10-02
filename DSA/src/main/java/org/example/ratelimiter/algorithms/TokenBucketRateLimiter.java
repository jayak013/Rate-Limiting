package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket implementation of Rate Limiter.
 * 
 * <p>Logic:
 * Imagine a bucket that holds tokens. Tokens are added to the bucket at a fixed rate.
 * Each request must consume a certain number of tokens. If the bucket has enough 
 * tokens, the request is allowed and tokens are removed. Otherwise, the request is rejected.
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li><b>Burstiness:</b> It allows for bursts of traffic up to the bucket's capacity.</li>
 *   <li><b>Refill:</b> The bucket is refilled lazily on each request based on the 
 *       elapsed time since the last refill.</li>
 * </ul>
 * 
 * <p>Trade-offs:
 * <ul>
 *   <li><b>Pros:</b> Very flexible, handles bursts well, widely used in industry (e.g., AWS, Stripe).</li>
 *   <li><b>Cons:</b> Can be slightly more complex to implement correctly than Fixed Window.</li>
 * </ul>
 * 
 * <p>Concurrency:
 * This implementation uses {@code synchronized} to ensure that refill and 
 * consumption are atomic.
 */
public class TokenBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private final long refillRatePerSecond;
    private final AtomicLong availableTokens;
    private final AtomicLong lastRefillTimestamp;

    public TokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.availableTokens = new AtomicLong(capacity);
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public synchronized boolean allow(int permits) {
        refill(); // Lazily add tokens based on elapsed time
        
        // If enough tokens are available, consume them and allow the request
        if (availableTokens.get() >= permits) {
            availableTokens.addAndGet(-permits);
            return true;
        }
        return false; // Not enough tokens
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTimestamp.get();
        long timePassed = now - lastRefill;
        
        if (timePassed > 0) {
            // Calculate new tokens based on time elapsed and the refill rate
            long tokensToAdd = (timePassed * refillRatePerSecond) / 1000;
            if (tokensToAdd > 0) {
                // Ensure we don't exceed the bucket's maximum capacity
                long newTokens = Math.min(capacity, availableTokens.get() + tokensToAdd);
                availableTokens.set(newTokens);
                lastRefillTimestamp.set(now);
            }
        }
    }
}
