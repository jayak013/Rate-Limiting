/**
 * This package contains various implementations of Rate Limiting algorithms.
 * 
 * <h2>Algorithms Overview</h2>
 * <ul>
 *   <li><b>Fixed Window Counter:</b> Simple, memory-efficient, but allows spikes at boundaries.</li>
 *   <li><b>Token Bucket:</b> Flexible, allows bursts, refilled at a fixed rate.</li>
 *   <li><b>Leaky Bucket:</b> Enforces a constant processing rate, smooths out traffic.</li>
 *   <li><b>Sliding Window Log:</b> Highly precise but memory-intensive.</li>
 *   <li><b>Sliding Window Counter:</b> A hybrid approach that balances precision and memory efficiency.</li>
 * </ul>
 * 
 * <h2>Concurrency Models</h2>
 * Each algorithm has two variants:
 * <ol>
 *   <li><b>Standard:</b> Uses {@code synchronized} blocks for simplicity and guaranteed correctness.</li>
 *   <li><b>Lock-Free:</b> Uses {@link java.util.concurrent.atomic.AtomicReference} with immutable 
 *       state records to provide high-throughput, non-blocking behavior.</li>
 * </ol>
 */
package org.example.ratelimiter.algorithms;
