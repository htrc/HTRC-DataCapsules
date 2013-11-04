package edu.indiana.d2i.sloan.bean;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class VmInfoBean {
	private String vmid, publicip, workDir, imagepath, policypath, vmloginId,
			vmloginPwd, imagename, policyname;
	private int sshport, vncport, numCPUs, memorySize, volumeSize;
	private VMMode vmmode;
	private VMMode requestedVMMode;
	private VMState vmstate;

	public void setImagePath(String imagePath) {
		this.imagepath = imagePath;
	}
	
	public void setRequestedVMMode(VMMode requestedVMMode) {
		this.requestedVMMode = requestedVMMode;
	}
	
	public void setVmState(VMState vmstate) {
		this.vmstate = vmstate;
	}
	
	public void setPolicypath(String policypath) {
		this.policypath = policypath;
	}

	public VmInfoBean(String vmid, String publicip, String workDir,
			String imagepath, String policypath, int sshport, int vncport,
			int numCPUs, int memorySize, int diskSpace, VMMode vmmode,
			VMState vmstate, String vmloginId, String vmloginPwd,
			String imagename, String policyname, VMMode requestedVMMode) {
		this.vmid = vmid;
		this.publicip = publicip;
		this.workDir = workDir;
		this.imagepath = imagepath;
		this.policypath = policypath;
		this.sshport = sshport;
		this.vncport = vncport;
		this.numCPUs = numCPUs;
		this.memorySize = memorySize;
		this.volumeSize = diskSpace;
		this.vmmode = vmmode;
		this.vmstate = vmstate;
		this.vmloginId = vmloginId;
		this.vmloginPwd = vmloginPwd;
		this.imagename = imagename;
		this.policyname = policyname;
		this.requestedVMMode = requestedVMMode;
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

	public String getVmloginId() {
		return vmloginId;
	}

	public String getVmloginPwd() {
		return vmloginPwd;
	}

	public int getNumCPUs() {
		return numCPUs;
	}

	public int getMemorySize() {
		return memorySize;
	}

	public int getVolumeSize() {
		return volumeSize;
	}

	public String getPolicyName() {
		return policyname;
	}

	public String getImageName() {
		return imagename;
	}

	public VMMode getRequestedVMMode() {
		return requestedVMMode;
	}
	
	@Override
	public String toString() {
		return String.format("[vmid=%s, publicip=%s, workDir=%s, imagename=%s, " +
			"imagepath=%s, sshport=%d, vncport=%d, vmloginId=%s, vmloginPwd=%s, " +
			"numCPUs=%d, memorySize=%d, volumeSize=%d, policypath=%s, vmmode=%s," +
			"vmstate=%s]",  vmid, publicip, workDir, imagename, imagepath, sshport,
			vncport, vmloginId, vmloginPwd, numCPUs, memorySize, volumeSize, policypath,
			vmmode.toString(), vmstate.toString());
	}
}
