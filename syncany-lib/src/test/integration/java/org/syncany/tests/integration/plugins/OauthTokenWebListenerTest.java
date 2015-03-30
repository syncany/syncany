package org.syncany.tests.integration.plugins;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.oauth.OAuthGenerator;
import org.syncany.plugins.transfer.oauth.OAuthTokenExtractor;
import org.syncany.plugins.transfer.oauth.OAuthTokenExtractors;
import org.syncany.plugins.transfer.oauth.OAuthTokenFinish;
import org.syncany.plugins.transfer.oauth.OAuthTokenInterceptors;
import org.syncany.plugins.transfer.oauth.OAuthTokenWebListener;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OAuthTokenWebListenerTest {

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
		OAuthTokenWebListener twl = OAuthTokenWebListener
						.forId("testSite")
						.setTokenExtractor(new OAuthTokenExtractors.NamedQueryTokenExtractor(TOKEN_ID))
						.build();

		URI baseUri = twl.start();
		Future<OAuthTokenFinish> submittedToken = twl.getToken();

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

		assertEquals(REFERENCE_TOKEN, submittedToken.get().getToken());
	}

	@Test
	public void testTokenAsHash() throws Exception {
		OAuthTokenWebListener twl = OAuthTokenWebListener
						.forId("testSite")
						.setTokenInterceptor(new OAuthTokenInterceptors.HashTokenInterceptor())
						.setTokenExtractor(new OAuthTokenExtractors.NamedQueryTokenExtractor(TOKEN_ID))
						.build();

		URI baseUri = twl.start();
		Future<OAuthTokenFinish> submittedToken = twl.getToken();

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

		assertEquals(REFERENCE_TOKEN, submittedToken.get().getToken());
	}

	@Test
	public void testOAuthGenerator() {
		OAuthGenerator testOAuthGenerator = new TestOAuthGenerator();

		assertThat(testOAuthGenerator, instanceOf(OAuthGenerator.class));
		assertThat(testOAuthGenerator, instanceOf(OAuthGenerator.WithExtractor.class));
		assertThat(testOAuthGenerator, not(instanceOf(OAuthGenerator.WithInterceptor.class)));
	}

	private static class TestOAuthGenerator implements OAuthGenerator, OAuthGenerator.WithExtractor {

		@Override
		public URI generateAuthUrl(URI redirectUri) throws StorageException {
			return URI.create("http://1234/?redirect=" + redirectUri);
		}

		@Override
		public void checkToken(String token, String csrfState) throws StorageException {
			// empty
		}

		@Override
		public OAuthTokenExtractor getExtractor() {
			return new OAuthTokenExtractors.NamedQueryTokenExtractor("custom");
		}
	}

}