package edu.indiana.d2i.sloan.bean;

public class VmStatusBean {
	private String vmid, mode, state, publicip;
	private int vncport, sshport;
	
	public VmStatusBean(String vmid, String mode, String state, 
		String publicip, int sshport, int vncport) {
		this.vmid = vmid;
		this.mode = mode;
		this.state = state;
		this.publicip = publicip;
		this.sshport = sshport;
		this.vncport = vncport;
	}
	
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getPublicip() {
		return publicip;
	}
	public void setPublicip(String publicip) {
		this.publicip = publicip;
	}
	public int getVncport() {
		return vncport;
	}
	public void setVncport(int vncport) {
		this.vncport = vncport;
	}
	public int getSshport() {
		return sshport;
	}
	public void setSshport(int sshport) {
		this.sshport = sshport;
	}
	public String getVmid() {
		return this.vmid;
	}
	
	@Override
	public String toString() {
		return String.format("vmstatus [vmid=%s, mode=%s, state=%s, publicip=%s, vncport=%d, sshport=%d]", 
			vmid, mode, state, publicip, vncport, sshport);
	}
}
