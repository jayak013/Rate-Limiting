package org.example.ratelimiter.algorithms;

import org.example.ratelimiter.RateLimiter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-Free Token Bucket implementation.
 * 
 * <p>Logic:
 * Non-blocking version of {@link TokenBucketRateLimiter}.
 * 
 * <p>Concurrency Design:
 * Uses {@link java.util.concurrent.atomic.AtomicReference} with an immutable 
 * {@code TokenState} record to ensure that token refill and consumption are atomic 
 * without using locks. This is critical because refill depends on both the 
 * current token count and the timestamp of the last refill. Updating them 
 * independently would lead to race conditions where tokens are "lost" or 
 * over-refilled.
 * 
 * <p>Precision:
 * To maintain precision for partial seconds, the implementation only updates 
 * the {@code lastRefill} timestamp when at least one full token has been added. 
 * This ensures that "sub-token" time progress is not lost on every request.
 */
public class LockFreeTokenBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private final long refillRatePerSecond;
    private final AtomicReference<TokenState> state;

    private record TokenState(long tokens, long lastRefill) {}

    public LockFreeTokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.state = new AtomicReference<>(new TokenState(capacity, System.currentTimeMillis()));
    }

    @Override
    public boolean allow() {
        return allow(1);
    }

    @Override
    public boolean allow(int permits) {
        if (permits <= 0) return true;

        // Keep trying until CAS succeeds
        while (true) {
            TokenState current = state.get();
            long now = System.currentTimeMillis();
            long timePassed = now - current.lastRefill;

            // Calculate how many tokens should have been added since the last request
            long tokensToAdd = (timePassed * refillRatePerSecond) / 1000;
            long refreshedTokens = Math.min(capacity, current.tokens + tokensToAdd);

            long nextTokens;
            long nextRefill;
            
            // If at least one full token was added, update the refill timestamp to 'now'.
            // If not, keep the 'lastRefill' timestamp so we don't lose progress towards the next token.
            if (tokensToAdd > 0) {
                nextTokens = refreshedTokens;
                nextRefill = now;
            } else {
                nextTokens = current.tokens;
                nextRefill = current.lastRefill;
            }

            // Check if we have enough tokens (including newly added ones)
            if (nextTokens < permits) {
                return false;
            }

            // Create new immutable state with subtracted permits
            TokenState nextState = new TokenState(nextTokens - permits, nextRefill);
            
            // Atomically update the state; if current has changed, retry
            if (state.compareAndSet(current, nextState)) {
                return true;
            }
        }
    }
}
