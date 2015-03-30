package org.syncany.plugins.transfer.oauth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OAuthTokenWebListener implements Callable<OAuthTokenFinish> {

	private static final Logger logger = Logger.getLogger(OAuthTokenWebListener.class.getName());
	private static final int PORT_LOWER = 55500;
	private static final int PORT_UPPER = 55599;
	private static final List<InetAddress> ALLOWED_CLIENT_IPS = Lists.newArrayList();

	private final int port;
	private final String id;
	private final SynchronousQueue<Object> ioQueue = Queues.newSynchronousQueue();
	private final OAuthTokenInterceptor interceptor;
	private final OAuthTokenExtractor extractor;
	private final List<InetAddress> allowedClients;

	private Undertow server;

	public static Builder forId(String id) {
		return new Builder(id);
	}

	public static class Builder {

		private final String id;
		private final List<InetAddress> allowedClients = Lists.newArrayList();

		private OAuthTokenInterceptor interceptor = new OAuthTokenInterceptors.RedirectTokenInterceptor();
		private OAuthTokenExtractor extractor = new OAuthTokenExtractors.NamedQueryTokenExtractor();
		private int port;

		private Builder(String id) {
			this.id = id;
			this.port = new Random().nextInt((PORT_UPPER - PORT_LOWER) + 1) + PORT_LOWER;
		}

		public Builder setTokenInterceptor(OAuthTokenInterceptor interceptor) {
			if (interceptor != null) {
				this.interceptor = interceptor;
			}

			return this;
		}

		public Builder setTokenExtractor(OAuthTokenExtractor extractor) {
			if (extractor != null) {
				this.extractor = extractor;
			}

			return this;
		}

		public Builder setPort(int port) {
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

		public OAuthTokenWebListener build() {
			return new OAuthTokenWebListener(id, port, interceptor, extractor, allowedClients);
		}
	}

	private OAuthTokenWebListener(String id, int port, OAuthTokenInterceptor interceptor, OAuthTokenExtractor extractor, List<InetAddress> allowedClients) {
		this.id = id;
		this.port = port;
		this.interceptor = interceptor;
		this.extractor = extractor;
		this.allowedClients = allowedClients;
	}

	public URI start() throws IOException {
		createServer();
		return URI.create(String.format("http://localhost:%d/%s/", port, id));
	}

	public Future<OAuthTokenFinish> getToken() {
		return Executors.newFixedThreadPool(1).submit(this);
	}

	@Override
	public OAuthTokenFinish call() throws Exception {
		logger.log(Level.INFO, "Waiting for token response");
		String urlWithIdAndToken = (String) ioQueue.take();

		logger.log(Level.INFO, "Parsing token response " + urlWithIdAndToken);

		OAuthTokenFinish tokenResponse;
		try {
			tokenResponse = extractor.parse(urlWithIdAndToken);
		}
		catch (NoSuchFieldException e) {
			logger.log(Level.SEVERE, "Unable to find token in response", e);
			ioQueue.put(OAuthResponses.createBadResponse());
			throw e;
		}

		ioQueue.put(OAuthResponses.createValidResponse());

		logger.log(Level.INFO, "Returning token");
		return tokenResponse;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();

		if (server != null) {
			logger.log(Level.INFO, "Stopping server");
			server.stop();
		}
	}

	private void createServer() {

		logger.log(Level.FINE, "Locked to build server...");

		OAuthTokenInterceptor extractingHttpHandler = new ExtractingTokenInterceptor(ioQueue);

		server = Undertow.builder()
						.addHttpListener(port, "localhost")
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

	private static boolean isPortAvailable(int port) {
		try (Socket ignored = new Socket("localhost", port)) {
			return false;
		} catch (IOException ignored) {
			return true;
		}
	}

	static final class ExtractingTokenInterceptor implements OAuthTokenInterceptor {

		public static final String PATH_PREFIX = "/extract";

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
			String urlWithIdAndToken = exchange.getRequestURL() + "?" + exchange.getQueryString();
			logger.log(Level.INFO, "Got a request to " + urlWithIdAndToken);
			queue.add(urlWithIdAndToken);

			TimeUnit.SECONDS.sleep(2);

			OAuthResponse oauthResponse = (OAuthResponse) queue.take();
			logger.log(Level.INFO, "Got an oauth response with code " + oauthResponse.getCode());
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
			exchange.setResponseCode(oauthResponse.getCode());
			exchange.getResponseSender().send(oauthResponse.getBody());
			exchange.endExchange();
		}
	}
}

