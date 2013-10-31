package edu.indiana.d2i.sloan.scheduler;

import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;

public class SchedulerFactory {
	private static Logger logger = Logger.getLogger(SchedulerFactory.class);

	private static Scheduler scheduler = null;

	private SchedulerFactory() {

	}

	static {
		String schedulerClassName = Configuration.getInstance().getString(
				Configuration.PropertyName.SCHEDULER_IMPL_CLASS,
				Constants.DEFAULT_SCHEDULER_IMPL_CLASS);
		logger.info("Load scheduler as " + schedulerClassName);

		try {
			Class<?> schedulerClass = Class.forName(schedulerClassName);
			Constructor<?> constructor = schedulerClass
					.getDeclaredConstructor();

			/* set accessible to true since constructor is private */
			constructor.setAccessible(true);

			scheduler = (Scheduler) constructor.newInstance();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		/*
		catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		} catch (InstantiationException e) {
			logger.error(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			logger.error(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			logger.error(e.getMessage(), e);
		}
		*/
	}

	public static Scheduler getInstance() {
		return scheduler;
	}
}
