package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-Free Sliding Window Log implementation.
 * 
 * <p>Logic:
 * Non-blocking version of {@link SlidingWindowLogRateLimiter}.
 * 
 * <p>Concurrency Design:
 * Uses {@link java.util.concurrent.atomic.AtomicReference} with an immutable 
 * {@code LogState} containing a list of timestamps. 
 * 
 * <p>Performance Note:
 * This implementation has significantly higher overhead than others because 
 * the entire list of timestamps is copied on every successful request to maintain 
 * immutability. It is primarily for educational purposes or scenarios with very 
 * low request limits where high precision and non-blocking behavior are both required.
 */
public class LockFreeSlidingWindowLogRateLimiter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMillis;
    private final AtomicReference<LogState> state;

    private record LogState(List<Long> timestamps) {}

    public LockFreeSlidingWindowLogRateLimiter(long maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.state = new AtomicReference<>(new LogState(Collections.emptyList()));
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public boolean allow(int permits) {
        if (permits <= 0) return true;

        // Loop until atomic update succeeds
        while (true) {
            LogState current = state.get();
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMillis;

            // Filter out timestamps that fall outside the current sliding window
            List<Long> filtered = new ArrayList<>();
            for (Long t : current.timestamps) {
                if (t >= windowStart) {
                    filtered.add(t);
                }
            }

            // Reject if the window is already full
            if (filtered.size() + permits > maxRequests) {
                return false;
            }

            // Create a new list containing the valid old timestamps plus the new ones
            for (int i = 0; i < permits; i++) {
                filtered.add(now);
            }

            // Attempt to update the shared state with the new immutable list
            LogState nextState = new LogState(Collections.unmodifiableList(filtered));
            if (state.compareAndSet(current, nextState)) {
                return true;
            }
        }
    }
}
