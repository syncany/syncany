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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.syncany.config.LocalEventBus;
import org.syncany.operations.daemon.WebServer;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.api.MessageFactory;
import org.syncany.operations.daemon.messages.api.Request;

/**
 * InteralRestHandler handles the REST requests sent to the daemon.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class InternalRestHandler implements HttpHandler {
	private static final Logger logger = Logger.getLogger(InternalRestHandler.class.getSimpleName());
	private WebServer daemonWebServer;
	private LocalEventBus eventBus;
	
	public InternalRestHandler(WebServer daemonWebServer) {
		this.daemonWebServer = daemonWebServer;
		this.eventBus = LocalEventBus.getInstance();
	}
	
	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		handleRestRequest(exchange);
	}
	
	private void handleRestRequest(HttpServerExchange exchange) throws IOException {
		logger.log(Level.INFO, "HTTP request received:" + exchange.getRelativePath());

		exchange.startBlocking();			

		if (exchange.getRelativePath().startsWith("/file/")) {	
			handleFileRequest(exchange);			
		}
		else {	
			handleNormalRequest(exchange);			
		}
	}

	private void handleNormalRequest(HttpServerExchange exchange) throws IOException {
		String message = IOUtils.toString(exchange.getInputStream()); // TODO [high] Read entire file to memory. Dangerous!
		logger.log(Level.INFO, "REST message received: " + message);

		try {
			Request request = MessageFactory.toRequest(message);

			daemonWebServer.putCacheRestRequest(request.getId(), exchange);				
			eventBus.post(request);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Invalid request received; cannot serialize to Request.", e);
			eventBus.post(new BadRequestResponse(-1, "Invalid request."));
		}
	}

	private void handleFileRequest(HttpServerExchange exchange) throws FileNotFoundException, IOException {
		String tempFileToken = exchange.getRelativePath().substring("/file/".length());
		File tempFile = daemonWebServer.getFileTokenTempFileFromCache(tempFileToken);
		
		if (tempFile != null) {
			logger.log(Level.INFO, "- Temp file: " + tempFileToken);
			
			IOUtils.copy(new FileInputStream(tempFile), exchange.getOutputStream());				
			exchange.endExchange();
		}
		else {
			logger.log(Level.WARNING, "Invalid request received; Cannot find file token " + tempFileToken);
			eventBus.post(new BadRequestResponse(-1, "Invalid request."));
		}
	}
}
