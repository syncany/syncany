package org.syncany.plugins.transfer.oauth;

import io.undertow.util.StatusCodes;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public abstract class OauthResponses {

	public static OauthResponse createValidResponse() {
		return new OauthResponse(StatusCodes.OK, "thanks for the token dude");
	}

	public static OauthResponse createBadResponse() {
		return new OauthResponse(StatusCodes.BAD_REQUEST, "please check your request");
	}

}
