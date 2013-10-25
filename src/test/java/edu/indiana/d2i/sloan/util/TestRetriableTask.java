package edu.indiana.d2i.sloan.util;

import java.util.concurrent.Callable;

import org.junit.Test;

import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.utils.RetriableTask;

public class TestRetriableTask {
	
	@Test(expected = RuntimeException.class)
	public void testNonRetriable() throws Exception {
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					throw new RuntimeException("unretriable exception.");
				}
			});
		r.call();
	}
	
	@Test(expected = RetriableException.class)
	public void testRetriable() throws Exception {
		java.util.Set<String> exceptions = new java.util.HashSet<String>();
		exceptions.add(RetriableException.class.getName());
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					throw new RetriableException("retriable exception.");
				}
			},  1000, 1, exceptions);
		r.call();
	}
}
