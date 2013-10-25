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

		// user related properties
		public static final String USER_DISK_QUOTA_IN_GB = "user.disk.quota.in.gb";
		public static final String USER_CPU_QUOTA_IN_NUM = "user.cpu.quota.in.num";
		public static final String USER_MEMORY_QUOTA_IN_MB = "user.memory.quota.in.mb";

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

		// hypervisor related properties

		/* credentials for ssh */
		public static final String SSH_USERNAME = "ssh.username";
		public static final String SSH_PASSWD = "ssh.passwd";
		public static final String SSH_PRIVATE_KEY_PATH = "ssh.private.key.path";

		/* timeout in milliseconds */
		public static final String HYPERVISOR_TASK_TIMEOUT = "hypervisor.task.timeout";

		/* hypervisor commands */
		public static final String CMD_CREATE_VM = "cmd.create.vm";
		public static final String CMD_LAUNCH_VM = "cmd.launch.vm";
		public static final String CMD_QUERY_VM = "cmd.query.vm";
		public static final String CMD_SWITCH_VM = "cmd.switch.vm";
		public static final String CMD_STOP_VM = "cmd.stop.vm";
		public static final String CMD_DELETE_VM = "cmd.delete.vm";

		/* delimiter used for the key-value pair by internal API response */
		public static final String RESP_KV_DELIMITER = "resp.kv.delimiter";
		public static final String RESP_VM_STATUS_KEY = "resp.vm.status.key";

		/* properties related with retriable task */
		public static final String USE_RETRY_TASK = "use.retry.task";
		public static final String RETRY_TASK_WAIT_IN_MILLIS = "retry.task.wait.in.millis";
		public static final String RETRY_TASK_MAX_ATTEMPT = "retry.task.max.attempt";
		/*
		 * semicolon separated list of fully qualified class names of exceptions
		 * that can be retried
		 */
		public static final String RETRY_TASK_RETRIABLE_EXPS = "retry.task.retriable.exps";
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
