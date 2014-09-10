/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.plugins.transfer;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.syncany.config.to.ConnectionTO;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.annotations.Encrypted;
import org.syncany.util.StringUtil;

/**
 * A connection represents the configuration settings of a storage/connection
 * plugin. It is created through the concrete implementation of a {@link Plugin}.
 *
 * <p>A connection must be initialized by calling the {@link #init(Map) init()} method,
 * using plugin specific configuration parameters.
 *
 * <p>Once initialized, a {@link TransferManager} can be created through the {@link #createTransferManager()}
 * method. The transfer manager can then be used to upload/download files.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferSettings implements ConnectionTO {
	private static final Logger logger = Logger.getLogger(TransferSettings.class.getName());
	protected UserInteractionListener userInteractionListener;

	public UserInteractionListener getUserInteractionListener() {
		return userInteractionListener;
	}

	public void setUserInteractionListener(UserInteractionListener userInteractionListener) {
		this.userInteractionListener = userInteractionListener;
	}

	public abstract PluginOptionSpecs getOptionSpecs();

	public abstract void init(Map<String, String> optionValues) throws StorageException;

	@Persist
	private void encrypt() throws Exception {

		for (Field f : this.getClass().getDeclaredFields()) {
			if (f.isAnnotationPresent(Encrypted.class)) {
				if (f.getType() != String.class) {
					throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted");
				}
				f.set(this, StringUtil.toHex(((String) f.get(this)).getBytes()));
			}
		}

		logger.log(Level.INFO, "Encrypted transfer setting values");

	}

	@Commit
	private void decrypt() throws Exception {

		for (Field f : this.getClass().getDeclaredFields()) {
			if (f.isAnnotationPresent(Encrypted.class)) {
				if (f.getType() != String.class) {
					throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted");
				}
				f.set(this, new String(StringUtil.fromHex((String) f.get(this))));
			}
		}

		logger.log(Level.INFO, "Decrypted transfer setting values");

	}
}
