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
		public static Map<String, Integer> columnIndex = new HashMap<String, Integer>(){{
	        put(VM_ID, 1); put(VM_MODE, 2); put(STATE, 3); put(PUBLIC_IP, 4); 
	        put(SSH_PORT, 5); put(VNC_PORT, 6); put(WORKING_DIR, 7); put(IMAGE_NAME, 8); 
	    }};
	}
	
	public static class UserTable {
		public static String TABLE_NAME = "users";
		public static String USER_NAME = "username";
		public static String USER_TYPE = "usertype";		
	}
	
	public static class UserVmTable {
		public static String TABLE_NAME = "uservm";
		public static String USER_NAME = "username";
		public static String VM_ID = "vmid";		
	}
}
