package org.syncany.plugins.transfer.oauth;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public interface OAuthTokenExtractor {

	public String parse(String urlWithToken) throws NoSuchFieldException;

}
