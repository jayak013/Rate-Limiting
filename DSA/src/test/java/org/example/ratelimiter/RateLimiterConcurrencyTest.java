package org.example.ratelimiter;

import org.example.ratelimiter.algorithms.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateLimiterConcurrencyTest {

    @Test
    public void testTokenBucketConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.TOKEN_BUCKET, 100, 0));
    }

    @Test
    public void testLockFreeTokenBucketConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.LOCK_FREE_TOKEN_BUCKET, 100, 0));
    }

    @Test
    public void testLeakyBucketConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.LEAKY_BUCKET, 100, 0));
    }

    @Test
    public void testLockFreeLeakyBucketConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.LOCK_FREE_LEAKY_BUCKET, 100, 0));
    }

    @Test
    public void testFixedWindowConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.FIXED_WINDOW, 100, 100000));
    }

    @Test
    public void testLockFreeFixedWindowConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.LOCK_FREE_FIXED_WINDOW, 100, 100000));
    }

    @Test
    public void testSlidingWindowLogConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.SLIDING_WINDOW_LOG, 100, 100000));
    }

    @Test
    public void testLockFreeSlidingWindowLogConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.LOCK_FREE_SLIDING_WINDOW_LOG, 100, 100000));
    }

    @Test
    public void testSlidingWindowCounterConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.SLIDING_WINDOW_COUNTER, 100, 100000));
    }

    @Test
    public void testLockFreeSlidingWindowCounterConcurrency() throws InterruptedException {
        testConcurrency(RateLimiterFactory.createRateLimiter(RateLimiterFactory.Algorithm.LOCK_FREE_SLIDING_WINDOW_COUNTER, 100, 100000));
    }

    private void testConcurrency(RateLimiter limiter) throws InterruptedException {
        int capacity = 100;
        int threadCount = 50;
        int requestsPerThread = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedRequests = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (limiter.allow()) {
                            allowedRequests.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(capacity, allowedRequests.get(), "Total allowed requests should match capacity for " + limiter.getClass().getSimpleName());
    }
}
