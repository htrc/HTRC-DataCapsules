package edu.indiana.d2i.sloan.bean;

public class PolicyInfoBean {
	private String name, path;

	public PolicyInfoBean( String name, String path) {
		this.name = name;
		this.path = path;
	}
	
	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}
}
