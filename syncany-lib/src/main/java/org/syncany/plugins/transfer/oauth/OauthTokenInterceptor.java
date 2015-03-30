package org.syncany.plugins.transfer.oauth;

import io.undertow.server.HttpHandler;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public interface OAuthTokenInterceptor extends HttpHandler {

	public String getPathPrefix();

}
