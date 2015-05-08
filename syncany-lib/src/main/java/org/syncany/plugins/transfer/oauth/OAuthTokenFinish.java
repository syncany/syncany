package org.syncany.plugins.transfer.oauth;

/**
 * A {@link OAuthTokenFinish} is a container to hold a pair of a token and a CSRF field.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OAuthTokenFinish {

	private final String token;
	private final String csrfState;

	/**
	 * Create a new, immutable pair of token and CSRF state.
	 *
	 * @param token required
	 * @param csrfState optional
	 */
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
