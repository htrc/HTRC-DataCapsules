package edu.indiana.d2i.sloan;

import java.util.HashMap;
import java.util.Map;

public final class Configuration {
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
		
		// mandatory properties
		public static final String DB_DRIVER_CLASS = "sloan.ws.db.driverclass";
		public static final String JDBC_URL = "sloan.ws.db.jdbcurl";
		public static final String DB_USER = "sloan.ws.db.user";
		public static final String DB_PWD = "sloan.ws.db.pwd";
		
		public static final String PORT_RANGE_MIN = "sloan.ws.port.range.min";
		public static final String PORT_RANGE_MAX = "sloan.ws.port.range.max";
		public static final String HOSTS = "sloan.ws.hosts";
	}

	public static synchronized Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
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
