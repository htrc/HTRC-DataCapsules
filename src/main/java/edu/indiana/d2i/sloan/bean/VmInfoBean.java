package edu.indiana.d2i.sloan.bean;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class VmInfoBean {
	private String vmid, publicip, workDir, imagepath, policypath;
	private int sshport, vncport;
	private VMMode vmmode; 
	private VMState vmstate;
	
	public VmInfoBean(String vmid, String publicip, String workDir, 
		String imagepath, String policypath, int sshport, int vncport,
		VMMode vmmode, VMState vmstate) {
		this.vmid = vmid;
		this.publicip = publicip;
		this.workDir = workDir;
		this.imagepath = imagepath;
		this.policypath = policypath;
		this.sshport = sshport;
		this.vncport = vncport;
		this.vmmode = vmmode;
		this.vmstate = vmstate;
	}
	
	public String getVmid() {
		return vmid;
	}
	public String getPublicip() {
		return publicip;
	}
	public String getWorkDir() {
		return workDir;
	}
	public int getSshport() {
		return sshport;
	}
	public int getVncport() {
		return vncport;
	}
	public VMMode getVmmode() {
		return vmmode;
	}
	public VMState getVmstate() {
		return vmstate;
	}

	public String getImagepath() {
		return imagepath;
	}

	public String getPolicypath() {
		return policypath;
	}
}
