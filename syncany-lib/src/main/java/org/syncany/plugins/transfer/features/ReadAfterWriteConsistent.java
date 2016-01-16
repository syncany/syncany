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
package org.syncany.plugins.transfer.features;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Some storage backends do not guarantee that a file immediately exists on the
 * remote side after it is uploaded or moved.<br/>
 * This feature handles such cases by relaxing the strong assumption that a file
 * is immediately available after creation due to upload or move operations.
 *
 * <p>The {@link ReadAfterWriteConsistentFeatureTransferManager} throttles existence check
 * using a simple exponential method:<br/>
 *
 * <code>throttle(n) = 3 ^ n * 100 ms, with n being the current iteration
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
@Feature(required = false)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadAfterWriteConsistent {
	/**
	 * @see ReadAfterWriteConsistentFeatureExtension
	 */
	Class<? extends ReadAfterWriteConsistentFeatureExtension> extension();

	/**
	 * Define how often a transfer manager will try to find a file on the remote side.
	 */
	int maxRetries() default 5;

	/**
	 * Define the maximum wait time until a file has to exist on the remote side (given in milliseconds).
	 */
	int maxWaitTime() default 10000;
}
