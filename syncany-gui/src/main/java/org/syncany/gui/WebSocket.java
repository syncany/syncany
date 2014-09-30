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

import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketVersion;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.security.sasl.Sasl;

import org.syncany.config.UserConfig;
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
        
        SSLContext context = UserConfig.createUserSSLContext();
        
        List<Property> tempProperties = new ArrayList<Property>();
        
        tempProperties.add(Property.of(Sasl.POLICY_NOPLAINTEXT, Boolean.FALSE.toString()));
        tempProperties.add(Property.of(Sasl.CREDENTIALS, "admin:admin"));
        
        OptionMap map = OptionMap.builder()
	        .set(Options.WORKER_IO_THREADS, 2)
	        .set(Options.WORKER_TASK_CORE_THREADS, 30)
	        .set(Options.WORKER_TASK_MAX_THREADS, 30)
	        .set(Options.SSL_PROTOCOL, context.getProtocol())
	        .set(Options.SSL_PROVIDER, context.getProvider().getName())
	        .set(Options.SASL_POLICY_PASS_CREDENTIALS, Boolean.TRUE)
	        .set(Options.SASL_PROPERTIES,  Sequence.of(tempProperties))
	        .set(Options.TCP_NODELAY, true)
	        .set(Options.CORK, true)
	        .getMap();
        
        XnioWorker worker = xnio.createWorker(map);
        XnioSsl xnioSsl = new JsseXnioSsl(xnio, OptionMap.create(Options.USE_DIRECT_BUFFERS, true), context);
        URI uri = new URI("wss://localhost:8443/api/ws/");

        final WebSocketChannel webSocketChannel = WebSocketClient.connect(worker, xnioSsl, buffer, map, uri, WebSocketVersion.V13).get();

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

	}
}
