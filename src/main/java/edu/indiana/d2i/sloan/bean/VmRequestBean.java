package edu.indiana.d2i.sloan.bean;

public class VmRequestBean {
	private int sshport, vncport;
	private String workDir, publicip;
	private CreateVmRequestBean request;
	
	public VmRequestBean(CreateVmRequestBean request, String publicip, int sshport, 
		int vncport, String workDir) {
		this.request = request;
		this.publicip = publicip;
		this.sshport = sshport;
		this.vncport = vncport;
		this.workDir = workDir;
	}
	
	public String getUserName() {
		return request.getUserName();
	}

	public String getImageName() {
		return request.getImageName();
	}

	public String getVmId() {
		return request.getVmId();
	}
	
	public int getMemory() {
		return request.getMemory();
	}

	public int getVcpu() {
		return request.getVcpu();
	}

	public int getVolumeSizeInGB() {
		return request.getVolumeSizeInGB();
	}
	
	public int getSshPort() {
		return sshport;
	}
	
	public int getVncPort() {
		return vncport;
	}
	
	public String getWorkDir() {
		return workDir;
	}
	
	public String getPublicIp() {
		return publicip;
	}
	
	public String getLoginPassword() {
		return request.getVmLoginPasswd();
	}
	
	public String getLoginID() {
		return request.getVmLoginID();
	}
	
	@Override
	public String toString() {
		return "";
	}
}
