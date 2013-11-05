package edu.indiana.d2i.sloan.bean;

public class CreateVmRequestBean {
	private String userName, imageName, vmid, vmLoginID, vmLoginPasswd, workDir;
	private int memory, vcpu, volumeSizeInGB;

	public CreateVmRequestBean(String userName, String imageName, String vmid,
			String vmLoginID, String vmLoginPasswd, int memory, int vcpu,
			int volumeSizeInGB, String workDir) {
		this.userName = userName;
		this.imageName = imageName;
		this.vmid = vmid;
		this.vmLoginID = vmLoginID;
		this.vmLoginPasswd = vmLoginPasswd;
		this.memory = memory;
		this.vcpu = vcpu;
		this.volumeSizeInGB = volumeSizeInGB;
		this.workDir = workDir;
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

	public String getVmLoginID() {
		return vmLoginID;
	}

	public String getVmLoginPasswd() {
		return vmLoginPasswd;
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
	
	public String getWorkDir() {
		return workDir;
	}

	@Override
	public String toString() {
		return String
				.format("[username=%s, imagename=%s, vmid=%s, vmloginID=%s vmloginPasswd=%s memory=%d, vcpu=%d, volumesize=%dGB]",
						userName, imageName, vmid, vmLoginID, vmLoginPasswd,
						memory, vcpu, volumeSizeInGB);
	}

}