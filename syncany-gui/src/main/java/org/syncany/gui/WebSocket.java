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
package org.syncany.gui;

import io.undertow.util.StringWriteChannelListener;
import io.undertow.websockets.WebSocketExtension;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSinkFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketFrameType;
import io.undertow.websockets.core.WebSocketVersion;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.security.sasl.Sasl;

import org.apache.commons.codec.binary.Base64;
import org.syncany.config.UserConfig;
import org.syncany.util.StringUtil;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Property;
import org.xnio.Sequence;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

/**
 * @author vwiencek
 *
 */
public class WebSocket {
	private final Pool<ByteBuffer> buffer = new ByteBufferSlicePool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 1024, 1024);
	
	public static void main(String[] args) throws Exception {
		WebSocket ws = new WebSocket();
		ws.start();
	}
	
	public void start() throws Exception{
		//DefaultServer.setRootHandler(AutobahnWebSocketServer.getRootHandler());
        Xnio xnio = Xnio.getInstance(this.getClass().getClassLoader());
        
        SSLContext sslContext = UserConfig.createUserSSLContext();        
        
        OptionMap workerOptions = OptionMap.builder()
	        .set(Options.WORKER_IO_THREADS, 2)
	        .set(Options.WORKER_TASK_CORE_THREADS, 30)
	        .set(Options.WORKER_TASK_MAX_THREADS, 30)
	        .set(Options.SSL_PROTOCOL, sslContext.getProtocol())
	        .set(Options.SSL_PROVIDER, sslContext.getProvider().getName())
	        //.set(Options.SASL_POLICY_PASS_CREDENTIALS, Boolean.TRUE)
	        //.set(Options.SASL_PROPERTIES,  Sequence.of(tempProperties))
	        .set(Options.TCP_NODELAY, true)
	        .set(Options.CORK, true)
	        .getMap();
        
        XnioWorker worker = xnio.createWorker(workerOptions);
        XnioSsl xnioSsl = new JsseXnioSsl(xnio, OptionMap.create(Options.USE_DIRECT_BUFFERS, true), sslContext);
        URI uri = new URI("wss://localhost:8443/api/ws");
        
        WebSocketClientNegotiation neg = new WebSocketClientNegotiation(new ArrayList<String>(), new ArrayList<WebSocketExtension>()) {
        	@Override
        	public void beforeRequest(Map<String, String> headers) {
        		headers.put("Authorization", "Basic " + Base64.encodeBase64String(StringUtil.toBytesUTF8("admin:admin")));
        	}
        };        

        final WebSocketChannel webSocketChannel = WebSocketClient.connect(worker, xnioSsl, buffer, workerOptions, uri, WebSocketVersion.V13, neg).get();

        webSocketChannel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                String data = message.getData();
                System.out.println(data);
            }

            @Override
            protected void onError(WebSocketChannel channel, Throwable error) {
                super.onError(channel, error);
                error.printStackTrace();
            }
        });
        webSocketChannel.resumeReceives();
        
        System.out.println("asdasd");
        StreamSinkFrameChannel streamSinkFrameChannel = webSocketChannel.send(WebSocketFrameType.TEXT);
        new StringWriteChannelListener("<listWatchesManagementRequest><id>1</id></listWatchesManagementRequest>").setup(streamSinkFrameChannel);        
	}
}
