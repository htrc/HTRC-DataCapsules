package edu.indiana.d2i.sloan.bean;

public class VmInfoBean {
	private int sshport, vncport;
	private String workDir;
	private CreateVmRequestBean request;
	
	public VmInfoBean(CreateVmRequestBean request, int sshport, int vncport, String workDir) {
		this.request = request;
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
	
	@Override
	public String toString() {
		return "";
	}
}
