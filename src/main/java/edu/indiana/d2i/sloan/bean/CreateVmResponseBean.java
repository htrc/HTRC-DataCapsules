package edu.indiana.d2i.sloan.bean;

public class CreateVmResponseBean {
	private String vmid;

	public CreateVmResponseBean(String vmid) {
		this.vmid = vmid;
	}

	public String getVmid() {
		return vmid;
	}

	public void setVmid(String vmid) {
		this.vmid = vmid;
	}

	@Override
	public String toString() {
		return String.format("createvm response [vmid=%s]", vmid);
	}
}
