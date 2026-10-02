package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Leaky Bucket implementation of Rate Limiter.
 * 
 * <p>Logic:
 * Imagine a bucket with a small hole at the bottom. Water (requests) is poured 
 * into the bucket. If the bucket overflows, new water is discarded. The water 
 * leaks out of the hole at a constant rate.
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li><b>Smoothing:</b> Unlike Token Bucket, which allows bursts, Leaky Bucket 
 *       enforces a constant output rate regardless of the input burstiness.</li>
 *   <li><b>Queueing:</b> It effectively acts as a buffer (queue) that processes 
 *       requests at a fixed speed.</li>
 * </ul>
 * 
 * <p>Trade-offs:
 * <ul>
 *   <li><b>Pros:</b> Ideal for traffic shaping and ensuring a stable load on 
 *       downstream services.</li>
 *   <li><b>Cons:</b> Can be less responsive to legitimate bursts than Token Bucket.</li>
 * </ul>
 * 
 * <p>Concurrency:
 * This implementation uses {@code synchronized} to ensure that leak and add 
 * operations are atomic.
 */
public class LeakyBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private final long leakRatePerSecond;
    private final AtomicLong currentWater;
    private final AtomicLong lastLeakTimestamp;

    public LeakyBucketRateLimiter(long capacity, long leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
        this.currentWater = new AtomicLong(0);
        this.lastLeakTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public synchronized boolean allow(int permits) {
        leak(); // Remove "water" (permits) from the bucket based on elapsed time
        
        // If there's enough space in the bucket, add the new permits
        if (currentWater.get() + permits <= capacity) {
            currentWater.addAndGet(permits);
            return true;
        }
        return false; // Bucket is full
    }

    private void leak() {
        long now = System.currentTimeMillis();
        long lastLeak = lastLeakTimestamp.get();
        long timePassed = now - lastLeak;

        if (timePassed > 0) {
            // Calculate how much water has leaked out since the last request
            long leakedAmount = (timePassed * leakRatePerSecond) / 1000;
            if (leakedAmount > 0) {
                // Update water level, ensuring it doesn't go below zero
                long newWater = Math.max(0, currentWater.get() - leakedAmount);
                currentWater.set(newWater);
                lastLeakTimestamp.set(now);
            }
        }
    }
}
