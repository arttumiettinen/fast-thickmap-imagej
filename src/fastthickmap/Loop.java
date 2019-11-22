package fastthickmap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

		ArrayList<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
		for (long i = start; i < stop;) {
			final long lo = i;
			i += chunksize;
			final long hi = (i < stop) ? i : stop;
			tasks.add(new Callable<Void>() {
	
				@Override
				public Void call() throws Exception {
					for (long i = lo; i < hi; i++)
						body.run(i);
					return null;
				}
				
			});
		}
		
		List<Future<Void>> results = executor.invokeAll(tasks);
		
		for(Future<Void> result : results) {
			try {
				result.get();
			}
			catch(ExecutionException e) {
				if(e.getCause() != null && e.getCause().getMessage() != null)
					throw new RuntimeException("Not all tasks completed succesfully. " + e.getCause().getMessage());
				else
					throw new RuntimeException("Not all tasks completed succesfully. " + e.getMessage());
			}
		}
		
		
		// This version blocks if an exception is thrown in the submitted task and latch never counts to zero.
//		int loops = (int)((stop - start + chunksize - 1) / chunksize);
//		final CountDownLatch latch = new CountDownLatch(loops);
//		for (long i = start; i < stop;) {
//			final long lo = i;
//			i += chunksize;
//			final long hi = (i < stop) ? i : stop;
//			executor.submit(new Runnable() {
//				public void run() {
//					for (long i = lo; i < hi; i++)
//						body.run(i);
//					latch.countDown();
//				}
//			});
//		}
//
//		latch.await();
	}
}
