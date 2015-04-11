package org.syncany.plugins.transfer.oauth;

import io.undertow.server.HttpHandler;

/**
 * A OAuthTokenInterceptor is an extension of an {@link HttpHandler} which intercepts calls to a callback URL depending
 * on the queried path.
 *
 * @see {@link OAuthTokenInterceptors}
 *
 * @author Christian Roth <christian.roth@port17.de>
 */

public interface OAuthTokenInterceptor extends HttpHandler {

	/**
	 * Get the path handled by the interceptor.
	 *
	 * @return The path handled by the interceptor.
	 */
	String getPathPrefix();

}
