package edu.indiana.d2i.sloan.hyper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.utils.RetriableTask;

public final class HypervisorProxy {
	private static Logger logger = Logger.getLogger(HypervisorProxy.class);
	private static HypervisorProxy instance = null;

	private ExecutorService executorService = null;

	class WorkerThreadFactory implements ThreadFactory {
		private final AtomicInteger n = new AtomicInteger(1);
		private final String PREFIX = "worker";

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, PREFIX + "-"
					+ n.getAndIncrement());
			thread.setPriority(Thread.NORM_PRIORITY);
			thread.setDaemon(true);
			return thread;
		}
	}

	class Worker implements Runnable {
		private HypervisorCommand command;
		private final int MAX_RETRY;
		private final long RETRY_DELAY_MS;

		public Worker(HypervisorCommand command) {
			this.command = command;
			MAX_RETRY = Configuration.getInstance().getInt(
					Configuration.PropertyName.MAX_RETRY);
			RETRY_DELAY_MS = 0;
		}

		@Override
		public void run() {
			RetriableTask<Void> r = new RetriableTask<Void>(
				new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						command.execute();
						return null;
					}
				}, RETRY_DELAY_MS, MAX_RETRY, 
				new HashSet<String>(Arrays.asList(RetriableException.class.getName())));
			
			try {
				r.call();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				try {
					command.cleanupOnFailed();
				} catch (Exception e) {
					logger.error("Uable to roll back after error because " + e.getMessage(), e);
				}
			}
		}
	}

	private HypervisorProxy() {
		int workers = Configuration.getInstance().getInt(Configuration.PropertyName.WORKER_POOL_SIZE); 
		executorService = Executors.newFixedThreadPool(workers,
			new WorkerThreadFactory());
	}

	public static synchronized HypervisorProxy getInstance() {
		if (instance == null) {
			instance = new HypervisorProxy();
		}
		return instance;
	}

	public void addCommand(HypervisorCommand command) {
		executorService.execute(new Worker(command));
	}

	public void shutdown() {
		executorService.shutdown();
	}
}
