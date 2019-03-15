package edu.indiana.d2i.sloan.bean;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
//import sun.jvm.hotspot.runtime.VM;

/**
 * Created by liang on 9/21/16.
 */
public class VmKeyInfoBean {
	private String vmid, username, userEmail, type;
	private int numCPUs, memorySize;
	private VMMode vmmode;
	private VMState vmState;
	private String host, created_at;
	private int numHostCPUCores;
	private int numHostMemoryGB;

	public VmKeyInfoBean(String vmid, String username, String userEmail,
						 int numCPUs, int memorySize, VMMode vmmode, VMState vmState,
						 String host, String created_at, int numHostCPUCores, int numHostMemoryGB, String type) {
		this.vmid = vmid;
		this.username = username;
		this.userEmail = userEmail;
		this.numCPUs = numCPUs;
		this.memorySize = memorySize;
		this.vmmode = vmmode;
		this.vmState = vmState;
		this.host = host;
		this.created_at = created_at;
		this.numHostCPUCores = numHostCPUCores;
		this.numHostMemoryGB = numHostMemoryGB;
		this.type = type;
	}

	public int getNumHostCPUCores() {
		return numHostCPUCores;
	}

	public int getNumHostMemoryGB() {
		return numHostMemoryGB;
	}

	public String getHost() {
		return host;
	}

	public String getCreated_at() {
		return created_at;
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

	public String getType() {
		return type;
	}
}

