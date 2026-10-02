package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding Window Counter implementation of Rate Limiter.
 * 
 * <p>Logic:
 * This is a hybrid approach that combines Fixed Window and Sliding Window Log.
 * It smooths out the boundary spikes of Fixed Window without the memory overhead
 * of Sliding Window Log.
 * 
 * <p>It uses the weighted average of the current and previous window counts.
 * Formula: 
 * {@code count = current_window_count + (previous_window_count * overlap_weight)}
 * where {@code overlap_weight = (window_size - time_passed_in_current_window) / window_size}.
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li><b>Memory:</b> Very efficient (fixed number of counters).</li>
 *   <li><b>Accuracy:</b> Good approximation of a sliding window, much better than 
 *       Fixed Window, though not as perfectly precise as Sliding Window Log.</li>
 * </ul>
 * 
 * <p>Concurrency:
 * This implementation uses {@code synchronized} for window transitions and 
 * counter updates.
 */
public class SlidingWindowCounterRateLimiter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMillis;
    private final AtomicLong currentWindowCount;
    private final AtomicLong previousWindowCount;
    private final AtomicLong lastWindowStartTimestamp;

    public SlidingWindowCounterRateLimiter(long maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.currentWindowCount = new AtomicLong(0);
        this.previousWindowCount = new AtomicLong(0);
        this.lastWindowStartTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public synchronized boolean allow(int permits) {
        long now = System.currentTimeMillis();
        long windowStart = lastWindowStartTimestamp.get();
        long timePassed = now - windowStart;

        // Check if we have crossed into a new time window
        if (timePassed >= windowSizeMillis) {
            long windowsPassed = timePassed / windowSizeMillis;
            
            // If exactly one window has passed, the old current becomes the new previous
            if (windowsPassed == 1) {
                previousWindowCount.set(currentWindowCount.get());
            } else {
                // If more than one window has passed, both windows are effectively empty
                previousWindowCount.set(0);
            }
            
            currentWindowCount.set(0);
            // Align the new window start to a multiple of windowSizeMillis
            lastWindowStartTimestamp.set(now - (timePassed % windowSizeMillis));
            windowStart = lastWindowStartTimestamp.get();
            timePassed = now - windowStart;
        }

        // Weighted average formula: current_count + (previous_count * proportion_of_previous_window_overlapping)
        double overlapWeight = (double) (windowSizeMillis - timePassed) / windowSizeMillis;
        long estimatedCount = (long) (currentWindowCount.get() + (previousWindowCount.get() * overlapWeight));

        // Allow if the estimated count plus new permits is within limits
        if (estimatedCount + permits <= maxRequests) {
            currentWindowCount.addAndGet(permits);
            return true;
        }
        return false;
    }
}
