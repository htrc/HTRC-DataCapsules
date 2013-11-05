package edu.indiana.d2i.sloan.vm;

public class VMPorts {
	public String publicip;
	public int vncport, sshport;
	
	public VMPorts(String publicip, int sshport, int vncport) {
		this.publicip = publicip;
		this.vncport = vncport; 
		this.sshport = sshport;
	}
	
	@Override
	public String toString() {
		return String.format("[publicip:%s, sshport:%s, vncport:%s]", publicip, sshport, vncport);
	}
}
