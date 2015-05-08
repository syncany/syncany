package org.syncany.plugins.transfer.oauth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.plugins.transfer.oauth.OAuthTokenExtractors.NamedQueryTokenExtractor;
import org.syncany.plugins.transfer.oauth.OAuthTokenInterceptors.RedirectTokenInterceptor;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Range;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.IPAddressAccessControlHandler;
import io.undertow.util.Headers;

/**
 * This class creates a server handling the OAuth callback URLs. It has two tasks. First it is responsible for executing
 * {@link OAuthTokenInterceptor} depending on a path defined by the interceptor itself. Furthermore it does the token
 * parsing in the URL using a {@link OAuthTokenExtractor}.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OAuthTokenWebListener implements Callable<OAuthTokenFinish> {

	private static final Logger logger = Logger.getLogger(OAuthTokenWebListener.class.getName());
	private static final Range<Integer> VALID_PORT_RANGE = Range.openClosed(0x0000, 0xFFFF);
	private static final int PORT_LOWER = 55500;
	private static final int PORT_UPPER = 55599;

	private final int port;
	private final String id;
	private final SynchronousQueue<Object> ioQueue = Queues.newSynchronousQueue();
	private final OAuthTokenInterceptor interceptor;
	private final OAuthTokenExtractor extractor;
	private final List<InetAddress> allowedClients;

	private Undertow server;

	/**
	 * Create a new {@link OAuthTokenWebListener} with some clever defaults for a {@link OAuthMode}.
	 *
	 * @param mode  {@link OAuthMode} supported by the {@link org.syncany.plugins.transfer.TransferPlugin}.
	 * @return A ready to use {@link OAuthTokenWebListener}.
	 */
	public static Builder forMode(OAuthMode mode) {
		return new Builder(mode);
	}

	public static class Builder {

		private final List<InetAddress> allowedClients = Lists.newArrayList();

		private OAuthTokenInterceptor interceptor;
		private OAuthTokenExtractor extractor;
		private int port;
		private String id;

		private Builder(OAuthMode mode) {
			this.interceptor = OAuthTokenInterceptors.newTokenInterceptorForMode(mode);
			this.extractor = OAuthTokenExtractors.newTokenExtractorForMode(mode);

			this.id = UUID.randomUUID().toString();
			this.port = new Random().nextInt((PORT_UPPER - PORT_LOWER) + 1) + PORT_LOWER;

			try {
				this.addAllowedClient(InetAddress.getByName("127.0.0.1"));
			}
			catch (UnknownHostException e) {
				throw new RuntimeException("127.0.0.1 is unknown. This should NEVER happen", e);
			}
		}

		/**
		 * Use a custom plugin id instead of a randomly generated one. Might be needed if the service provider does not
		 * allow wildcard redirect URLs.
		 */
		public Builder setId(String id) {
			if (id != null) {
				this.id = id;
			}

			return this;
		}

		/**
		 * Use a custom interceptor (default {@link RedirectTokenInterceptor})
		 */
		public Builder setTokenInterceptor(OAuthTokenInterceptor interceptor) {
			if (interceptor != null) {
				this.interceptor = interceptor;
			}

			return this;
		}

		/**
		 * Use a custom extractor (default {@link NamedQueryTokenExtractor})
		 */
		public Builder setTokenExtractor(OAuthTokenExtractor extractor) {
			if (extractor != null) {
				this.extractor = extractor;
			}

			return this;
		}

		/**
		 * Use a fixed port, otherwise the port is randomly chosen from a range of {@link OAuthTokenWebListener#PORT_LOWER}
		 * and {@link OAuthTokenWebListener#PORT_UPPER}.
		 *
		 * @param port Fixed port to use
		 *
		 * @throws IllegalArgumentException Thrown if the chosen port is not in the valid port range (1-65535).
		 * @throws RuntimeException Thrown if the chosen port is already taken.
		 */
		public Builder setPort(int port) {
			if (!VALID_PORT_RANGE.contains(port)) {
				throw new IllegalArgumentException("Invalid port number " + port);
			}

			if (!isPortAvailable(port)) {
				throw new RuntimeException("Token listener tried to use a defined but already taken port " + port);
			}

			this.port = port;
			return this;
		}

		public Builder addAllowedClient(InetAddress... clientIp) {
			allowedClients.addAll(Lists.newArrayList(clientIp));
			return this;
		}

		/**
		 * Build an immutable {@link OAuthTokenWebListener}.
		 */
		public OAuthTokenWebListener build() {
			return new OAuthTokenWebListener(id, port, interceptor, extractor, allowedClients);
		}

		private static boolean isPortAvailable(int port) {
			try (Socket ignored = new Socket("localhost", port)) {
				return false;
			} catch (IOException ignored) {
				return true;
			}
		}
	}

	private OAuthTokenWebListener(String id, int port, OAuthTokenInterceptor interceptor, OAuthTokenExtractor extractor, List<InetAddress> allowedClients) {
		this.id = id;
		this.port = port;
		this.interceptor = interceptor;
		this.extractor = extractor;
		this.allowedClients = allowedClients;
	}

	/**
	 * Start the server created by the @{link Builder}.
	 *
	 * @return A callback URI which should be used during the OAuth process.
	 */
	public URI start() {
		createServer();
		return URI.create(String.format("http://localhost:%d/%s/", port, id));
	}

	/**
	 * Get the token generated by the OAuth process. In fact, this class returns a {@link Future} because the token may not
	 * be received by the server when this method is called.
	 *
	 * @return Returns an {@link OAuthTokenFinish} wrapped in a {@link Future}. The {@link OAuthTokenFinish} should at least
	 * contain a token
	 */
	public Future<OAuthTokenFinish> getToken() {
		return Executors.newFixedThreadPool(1).submit(this);
	}

	@Override
	public OAuthTokenFinish call() throws Exception {
		logger.log(Level.INFO, "Waiting for token response");
		final String urlWithIdAndToken = (String) ioQueue.take();

		logger.log(Level.INFO, "Parsing token response " + urlWithIdAndToken);

		OAuthTokenFinish tokenResponse = null; // null if parsing failed (user canceled, api error, ...
		try {
			tokenResponse = extractor.parse(urlWithIdAndToken);
			ioQueue.put(OAuthWebResponses.createValidResponse());
		}
		catch (NoSuchFieldException e) {
			logger.log(Level.SEVERE, "Unable to find token in response", e);
			ioQueue.put(OAuthWebResponses.createBadResponse());
		}

		ioQueue.take(); // make sure undertow has send a response
		stop();

		logger.log(Level.INFO, tokenResponse != null ? "Returning token" : "No token received, returning null");
		return tokenResponse;
	}

	/**
	 * Stop the listener server so the port becomes available again.
	 */
	public void stop() {
		if (server != null) {
			logger.log(Level.INFO, "Stopping server");
			server.stop();
			server = null;
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		stop();
	}

	private void createServer() {
		logger.log(Level.FINE, "Locked to build server...");

		OAuthTokenInterceptor extractingHttpHandler = new ExtractingTokenInterceptor(ioQueue);

		IPAddressAccessControlHandler ipAddressAccessControlHandler = new IPAddressAccessControlHandler();
		ipAddressAccessControlHandler.setDefaultAllow(false);

		for (InetAddress inetAddress : allowedClients) {
			ipAddressAccessControlHandler.addAllow(inetAddress.getHostAddress());
		}

		server = Undertow.builder()
						.addHttpListener(port, "localhost")
						.setHandler(ipAddressAccessControlHandler)
						.setHandler(Handlers.path()
														.addExactPath(createPath(extractingHttpHandler.getPathPrefix()), extractingHttpHandler)
														.addExactPath(createPath(interceptor.getPathPrefix()), interceptor)
						)
						.build();

		logger.log(Level.INFO, "Starting token web listener...");
		server.start();
	}

	private String createPath(String prefix) {
		return URI.create(String.format("/%s/%s", id, prefix)).normalize().toString();
	}

	/**
	 * Default {@link OAuthTokenInterceptor} which notifies the listener about an existing token. It also sends feedback
	 * to a user.
	 */
	static final class ExtractingTokenInterceptor implements OAuthTokenInterceptor {

		static final String PATH_PREFIX = "/extract";

		private final SynchronousQueue<Object> queue;

		private ExtractingTokenInterceptor(SynchronousQueue<Object> queue) {
			this.queue = queue;
		}

		@Override
		public String getPathPrefix() {
			return PATH_PREFIX;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			final String urlWithIdAndToken = exchange.getRequestURL() + "?" + exchange.getQueryString();
			logger.log(Level.INFO, "Got a request to " + urlWithIdAndToken);
			queue.add(urlWithIdAndToken);

			TimeUnit.SECONDS.sleep(2);

			final OAuthWebResponse oauthWebResponse = (OAuthWebResponse) queue.take();
			logger.log(Level.INFO, "Got an oauth response with code " + oauthWebResponse.getCode());
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
			exchange.setResponseCode(oauthWebResponse.getCode());
			exchange.getResponseSender().send(oauthWebResponse.getBody());
			exchange.endExchange();
			queue.add(Boolean.TRUE);
		}
	}
}

