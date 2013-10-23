package edu.indiana.d2i.sloan.vm;

public class VMPorts {
	public String host;
	public int vncport, sshport;
	
	public VMPorts(String host, int sshport, int vncport) {
		this.host = host;
		this.vncport = vncport; 
		this.sshport = sshport;
	}
}
