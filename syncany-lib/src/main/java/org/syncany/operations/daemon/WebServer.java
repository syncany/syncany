/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.operations.daemon;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

import javax.net.ssl.SSLContext;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.syncany.config.LocalEventBus;
import org.syncany.config.UserConfig;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.UserTO;
import org.syncany.config.to.WebServerTO;
import org.syncany.crypto.CipherParams;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.daemon.handlers.InternalRestHandler;
import org.syncany.operations.daemon.handlers.InternalWebInterfaceHandler;
import org.syncany.operations.daemon.handlers.InternalWebSocketHandler;
import org.syncany.operations.daemon.messages.GetFileFolderResponse;
import org.syncany.operations.daemon.messages.GetFileFolderResponseInternal;
import org.syncany.operations.daemon.messages.api.ExternalEvent;
import org.syncany.operations.daemon.messages.api.JsonMessageFactory;
import org.syncany.operations.daemon.messages.api.Message;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.daemon.messages.api.XmlMessageFactory;
import org.syncany.plugins.web.WebInterfacePlugin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import io.undertow.Undertow;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

/**
 * The web server provides a HTTP/REST and WebSocket API to thin clients,
 * as well as a mechanism to run a web interface by implementing a
 * {@link WebInterfacePlugin}.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WebServer {
	public static final String API_ENDPOINT_WS_XML = "/api/ws/xml";
	public static final String API_ENDPOINT_WS_JSON = "/api/ws/json";
	public static final String API_ENDPOINT_REST_XML = "/api/rs/xml";
	public static final String API_ENDPOINT_REST_JSON = "/api/rs/json";
	
	public enum RequestFormatType {
		XML, JSON
	}

	private static final Logger logger = Logger.getLogger(WebServer.class.getSimpleName());
	private static final RequestFormatType DEFAULT_RESPONSE_FORMAT = RequestFormatType.XML;

	private Undertow webServer;
	private LocalEventBus eventBus;

	private Cache<Integer, WebSocketChannel> requestIdWebSocketCache;
	private Cache<Integer, HttpServerExchange> requestIdRestSocketCache;
	private Cache<Integer, RequestFormatType> requestIdRestFormatCache;
	private Cache<String, File> fileTokenTempFileCache;

	private Map<WebSocketChannel, RequestFormatType> webSocketChannelRequestFormatMap;

	public WebServer(DaemonConfigTO daemonConfig) throws Exception {
		this.webSocketChannelRequestFormatMap = Maps.newConcurrentMap();
		
		initCaches();
		initEventBus();
		initServer(daemonConfig);
	}

	public void start() throws ServiceAlreadyStartedException {
		webServer.start();
	}

	public void stop() {
		try {
			logger.log(Level.INFO, "Shutting down websocket server.");
			webServer.stop();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Could not stop websocket server.", e);
		}
	}

	private void initCaches() {
		requestIdWebSocketCache = CacheBuilder.newBuilder().maximumSize(10000)
						.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();

		requestIdRestSocketCache = CacheBuilder.newBuilder().maximumSize(10000)
						.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();

		fileTokenTempFileCache = CacheBuilder.newBuilder().maximumSize(10000)
						.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();

		requestIdRestFormatCache = CacheBuilder.newBuilder().maximumSize(10000)
						.concurrencyLevel(2).expireAfterAccess(1, TimeUnit.MINUTES).build();
	}

	private void initEventBus() {
		eventBus = LocalEventBus.getInstance();
		eventBus.register(this);
	}

	private void initServer(DaemonConfigTO daemonConfigTO) throws Exception {
		WebServerTO webServerConfig = daemonConfigTO.getWebServer();

		// Bind address and port
		String bindAddress = webServerConfig.getBindAddress();
		int bindPort = webServerConfig.getBindPort();

		// Users (incl. CLI user!)
		List<UserTO> users = readWebServerUsers(daemonConfigTO);
		IdentityManager identityManager = new MapIdentityManager(users);

		// (Re-)generate keypair/certificate (if requested)
		boolean certificateAutoGenerate = webServerConfig.isCertificateAutoGenerate();
		String certificateCommonName = webServerConfig.getCertificateCommonName();

		if (certificateAutoGenerate && certificateCommonNameChanged(certificateCommonName)) {
			generateNewKeyPairAndCertificate(certificateCommonName);
		}

		// Set up the handlers for WebSocket, REST and the web interface
		HttpHandler pathHttpHandler = path()
						.addPrefixPath(API_ENDPOINT_WS_XML, websocket(new InternalWebSocketHandler(this, certificateCommonName, RequestFormatType.XML)))
						.addPrefixPath(API_ENDPOINT_WS_JSON, websocket(new InternalWebSocketHandler(this, certificateCommonName, RequestFormatType.JSON)))
						.addPrefixPath(API_ENDPOINT_REST_XML, new InternalRestHandler(this, RequestFormatType.XML))
						.addPrefixPath(API_ENDPOINT_REST_JSON, new InternalRestHandler(this, RequestFormatType.JSON))
						.addPrefixPath("/", new InternalWebInterfaceHandler());

		// Add some security spices
		HttpHandler securityPathHttpHandler = addSecurity(pathHttpHandler, identityManager);
		SSLContext sslContext = UserConfig.createUserSSLContext();

		// And go for it!
		webServer = Undertow
						.builder()
						.addHttpsListener(bindPort, bindAddress, sslContext)
						.setHandler(securityPathHttpHandler)
						.build();

		logger.log(Level.INFO, "Initialized web server.");
	}

	private List<UserTO> readWebServerUsers(DaemonConfigTO daemonConfigTO) {
		List<UserTO> users = daemonConfigTO.getUsers();

		if (users == null) {
			users = new ArrayList<UserTO>();
		}

		// Add CLI credentials
		if (daemonConfigTO.getPortTO() != null) {
			users.add(daemonConfigTO.getPortTO().getUser());
		}

		return users;
	}

	private boolean certificateCommonNameChanged(String certificateCommonName) {
		try {
			KeyStore userKeyStore = UserConfig.getUserKeyStore();
			X509Certificate currentCertificate = (X509Certificate) userKeyStore.getCertificate(CipherParams.CERTIFICATE_IDENTIFIER);

			if (currentCertificate != null) {
				X500Name currentCertificateSubject = new JcaX509CertificateHolder(currentCertificate).getSubject();
				RDN currentCertificateSubjectCN = currentCertificateSubject.getRDNs(BCStyle.CN)[0];

				String currentCertificateSubjectCnStr = IETFUtils.valueToString(currentCertificateSubjectCN.getFirst().getValue());

				if (!certificateCommonName.equals(currentCertificateSubjectCnStr)) {
					logger.log(Level.INFO, "- Certificate regeneration necessary: Cert common name in daemon config changed from " + currentCertificateSubjectCnStr + " to " + certificateCommonName + ".");
					return true;
				}
			}
			else {
				logger.log(Level.INFO, "- Certificate regeneration necessary, because no certificate found in key store.");
				return true;
			}

			return false;
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot (re-)generate server certificate for hostname: " + certificateCommonName, e);
		}
	}

	public static void generateNewKeyPairAndCertificate(String certificateCommonName) {
		try {
			logger.log(Level.INFO, "(Re-)generating keypair and certificate for hostname " + certificateCommonName + " ...");

			// Generate key pair and certificate
			KeyPair keyPair = CipherUtil.generateRsaKeyPair();
			X509Certificate certificate = CipherUtil.generateSelfSignedCertificate(certificateCommonName, keyPair);

			// Add key and certificate to key store
			UserConfig.getUserKeyStore().setKeyEntry(CipherParams.CERTIFICATE_IDENTIFIER, keyPair.getPrivate(), new char[0], new Certificate[]{certificate});
			UserConfig.storeUserKeyStore();

			// Add certificate to trust store (for CLI->API connection)
			UserConfig.getUserTrustStore().setCertificateEntry(CipherParams.CERTIFICATE_IDENTIFIER, certificate);
			UserConfig.storeTrustStore();
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to read key store or generate self-signed certificate.", e);
		}
	}

	private static HttpHandler addSecurity(final HttpHandler toWrap, IdentityManager identityManager) {
		List<AuthenticationMechanism> mechanisms =
						Collections.<AuthenticationMechanism>singletonList(new BasicAuthenticationMechanism("Syncany"));

		HttpHandler handler = toWrap;

		handler = new AuthenticationCallHandler(handler);
		handler = new AuthenticationConstraintHandler(handler);
		handler = new AuthenticationMechanismsHandler(handler, mechanisms);
		handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);

		return handler;
	}

	@Subscribe
	public void onGetFileResponseInternal(GetFileFolderResponseInternal fileResponseInternal) {
		File tempFile = fileResponseInternal.getTempFile();
		GetFileFolderResponse fileResponse = fileResponseInternal.getFileResponse();

		fileTokenTempFileCache.asMap().put(fileResponse.getTempToken(), tempFile);
		eventBus.post(fileResponse);
	}

	@Subscribe
	public void onEvent(ExternalEvent event) {
		try {
			sendBroadcast(event);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot send event.", e);
		}
	}

	@Subscribe
	public void onResponse(Response response) {
		try {
			// Send to one or many receivers
			boolean responseWithoutRequest = response.getRequestId() == null || response.getRequestId() <= 0;

			if (responseWithoutRequest) {
				sendBroadcast(response);
			}
			else {
				HttpServerExchange responseToHttpServerExchange = requestIdRestSocketCache.asMap().get(response.getRequestId());
				WebSocketChannel responseToWebSocketChannel = requestIdWebSocketCache.asMap().get(response.getRequestId());

				if (responseToHttpServerExchange != null) {
					sendTo(responseToHttpServerExchange, response);
				}
				else if (responseToWebSocketChannel != null) {
					sendTo(responseToWebSocketChannel, response);
				}
				else {
					logger.log(Level.WARNING, "Cannot send message, because request ID in response is unknown or timed out." + response);
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot send response.", e);
		}
	}

	private void sendBroadcast(Message message) throws Exception {
		logger.log(Level.INFO, "Sending broadcast message to " + webSocketChannelRequestFormatMap.size() + " websocket client(s)");

		synchronized (webSocketChannelRequestFormatMap) {
			for (WebSocketChannel clientChannel : webSocketChannelRequestFormatMap.keySet()) {
				sendTo(clientChannel, message);
			}
		}
	}

	private void sendTo(WebSocketChannel clientChannel, Message message) throws Exception {
		String messageStr = createMessageStr(clientChannel, message);
		
		logger.log(Level.INFO, "Sending message to " + clientChannel + ": " + messageStr);
		WebSockets.sendText(messageStr, clientChannel, null);
	}

	private void sendTo(HttpServerExchange serverExchange, Response response) throws Exception {
		String responseStr = createMessageStr(response);

		logger.log(Level.INFO, "Sending message to " + serverExchange.getHostAndPort() + ": " + responseStr);

		serverExchange.getResponseSender().send(responseStr);
		serverExchange.endExchange();
	}
	
	private String createMessageStr(WebSocketChannel channel, Message message) throws Exception {
		RequestFormatType requestFormatType = webSocketChannelRequestFormatMap.get(channel);
		return createMessageStr(message, requestFormatType);
	}

	private String createMessageStr(Response response) throws Exception {
		RequestFormatType requestFormatType = requestIdRestFormatCache.getIfPresent(response.getRequestId());
		return createMessageStr(response, requestFormatType);		
	}

	private String createMessageStr(Message message, RequestFormatType outputFormat) throws Exception {
		if (outputFormat == null) {
			outputFormat = DEFAULT_RESPONSE_FORMAT;
		}

		switch (outputFormat) {
			case JSON:
				return JsonMessageFactory.toJson(message);

			case XML:
			default:
				return XmlMessageFactory.toXml(message);
		}
	}

	// Client channel access methods

	public void addClientChannel(WebSocketChannel clientChannel, RequestFormatType format) {
		webSocketChannelRequestFormatMap.put(clientChannel, format);
	}

	public void removeClientChannel(WebSocketChannel clientChannel) {
		webSocketChannelRequestFormatMap.remove(clientChannel);
	}

	// Cache access methods

	public void putCacheRestRequest(int id, HttpServerExchange exchange) {
		synchronized (requestIdRestSocketCache) {
			requestIdRestSocketCache.put(id, exchange);
		}
	}

	public void putCacheWebSocketRequest(int id, WebSocketChannel clientSocket) {
		synchronized (requestIdWebSocketCache) {
			requestIdWebSocketCache.put(id, clientSocket);
		}
	}

	public void putRequestFormatType(int id, RequestFormatType requestFormatType) {
		synchronized (requestIdRestFormatCache) {
			requestIdRestFormatCache.put(id, requestFormatType);
		}
	}

	public File getFileTokenTempFileFromCache(String fileToken) {
		return fileTokenTempFileCache.asMap().get(fileToken);
	}
}