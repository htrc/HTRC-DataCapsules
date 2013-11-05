package edu.indiana.d2i.sloan.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import edu.indiana.d2i.sloan.utils.RetriableTask;

public class TestRetriableTask {
	
	@Test(expected = RuntimeException.class)
	public void testRetriable() throws Exception {
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					throw new RuntimeException("retry exception.");
				}
			}, 500, 1);
		r.call();
	}
	
	@Test(expected = java.util.concurrent.TimeoutException.class)
	public void testThreadException() throws Exception {		
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					ExecutorService executor = null;
					try {
						executor = Executors.newSingleThreadExecutor();
						Future<Void> future = executor.submit(
							new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									Thread.sleep(2000);
									return null;
								}
							} );
						return future.get(500, TimeUnit.MILLISECONDS);
					} finally {
						if (executor != null)
							executor.shutdownNow();
					}
				}
			},  1000, 2);
		r.call();
	}
}
