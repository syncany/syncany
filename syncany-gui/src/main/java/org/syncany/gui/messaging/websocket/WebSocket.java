/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui.messaging.websocket;

import io.undertow.websockets.WebSocketExtension;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketVersion;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.syncany.config.ConfigException;
import org.syncany.config.LocalEventBus;
import org.syncany.config.UserConfig;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.UserTO;
import org.syncany.gui.Launcher;
import org.syncany.operations.daemon.messages.api.Message;
import org.syncany.operations.daemon.messages.api.MessageFactory;
import org.syncany.operations.daemon.messages.api.Request;
import org.syncany.util.StringUtil;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

import com.google.common.eventbus.Subscribe;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WebSocket {
	private static final Logger logger = Logger.getLogger(WebSocket.class.getSimpleName());
	private final static String PROTOCOL = "wss://";
	private final static String ENDPOINT = "/api/ws";
	
	private WebSocketChannel webSocketChannel;
	private LocalEventBus bus = LocalEventBus.getInstance();
	
	private DaemonConfigTO daemonConfig = null;
	
	public void init(){
		File daemonConfigFile = new File(UserConfig.getUserConfigDir(), UserConfig.DAEMON_FILE);
		
		if (daemonConfigFile.exists()) {
			try {
				daemonConfig = DaemonConfigTO.load(daemonConfigFile);
				List<UserTO> users = readWebSocketServerUsers(daemonConfig);
				bus.register(this);
				LocalEventBus.getInstance().register(this);
				if (users.size() > 0){
					start(users.get(0).getUsername(), users.get(0).getPassword());
				}
			}
			catch (ConfigException e) {
				logger.log(Level.WARNING, "Unable to load daemon configuration");
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Unable to start websocket");
			}
		}
	}
	
	private List<UserTO> readWebSocketServerUsers(DaemonConfigTO daemonConfigTO) {
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

	private void start(final String username, final String password) throws Exception {
        SSLContext sslContext = UserConfig.createUserSSLContext();        
        Xnio xnio = Xnio.getInstance(this.getClass().getClassLoader());
        Pool<ByteBuffer> buffer = new ByteBufferSlicePool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 1024, 1024);
    	
        OptionMap workerOptions = OptionMap.builder()
	        .set(Options.WORKER_IO_THREADS, 2)
	        .set(Options.WORKER_TASK_CORE_THREADS, 30)
	        .set(Options.WORKER_TASK_MAX_THREADS, 30)
	        .set(Options.SSL_PROTOCOL, sslContext.getProtocol())
	        .set(Options.SSL_PROVIDER, sslContext.getProvider().getName())
	        .set(Options.TCP_NODELAY, true)
	        .set(Options.CORK, true)
	        .getMap();
        
        XnioWorker worker = xnio.createWorker(workerOptions);
        XnioSsl xnioSsl = new JsseXnioSsl(xnio, OptionMap.create(Options.USE_DIRECT_BUFFERS, true), sslContext);
        URI uri = new URI(PROTOCOL + daemonConfig.getWebServer().getBindAddress() + ":" + daemonConfig.getWebServer().getBindPort() + ENDPOINT);
        
        WebSocketClientNegotiation neg = new WebSocketClientNegotiation(new ArrayList<String>(), new ArrayList<WebSocketExtension>()) {
        	@Override
        	public void beforeRequest(Map<String, String> headers) {
        		headers.put("Authorization", "Basic " + Base64.encodeBase64String(StringUtil.toBytesUTF8(username + ":" + password)));
        	}
        };        

        webSocketChannel = WebSocketClient.connect(worker, xnioSsl, buffer, workerOptions, uri, WebSocketVersion.V13, neg).get();
        webSocketChannel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
            	Message m;
				try {
					m = MessageFactory.toMessage(message.getData());
					bus.post(m);
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Unable to parse message: "+e);
				}
            }

            @Override
            protected void onError(WebSocketChannel channel, Throwable error) {
            	logger.log(Level.WARNING, "Error: "+error.getMessage());
            	markAsDeconnected();
            }
        });
        webSocketChannel.resumeReceives();
        
	}
	
	protected void markAsDeconnected() {
		try {
			Thread.sleep(5000);
			Launcher.startDeamon();
			init();
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Unable to start daemon");
		}
		catch (InterruptedException e) {
			logger.log(Level.WARNING, "Unable to start daemon");
		}
	}

	@Subscribe
	public void requestSubscription(Request request){
		//String message = "<listWatchesManagementRequest><id>1</id></listWatchesManagementRequest>";
		try{
			postMessage(MessageFactory.toXml(request));
		}
		catch (Exception e){
			logger.log(Level.WARNING, "Unable to transform request to XML");
		}
	}
	
	private void postMessage(String message){
		WebSockets.sendText(message, webSocketChannel, new WebSocketCallback<Void>() {
			
			@Override
			public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
				throwable.printStackTrace();
			}
			
			@Override
			public void complete(WebSocketChannel channel, Void context) {
				System.out.println("complete");
			}
		});  
	}
}
