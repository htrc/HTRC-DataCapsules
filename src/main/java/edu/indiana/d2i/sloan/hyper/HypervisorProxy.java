package edu.indiana.d2i.sloan.hyper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;

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

		public Worker(HypervisorCommand command) {
			this.command = command;
			MAX_RETRY = Integer.valueOf(Configuration.getInstance().getProperty(
				Configuration.PropertyName.MAX_RETRY, Constants.DEFAULT_HYPER_MAX_RETRY));
		}

		@Override
		public void run() {
			// TODO: retry in command itself?
			for (int i = 1; i <= MAX_RETRY; i++) {
				try {
					command.execute();
					return;
				} catch (Exception e) {
					logger.error(String.format(
						"Unable to execute command % s because %s", command, e.getMessage()), e);
					logger.info("retry command " + command + " " + i + " times");
				}
			}
			logger.error("Unable to execute " + command);
			
			// TODO: clean up should go here
			try {
				command.cleanupOnFailed();
			} catch (Exception e) {
				logger.error("Uable to roll back before error because " + e.getMessage(), e);
			}
		}
	}

	private HypervisorProxy() {
		int workers = Integer.valueOf(Configuration.getInstance().getProperty(
			Configuration.PropertyName.WORKER_POOL_SIZE, Constants.DEFAULT_HYPER_WORKERS));
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
