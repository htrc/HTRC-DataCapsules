package edu.indiana.d2i.sloan.hyper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;

class HypervisorFactory {
	private static Logger logger = Logger.getLogger(HypervisorFactory.class);

	private static Constructor<?> constructor;

	static {
		String hyperClassName = Configuration.getInstance().getString(
				Configuration.PropertyName.HYPERVISOR_FULL_CLASS_NAME);
		logger.info("Load hyper as " + hyperClassName);

		try {
			Class<?> hyperClass = Class.forName(hyperClassName);
			constructor = hyperClass.getDeclaredConstructor();

			/* set accessible to true since constructor is private */
			constructor.setAccessible(true);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static IHypervisor createHypervisor()
			throws IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		return (IHypervisor) constructor.newInstance();
	}
}
