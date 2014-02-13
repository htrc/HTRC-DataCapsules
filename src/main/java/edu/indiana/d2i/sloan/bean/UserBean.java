package edu.indiana.d2i.sloan.bean;

public class UserBean {
	private String username, useremail;
	
	public UserBean(String username, String useremail) {
		this.username = username;
		this.useremail = useremail;
	}
	
	public String getUserName() {
		return username;
	}
	
	public String getUserEmail() {
		return useremail;
	}
	
	@Override
	public String toString() {
		return String.format("[username: %s]", username, useremail);
	}
}
