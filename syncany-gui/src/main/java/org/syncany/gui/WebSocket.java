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
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketVersion;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.xnio.ByteBufferSlicePool;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

/**
 * @author vwiencek
 *
 */
public class WebSocket {

	public static void main(String[] args) throws IllegalArgumentException, IOException, URISyntaxException {
		WebSocket ws = new WebSocket();
		ws.start();
	}
	
	public void start() throws IllegalArgumentException, IOException, URISyntaxException{
		XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true));
        Pool<ByteBuffer> buffers = new ByteBufferSlicePool(1024, 10240);
        
        URI uri = new URI("ws://admin:admin@localhost:8443/api/ws");
        
        IoFuture<WebSocketChannel> futureChannel = WebSocketClient.connect(worker, buffers, OptionMap.EMPTY, uri, WebSocketVersion.V13);
        
        WebSocketChannel channel = futureChannel.get();
        
	}
}
