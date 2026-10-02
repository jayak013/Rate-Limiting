package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A correct Lock-Free Fixed Window Counter implementation.
 * 
 * <p>Logic:
 * Similar to {@link FixedWindowRateLimiter}, but uses non-blocking synchronization.
 * 
 * <p>Concurrency Design (Important):
 * This implementation avoids {@code synchronized} blocks to improve throughput under 
 * high contention. It uses {@link java.util.concurrent.atomic.AtomicReference} with 
 * an immutable {@code WindowState} record to ensure that the window transition 
 * (resetting the counter and updating the timestamp) happens atomically.
 * 
 * <p>Why not use {@code AtomicLong} for the counter and timestamp separately?
 * A common mistake in lock-free implementations is to update the timestamp and 
 * counter in two separate atomic steps:
 * <pre>{@code
 * if (now - lastTimestamp >= windowSize) {
 *     lastTimestamp.set(now);
 *     counter.set(0); // RACE CONDITION HERE
 * }
 * }</pre>
 * In high contention, one thread could update the timestamp but get preempted before 
 * resetting the counter. Other threads would then see the "new" window but add to 
 * the "old" counter value, leading to incorrect rejections or lost permits.
 * 
 * <p>By using an immutable state object, we ensure the entire transition is atomic 
 * via a single Compare-And-Swap (CAS) operation.
 */
public class LockFreeFixedWindowRateLimiter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMillis;
    private final AtomicReference<WindowState> state;

    private record WindowState(long windowStart, long count) {}

    public LockFreeFixedWindowRateLimiter(long maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.state = new AtomicReference<>(new WindowState(System.currentTimeMillis(), 0));
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public boolean allow(int permits) {
        if (permits <= 0) return true;

        // Loop until the state is successfully updated (CAS)
        while (true) {
            WindowState current = state.get();
            long now = System.currentTimeMillis();

            if (now - current.windowStart >= windowSizeMillis) {
                // Window expired: Try to start a new window atomically
                WindowState next = new WindowState(now, permits);
                
                // If the requested permits alone exceed the max, reject early
                if (permits > maxRequests) return false;
                
                // Attempt to replace the old window state with the new one
                if (state.compareAndSet(current, next)) {
                    return true;
                }
            } else {
                // Within current window: Check if the limit would be exceeded
                if (current.count + permits > maxRequests) {
                    return false;
                }
                
                // Try to increment counter atomically while staying in the same window
                WindowState next = new WindowState(current.windowStart, current.count + permits);
                if (state.compareAndSet(current, next)) {
                    return true;
                }
            }
            // CAS failed (another thread updated the state first): Retry the loop
        }
    }
}
