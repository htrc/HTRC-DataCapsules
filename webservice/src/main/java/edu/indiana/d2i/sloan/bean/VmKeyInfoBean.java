package edu.indiana.d2i.sloan.bean;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import sun.jvm.hotspot.runtime.VM;

/**
 * Created by liang on 9/21/16.
 */
public class VmKeyInfoBean {
	private String vmid, username, userEmail;
	private int numCPUs, memorySize;
	private VMMode vmmode;
	private VMState vmState;

	public VmKeyInfoBean(String vmid, String username, String userEmail, int numCPUs, int memorySize, VMMode vmmode, VMState vmState) {
		this.vmid = vmid;
		this.username = username;
		this.userEmail = userEmail;
		this.numCPUs = numCPUs;
		this.memorySize = memorySize;
		this.vmmode = vmmode;
		this.vmState = vmState;
	}

	public String getVmid() {
		return vmid;
	}

	public String getUsername() {
		return username;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public int getNumCPUs() {
		return numCPUs;
	}

	public int getMemorySize() {
		return memorySize;
	}

	public VMMode getVmmode() {
		return vmmode;
	}

	public VMState getVmState() {
		return vmState;
	}
}

