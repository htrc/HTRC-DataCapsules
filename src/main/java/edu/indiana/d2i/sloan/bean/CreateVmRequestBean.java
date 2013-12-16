package edu.indiana.d2i.sloan.bean;

public class CreateVmRequestBean {
	private String userName, imageName, vmid, vncLoginID, vncLoginPasswd, workDir;
	private int memory, vcpu, volumeSizeInGB;

	public CreateVmRequestBean(String userName, String imageName, String vmid,
			String vncLoginID, String vncLoginPasswd, int memory, int vcpu,
			int volumeSizeInGB, String workDir) {
		this.userName = userName;
		this.imageName = imageName;
		this.vmid = vmid;
		this.vncLoginID = vncLoginID;
		this.vncLoginPasswd = vncLoginPasswd;
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

	public String getVncLoginID() {
		return vncLoginID;
	}

	public String getVncLoginPasswd() {
		return vncLoginPasswd;
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
						userName, imageName, vmid, vncLoginID, vncLoginPasswd,
						memory, vcpu, volumeSizeInGB);
	}

}