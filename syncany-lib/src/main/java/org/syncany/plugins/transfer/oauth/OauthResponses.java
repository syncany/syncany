package org.syncany.plugins.transfer.oauth;

import org.apache.http.HttpStatus;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public abstract class OauthResponses {

	public static OauthResponse createValidResponse() {
		return new OauthResponse(HttpStatus.SC_OK, "thanks for the token dude");
	}

	public static OauthResponse createBadResponse() {
		return new OauthResponse(HttpStatus.SC_BAD_REQUEST, "please check your request");
	}

}
