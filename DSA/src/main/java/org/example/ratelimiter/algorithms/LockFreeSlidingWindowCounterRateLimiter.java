package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-Free Sliding Window Counter implementation.
 * 
 * <p>Logic:
 * Non-blocking version of {@link SlidingWindowCounterRateLimiter}.
 * 
 * <p>Concurrency Design:
 * Uses {@link java.util.concurrent.atomic.AtomicReference} with an immutable 
 * {@code CounterState} record to ensure that window transitions and counter 
 * updates are atomic. This prevents the "lost update" or "incorrect window reset" 
 * problems that would occur if using multiple {@code AtomicLong} fields without 
 * synchronization.
 */
public class LockFreeSlidingWindowCounterRateLimiter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMillis;
    private final AtomicReference<CounterState> state;

    private record CounterState(long prevCount, long currentCount, long windowStart) {}

    public LockFreeSlidingWindowCounterRateLimiter(long maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.state = new AtomicReference<>(new CounterState(0, 0, System.currentTimeMillis()));
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public boolean allow(int permits) {
        if (permits <= 0) return true;

        // CAS loop for thread-safe state updates
        while (true) {
            CounterState current = state.get();
            long now = System.currentTimeMillis();
            long windowStart = current.windowStart;
            long timePassed = now - windowStart;

            long nextPrevCount = current.prevCount;
            long nextCurrentCount = current.currentCount;
            long nextWindowStart = current.windowStart;

            // Handle window transitions if time has moved past the current window
            if (timePassed >= windowSizeMillis) {
                long windowsPassed = timePassed / windowSizeMillis;
                if (windowsPassed == 1) {
                    nextPrevCount = current.currentCount;
                } else {
                    nextPrevCount = 0;
                }
                nextCurrentCount = 0;
                nextWindowStart = now - (timePassed % windowSizeMillis);
                timePassed = now - nextWindowStart;
            }

            // Estimate the current sliding window count using weighted average of the last two fixed windows
            double overlapWeight = (double) (windowSizeMillis - timePassed) / windowSizeMillis;
            long estimatedCount = (long) (nextCurrentCount + (nextPrevCount * overlapWeight));

            // Reject if the estimated count exceeds the limit
            if (estimatedCount + permits > maxRequests) {
                return false;
            }

            // Attempt to atomically swap the old state with the updated counts and window info
            CounterState nextState = new CounterState(nextPrevCount, nextCurrentCount + permits, nextWindowStart);
            if (state.compareAndSet(current, nextState)) {
                return true;
            }
        }
    }
}
