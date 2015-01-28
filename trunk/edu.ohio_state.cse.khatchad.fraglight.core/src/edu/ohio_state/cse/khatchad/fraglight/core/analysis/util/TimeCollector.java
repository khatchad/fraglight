/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis.util;

/**
 * @author raffi
 *
 */
public class TimeCollector {
	
	private long collectedTime;
	private long start;
	
	public void start() {
		start = System.currentTimeMillis();
	}
	
	public void stop() {
		final long elapsed = System.currentTimeMillis() - start;
		collectedTime += elapsed;
	}
	
	public long getCollectedTime() {
		return collectedTime;
	}

	public void clear() {
		collectedTime = 0;
	}
}
