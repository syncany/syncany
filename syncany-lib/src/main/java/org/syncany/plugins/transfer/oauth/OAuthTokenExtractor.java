package org.syncany.plugins.transfer.oauth;

/**
 * A OAuthTokenExtractor is responsible for the extraction of an {@link OAuthTokenFinish} from a given URL string.
 * Such URLs a are typically callback URLs called by an oauth provider who adds token and status fields to such an URL.
 * {@link OAuthTokenExtractors} provides some predefined OAuthTokenExtractors.
 *
 * @see {@link OAuthTokenExtractors}
 *
 * @author Christian Roth <christian.roth@port17.de>
 */

public interface OAuthTokenExtractor {

	/**
	 * Extract a {@link OAuthTokenFinish} from a given URL. It has to fail with an exception instead of returning null.
	 *
	 * @param urlWithToken The callback URL as it is invoked by the oauth provider
	 * @return A {@link OAuthTokenFinish} with a token and sometimes a state secret
	 * @throws NoSuchFieldException Thrown if the URL does not contain a token or a state field (depending on the implementation)
	 */
	OAuthTokenFinish parse(String urlWithToken) throws NoSuchFieldException;

}
