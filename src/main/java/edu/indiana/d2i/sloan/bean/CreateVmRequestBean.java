package edu.indiana.d2i.sloan.bean;

public class CreateVmRequestBean {
	private String userName, imageName, vmid;
	private int memory, vcpu, volumeSizeInGB;

	public CreateVmRequestBean(String userName, String imageName, 
		String vmid, int memory, int vcpu, int volumeSizeInGB) {
		this.userName = userName;
		this.imageName = imageName;
		this.vmid = vmid;
		this.memory = memory;
		this.vcpu = vcpu;
		this.volumeSizeInGB = volumeSizeInGB;
	}

	public String getUserName() {
		return userName;
	}

	public String getImageName() {
		return imageName;
	}

	public String getVmId() {
		return vmid;
	}
	
	public int getMemory() {
		return memory;
	}

	public int getVcpu() {
		return vcpu;
	}

	public int getVolumeSizeInGB() {
		return volumeSizeInGB;
	}
	


	@Override
	public String toString() {
		return String.format("[username=%s, imagename=%s, vmid=%s, memory=%d, vcpu=%d, volumesize=%dGB]",
			userName, imageName, vmid, memory, vcpu, volumeSizeInGB);
	}
}
