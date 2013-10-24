package edu.indiana.d2i.sloan.db;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
class DBSchema {
	public static String DB_NAME = "vmdb";

	public static class VmTable {
		public static String TABLE_NAME = "vms";
		public static String VM_ID = "vmid";
		public static String VM_MODE = "vmmode";
		public static String STATE = "vmstate";
		public static String PUBLIC_IP = "publicip";
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
		public static String VM_USERNAME = "vmusername";
		public static String VM_PASSWORD = "vmpassword";
		
		public static String NUM_CPUS = "numcpus";
		public static String MEMORY_SIZE = "memorysize";
		public static String DISK_SPACE = "diskspace";
		
		public static Map<String, Integer> columnIndex = new HashMap<String, Integer>() {
			{
				put(VM_ID, 1);
				put(VM_MODE, 2);
				put(STATE, 3);
				put(PUBLIC_IP, 4);
				put(SSH_PORT, 5);
				put(VNC_PORT, 6);
				put(WORKING_DIR, 7);
				put(IMAGE_NAME, 8);
				put(VM_USERNAME, 9);
				put(VM_PASSWORD, 10);
				put(NUM_CPUS, 11);
				put(MEMORY_SIZE, 12);
				put(DISK_SPACE, 13);
				put(POLICY_NAME, 14);
			}
		};
	}

	public static class UserTable {
		public static String TABLE_NAME = "users";
		public static String USER_NAME = "username";
		
		/* directory that hosts user's volume (disk) files */
		public static String VOLUME_DIR = "volumedir";
		
		/* remaining CPU quota */
		public static String CPU_LEFT_QUOTA = "cpuLeftQuota";
		
		/* remaining memory quota in MB */
		public static String MEMORY_LEFT_QUOTA = "memoryLeftQuota";
		
		/* remaining disk space quota in GB */
		public static String DISK_LEFT_QUOTA = "diskLeftQuota";
		
		/* currently all users are of the same type */
		public static String USER_TYPE = "usertype";
	}

	public static class UserVmTable {
		public static String TABLE_NAME = "uservm";
		public static String USER_NAME = "username";
		public static String VM_ID = "vmid";
	}
	
	public static class ImageTable {
		public static String TABLE_NAME = "images";
		public static String IMAGE_NAME = "imagename";
		public static String IMAGE_PATH = "imagepath";
	}
}
