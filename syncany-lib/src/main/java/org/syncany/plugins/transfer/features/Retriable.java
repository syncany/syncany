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

import org.syncany.plugins.transfer.TransferManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Feature annotation to make a transfer manager more reliable by making 
 * its core methods retriable. 
 * 
 * <p>This annotation is only recognized if used on a {@link TransferManager}. If
 * applied, it wraps the original transfer manager in a {@link RetriableFeatureTransferManager}.
 * 
 * <p>The options that can be defined in this feature annotation are how often a method
 * will be retried, and how long the sleep interval between these retries is.
 * 
 * @author Christian Roth (christian.roth@port17.de)
 */
@Feature(required = true)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retriable {
	/**
	 * Defines the number of retries each method in a transfer method will
	 * be retried if it fails.
	 */
	int numberRetries() default 3;
	
	/**
	 * Defines the number of milliseconds to wait between each
	 * retry attempts.
	 */
	int sleepInterval() default 3000;
}
