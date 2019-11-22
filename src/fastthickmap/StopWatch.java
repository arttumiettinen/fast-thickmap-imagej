package fastthickmap;

/**
 * Simple stop watch.
 * @author miettinen_a
 *
 */
public class StopWatch {
	
	public StopWatch() {
		
	}
	
	private long startTime;
	private long endTime;
	
	/**
	 * Starts the timer.
	 */
	public void start() {
		startTime = System.nanoTime();
		endTime = startTime;
	}
	
	/**
	 * Stops the watch and returns time since last start() call in milliseconds.
	 * @return
	 */
	public long stop() {
		endTime = System.nanoTime();
		return getElapsed();
	}
	
	/**
	 * Returns time elapsed between last start() and stop() calls in milliseconds.
	 * @return
	 */
	public long getElapsed() {
		return (endTime - startTime) / 1000000;
	} 

}
