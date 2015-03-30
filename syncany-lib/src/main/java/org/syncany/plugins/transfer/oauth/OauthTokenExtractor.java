package org.syncany.plugins.transfer.oauth;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public interface OauthTokenExtractor {

	public String parse(String urlWithToken) throws NoSuchFieldException;

}
