package org.syncany.plugins.transfer.oauth;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.google.common.base.Charsets;

/**
 * Factory class to generate some common {@link OAuthTokenExtractor}s.
 *
 * @author Christian Roth (christian.roth@port17.de)
 */
public abstract class OAuthTokenExtractors {
	private static final Logger logger = Logger.getLogger(OAuthTokenExtractors.class.getName());

	public static final String RFC_CODE_FIELD = "code";
	public static final String RFC_ACCESS_TOKEN_FIELD = "access_token";
	public static final String RFC_STATE_FIELD = "state";

	/**
	 * Get a common {@link OAuthTokenExtractor} depending on the chosen {@link OAuthMode}. More precisely, this creates a
	 * {@link OAuthTokenExtractors.NamedQueryTokenExtractor} with token field id set to
	 * {@value #RFC_STATE_FIELD} in {@link OAuthMode#SERVER} and {@value #RFC_ACCESS_TOKEN_FIELD} in {@link OAuthMode#BROWSER}.
	 * However, {@value #RFC_STATE_FIELD} is used in both cases to identify a potential CSRF value.
	 *
	 * @param mode {@link OAuthMode} supported by the {@link org.syncany.plugins.transfer.TransferPlugin}.
	 * @return A corresponding {@link OAuthTokenExtractors.NamedQueryTokenExtractor}.
	 */
	public static OAuthTokenExtractor newTokenExtractorForMode(OAuthMode mode) {
		switch (mode) {
			case BROWSER:
				return new NamedQueryTokenExtractor(RFC_ACCESS_TOKEN_FIELD, RFC_STATE_FIELD);

			case SERVER:
				return new NamedQueryTokenExtractor(RFC_CODE_FIELD, RFC_STATE_FIELD);

			default:
				throw new RuntimeException("Unknown OAuth mode");
		}
	}

	/**
	 * A {@link NamedQueryTokenExtractor} is a simple {@link OAuthTokenExtractor} which looks for a token and a CSRF secret
	 * in the redirect URL. Field names a variables.
	 */
	public static class NamedQueryTokenExtractor implements OAuthTokenExtractor {
		private final String tokenId;
		private final String stateId;

		NamedQueryTokenExtractor(String tokenId, String stateId) {
			this.tokenId = tokenId;
			this.stateId = stateId;
		}

		@Override
		public OAuthTokenFinish parse(String uriWithToken) throws NoSuchFieldException {
			List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uriWithToken), Charsets.UTF_8.name());

			String token = null;
			String state = null;

			for (NameValuePair param : params) {
				if (tokenId.equalsIgnoreCase(param.getName())) {
					token = param.getValue();
					logger.log(Level.FINE, "Found token in URL " + token);
				}
				else if (stateId.equalsIgnoreCase(param.getName())) {
					state = param.getValue();
					logger.log(Level.FINE, "Found state in URL " + state);
				}
			}

			if (token == null || state == null) {
				throw new NoSuchFieldException(String.format("URI (%s) does not contain token field (%s, %s)", uriWithToken, tokenId, stateId));
			}

			return new OAuthTokenFinish(token, state);
		}
	}

}
