package org.syncany.plugins.transfer.oauth;

import java.net.URI;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.google.common.base.Charsets;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class OAuthTokenExtractors {

	public static NamedQueryTokenExtractor newNamedQueryTokenExtractor() {
		return new NamedQueryTokenExtractor();
	}

	public static NamedQueryTokenExtractor newNamedQueryTokenExtractor(String tokenId) {
		return new NamedQueryTokenExtractor(tokenId);
	}

	public static class NamedQueryTokenExtractor implements OAuthTokenExtractor {

		public static final String DEFAULT_TOKEN_ID = "token";

		private final String tokenId;

		public NamedQueryTokenExtractor() {
			this.tokenId = DEFAULT_TOKEN_ID;
		}

		public NamedQueryTokenExtractor(String tokenId) {
			this.tokenId = tokenId;
		}

		@Override
		public String parse(String uriWithToken) throws NoSuchFieldException {
			List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uriWithToken), Charsets.UTF_8.name());

			for (NameValuePair param : params) {
				if (tokenId.equalsIgnoreCase(param.getName())) {
					return param.getValue();
				}
			}

			throw new NoSuchFieldException(String.format("URI (%s) does not contain token field (%s)", uriWithToken, tokenId));
		}
	}

}
