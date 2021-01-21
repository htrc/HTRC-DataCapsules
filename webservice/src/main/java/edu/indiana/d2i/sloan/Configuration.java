/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class Configuration {
	private static Configuration instance = null;
	private static Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	private void loadConfigurations(String xmlPath) {
		try {
		    XMLConfiguration config = new XMLConfiguration(xmlPath);
		    int size = config.getList("property.name").size();
		    for (int i = 0; i < size; i++) {
		    	HierarchicalConfiguration sub = config.configurationAt(
		    		String.format("property(%d)", i));
		    	String name = sub.getString("name");
		    	String val = sub.getString("value");
		    	properties.put(name, val);
		    }
		} catch(ConfigurationException cex) {
		    throw new RuntimeException(cex);
		}
	}
	
	private Configuration() {
		properties = new HashMap<String, String>();
		loadConfigurations("default.xml");
		String sitesXmlPath = properties.get("sites.xml.path");
		loadConfigurations(sitesXmlPath);
		logger.debug(properties.toString());
	}

	private Map<String, String> properties = null;

	public static class PropertyName {
		// classes serve as resources
		public static final String RESOURCES_NAMES = "sloan.ws.resources.names";
		
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
		public static final String SSH_USERNAME = "host.ssh.username";
		public static final String SSH_PASSWD = "host.ssh.passwd";
		public static final String SSH_PRIVATE_KEY_PATH = "host.ssh.private.key.path";

		/* timeout in milliseconds */
		public static final String HYPERVISOR_TASK_TIMEOUT = "hypervisor.task.timeout.in.ms";

		/* hypervisor commands */
		public static final String CMD_CREATE_VM = "cmd.create.vm";
		public static final String CMD_LAUNCH_VM = "cmd.launch.vm";
		public static final String CMD_QUERY_VM = "cmd.query.vm";
		public static final String CMD_SWITCH_VM = "cmd.switch.vm";
		public static final String CMD_STOP_VM = "cmd.stop.vm";
		public static final String CMD_DELETE_VM = "cmd.delete.vm";
		public static final String CMD_UPDATE_KEY = "cmd.update.key";
		public static final String MIGRATE_VM = "cmd.migrate.vm";
		public static final String CMD_DELETE_KEY = "cmd.delete.key";

		/* hypervisor fire wall policy */
		public static final String MAINTENANCE_FIREWALL_POLICY = "hypervisor.fw.maintenance";
		public static final String SECURE_FIREWALL_POLICY = "hypervisor.fw.secure";

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

		/* properties for random fail hypervisor */

		/* value between [0.0 1.0] */
		public static final String RFHYPER_RANDOM_FAIL_PROB = "rfhyper.random.fail.prob";
		/* value between [0.0 1.0] */
		public static final String RFHYPER_RANDOM_EXP_PROB = "rfhyper.random.exp.prob";

		public static final String HYPERVISOR_FULL_CLASS_NAME = "hypervisor.full.class.name";
		
		/* vm working directory prefix */
		public static final String DEFAULT_VM_WORKDIR_PREFIX = "sloan.ws.vm.workdir.prefix";
		
		/* email setting */
		public static final String EMAIL_SENDERNAME = "email.sendername";
		public static final String EMAIL_SENDER_ADDR = "email.sender.addr";
		public static final String EMAIL_PASSWORD = "email.password";
		public static final String EMAIL_SMTP_HOST = "email.smtp.host";
		public static final String EMAIL_SMTP_PORT = "email.smtp.port";
		
		/* result relative */
		public static final String RESULT_DOWNLOAD_URL_PREFIX = "result.download.prefix";
		public static final String RESULT_EXPIRE_IN_SECOND = "result.expire.sec";
		public static final String RESULT_HUMAN_REVIEW = "result.review.human";
		public static final String RESULT_HUMAN_REVIEW_EMAIL = "result.review.email";
		public static final String RESULT_FILES_DIR = "result.files.dir";
		public static final String RESULT_BACKUP_FILES_DIR = "result.backup.file.dir";

		public static final String THREAD_SLEEP_DURATION = "ssh.proxy.thread.sleep.duration";
		
		/*support user email and guid*/
		public static final String SUPPORT_USER_EMAIL = "support.user.email";
		public static final String SUPPORT_USER_GUID = "support.user.guid";
		
		/*No of sharees*/
		public static final String MAX_NO_OF_SHAREES = "max.no.of.sharees";

	}
	
	public static synchronized Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}

	public String getString(String name) {
		return properties.get(name);
	}

	public String getString(String name, String defaultVal) {
		String res = properties.get(name);
		return (res == null) ? defaultVal : res;
	}
	
	public int getInt(String name) {
		return Integer.parseInt(getString(name));
	}
	
	public int getInt(String name, int defaultVal) {
		return Integer.parseInt(getString(name, String.valueOf(defaultVal)));
	}
	
	public long getLong(String name) {
		return Long.parseLong(getString(name));
	}
	
	public long getLong(String name, long defaultVal) {
		return Long.parseLong(getString(name, String.valueOf(defaultVal)));
	}
	
	public boolean getBoolean(String name) {
		return Boolean.parseBoolean(getString(name));
	}
	
	public boolean getBoolean(String name, boolean defaultVal) {
		return Boolean.parseBoolean(getString(name, String.valueOf(defaultVal)));
	}
	
	/** unit test purpose */
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}
}
