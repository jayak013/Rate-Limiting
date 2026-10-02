package org.example;

import org.example.ratelimiter.RateLimitManager;
import org.example.ratelimiter.RateLimiter;
import org.example.ratelimiter.RateLimiterFactory;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Rate Limiter Implementation Demo ---");

        // 1. Global Rate Limiter Demo
        // All requests share the same counter.
        System.out.println("\n[Global Rate Limiter Demo] - 3 requests total per 2 seconds");
        RateLimiter globalLimiter = RateLimiterFactory.createRateLimiter(
                RateLimiterFactory.Algorithm.FIXED_WINDOW, 3, 2000);
        
        System.out.println("Client A calls 2 times:");
        simulateRequests(globalLimiter, 3);
//        Thread.sleep(2000);
        System.out.println("Client B calls 2 times (shares same limit):");
        simulateRequests(globalLimiter, 2);

        // 2. Client-Specific Rate Limiter Demo
        // Each client has its own independent counter.
        System.out.println("\n[Client-Specific Rate Limiter Demo] - 3 requests PER CLIENT per 2 seconds");
        RateLimitManager manager = new RateLimitManager(
                RateLimiterFactory.Algorithm.FIXED_WINDOW, 3, 2000);

        System.out.println("Client A calls 3 times:");
        simulateClientRequests(manager, "Client-A", 3);
        System.out.println("Client B calls 3 times (independent of Client A):");
        simulateClientRequests(manager, "Client-B", 3);
        System.out.println("Client A calls again (should be rejected as it exceeded its own limit):");
        simulateClientRequests(manager, "Client-A", 1);

        // 3. Manual Limiter Retrieval Demo
        System.out.println("\n[Manual Limiter Retrieval Demo] - Accessing the limiter directly for Client B");
        RateLimiter clientBLimiter = manager.getLimiter("Client-B");
        // Client B already used 3 requests in the previous step, so it should be rejected now.
        boolean allowed = clientBLimiter.allow();
        System.out.println("  Client B direct call: " + (allowed ? "ALLOWED" : "REJECTED"));
    }

    private static void simulateRequests(RateLimiter limiter, int count) {
        for (int i = 1; i <= count; i++) {
            boolean allowed = limiter.allow();
            System.out.println("  Request " + i + ": " + (allowed ? "ALLOWED" : "REJECTED"));
        }
    }

    private static void simulateClientRequests(RateLimitManager manager, String clientId, int count) {
        for (int i = 1; i <= count; i++) {
            boolean allowed = manager.allow(clientId);
            System.out.println("  " + clientId + " Request " + i + ": " + (allowed ? "ALLOWED" : "REJECTED"));
        }
    }
}
