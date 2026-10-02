package org.example.ratelimiter;

import org.example.ratelimiter.algorithms.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {

    @Test
    public void testRateLimitManager() {
        RateLimitManager manager = new RateLimitManager(RateLimiterFactory.Algorithm.FIXED_WINDOW, 2, 1000);
        
        // Client A
        assertTrue(manager.allow("ClientA"), "Client A's first request should be allowed");
        assertTrue(manager.allow("ClientA"), "Client A's second request should be allowed");
        assertFalse(manager.allow("ClientA"), "Client A's third request should be rejected");
        
        // Client B (should be independent)
        assertTrue(manager.allow("ClientB"), "Client B's first request should be allowed independently");
        assertTrue(manager.allow("ClientB"), "Client B's second request should be allowed independently");
        assertFalse(manager.allow("ClientB"), "Client B's third request should be rejected independently");
        
        // Test getLimiter
        RateLimiter limiterA = manager.getLimiter("ClientA");
        assertNotNull(limiterA, "Limiter for Client A should not be null");
        assertFalse(limiterA.allow(), "Limiter A should still be in exhausted state"); 
    }

    @Test
    public void testTokenBucket() throws InterruptedException {
        testTokenBucketLogic(new TokenBucketRateLimiter(5, 1));
    }

    @Test
    public void testLockFreeTokenBucket() throws InterruptedException {
        testTokenBucketLogic(new LockFreeTokenBucketRateLimiter(5, 1));
    }

    private void testTokenBucketLogic(RateLimiter limiter) throws InterruptedException {
        // Use all tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allow(), "Should allow request " + i + " within capacity");
        }
        assertFalse(limiter.allow(), "Should not allow request after capacity is exhausted");
        
        // Test multiple permits
        Thread.sleep(2100); // Wait for 2 tokens
        assertTrue(limiter.allow(2), "Should allow request for 2 permits after refill");
        assertFalse(limiter.allow(1), "Should not allow request after permits consumed");
    }

    @Test
    public void testLeakyBucket() throws InterruptedException {
        testLeakyBucketLogic(new LeakyBucketRateLimiter(3, 1));
    }

    @Test
    public void testLockFreeLeakyBucket() throws InterruptedException {
        testLeakyBucketLogic(new LockFreeLeakyBucketRateLimiter(3, 1));
    }

    private void testLeakyBucketLogic(RateLimiter limiter) throws InterruptedException {
        assertTrue(limiter.allow(), "First drop should be allowed");
        assertTrue(limiter.allow(), "Second drop should be allowed");
        assertTrue(limiter.allow(), "Third drop should be allowed");
        assertFalse(limiter.allow(), "Bucket should be full");

        Thread.sleep(1100); // 1 permit should leak out
        assertTrue(limiter.allow(), "Should allow 1 drop after 1 second leak");
        assertFalse(limiter.allow(), "Should be full again");
    }

    @Test
    public void testFixedWindow() throws InterruptedException {
        RateLimiter limiter = new FixedWindowRateLimiter(2, 1000); // 2 requests per 1000ms
        
        assertTrue(limiter.allow(), "First request in window allowed");
        assertTrue(limiter.allow(), "Second request in window allowed");
        assertFalse(limiter.allow(), "Third request in window rejected");
        
        Thread.sleep(1100);
        assertTrue(limiter.allow(), "First request in new window allowed");
    }

    @Test
    public void testLockFreeFixedWindow() throws InterruptedException {
        RateLimiter limiter = new LockFreeFixedWindowRateLimiter(2, 1000);
        
        assertTrue(limiter.allow(), "First request in window allowed");
        assertTrue(limiter.allow(), "Second request in window allowed");
        assertFalse(limiter.allow(), "Third request in window rejected");
        
        Thread.sleep(1100);
        assertTrue(limiter.allow(), "First request in new window allowed");
    }

    @Test
    public void testSlidingWindowLog() throws InterruptedException {
        testSlidingWindowLogLogic(new SlidingWindowLogRateLimiter(2, 1000));
    }

    @Test
    public void testLockFreeSlidingWindowLog() throws InterruptedException {
        testSlidingWindowLogLogic(new LockFreeSlidingWindowLogRateLimiter(2, 1000));
    }

    private void testSlidingWindowLogLogic(RateLimiter limiter) throws InterruptedException {
        assertTrue(limiter.allow(), "Request 1 at T=0 allowed");
        Thread.sleep(500);
        assertTrue(limiter.allow(), "Request 2 at T=500 allowed");
        assertFalse(limiter.allow(), "Request 3 at T=500 rejected");
        
        Thread.sleep(600); // T=1100
        assertTrue(limiter.allow(), "Request 4 at T=1100 allowed (Request 1 expired)");
        assertFalse(limiter.allow(), "Request 5 at T=1100 rejected (Request 2 still active)");
    }

    @Test
    public void testSlidingWindowCounter() throws InterruptedException {
        testSlidingWindowCounterLogic(new SlidingWindowCounterRateLimiter(10, 1000));
    }

    @Test
    public void testLockFreeSlidingWindowCounter() throws InterruptedException {
        testSlidingWindowCounterLogic(new LockFreeSlidingWindowCounterRateLimiter(10, 1000));
    }

    private void testSlidingWindowCounterLogic(RateLimiter limiter) throws InterruptedException {
        // Fill 60% of current window
        for(int i=0; i<6; i++) assertTrue(limiter.allow());
        
        Thread.sleep(500);
        // At T=500, we are halfway through the window.
        
        Thread.sleep(600); // Now at T=1100, new window.
        // estimated count = 0 + 6 * 0.9 = 5.4 -> 5
        // So we should have 10 - 5 = 5 permits left.
        
        for(int i=0; i<5; i++) assertTrue(limiter.allow(), "Request " + i + " in new window should be allowed");
        assertFalse(limiter.allow(), "Request 6 in new window should be rejected due to weighted average");
    }

    @Test
    public void testRateLimiterFactory() {
        for (RateLimiterFactory.Algorithm algo : RateLimiterFactory.Algorithm.values()) {
            RateLimiter limiter = RateLimiterFactory.createRateLimiter(algo, 10, 1000);
            assertNotNull(limiter, "Factory should create limiter for " + algo);
            
            // Basic check
            assertTrue(limiter.allow(), "Should allow first request for " + algo);
        }
    }
}
