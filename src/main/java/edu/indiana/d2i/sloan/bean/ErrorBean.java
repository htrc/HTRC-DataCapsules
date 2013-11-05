package edu.indiana.d2i.sloan.bean;

public class ErrorBean {
	private int statusCode = 400;
	private String description = null;

	public ErrorBean(int statusCode, String description) {
		this.statusCode = statusCode;
		this.description = description;
	}

	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
