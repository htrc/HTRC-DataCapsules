package edu.indiana.d2i.sloan.bean;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class UserVmInfoBean {

	private String vmid, publicip, initialVmloginId, initialVmloginPwd,
			imagename;
	private int sshport, vncport, numCPUs, memorySize, diskSpace;
	private VMMode vmmode;
	private VMState vmstate;

	public UserVmInfoBean(String vmid, String publicip,
			String initialVmloginId, String initialVmloginPwd,
			String imagename, int sshport, int vncport, int numCPUs,
			int memorySize, int diskSpace, VMMode vmmode, VMState vmstate) {
		super();
		this.vmid = vmid;
		this.publicip = publicip;
		this.initialVmloginId = initialVmloginId;
		this.initialVmloginPwd = initialVmloginPwd;
		this.imagename = imagename;
		this.sshport = sshport;
		this.vncport = vncport;
		this.numCPUs = numCPUs;
		this.memorySize = memorySize;
		this.diskSpace = diskSpace;
		this.vmmode = vmmode;
		this.vmstate = vmstate;
	}

	/**
	 * method that transforms VmInfoBean to UserVmInfoBean since we don't want
	 * to expose certain info to user, e.g. working directory, image path and
	 * firewall policy
	 * 
	 * @param vmInfoBean
	 * @return
	 */
	public static UserVmInfoBean fullVmInfo2UserInfo(VmInfoBean vmInfoBean) {
		return new UserVmInfoBean(vmInfoBean.getVmid(),
				vmInfoBean.getPublicip(), vmInfoBean.getVmloginId(),
				vmInfoBean.getVmloginPwd(), vmInfoBean.getImageName(),
				vmInfoBean.getSshport(), vmInfoBean.getVncport(),
				vmInfoBean.getNumCPUs(), vmInfoBean.getMemorySize(),
				vmInfoBean.getVolumeSize(), vmInfoBean.getVmmode(),
				vmInfoBean.getVmstate());
	}

	public String getVmid() {
		return vmid;
	}

	public void setVmid(String vmid) {
		this.vmid = vmid;
	}

	public String getPublicip() {
		return publicip;
	}

	public void setPublicip(String publicip) {
		this.publicip = publicip;
	}

	public String getInitialVmloginId() {
		return initialVmloginId;
	}

	public void setInitialVmloginId(String initialVmloginId) {
		this.initialVmloginId = initialVmloginId;
	}

	public String getInitialVmloginPwd() {
		return initialVmloginPwd;
	}

	public void setInitialVmloginPwd(String initialVmloginPwd) {
		this.initialVmloginPwd = initialVmloginPwd;
	}

	public String getImagename() {
		return imagename;
	}

	public void setImagename(String imagename) {
		this.imagename = imagename;
	}

	public int getSshport() {
		return sshport;
	}

	public void setSshport(int sshport) {
		this.sshport = sshport;
	}

	public int getVncport() {
		return vncport;
	}

	public void setVncport(int vncport) {
		this.vncport = vncport;
	}

	public int getNumCPUs() {
		return numCPUs;
	}

	public void setNumCPUs(int numCPUs) {
		this.numCPUs = numCPUs;
	}

	public int getMemorySize() {
		return memorySize;
	}

	public void setMemorySize(int memorySize) {
		this.memorySize = memorySize;
	}

	public int getDiskSpace() {
		return diskSpace;
	}

	public void setDiskSpace(int diskSpace) {
		this.diskSpace = diskSpace;
	}

	public VMMode getVmmode() {
		return vmmode;
	}

	public void setVmmode(VMMode vmmode) {
		this.vmmode = vmmode;
	}

	public VMState getVmstate() {
		return vmstate;
	}

	public void setVmstate(VMState vmstate) {
		this.vmstate = vmstate;
	}

}
