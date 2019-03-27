package edu.indiana.d2i.sloan.bean;

import java.util.List;

public class UserResultBean {
	private String username, useremail, resultId, vmId;
	private List<VmUserRole> roles;

	public UserResultBean(String username, String useremail, String resultId, String vmId, List<VmUserRole> roles) {
		this.username = username;
		this.useremail = useremail;
		this.resultId = resultId;
		this.vmId = vmId;
		this.roles = roles;
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

	public String getVmId() {
		return vmId;
	}

	public List<VmUserRole> getRoles() {
		return this.roles;
	}
}
