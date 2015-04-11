package org.syncany.plugins.transfer.oauth;

/**
 * A website which is shown to a user during the oauth process.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */

class OAuthWebResponse {

	private final int code;
	private final String body;

	OAuthWebResponse(int code, String body) {
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
