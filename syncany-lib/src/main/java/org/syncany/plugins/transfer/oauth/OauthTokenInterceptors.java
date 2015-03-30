package org.syncany.plugins.transfer.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public abstract class OAuthTokenInterceptors {

	private static final Logger logger = Logger.getLogger(OAuthTokenInterceptors.class.getName());
	public static final String PATH_PREFIX = "/";

	public static class HashTokenInterceptor implements OAuthTokenInterceptor {

		public static final String PLACEHOLDER_FOR_EXTRACT_PATH = "%extractPath%";
		private static final InputStream HTML_SITE_STREAM = HashTokenInterceptor.class.getResourceAsStream("/org/syncany/plugins/oauth/HashTokenInterceptor.html");

		private final String html;

		public HashTokenInterceptor() {
			try {
				this.html = IOUtils.toString(HTML_SITE_STREAM).replace(PLACEHOLDER_FOR_EXTRACT_PATH, OAuthTokenWebListener.ExtractingTokenInterceptor.PATH_PREFIX);
			}
			catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to read html site from " + HTML_SITE_STREAM, e);
				throw new RuntimeException("Unable to read html site from " + HTML_SITE_STREAM);
			}
		}

		@Override
		public String getPathPrefix() {
			return PATH_PREFIX;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
			exchange.setResponseCode(StatusCodes.OK);
			exchange.getResponseSender().send(html);
			exchange.endExchange();
		}
	}

	public static class RedirectTokenInterceptor implements OAuthTokenInterceptor {

		@Override
		public String getPathPrefix() {
			return PATH_PREFIX;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			String redirectToUrl = String.format("%s/%s?%s", exchange.getRequestURL(), OAuthTokenWebListener.ExtractingTokenInterceptor.PATH_PREFIX, exchange.getQueryString());
			URI redirectToUri = URI.create(redirectToUrl).normalize();
			logger.log(Level.INFO, "Redirecting to " + redirectToUri);

			exchange.setResponseCode(StatusCodes.FOUND);
			exchange.getResponseHeaders().put(Headers.LOCATION, redirectToUri.toString());
			exchange.endExchange();
		}
	}

}