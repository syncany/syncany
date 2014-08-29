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
package org.syncany.gui.command;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.syncany.Client;
import org.syncany.config.Logging;
import org.syncany.config.UserConfig;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Request;
import org.syncany.operations.daemon.messages.Response;


/**
 *  
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class GUIClient extends Client {
	private static final Logger logger = Logger.getLogger(GUIClient.class.getSimpleName());
	
	private static final String userName = "admin";
	private static final String password = "admin";
	
	private static final String SERVER_SCHEMA = "https://";
	private static final String SERVER_HOSTNAME = "localhost";
	private static final String SERVER_REST_API = "/api/rs";
	
	static {
		Logging.init();
	}
	
	public Response runCommand(Request request) {
		return sendToRest(request, 8443, userName, password);
	}
	
	private Response sendToRest(Request request, int port, String userName, String password) {
		try {
			// Create authentication details
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
				new AuthScope(SERVER_HOSTNAME, port), 
				new UsernamePasswordCredentials(userName, password));
			
			// Allow all hostnames in CN; this is okay as long as hostname is localhost/127.0.0.1!
			// See: https://github.com/syncany/syncany/pull/196#issuecomment-52197017
			X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();			

			// Fetch the SSL context (using the user key/trust store)
			SSLContext sslContext = UserConfig.createUserSSLContext();
			
			// Create client with authentication details
			CloseableHttpClient client = HttpClients
				.custom()
				.setSslcontext(sslContext)
				.setHostnameVerifier(hostnameVerifier)
				.setDefaultCredentialsProvider(credentialsProvider)
				.build();

			String SERVER_URI = SERVER_SCHEMA + SERVER_HOSTNAME + ":" + port + SERVER_REST_API;
			HttpPost post = new HttpPost(SERVER_URI);
			
			logger.log(Level.INFO, "Sending HTTP Request to: " + SERVER_URI);
			
			post.setEntity(new StringEntity(MessageFactory.toRequest(request)));
			
			// Handle response
			HttpResponse httpResponse = client.execute(post);
			logger.log(Level.FINE, "Received HttpResponse: " + httpResponse);
			
			String responseStr = IOUtils.toString(httpResponse.getEntity().getContent());			
			logger.log(Level.FINE, "Responding to message with responseString: " + responseStr);
			
			Response response = MessageFactory.createResponse(responseStr);
			return response;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Request " + request.toString() + " FAILED. ", e);
		}		
		return null;
	}

	public void watchFolder(String folder) {
		// TODO Auto-generated method stub
		
	}
}
