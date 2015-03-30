package org.syncany.plugins.transfer.oauth;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class OauthTokenInterceptors {

	private static final Logger logger = Logger.getLogger(OauthTokenInterceptors.class.getName());
	public static final String PATH_PREFIX = "/";

	public static HashTokenInterceptor newHashTokenInterceptor() {
		return new HashTokenInterceptor();
	}

	public static HashTokenInterceptor newHashTokenInterceptor(String tokenId) {
		return new HashTokenInterceptor(tokenId);
	}

	public static RedirectTokenInterceptor newRedirectTokenInterceptor() {
		return new RedirectTokenInterceptor();
	}

	static class HashTokenInterceptor implements OauthTokenInterceptor {

		public static final String DEFAULT_TOKEN_ID = "token";

		private String tokenId = DEFAULT_TOKEN_ID;

		public HashTokenInterceptor() {
			this.tokenId = DEFAULT_TOKEN_ID;
		}

		public HashTokenInterceptor(String tokenId) {
			this.tokenId = tokenId;
		}

		@Override
		public String getPathPrefix() {
			return PATH_PREFIX;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {

		}
	}

	static class RedirectTokenInterceptor implements OauthTokenInterceptor {

		@Override
		public String getPathPrefix() {
			return PATH_PREFIX;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			String redirectToUrl = String.format("%s/%s?%s", exchange.getRequestURL(), OauthTokenWebListener.ExtractingTokenInterceptor.PATH_PREFIX, exchange.getQueryString());
			URI redirectToUri = URI.create(redirectToUrl).normalize();
			logger.log(Level.INFO, "Redirecting to " + redirectToUri);

			exchange.setResponseCode(StatusCodes.FOUND);
			exchange.getResponseHeaders().put(Headers.LOCATION, redirectToUri.toString());
			exchange.endExchange();
		}
	}

}