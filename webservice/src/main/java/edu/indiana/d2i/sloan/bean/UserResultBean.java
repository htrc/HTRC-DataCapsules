package edu.indiana.d2i.sloan.bean;

public class UserResultBean {
	private String username, useremail, resultId;
	
	public UserResultBean(String username, String useremail, String resultId) {
		this.username = username;
		this.useremail = useremail;
		this.resultId = resultId;
	}

	public String getUsername() {
		return username;
	}

	public String getUseremail() {
		return useremail;
	}

	public String getResultId() {
		return resultId;
	}
}
