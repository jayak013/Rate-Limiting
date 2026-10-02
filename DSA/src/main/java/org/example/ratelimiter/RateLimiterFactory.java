package org.example.ratelimiter;

import org.example.ratelimiter.algorithms.*;

/**
 * Factory class for creating RateLimiter instances.
 */
public class RateLimiterFactory {
    
    /**
     * Supported Rate Limiting Algorithms.
     */
    public enum Algorithm {
        /**
         * Tokens are added to a bucket at a fixed rate. Requests consume tokens.
         * Allows for bursts of traffic.
         */
        TOKEN_BUCKET,

        /**
         * Lock-free version of Token Bucket using AtomicReference.
         */
        LOCK_FREE_TOKEN_BUCKET,

        /**
         * Requests enter a bucket and leak out at a constant rate.
         * Smooths out traffic spikes.
         */
        LEAKY_BUCKET,

        /**
         * Lock-free version of Leaky Bucket.
         */
        LOCK_FREE_LEAKY_BUCKET,

        /**
         * Simple counter per fixed time window.
         * Memory efficient but can allow spikes at window boundaries.
         */
        FIXED_WINDOW,

        /**
         * Lock-free version of Fixed Window.
         */
        LOCK_FREE_FIXED_WINDOW,

        /**
         * Tracks every request timestamp in a log.
         * Highly accurate but memory intensive.
         */
        SLIDING_WINDOW_LOG,

        /**
         * Lock-free version of Sliding Window Log.
         */
        LOCK_FREE_SLIDING_WINDOW_LOG,

        /**
         * Hybrid approach using weighted average of current and previous windows.
         * Smooths out spikes while remaining memory efficient.
         */
        SLIDING_WINDOW_COUNTER,

        /**
         * Lock-free version of Sliding Window Counter.
         */
        LOCK_FREE_SLIDING_WINDOW_COUNTER
    }

    public static RateLimiter createRateLimiter(Algorithm algorithm, long limit, long rateOrWindow) {
        return switch (algorithm) {
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(limit, rateOrWindow);
            case LOCK_FREE_TOKEN_BUCKET -> new LockFreeTokenBucketRateLimiter(limit, rateOrWindow);
            case LEAKY_BUCKET -> new LeakyBucketRateLimiter(limit, rateOrWindow);
            case LOCK_FREE_LEAKY_BUCKET -> new LockFreeLeakyBucketRateLimiter(limit, rateOrWindow);
            case FIXED_WINDOW -> new FixedWindowRateLimiter(limit, rateOrWindow);
            case LOCK_FREE_FIXED_WINDOW -> new LockFreeFixedWindowRateLimiter(limit, rateOrWindow);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(limit, rateOrWindow);
            case LOCK_FREE_SLIDING_WINDOW_LOG -> new LockFreeSlidingWindowLogRateLimiter(limit, rateOrWindow);
            case SLIDING_WINDOW_COUNTER -> new SlidingWindowCounterRateLimiter(limit, rateOrWindow);
            case LOCK_FREE_SLIDING_WINDOW_COUNTER -> new LockFreeSlidingWindowCounterRateLimiter(limit, rateOrWindow);
        };
    }
}
