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

public class OauthTokenInterceptors {

	private static final Logger logger = Logger.getLogger(OauthTokenInterceptors.class.getName());
	public static final String PATH_PREFIX = "/";

	public static HashTokenInterceptor newHashTokenInterceptor() {
		return new HashTokenInterceptor();
	}

	public static RedirectTokenInterceptor newRedirectTokenInterceptor() {
		return new RedirectTokenInterceptor();
	}

	static class HashTokenInterceptor implements OauthTokenInterceptor {

		public static final String PLACEHOLDER_FOR_EXTRACT_PATH = "%extractPath%";
		private static final InputStream HTML_SITE_STREAM = HashTokenInterceptor.class.getResourceAsStream("/org/syncany/plugins/oauth/HashTokenInterceptor.html");

		private final String html;

		public HashTokenInterceptor() {
			try {
				this.html = IOUtils.toString(HTML_SITE_STREAM).replace(PLACEHOLDER_FOR_EXTRACT_PATH, OauthTokenWebListener.ExtractingTokenInterceptor.PATH_PREFIX);
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