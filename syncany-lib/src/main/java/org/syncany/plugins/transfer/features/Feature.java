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

import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferManagerFactory;

/**
 * Annotation to identify and configure {@link TransferManager} extensions.
 * 
 * <p>Features are extensions for transfer managers, such as path awareness,
 * transaction awareness or retriability. This annotation is used to mark
 * a feature annotation, which in turn corresponds to a {@link FeatureTransferManager}.
 * 
 * <p><b>Note</b>: This annotation must only be used to annotate feature
 * annotations, i.e. it is an annotation annotation. It must not be used to
 * mark feature transfer managers directly.
 * 
 * @see TransferManager
 * @see TransferManagerFactory
 * @see FeatureTransferManager
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Feature {
	/**
	 * Specifies whether or not a feature is required. 
	 * 
	 * <p>If a feature is required, but the concrete feature annotation
	 * is not present at the original transfer manager, the {@link TransferManagerFactory}
	 * will throw an exception.
	 */
	boolean required();
}
