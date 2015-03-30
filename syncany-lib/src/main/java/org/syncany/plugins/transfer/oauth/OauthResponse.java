package org.syncany.plugins.transfer.oauth;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

class OauthResponse {

	private final int code;
	private final String body;

	OauthResponse(int code, String body) {
		this.code = code;
		this.body = body;
	}

	public int getCode() {
		return code;
	}

	public String getBody() {
		return body;
	}
}
