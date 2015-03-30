package org.syncany.plugins.transfer.oauth;

import java.io.IOException;
import java.net.InetAddress;
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

public class OauthTokenWebListener implements Callable<String> {

	private static final Logger logger = Logger.getLogger(OauthTokenWebListener.class.getName());
	private static final int PORT_LOWER = 55500;
	private static final int PORT_UPPER = 55599;
	private static final List<InetAddress> ALLOWED_CLIENT_IPS = Lists.newArrayList();

	private final int port;
	private final String id;
	private final SynchronousQueue<Object> ioQueue = Queues.newSynchronousQueue();
	private final OauthTokenInterceptor interceptor;
	private final OauthTokenExtractor extractor;
	private final List<InetAddress> allowedClients;

	private Undertow server;

	public static Builder forId(String id) {
		return new Builder(id);
	}

	public static class Builder {

		private final String id;
		private final List<InetAddress> allowedClients = Lists.newArrayList();

		private OauthTokenInterceptor interceptor = OauthTokenInterceptors.newRedirectTokenInterceptor();
		private OauthTokenExtractor extractor = OauthTokenExtractors.newNamedQueryOauthTokenExtractor();
		private int port;

		private Builder(String id) {
			this.id = id;
			this.port = new Random().nextInt((PORT_UPPER - PORT_LOWER) + 1) + PORT_LOWER;
		}

		public Builder setTokenInterceptor(OauthTokenInterceptor interceptor) {
			this.interceptor = interceptor;
			return this;
		}

		public Builder setTokenExtractor(OauthTokenExtractor extractor) {
			this.extractor = extractor;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder addAllowedClient(InetAddress... clientIp) {
			allowedClients.addAll(Lists.newArrayList(clientIp));
			return this;
		}

		public OauthTokenWebListener build() {
			return new OauthTokenWebListener(id, port, interceptor, extractor, allowedClients);
		}

	}

	private OauthTokenWebListener(String id, int port, OauthTokenInterceptor interceptor, OauthTokenExtractor extractor, List<InetAddress> allowedClients) {
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

	public Future<String> getToken() {
		return Executors.newFixedThreadPool(1).submit(this);
	}

	@Override
	public String call() throws Exception {
		logger.log(Level.INFO, "Waiting for token response");
		String urlWithIdAndToken = (String) ioQueue.take();

		logger.log(Level.INFO, "Parsing token response " + urlWithIdAndToken);

		String token;
		try {
			token = extractor.parse(urlWithIdAndToken);
		}
		catch (NoSuchFieldException e) {
			logger.log(Level.SEVERE, "Unable to find token in respobse", e);
			ioQueue.put(OauthResponses.createBadResponse());
			throw e;
		}

		ioQueue.put(OauthResponses.createValidResponse());

		logger.log(Level.INFO, "Returning token");
		return token;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();

		if (server != null) {
			server.stop();
		}
	}

	private void createServer() {

		logger.log(Level.FINE, "Locked to build server...");

		OauthTokenInterceptor extractingHttpHandler = new ExtractingTokenInterceptor(ioQueue);

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

	static final class ExtractingTokenInterceptor implements OauthTokenInterceptor {

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

			OauthResponse oauthResponse = (OauthResponse) queue.take();
			logger.log(Level.INFO, "Got an oauth response with code " + oauthResponse.getCode());
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
			exchange.setResponseCode(oauthResponse.getCode());
			exchange.getResponseSender().send(oauthResponse.getBody());
			exchange.endExchange();

			logger.log(Level.INFO, "Stopping server");
		}
	}
}

