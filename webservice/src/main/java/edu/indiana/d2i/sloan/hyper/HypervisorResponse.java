/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan.hyper;

import java.util.HashMap;
import java.util.Map;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;
import edu.indiana.d2i.sloan.vm.VMState;

class HypervisorResponse {
	public static final String KV_DELIMITER;
	public static final String VM_STATUS_KEY;

	private final int responseCode; // code from hyper script
	private final VMState state; // non-error, or error
	private final String description;
	private final Map<String, String> attributes;

	/* string representation of the commands being executed */
	private String cmdsString;

	/* host where the commands are being executed */
	private String hostname;

	static {
		KV_DELIMITER = Configuration.getInstance().getString(
				Configuration.PropertyName.RESP_KV_DELIMITER);

		VM_STATUS_KEY = Configuration.getInstance().getString(
				Configuration.PropertyName.RESP_VM_STATUS_KEY);
	}

	private HypervisorResponse(String cmdsString, String hostname,
			int responseCode, String description, VMState state, 
			Map<String, String> attributes) {
		this.cmdsString = cmdsString;
		this.hostname = hostname;
		this.responseCode = responseCode;
		this.description = description;
		this.attributes = attributes;
		this.state = state;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getDescription() {
		return description;
	}
	
	public VMState getVmState() {
		return state;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}

	public String getAttribute(String key) {
		return attributes.get(key);
	}

	public static HypervisorResponse commandRes2HyResp(CmdsExecResult cmdRes) {
		int respCode = cmdRes.getExitCode();
		String description = cmdRes.getScreenOutput().trim();
		
		// extract "Status"
//		for (String line : description.split("\n")) {
//			if (line.contains(VM_STATUS_KEY)) {
//				String[] items = line.split("\\s");
//				
//			}
//		}
		
		// TODO: how to set vm state as error?
		
		HypervisorResponse hyperResp = new HypervisorResponse(cmdRes.getCmds()
			.getConcatenatedForm(), cmdRes.getHostname(), respCode, description, 
			(respCode == 0) ? VMState.RUNNING: VMState.ERROR, 
			new HashMap<String, String>());			
		return hyperResp;
		
		
		/*
		String[] lines = cmdRes.getScreenOutput().split("[\\r\\n]+");

		int respCode = cmdRes.getExitCode();

		if ((lines != null) && (lines.length > 0)) {
			if (StringUtils.isNumeric(lines[0].trim())) {
				respCode = Integer.parseInt(lines[0].trim());
			} 			
		}

		String description = ((lines != null) && (lines.length > 1))
				? lines[1].trim()
				: "";

		// parse key-value pairs
		Map<String, String> attributes = new HashMap<String, String>();
		if ((lines != null) && (lines.length > 2)) {
			for (int i = 2; i < lines.length; i++) {
				String[] kvpair = lines[i].split(KV_DELIMITER);
				if (kvpair.length == 2)
					attributes.put(kvpair[0].trim(), kvpair[1].trim());
			}
		}
		

		// TODO: extra parsing is needed to convert vm state from script string to VMState string
		
		HypervisorResponse hyperResp = new HypervisorResponse(cmdRes.getCmds()
			.getConcatenatedForm(), cmdRes.getHostname(), respCode, description, 
			(respCode == 0) ? VMState.RUNNING: VMState.ERROR, 
			attributes);
		
		return hyperResp;
		*/
	}

	@Override
	public String toString() {
		StringBuilder digest = new StringBuilder();

		digest.append("\nCommand(s) executed: ").append(cmdsString).append("\n")
				.append("Host where commands being executed: ")
				.append(hostname).append("\n").append("Response Code: ")
				.append(responseCode).append("\n").append("Description: ")
				.append(description).append("\n");

		for (Map.Entry<String, String> kvpair : attributes.entrySet()) {
			digest.append("Key: ").append(kvpair.getKey()).append(" ")
					.append("value: ").append(kvpair.getValue()).append("\n");
		}

		return digest.toString();
	}
	
	// unit test only
	public static HypervisorResponse createTestHypervisorResp(String cmdsString, 
			String hostname,
			int responseCode, String description, VMState state, 
			Map<String, String> attributes) {
		return new HypervisorResponse(cmdsString, hostname, 
			responseCode, description, state, attributes);
	}
}
