package edu.indiana.d2i.sloan.bean;

public class VmStatusBean {
	private final VmInfoBean vminfo;
	
	public VmStatusBean(VmInfoBean vminfo) {
		this.vminfo = vminfo;
	}
	
	public String getMode() {
		return vminfo.getVmmode().toString();
	}
	public String getState() {
		return vminfo.getVmstate().toString();
	}
	public String getPublicip() {
		return vminfo.getPublicip();
	}
	public int getVncport() {
		return vminfo.getVncport();
	}
	public int getSshport() {
		return vminfo.getSshport();
	}
	public String getVmid() {
		return vminfo.getVmid();
	}
	public String getVmInitialLoginId() {
		return vminfo.getVmloginId();
	}
	public String getVmInitialLoginPassword() {
		return vminfo.getVmloginPwd();
	}
	public int getVCPUs() {
		return vminfo.getNumCPUs();
	}
	public int getMemSize() {
		return vminfo.getMemorySize();
	}
	public int getVolumeSize() {
		return vminfo.getVolumeSize();
	}
	public String getImageName() {
		return vminfo.getImageName();
	}
}
