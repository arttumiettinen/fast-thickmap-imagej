package fastthickmap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides simple parallel for each loop. This is from StackOverflow:
 * http://stackoverflow.com/questions/4010185/parallel-for-for-java
 * 
 * @author user
 *
 */
public class Loop {
	public interface Each {
		void run(long i);
	}

	private static final int CPUs = Runtime.getRuntime().availableProcessors();

	private static ExecutorService executor = Executors.newFixedThreadPool(CPUs);

	public static void withIndex(long start, long stop, final Each body) throws InterruptedException {
		long chunksize = (stop - start + CPUs - 1) / CPUs;
		int loops = (int)((stop - start + chunksize - 1) / chunksize);

		final CountDownLatch latch = new CountDownLatch(loops);
		for (long i = start; i < stop;) {
			final long lo = i;
			i += chunksize;
			final long hi = (i < stop) ? i : stop;
			executor.submit(new Runnable() {
				public void run() {
					for (long i = lo; i < hi; i++)
						body.run(i);
					latch.countDown();
				}
			});
		}

		latch.await();
	}
}
