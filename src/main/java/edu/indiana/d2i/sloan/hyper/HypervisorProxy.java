package edu.indiana.d2i.sloan.hyper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;

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

		public Worker(HypervisorCommand command) {
			this.command = command;
		}

		@Override
		public void run() {
			try {
				command.execute();
			} catch (Exception ex) {
				try {
					logger.error(ex.getMessage(), ex);
					command.cleanupOnFailed();
				} catch (Exception e) {
					logger.error("Uable to clean up after error because " + e.getMessage(), e);
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
