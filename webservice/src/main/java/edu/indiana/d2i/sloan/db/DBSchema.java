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
package edu.indiana.d2i.sloan.db;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class DBSchema {
	public static String DB_NAME = "htrcvirtdb";

	public static class VmTable {
		public static String TABLE_NAME = "vms";
		public static String VM_ID = "vmid";
		public static String VM_MODE = "vmmode";
		public static String STATE = "vmstate";
		public static String SSH_PORT = "sshport";
		public static String VNC_PORT = "vncport";
		public static String WORKING_DIR = "workingdir";
		public static String IMAGE_NAME = "imagename";
		public static String POLICY_NAME = "policyname";
		
		/*
		 * the username/password pair that user uses to initially login to the
		 * launched VM. This pair is passed to web service layer from web front
		 * end and user can change the credentials later in the VM if he/she
		 * wants.
		 */
		public static String VNC_USERNAME = "vncusername";
		public static String VNC_PASSWORD = "vncpassword";
		
		public static String NUM_CPUS = "numcpus";
		public static String MEMORY_SIZE = "memorysize";
		public static String DISK_SPACE = "diskspace";

		public static String CREATED_AT = "created_at";
		public static String USERNAME = "username";
		public static String HOST = "host";

		public static Map<String, Integer> columnIndex = new HashMap<String, Integer>() {
			{
				put(VM_ID, 1);
				put(VM_MODE, 2);
				put(STATE, 3);
				put(SSH_PORT, 4);
				put(VNC_PORT, 5);
				put(WORKING_DIR, 6);
				put(IMAGE_NAME, 7);
				put(VNC_USERNAME, 8);
				put(VNC_PASSWORD, 9);
				put(NUM_CPUS, 10);
				put(MEMORY_SIZE, 11);
				put(DISK_SPACE, 12);
				put(CREATED_AT, 13);
				put(USERNAME, 14);
				put(HOST, 15);
				put(POLICY_NAME, 16);
			}
		};
	}

	public static class UserTable {
		public static String TABLE_NAME = "users";
		public static String USER_NAME = "username";
		public static String USER_EMAIL = "useremail";
		
		/* directory that hosts user's volume (disk) files */
		public static String VOLUME_DIR = "volumedir";
		
		/* remaining CPU quota */
		public static String CPU_LEFT_QUOTA = "cpuleftquota";
		
		/* remaining memory quota in MB */
		public static String MEMORY_LEFT_QUOTA = "memoryleftquota";
		
		/* remaining disk space quota in GB */
		public static String DISK_LEFT_QUOTA = "diskleftquota";
		
		/* currently all users are of the same type */
		public static String USER_TYPE = "usertype";
	}

	public static class ImageTable {
		public static String TABLE_NAME = "images";
		public static String IMAGE_NAME = "imagename";
		public static String IMAGE_STATUS = "status";
		public static String IMAGE_PATH = "imagepath";
		public static String IMAGE_LOGIN_ID = "loginusername";
		public static String IMAGE_LOGIN_PASSWORD = "loginpassword";
		public static String IMAGE_DESCRIPTION = "imagedescription";
	}
	
	public static class ResultTable {
		public static String TABLE_NAME = "results";
		public static String VM_ID = "vmid";
		public static String RESULT_ID = "resultid";
		public static String DATA_FIELD = "datafield";
		public static String NOTIFIED = "notified";
		public static String CREATE_TIME= "createtime";
		public static String NOTIFIED_TIME= "notifiedtime";
		public static String REVIEWER="reviewer";
		public static String STATUS = "status";
		public static String COMMENT = "comment";
	}

	public static class HostTable {
		public static String TABLE_NAME = "vmhosts";
		public static String HOST_NAME = "hostname";
		public static String CPU_CORES = "cpu_cores";
		public static String MEMORY_GB = "mem_gb";
	}

	public static class ActivityTable {
		public static String TABLE_NAME = "vmactivity";
		public static String ACTIVITY_ID = "id";
		public static String TIMESTAMP = "ts";
		public static String VM_ID = "vmid";
		public static String PREV_MODE = "prev_mode";
		public static String CURR_MODE = "curr_mode";
		public static String PREV_STATE = "prev_state";
		public static String CURR_STATE = "curr_state";
		public static String USERNAME = "username";
	}
}
