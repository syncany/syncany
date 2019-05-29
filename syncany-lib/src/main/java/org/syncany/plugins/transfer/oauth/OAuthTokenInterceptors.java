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

/**
 * Factory class to generate some common {@link OAuthTokenInterceptor}s.
 *
 * @author Christian Roth (christian.roth@port17.de)
 */
public abstract class OAuthTokenInterceptors {

	private static final Logger logger = Logger.getLogger(OAuthTokenInterceptors.class.getName());

	/**
	 * Has to be {@value} because it's the first step of the OAuth process.
	 */
	static final String PATH_PREFIX = "/";

	/**
	 * Get a common {@link OAuthTokenInterceptor} depending on the chosen {@link OAuthMode}.
	 * If {@link OAuthMode#BROWSER} is used a {@link HashTokenInterceptor}
	 * is returned and a {@link OAuthTokenInterceptors.RedirectTokenInterceptor} in {@link OAuthMode#SERVER}.
	 *
	 * @param mode {@link OAuthMode} supported by the {@link org.syncany.plugins.transfer.TransferPlugin}.
	 * @return Either a {@link HashTokenInterceptor} or a {@link OAuthTokenInterceptors.RedirectTokenInterceptor}
	 */
	public static OAuthTokenInterceptor newTokenInterceptorForMode(OAuthMode mode) {
		switch (mode) {
			case BROWSER:
				return new HashTokenInterceptor();

			case SERVER:
				return new RedirectTokenInterceptor();

			default:
				throw new RuntimeException("Unknown OAuth mode");
		}
	}

	/**
	 * {@link OAuthTokenInterceptor} implementation which bypasses some protection mechanisms to allow the token extraction.
	 * In {@link OAuthMode#BROWSER}, the service provider uses the fragment part (the part after the #) of a URL to send over
	 * a token. However, this part cannot be retrieved by a WebServer. A {@link HashTokenInterceptor}
	 * appends the fragment variables to the query parameters of the URL.
	 */
	public static class HashTokenInterceptor implements OAuthTokenInterceptor {

		public static final String PLACEHOLDER_FOR_EXTRACT_PATH = "%extractPath%";
		private static final String HTML_SITE_RESOURCE_PATH = "/org/syncany/plugins/oauth/HashTokenInterceptor.html";

		private final String html;

		public HashTokenInterceptor() {
			try(InputStream htmlSiteStream = HashTokenInterceptor.class.getResourceAsStream(HTML_SITE_RESOURCE_PATH)) {
				this.html = IOUtils.toString(htmlSiteStream).replace(PLACEHOLDER_FOR_EXTRACT_PATH, OAuthTokenWebListener.ExtractingTokenInterceptor.PATH_PREFIX);
			}
			catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to read html site from " + HTML_SITE_RESOURCE_PATH, e);
				throw new RuntimeException("Unable to read html site from " + HTML_SITE_RESOURCE_PATH);
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

	/**
	 * A {@link RedirectTokenInterceptor} can be seen as an empty {@link OAuthTokenInterceptor} because it only redirects
	 * to the next step of the OAuth process which is the extraction of the token from the URL. It's needed in {@link OAuthMode#SERVER}
	 * since the token parameter is already provided in the URL's query part.
	 */
	public static class RedirectTokenInterceptor implements OAuthTokenInterceptor {

		@Override
		public String getPathPrefix() {
			return PATH_PREFIX;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			final String redirectToUrl = String.format("%s/%s?%s", exchange.getRequestURL(), OAuthTokenWebListener.ExtractingTokenInterceptor.PATH_PREFIX, exchange.getQueryString());
			final URI redirectToUri = URI.create(redirectToUrl).normalize();

			logger.log(Level.INFO, "Redirecting to " + redirectToUri);

			exchange.setResponseCode(StatusCodes.FOUND);
			exchange.getResponseHeaders().put(Headers.LOCATION, redirectToUri.toString());
			exchange.endExchange();
		}
	}

}