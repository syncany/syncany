package org.syncany.tests.integration.plugins;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.plugins.transfer.oauth.OauthTokenExtractors;
import org.syncany.plugins.transfer.oauth.OauthTokenInterceptors;
import org.syncany.plugins.transfer.oauth.OauthTokenWebListener;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OauthTokenWebListenerTest {

	public static final String TOKEN_ID = "token_field";
	public static final String REFERENCE_TOKEN = "aabbccddeeff";

	private WebClient webClient;

	@Before
	public void createWebclient() {
		webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	}

	@After
	public void closeWebclient() {
		webClient.closeAllWindows();
		webClient = null;
	}

	@Test
	public void testTokenAsQuery() throws Exception {
		OauthTokenWebListener twl = OauthTokenWebListener
						.forId("testSite")
						.setTokenExtractor(OauthTokenExtractors.newNamedQueryOauthTokenExtractor(TOKEN_ID))
						.build();

		URI baseUri = twl.start();
		Future<String> submittedToken = twl.getToken();

		final URI requestUri = URI.create(baseUri.toString() + "?" + TOKEN_ID + "=" + REFERENCE_TOKEN);

		new Thread(new Runnable() {
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(2);
					System.out.println("Requesting: " + requestUri);
					HtmlPage page = webClient.getPage(requestUri.toURL());

					assertEquals(200, page.getWebResponse().getStatusCode());
				}
				catch (ScriptException e) {
					// ignore  "Connection is not open"
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}).start();

		assertEquals(REFERENCE_TOKEN, submittedToken.get());
	}

	@Test
	public void testTokenAsHash() throws Exception {
		OauthTokenWebListener twl = OauthTokenWebListener
						.forId("testSite")
						.setTokenInterceptor(OauthTokenInterceptors.newHashTokenInterceptor())
						.setTokenExtractor(OauthTokenExtractors.newNamedQueryOauthTokenExtractor(TOKEN_ID))
						.build();

		URI baseUri = twl.start();
		Future<String> submittedToken = twl.getToken();

		final URI requestUri = URI.create(baseUri.toString() + "?a=b#" + TOKEN_ID + "=" + REFERENCE_TOKEN);

		new Thread(new Runnable() {
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(2);

					System.out.println("Requesting: " + requestUri);
					HtmlPage page = webClient.getPage(requestUri.toURL());

					assertEquals(200, page.getWebResponse().getStatusCode());
				}
				catch (ScriptException e) {
					// ignore  "Connection is not open"
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}).start();

		assertEquals(REFERENCE_TOKEN, submittedToken.get());
	}

}