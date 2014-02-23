package edu.indiana.d2i.sloan.bean;

import java.io.InputStream;
import java.util.Date;


public class ResultBean {
	private InputStream inputstream;
	private Date startdate;
	
	public ResultBean(InputStream inputstream, Date startdate) {
		this.inputstream = inputstream;
		this.startdate = startdate;
	}
	
	public InputStream getInputstream() {
		return inputstream;
	}
	
	public Date getStartdate() {
		return startdate;
	}
}
