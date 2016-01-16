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
import org.syncany.plugins.transfer.TransferManagerFactory;

/**
 * Feature transfer managers extend the functionality of regular {@link TransferManager TransferManagers}
 * by adding special behavior (such as path awareness, transaction awareness or retriability).
 * 
 * <p>A feature transfer manager is typically instantiated by the {@link TransferManagerFactory} and is 
 * wrapped around the original plugin transfer manager. Its methods always call the underlying (or 
 * original) transfer manager or perform the actual action.  
 * 
 * <p>Each feature transfer manager <b>must</b> have the following constructor signature, in this example
 * for the the <code>PathAware</code> feature:
 * 
 * <pre>
 *  public PathAwareFeatureTransferManager(TransferManager originalTransferManager, 
 *     ransferManager underlyingTransferManager, Config config, PathAware pathAwareAnnotation);
 * </pre>
 * 
 * @see Feature
 * @see FeatureExtension
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface FeatureTransferManager extends TransferManager {
	// Marker interface
}
