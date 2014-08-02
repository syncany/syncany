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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.plugins.Plugins;
import org.syncany.plugins.web.WebInterfacePlugin;

/**
 * InternalWebInterfaceHandler is responsible for handling requests 
 * to the web interface.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class InternalWebInterfaceHandler implements HttpHandler {
	private static final Logger logger = Logger.getLogger(InternalWebInterfaceHandler.class.getSimpleName());
	
	private List<WebInterfacePlugin> webInterfacePlugins;
	private WebInterfacePlugin webInterfacePlugin;
	private HttpHandler requestHandler;
	
	public InternalWebInterfaceHandler() {
		webInterfacePlugins = Plugins.list(WebInterfacePlugin.class);
		
		if (webInterfacePlugins.size() == 1) {
			initWebInterfacePlugin();
		}
	}

	private void initWebInterfacePlugin() {
		try {				
			webInterfacePlugin = webInterfacePlugins.iterator().next();
			requestHandler = webInterfacePlugin.createRequestHandler();
			
			logger.log(Level.INFO, "Starting webInterfacePlugin: " + webInterfacePlugin.getId());
			webInterfacePlugin.start();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (requestHandler != null) {
			handleRequestWithResourceHandler(exchange);
		}
		else {
			handleRequestNoHandler(exchange);
		}
	}

	private void handleRequestWithResourceHandler(HttpServerExchange exchange) throws Exception {
		logger.log(Level.FINE, "Sending request to webInterfacePlugin handler: " + exchange.toString());
		requestHandler.handleRequest(exchange);
	}

	private void handleRequestNoHandler(HttpServerExchange exchange) {
		if (webInterfacePlugins.size() == 0) {
			exchange.getResponseSender().send("No web interface configured.");
		}
		else {
			exchange.getResponseSender().send("Only one web interface can be installed, but " + webInterfacePlugins.size() + " plugins found.");
		}
	}
}
