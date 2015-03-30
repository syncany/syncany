package org.syncany.tests.integration.plugins;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.syncany.plugins.transfer.oauth.OauthTokenInterceptor;
import org.syncany.plugins.transfer.oauth.OauthTokenInterceptors;
import org.syncany.plugins.transfer.oauth.OauthTokenWebListener;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OauthTokenWebListenerTest {

	public static final String REFERENCE_TOKEN = "aabbccddeeff";
	public static final OauthTokenInterceptor TOKEN_EXTRACTOR = OauthTokenInterceptors.newHashTokenInterceptor("token");

	@Test
	public void testTokenAsParameter() throws Exception {
		OauthTokenWebListener twl = OauthTokenWebListener.forId("testSite").build();
		URI baseUri = twl.start();
		Future<String> submittedToken = twl.getToken();

		final URI requestUri = URI.create(baseUri.toString() + "?token=" + REFERENCE_TOKEN);
		final HttpClient client = HttpClientBuilder.create().build();
		final HttpGet uriRequest = new HttpGet(requestUri);

		new Thread(new Runnable() {
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(2);
					System.out.println("Requesting: " + requestUri);
					HttpResponse response = client.execute(uriRequest);
					assertEquals(200, response.getStatusLine().getStatusCode());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}).start();

		assertEquals(REFERENCE_TOKEN, submittedToken.get());
	}

}