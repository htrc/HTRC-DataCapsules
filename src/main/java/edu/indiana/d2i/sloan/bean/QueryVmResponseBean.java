package edu.indiana.d2i.sloan.bean;

import java.util.List;

public class QueryVmResponseBean {
	private List<VmStatusBean> status;
	
	public QueryVmResponseBean(List<VmStatusBean> status) {
		this.status = status;
	}
	
	public List<VmStatusBean> getStatus() {
		return status;
	}
	
	@Override
	public String toString() {
		return status.toString();
	}
}
