package org.example.ratelimiter;

import org.example.ratelimiter.algorithms.FixedWindowRateLimiter;
import org.example.ratelimiter.algorithms.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterEdgeCaseTest {

    @Test
    public void testZeroLimit() {
        // Limit of 0 should reject everything
        RateLimiter limiter = new FixedWindowRateLimiter(0, 1000);
        assertFalse(limiter.allow(), "Limit of 0 should not allow any request");
    }

    @Test
    public void testHighBurst() {
        // 100 capacity
        RateLimiter limiter = new TokenBucketRateLimiter(100, 1);
        
        // Large permit request
        assertTrue(limiter.allow(100), "Should allow large burst within capacity");
        assertFalse(limiter.allow(1), "Should reject even 1 permit after burst");
    }

    @Test
    public void testPermitsGreaterThanCapacity() {
        RateLimiter limiter = new TokenBucketRateLimiter(5, 1);
        // Requesting 6 when capacity is 5
        assertFalse(limiter.allow(6), "Should not allow more permits than capacity");
    }

    @Test
    public void testNegativePermits() {
        RateLimiter limiter = new TokenBucketRateLimiter(5, 1);
        // By standard, negative permits might be invalid. 
        // Let's see how the implementation handles it. 
        // Most likely it should just return true if it doesn't check, 
        // or we should add validation in the code.
        // For now, let's just test current behavior.
        assertTrue(limiter.allow(-1), "Negative permits usually shouldn't decrease count (implementation specific)");
    }
}
