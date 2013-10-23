package edu.indiana.d2i.sloan.hyper;

import java.util.HashMap;
import java.util.Map;

class HypervisorResponse {
	public enum Response{
		SUCCEED,
		FAILED
	}
	
	private final Response responseCode;
	private final String description;
	private final Map<String, String> attributes;

	public HypervisorResponse(Response responseCode, String description) {
		this.responseCode = responseCode;
		this.description = description;
		this.attributes = new HashMap<String, String>();
	}
	
	public Response getResponseCode() {
		return responseCode;
	}

	public String getDescription() {
		return description;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
}
