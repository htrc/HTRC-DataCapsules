package edu.indiana.d2i.sloan;

public class Constants {
	/** http header */
	public static final String USER_NAME = "htrc-remote-user";
	public static final String USER_EMAIL = "htrc-remote-user-email";

	/** hypervisor relative variables */
	public static final String DEFAULT_HYPER_WORKERS = "1024";
	public static final String DEFAULT_HYPER_MAX_RETRY = "3";
	public static final String DEFAULT_HYPER_TIMEOUT_SEC = "30";
	public static final String DEFAULT_VOLUME_SIZE_IN_GB = "10";

	/** database relative variables */

	/* user related parameters */
	public static final String DEFAULT_USER_DISK_QUOTA_IN_GB = "300";
	public static final String DEFAULT_USER_CPU_QUOTA_IN_NUM = "20";
	public static final String DEFAULT_USER_MEMORY_QUOTA_IN_MB = "20480";

	/* scheduler related properties */
	public static final String DEFAULT_SCHEDULER_IMPL_CLASS = "edu.indiana.d2i.sloan.scheduler.RoundRobinScheduler";
	public static final String DEFAULT_SCHEDULER_MAX_NUM_ATTEMPTS = "5";

	/* hypervisor related properties */
	/* timeout in milliseconds */
	public static final String DEFAULT_HYPERVISOR_TASK_TIMEOUT = "3000";

	public static final String DEFAULT_HYPERVISOR_FULL_CLASS_NAME = "edu.indiana.d2i.sloan.hyper.CapsuleHypervisor";

}
