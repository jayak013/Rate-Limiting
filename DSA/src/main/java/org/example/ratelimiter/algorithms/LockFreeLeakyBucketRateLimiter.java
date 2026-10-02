package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-Free Leaky Bucket implementation.
 * 
 * <p>Logic:
 * Non-blocking version of {@link LeakyBucketRateLimiter}.
 * 
 * <p>Concurrency Design:
 * Uses {@link java.util.concurrent.atomic.AtomicReference} with an immutable 
 * {@code LeakyState} record to ensure that water leak and addition are atomic. 
 * This avoids the need for locks while preventing race conditions where multiple 
 * threads might leak water concurrently or add water based on stale leak 
 * information.
 */
public class LockFreeLeakyBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private final long leakRatePerSecond;
    private final AtomicReference<LeakyState> state;

    private record LeakyState(long water, long lastLeak) {}

    public LockFreeLeakyBucketRateLimiter(long capacity, long leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
        this.state = new AtomicReference<>(new LeakyState(0, System.currentTimeMillis()));
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public boolean allow(int permits) {
        if (permits <= 0) return true;

        // Non-blocking loop
        while (true) {
            LeakyState current = state.get();
            long now = System.currentTimeMillis();
            long timePassed = now - current.lastLeak;

            // Calculate current water level after leaking based on time passed
            long leakedAmount = (timePassed * leakRatePerSecond) / 1000;
            long refreshedWater = Math.max(0, current.water - leakedAmount);

            long nextWater;
            long nextLeak;

            // Update timestamp only if a full unit of water has leaked to avoid losing partial progress
            if (leakedAmount > 0) {
                nextWater = refreshedWater;
                nextLeak = now;
            } else {
                nextWater = current.water;
                nextLeak = current.lastLeak;
            }

            // Check if adding permits would overflow the bucket
            if (nextWater + permits > capacity) {
                return false;
            }

            // Create new state with added permits
            LeakyState nextState = new LeakyState(nextWater + permits, nextLeak);
            
            // Try atomic update; if another thread beat us to it, retry
            if (state.compareAndSet(current, nextState)) {
                return true;
            }
        }
    }
}
