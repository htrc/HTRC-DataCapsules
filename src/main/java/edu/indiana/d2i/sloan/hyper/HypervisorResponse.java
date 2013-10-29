package edu.indiana.d2i.sloan.hyper;

import java.util.HashMap;
import java.util.Map;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;

class HypervisorResponse {
	public static final String KV_DELIMITER;
	public static final String VM_STATUS_KEY;

	private final int responseCode;
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

	public HypervisorResponse(String cmdsString, String hostname,
			int responseCode, String description) {
		this.cmdsString = cmdsString;
		this.hostname = hostname;
		this.responseCode = responseCode;
		this.description = description;
		this.attributes = new HashMap<String, String>();
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getDescription() {
		return description;
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
		String[] lines = cmdRes.getScreenOutput().split("[\\r\\n]+");

		int respCode = cmdRes.getExitCode();

		if ((lines != null) && (lines.length > 0)) {
			respCode = Integer.parseInt(lines[0].trim());
		}

		String description = ((lines != null) && (lines.length > 1))
				? lines[1]
				: null;

		HypervisorResponse hyperResp = new HypervisorResponse(cmdRes.getCmds()
				.getConcatenatedForm(), cmdRes.getHostname(), respCode,
				description);

		// parse key-value pairs
		if ((lines != null) && (lines.length > 2)) {
			for (int i = 2; i < lines.length; i++) {
				String[] kvpair = lines[i].split(KV_DELIMITER);
				hyperResp.setAttribute(kvpair[0].trim(), kvpair[1].trim());
			}
		}

		return hyperResp;
	}

	@Override
	public String toString() {
		StringBuilder digest = new StringBuilder();

		digest.append("Command(s) executed: ").append(cmdsString).append("\n")
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
}
