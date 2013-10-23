package edu.indiana.d2i.sloan;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class Configuration {
	private static Logger logger = Logger.getLogger(Configuration.class);

	private static Configuration instance = null;

	private Configuration() {
		properties = new HashMap<String, String>();
	}

	private Map<String, String> properties = null;

	public static class PropertyName {
		// optional properties
		public static final String WORKER_POOL_SIZE = "sloan.ws.hyper.workers";
		public static final String MAX_RETRY = "sloan.ws.hyper.maxretry";
		public static final String OPERATION_TIMEOUT_MS = "sloan.ws.hyper.timeout.sec";
		public static final String VOLUME_SIZE_IN_GB = "sloan.ws.volume.size.gb";

		// user related properties
		public static final String USER_DISK_QUOTA_IN_GB = "user.disk.quota.in.gb";

		// scheduler related properties
		public static final String SCHEDULER_IMPL_CLASS = "scheduler.impl.class";
		public static final String SCHEDULER_MAX_NUM_ATTEMPTS = "scheduler.max.num.attempts";

		// mandatory properties
		public static final String DB_DRIVER_CLASS = "sloan.ws.db.driverclass";
		public static final String JDBC_URL = "sloan.ws.db.jdbcurl";
		public static final String DB_USER = "sloan.ws.db.user";
		public static final String DB_PWD = "sloan.ws.db.pwd";

		public static final String PORT_RANGE_MIN = "sloan.ws.port.range.min";
		public static final String PORT_RANGE_MAX = "sloan.ws.port.range.max";
		public static final String HOSTS = "sloan.ws.hosts";
	}

	private static void init(String propFileName) throws IOException {
		if (instance == null)
			return;

		/* load property file from class loader */
		Properties props = new Properties();

		/*
		 * change to Properties.loadFromXML() if the configuration file is an
		 * XML file
		 */
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream(
				propFileName));

		for (Enumeration<?> propNames = props.propertyNames(); propNames
				.hasMoreElements();) {
			String key = (String) propNames.nextElement();
			instance.setProperty(key, props.getProperty(key));
		}

	}

	public static synchronized Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();

			try {

				// try to load site specific configuration file
				init(Constants.CONFIGURATION_FILE_NAME);

			} catch (IOException e) {
				logger.error(String
						.format("Unable to load site specific configuration file %s from classpath, going to user default settings from file %s",
								Constants.CONFIGURATION_FILE_NAME,
								Constants.DEFAULT_CONFIGURATION_FILE_NAME));

				/* clear first */
				instance.clearAllProperties();

				try {
					/*
					 * The default configuration file should be bundled within
					 * the sloan-ws-1.0-SNAPSHOT.war
					 */
					init(Constants.DEFAULT_CONFIGURATION_FILE_NAME);
				} catch (IOException ioe) {

					logger.error(e.getMessage(), ioe);
				}
			}
		}

		return instance;
	}

	private void clearAllProperties() {
		properties.clear();
	}

	public String getProperty(String name) {
		return properties.get(name);
	}

	public String getProperty(String name, String defaul) {
		String res = properties.get(name);
		return (res == null) ? defaul : res;
	}

	public void setProperty(String name, String value) {
		properties.put(name, value);
	}
}
