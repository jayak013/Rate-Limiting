package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Sliding Window Log implementation of Rate Limiter.
 * 
 * <p>Logic:
 * This algorithm keeps a log (queue) of timestamps for every request.
 * When a new request arrives:
 * <ol>
 *   <li>Outdated timestamps (older than the window start) are removed from the log.</li>
 *   <li>The remaining log size is compared against the maximum limit.</li>
 *   <li>If under the limit, the current timestamp is added to the log and the request is allowed.</li>
 * </ol>
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li><b>Precision:</b> Extremely accurate; it does not suffer from the boundary 
 *       issues of Fixed Window.</li>
 *   <li><b>Memory:</b> High memory usage, proportional to the number of requests 
 *       within the window (O(maxRequests)).</li>
 * </ul>
 * 
 * <p>Trade-offs:
 * <ul>
 *   <li><b>Pros:</b> Highest precision.</li>
 *   <li><b>Cons:</b> High memory footprint and potentially slow if the log is very large.</li>
 * </ul>
 * 
 * <p>Concurrency:
 * This implementation uses {@code synchronized} on the {@code allow} method to 
 * protect the non-thread-safe {@link java.util.LinkedList} used for the log.
 */
public class SlidingWindowLogRateLimiter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMillis;
    private final Queue<Long> requestTimestamps;

    public SlidingWindowLogRateLimiter(long maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.requestTimestamps = new LinkedList<>();
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public synchronized boolean allow(int permits) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSizeMillis;

        // Remove timestamps that are older than the current sliding window
        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
            requestTimestamps.poll();
        }

        // Check if adding the new permits would exceed the limit for this window
        if (requestTimestamps.size() + permits <= maxRequests) {
            for (int i = 0; i < permits; i++) {
                requestTimestamps.add(now); // Log the current request
            }
            return true;
        }
        return false; // Limit exceeded
    }

    public static void main(String[] args) {

    }
}
