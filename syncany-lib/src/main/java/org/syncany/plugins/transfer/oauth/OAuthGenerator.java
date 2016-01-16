/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.plugins.transfer.oauth;

import java.net.URI;

import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * For {@link TransferPlugin}s that base their authentication on OAuth,
 * a generator class can be used to create the authentication URL and
 * check the user-provided token. The concrete implementation of this
 * interface should be provided to the {@link TransferSettings} class
 * via the {@link OAuth} annotation.<br/>
 * A generator can be extended with {@link OAuthGenerator.WithInterceptor},
 * {@link OAuthGenerator.WithExtractor} and
 * {@link OAuthGenerator.WithNoRedirectMode}.
 *
 * @author Philipp Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public interface OAuthGenerator {
	/**
	 * Generate a URL which can be accessed by a user to authorize Syncany
	 *
	 * @param redirectUri The URL to which the OAuth provider should redirect in any case (either success or failure)
	 * @return A URL to authorize Syncany using the provided redirectUri
	 * @throws StorageException
	 */
	URI generateAuthUrl(URI redirectUri) throws StorageException;

	/**
	 * Validate the given token and (optional) csrf parameter.
	 *
	 * @param token Token provided by {@link OAuthTokenWebListener}.
	 * @param csrfState Content of the state parameter (optional).
	 * @throws StorageException
	 */
	void checkToken(String token, /*@Nullable*/ String csrfState) throws StorageException;

	// Annotation don't support concrete instances of objects, only classes. Thus we need to add two additional interfaces
	// if a plugin requires custom interceptors or extractors

	/**
	 * Use a custom {@link OAuthTokenInterceptor} instead of the default one which depends on the {@link OAuthMode} in use.
	 *
	 * @see {@link OAuthTokenInterceptor}
	 */
	interface WithInterceptor {
		OAuthTokenInterceptor getInterceptor();
	}

	/**
	 * Use a custom {@link OAuthTokenExtractor} instead of the default one which depends on the {@link OAuthMode} in use.
	 *
	 * @see {@link OAuthTokenExtractor}
	 */
	interface WithExtractor {
		OAuthTokenExtractor getExtractor();
	}

	/**
	 * If an OAuth based plugin also supports copy&pasting a token from a website it should extend this interface.
	 */
	interface WithNoRedirectMode {
		/**
		 * Called if Syncany is started in headless mode which does not support redirect_to URLs.<br/>
		 * The website should output a token which can be copied over to Syncany's repo wizard.
		 *
		 * @return A URL with no redirect URL
		 * @throws StorageException
		 */
		URI generateAuthUrl() throws StorageException;
	}
}
