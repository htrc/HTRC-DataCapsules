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
package edu.indiana.d2i.sloan.internal;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import edu.indiana.d2i.sloan.vm.VMMode;

@SuppressWarnings("serial")
public abstract class HypervisorCmdSimulator extends CommandSimulator {
	public static String ip = null;

	public static final String VM_INFO_FILE_NAME = "vminfo.txt";

	public static enum ERROR_STATE {
		INVALID_INPUT_ARGS, IMAGE_NOT_EXIST, NOT_ENOUGH_CPU, 
		NOT_ENOUGH_MEM, IO_ERR, VM_NOT_EXIST, FIREWALL_POLICY_NOT_EXIST, 
		INVALID_VM_MODE, VM_STATE_FILE_NOT_FOUND, VM_ALREADY_IN_REQUESTED_MODE,
		VM_NOT_RUNNING, VM_NOT_SHUTDOWN, VM_ALREADY_EXIST
	}

	/* key is error type enumeration, value is error code */
	public static Map<ERROR_STATE, Integer> ERROR_CODE;

	/* command line flag key */
	public static enum CMD_FLAG_KEY {
		IMAGE_PATH, VCPU, MEM, WORKING_DIR, 
		VNC_PORT, SSH_PORT, LOGIN_USERNAME, 
		LOGIN_PASSWD, POLICY_PATH, VM_MODE, VM_STATE
	}

	public static Map<CMD_FLAG_KEY, String> CMD_FLAG_VALUE;

	static {

		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			ip = "Unavailable";
		}

		/* code 1 is reserved for uncaught runtime exception */
		ERROR_CODE = new HashMap<ERROR_STATE, Integer>() {
			{
				put(ERROR_STATE.INVALID_INPUT_ARGS, 2);
				put(ERROR_STATE.IMAGE_NOT_EXIST, 3);
				put(ERROR_STATE.NOT_ENOUGH_CPU, 4);
				put(ERROR_STATE.NOT_ENOUGH_MEM, 5);
				put(ERROR_STATE.IO_ERR, 6);
				put(ERROR_STATE.VM_NOT_EXIST, 7);
				put(ERROR_STATE.FIREWALL_POLICY_NOT_EXIST, 8);
				put(ERROR_STATE.INVALID_VM_MODE, 9);
				put(ERROR_STATE.VM_STATE_FILE_NOT_FOUND, 10);
				put(ERROR_STATE.VM_ALREADY_IN_REQUESTED_MODE, 11);
				put(ERROR_STATE.VM_NOT_RUNNING, 12);
				put(ERROR_STATE.VM_NOT_SHUTDOWN, 13);
				put(ERROR_STATE.VM_ALREADY_EXIST, 14);
			}
		};

		CMD_FLAG_VALUE = new HashMap<CMD_FLAG_KEY, String>() {
			{
				put(CMD_FLAG_KEY.IMAGE_PATH, "image");
				put(CMD_FLAG_KEY.VCPU, "vcpu");
				put(CMD_FLAG_KEY.MEM, "mem");
				put(CMD_FLAG_KEY.WORKING_DIR, "wdir");
				put(CMD_FLAG_KEY.VNC_PORT, "vnc");
				put(CMD_FLAG_KEY.SSH_PORT, "ssh");
				put(CMD_FLAG_KEY.LOGIN_PASSWD, "loginid");
				put(CMD_FLAG_KEY.LOGIN_USERNAME, "loginpwd");
				put(CMD_FLAG_KEY.POLICY_PATH, "policy");
				put(CMD_FLAG_KEY.VM_MODE, "mode");
				put(CMD_FLAG_KEY.VM_STATE, "state");
			}
		};
	}

	/* check whether a file or directory exist */
	public static boolean resourceExist(String filePath) {
		return new File(filePath).exists();
	}

	public static String cleanPath(String path) {
		if (path.endsWith(File.separator))
			return path;

		return path + File.separator;
	}

	public static VMMode getVMMode(String modeStr) {
		modeStr = modeStr.trim();
		if (VMMode.MAINTENANCE.toString().equalsIgnoreCase(modeStr))
			return VMMode.MAINTENANCE;

		if (VMMode.SECURE.toString().equalsIgnoreCase(modeStr))
			return VMMode.SECURE;

		return null;
	}
}
