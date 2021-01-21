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

public class Constants {
	/** http header */
	public static final String USER_NAME = "htrc-remote-user";
	public static final String USER_EMAIL = "htrc-remote-user-email";
	public static final String OPERATOR = "htrc-remote-operator";
	public static final String OPERATOR_EMAIL = "htrc-remote-operator-email";

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

	public static final int DEFAULT_MAX_NO_OF_SHAREES = 6;

}
