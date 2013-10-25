package edu.indiana.d2i.sloan.hyper;

import java.util.HashMap;
import java.util.Map;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;

class HypervisorResponse {
	private static final String KV_DELIMITER;

	private final int responseCode;
	private final String description;
	private final Map<String, String> attributes;

	static {
		KV_DELIMITER = Configuration.getInstance().getProperty(
				Configuration.PropertyName.KV_DELIMITER);
	}

	public HypervisorResponse(int responseCode, String description) {
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

	public static HypervisorResponse commandRes2HyResp(CmdsExecResult cmdRes) {
		String[] lines = cmdRes.getScreenOutput().split("[\\r\\n]+");

		int respCode = cmdRes.getExitCode();

		if ((lines != null) && (lines.length > 0)) {
			respCode = Integer.parseInt(lines[0].trim());
		}

		String description = ((lines != null) && (lines.length > 1))
				? lines[1]
				: null;

		HypervisorResponse hyperResp = new HypervisorResponse(respCode,
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

		digest.append("Response Code: ").append(responseCode).append("\n")
				.append("Description: ").append(description).append("\n");

		for (Map.Entry<String, String> kvpair : attributes.entrySet()) {
			digest.append("Key: ").append(kvpair.getKey()).append(" ")
					.append("value: ").append(kvpair.getValue()).append("\n");
		}

		return digest.toString();
	}
}
