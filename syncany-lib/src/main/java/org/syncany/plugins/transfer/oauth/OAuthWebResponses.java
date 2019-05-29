package org.syncany.plugins.transfer.oauth;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import io.undertow.util.StatusCodes;

/**
 * Factory class to generate some common {@link OAuthWebResponse}s. It uses html sites residing in the ressource folder,
 * but provides plain text fallbacks if a specific site cannot be read.
 *
 * @author Christian Roth (christian.roth@port17.de)
 */

public abstract class OAuthWebResponses {

	public static final String RESOURCE_DIR = "/org/syncany/plugins/oauth/";

	/**
	 * This {@link OAuthWebResponse} is used when a token was succesfully extracted. It uses <code>ValidWebResponse.html</code> which should be placed in
	 * {@value #RESOURCE_DIR}.
	 *
	 * @return Either the parsed html file or a fallback string.
	 */
	public static OAuthWebResponse createValidResponse() {
		return new OAuthWebResponse(StatusCodes.OK, loadHtml("ValidWebResponse.html", "Token successfully extracted."));
	}

	/**
	 * This {@link OAuthWebResponse} is used when there was an error during the OAuth process. It uses <code>BadRequestWebResponse.html</code> which
	 * should be placed in {@value #RESOURCE_DIR}.
	 *
	 * @return Either the parsed html file or a fallback string.
	 */
	public static OAuthWebResponse createBadResponse() {
		return new OAuthWebResponse(StatusCodes.BAD_REQUEST, loadHtml("BadRequestWebResponse.html", "Error while acquiring token."));
	}

	private static String loadHtml(String fileName, String fallbackString) {
		String html = fallbackString;

		try (InputStream resource = OAuthWebResponses.class.getResourceAsStream(RESOURCE_DIR + fileName)) {
			html = IOUtils.toString(resource);
		}
		catch (Exception e) {
			// use fallback plain string
		}

		return html;
	}

}
