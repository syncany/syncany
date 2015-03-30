package org.syncany.plugins.transfer.oauth;

import io.undertow.util.StatusCodes;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public abstract class OAuthResponses {

	public static OAuthResponse createValidResponse() {
		return new OAuthResponse(StatusCodes.OK, "thanks for the token dude");
	}

	public static OAuthResponse createBadResponse() {
		return new OAuthResponse(StatusCodes.BAD_REQUEST, "please check your request");
	}

}
