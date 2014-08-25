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

import java.io.File;
import java.util.Arrays;
import java.util.Random;
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
import org.simpleframework.xml.core.Persister;
import org.syncany.cli.Command;
import org.syncany.config.UserConfig;
import org.syncany.config.to.PortTO;
import org.syncany.operations.daemon.messages.CliRequest;
import org.syncany.operations.daemon.messages.CliResponse;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Response;

/**
 * @author vwiencek
 *
 */
public class CommandFactory {
	private static final Logger logger = Logger.getLogger(CommandFactory.class.getSimpleName());
	
	private static final String SERVER_SCHEMA = "https://";
	private static final String SERVER_HOSTNAME = "127.0.0.1";
	private static final String SERVER_REST_API = "/api/rs";
	
	private PortTO readPortConfig(File portFile) {
		try {
			return new Persister().read(PortTO.class, portFile);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "ERROR: Could not read portFile to connect to daemon.", e);

			showErrorAndExit("Cannot connect to daemon.");			
			return null; // Never reached!
		}
	}
	
	private void showErrorAndExit(String errorMessage) {

	}
	
	public int sendToRest(Command command, String commandName, String[] commandArgs, File portFile) {
		try {
			// Read port config (for daemon) from port file
			PortTO portConfig = readPortConfig(portFile);

			// Create authentication details
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
				new AuthScope(SERVER_HOSTNAME, portConfig.getPort()), 
				new UsernamePasswordCredentials(portConfig.getUser().getUsername(), portConfig.getUser().getPassword()));
			
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

			String SERVER_URI = SERVER_SCHEMA + SERVER_HOSTNAME + ":" + portConfig.getPort() + SERVER_REST_API;
			HttpPost post = new HttpPost(SERVER_URI);
			
			logger.log(Level.INFO, "Sending HTTP Request to: " + SERVER_URI);
			
			// Create and send HTTP/REST request
			CliRequest cliRequest = new CliRequest();
			
			cliRequest.setId(Math.abs(new Random().nextInt()));
			cliRequest.setRoot("rootFolder");
			cliRequest.setCommand(commandName);
			cliRequest.setCommandArgs(Arrays.asList(commandArgs));
			
			post.setEntity(new StringEntity(MessageFactory.toRequest(cliRequest)));
			
			// Handle response
			HttpResponse httpResponse = client.execute(post);
			logger.log(Level.FINE, "Received HttpResponse: " + httpResponse);
			
			String responseStr = IOUtils.toString(httpResponse.getEntity().getContent());			
			logger.log(Level.FINE, "Responding to message with responseString: " + responseStr);
			
			Response response = MessageFactory.createResponse(responseStr);
			
			if (response instanceof CliResponse) {
				//out.print(((CliResponse) response).getOutput());	
			}
			else {
				//out.println(response.getMessage());
			}
			
			return 0;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Command " + command.toString() + " FAILED. ", e);
			showErrorAndExit(e.getMessage());
			return -1;
		}		
	}
}
