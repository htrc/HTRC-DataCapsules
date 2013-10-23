package edu.indiana.d2i.sloan;

public class Constants {
	/** http header */
	public static final String USER_NAME = "htrc-remote-user";

	/** hypervisor relative variables */
	public static final String DEFAULT_HYPER_WORKERS = "1024";
	public static final String DEFAULT_HYPER_MAX_RETRY = "3";
	public static final String DEFAULT_HYPER_TIMEOUT_SEC = "30";
	public static final String DEFAULT_VOLUME_SIZE_IN_GB = "10";

	/** database relative variables */

	/* user related parameters */
	public static final String DEFAULT_USER_DISK_QUOTA_IN_GB = "300";

	/* scheduler related properties */
	public static final String DEFAULT_SCHEDULER_IMPL_CLASS = "edu.indiana.d2i.sloan.scheduler.RoundRobinScheduler";
	public static final String DEFAULT_SCHEDULER_MAX_NUM_ATTEMPTS = "5";

	/* configuration file name */
	public static final String CONFIGURATION_FILE_NAME = "sloan-site-conf.properties";
	public static final String DEFAULT_CONFIGURATION_FILE_NAME = "default-sloan-conf.properties";

	public static final String DEFAULT_VM_WORKINGDIR_PREFIX = "/var/instance/";

}
