package org.syncany.plugins.transfer.oauth;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.google.common.base.Charsets;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public abstract class OAuthTokenExtractors {
	private static final Logger logger = Logger.getLogger(OAuthTokenExtractors.class.getName());

	public static final String RFC_CODE_FIELD = "code";
	public static final String RFC_ACCESS_TOKEN_FIELD = "access_token";
	public static final String RFC_STATE_FIELD = "state";

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
				else if (this.stateId.equalsIgnoreCase(param.getName())) {
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
