package edu.indiana.d2i.sloan.utils;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

public class RetriableTask<T> implements Callable<T> {
	private static Logger logger = Logger.getLogger(RetriableTask.class);
	
	private final Callable<T> task;
	private long waitInMs = 1000;
	private int retry = 5;
	private Set<String> retriableExceptions = null;

	public RetriableTask(Callable<T> task) {
		this.task = task;
	}
	
	public RetriableTask(Callable<T> task, long waitInMs, int maxRetry) {
		this.task = task;
		this.waitInMs = waitInMs;
		this.retry = maxRetry;
	}
	
	public RetriableTask(Callable<T> task, long waitInMs, int maxRetry,
		Set<String> retriableExceptions) {
		this.task = task;
		this.waitInMs = waitInMs;
		this.retry = maxRetry;
		this.retriableExceptions = retriableExceptions;
	}

	@Override
	public T call() throws Exception {
		while (true) {
			try {
				return task.call();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				if (retriableExceptions == null || 
					retriableExceptions.contains(ex.getClass().getName())) {
					if (retry == 0) throw ex;
					Thread.sleep(waitInMs);
					logger.info("Retry " + task.getClass().getName() + ", " + retry + " times left.");
					retry--;
				} else {
					throw ex;
				}
			}
		}
	}
}
