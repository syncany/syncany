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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.syncany.plugins.transfer.TransferSettings;

/**
 * This annotation is used to identify OAuth plugins by marking the corresponding
 * {@link TransferSettings} class. An OAuth plugin will provide a 'token' field
 * during the initialization process and the {@link OAuthGenerator} (provided via the
 * help of the {@link #value()} field) will be able to check that token.
 *
 * @author Philipp Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAuth {
	String PLUGIN_ID = "%pluginid%";
	int RANDOM_PORT = -1;

	/**
	 * @see OAuthGenerator
	 */
	Class<? extends OAuthGenerator> value();

	/**
	 * The default Mode is {@link OAuthMode#SERVER}
	 *
	 * @see OAuthMode
	 */
	OAuthMode mode() default OAuthMode.SERVER;

	/**
	 * If no specific port is provided (or {@value #RANDOM_PORT} is used), the {@link OAuthTokenWebListener} will choose a
	 * random port from the range of {@value OAuthTokenWebListener#PORT_LOWER} and
	 * {@value OAuthTokenWebListener#PORT_UPPER}.<br/>
	 * Needed if an OAuth provider uses preset and strict redirect URLs.
	 */
	int callbackPort() default RANDOM_PORT; // -1 is random

	/**
	 * If no specific name is provided (or {@value #PLUGIN_ID} is used), the {@link OAuthTokenWebListener} will choose a
	 * random identifier for the OAuth process.<br/>
	 * Needed if an OAuth provider uses preset and strict redirect URLs.
	 */
	String callbackId() default PLUGIN_ID; // equals plugin id
}
