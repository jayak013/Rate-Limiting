package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed Window Counter implementation of Rate Limiter.
 * 
 * <p>Logic:
 * This algorithm divides time into fixed windows (e.g., 1-second intervals).
 * It maintains a counter for each window. When a request arrives, the counter
 * for the current window is incremented. If the counter exceeds the maximum
 * limit, the request is rejected.
 * 
 * <p>Trade-offs:
 * <ul>
 *   <li><b>Pros:</b> Extremely memory efficient (only needs a counter and a timestamp).</li>
 *   <li><b>Cons:</b> "Spiky" at window boundaries. A burst of requests can occur at the 
 *       end of one window and the beginning of the next, effectively allowing 
 *       2x the limit in a short period.</li>
 * </ul>
 * 
 * <p>Concurrency:
 * This implementation uses {@code synchronized} to ensure thread safety during
 * counter updates and window transitions.
 * 
 * @see LockFreeFixedWindowRateLimiter for a non-blocking version.
 */
public class FixedWindowRateLimiter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMillis;
    private final AtomicLong counter;
    private final AtomicLong windowStartTimestamp;

    public FixedWindowRateLimiter(long maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.counter = new AtomicLong(0);
        this.windowStartTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public synchronized boolean allow(int permits) {
        long now = System.currentTimeMillis();
        long windowStart = windowStartTimestamp.get();

        // If the current time has moved past the end of the window, reset the counter
        if (now - windowStart > windowSizeMillis) {
            counter.set(0);
            windowStartTimestamp.set(now);
        }

        // Check if there are enough permits remaining in the current window
        if (counter.get() + permits <= maxRequests) {
            counter.addAndGet(permits); // Consume the permits
            return true;
        }
        return false; // Limit exceeded
    }
}
