package org.syncany.plugins.transfer.oauth;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OAuthTokenFinish {

	private final String token;
	private final String csrfState;

	OAuthTokenFinish(String token, String csrfState) {
		this.token = token;
		this.csrfState = csrfState;
	}

	public String getToken() {
		return token;
	}

	public String getCsrfState() {
		return csrfState;
	}
}
