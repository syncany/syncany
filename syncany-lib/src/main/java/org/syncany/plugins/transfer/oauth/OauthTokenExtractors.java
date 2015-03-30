package org.syncany.plugins.transfer.oauth;

import java.net.URI;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.google.common.base.Charsets;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public abstract class OAuthTokenExtractors {

	public static class NamedQueryTokenExtractor implements OAuthTokenExtractor {

		public static final String DEFAULT_TOKEN_ID = "access_token";
		public static final String DEFAULT_CSRF_ID = "state";

		private final String tokenId;
		private final String csrfId;

		public NamedQueryTokenExtractor() {
			this.tokenId = DEFAULT_TOKEN_ID;
			this.csrfId = DEFAULT_CSRF_ID;
		}

		public NamedQueryTokenExtractor(String tokenId) {
			this.tokenId = tokenId;
			this.csrfId = DEFAULT_CSRF_ID;
		}

		public NamedQueryTokenExtractor(String tokenId, String csrfId) {
			this.tokenId = tokenId;
			this.csrfId = csrfId;
		}

		@Override
		public OAuthTokenFinish parse(String uriWithToken) throws NoSuchFieldException {
			List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uriWithToken), Charsets.UTF_8.name());

			String token = null;
			String state = null;

			for (NameValuePair param : params) {
				if (tokenId.equalsIgnoreCase(param.getName())) {
					token = param.getValue();
				}
				else if (csrfId.equalsIgnoreCase(param.getName())) {
					state = param.getValue();
				}
			}

			if (token == null || state == null) {
				throw new NoSuchFieldException(String.format("URI (%s) does not contain token field (%s)", uriWithToken, tokenId));
			}

			return new OAuthTokenFinish(token, state);
		}
	}

}
