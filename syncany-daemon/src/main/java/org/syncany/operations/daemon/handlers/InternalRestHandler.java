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
package org.syncany.operations.daemon.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.syncany.operations.daemon.DaemonEventBus;
import org.syncany.operations.daemon.DaemonWebServer;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Request;

/**
 * InteralRestHandler handles the REST requests sent to the daemon.
 *
 */
public class InternalRestHandler implements HttpHandler {
	private static final Logger logger = Logger.getLogger(InternalRestHandler.class.getSimpleName());
	private DaemonWebServer daemonWebServer;
	private DaemonEventBus eventBus;
	
	public InternalRestHandler(DaemonWebServer daemonWebServer) {
		this.daemonWebServer = daemonWebServer;
		eventBus = DaemonEventBus.getInstance();
	}
	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		handleRestRequest(exchange);
	}
	
	private void handleRestRequest(HttpServerExchange exchange) throws IOException {
		logger.log(Level.INFO, "HTTP request received:" + exchange.getRelativePath());

		exchange.startBlocking();			

		if (exchange.getRelativePath().startsWith("/file/")) {	
			String tempFileToken = exchange.getRelativePath().substring("/file/".length());
			File tempFile = daemonWebServer.fileTokenTempFileCacheGet(tempFileToken);
			
			logger.log(Level.INFO, "- Temp file: " + tempFileToken);
			
			IOUtils.copy(new FileInputStream(tempFile), exchange.getOutputStream());
			
			exchange.endExchange();
		}
		else {	
			String message = IOUtils.toString(exchange.getInputStream());
			logger.log(Level.INFO, "REST message received: " + message);
	
			try {
				Request request = MessageFactory.createRequest(message);
	
				daemonWebServer.cacheRestRequest(request.getId(), exchange);
				
				eventBus.post(request);
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
				eventBus.post(new BadRequestResponse(-1, "Invalid request."));
			}
		}
	}
}
