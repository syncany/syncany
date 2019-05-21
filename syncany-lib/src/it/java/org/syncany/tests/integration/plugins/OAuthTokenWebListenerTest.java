package org.syncany.tests.integration.plugins;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.oauth.OAuthGenerator;
import org.syncany.plugins.transfer.oauth.OAuthMode;
import org.syncany.plugins.transfer.oauth.OAuthTokenExtractor;
import org.syncany.plugins.transfer.oauth.OAuthTokenExtractors;
import org.syncany.plugins.transfer.oauth.OAuthTokenFinish;
import org.syncany.plugins.transfer.oauth.OAuthTokenWebListener;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author Christian Roth (christian.roth@port17.de)
 */

public class OAuthTokenWebListenerTest {
	public static final String REFERENCE_TOKEN = "aabbccddeeff";

	private static final WebClient WEB_CLIENT = new WebClient(BrowserVersion.CHROME);

	@BeforeClass
	public static void setupWebClient() {
		WEB_CLIENT.getOptions().setThrowExceptionOnFailingStatusCode(false);
	}

	@AfterClass
	public static void closeWebClient() {
		WEB_CLIENT.closeAllWindows();
	}

	@Test(timeout = 30000)
	public void testTokenAsQuery() throws Exception {
		OAuthTokenWebListener twl = OAuthTokenWebListener
						.forMode(OAuthMode.SERVER)
						.build();

		URI baseUri = twl.start();
		Future<OAuthTokenFinish> submittedToken = twl.getToken();

		final URI requestUri = URI.create(baseUri.toString() + "?" + OAuthTokenExtractors.RFC_CODE_FIELD + "=" + REFERENCE_TOKEN + "&" + OAuthTokenExtractors.RFC_STATE_FIELD + "=1234");

		new Thread(new Runnable() {
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(2);

					System.out.println("Requesting: " + requestUri);
					HtmlPage page = WEB_CLIENT.getPage(requestUri.toURL());

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

	@Test(timeout = 30000)
	public void testTokenAsHash() throws Exception {
		OAuthTokenWebListener twl = OAuthTokenWebListener
						.forMode(OAuthMode.BROWSER)
						.build();

		URI baseUri = twl.start();
		Future<OAuthTokenFinish> submittedToken = twl.getToken();

		final URI requestUri = URI.create(baseUri.toString() + "?a=b#" + OAuthTokenExtractors.RFC_ACCESS_TOKEN_FIELD + "=" + REFERENCE_TOKEN + "&" + OAuthTokenExtractors.RFC_STATE_FIELD + "=1234");

		new Thread(new Runnable() {
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(2);

					System.out.println("Requesting: " + requestUri);
					HtmlPage page = WEB_CLIENT.getPage(requestUri.toURL());

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

	@Test(timeout = 30000)
	public void testMissingField() throws Exception {
		OAuthTokenWebListener twl = OAuthTokenWebListener
						.forMode(OAuthMode.SERVER)
						.build();

		URI baseUri = twl.start();
		Future<OAuthTokenFinish> submittedToken = twl.getToken();

		final URI requestUri = URI.create(baseUri.toString() + "?" + OAuthTokenExtractors.RFC_STATE_FIELD + "=1234");

		new Thread(new Runnable() {
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(2);

					System.out.println("Requesting: " + requestUri);
					HtmlPage page = WEB_CLIENT.getPage(requestUri.toURL());

					assertEquals(400, page.getWebResponse().getStatusCode());
				}
				catch (ScriptException e) {
					// ignore  "Connection is not open"
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}).start();

		assertNull(submittedToken.get());
	}

	@Test(timeout = 30000)
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
			return OAuthTokenExtractors.newTokenExtractorForMode(OAuthMode.BROWSER);
		}
	}

}