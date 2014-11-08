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
package org.syncany.operations.init;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigHelper;
import org.syncany.config.to.ConfigTO;

/**
 * This operation generates a link which can be shared among users to connect to
 * a repository. The operation is used by other initializing operations, e.g. connect
 * and init.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class GenlinkOperation extends AbstractInitOperation {
	private static final Logger logger = Logger.getLogger(GenlinkOperation.class.getSimpleName());
	
	private GenlinkOperationOptions options;
	private ConfigTO configTO;

	public GenlinkOperation(Config config, GenlinkOperationOptions options) {
		super(config, null);
		this.options = options;
	}

	public GenlinkOperation(ConfigTO configTO, GenlinkOperationOptions options) {
		this((Config) null, options);
		this.configTO = configTO;
	}

	@Override
	public GenlinkOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'GenLink'");
		logger.log(Level.INFO, "--------------------------------------------");

		if (configTO == null) {
			configTO = ConfigHelper.loadConfigTO(config.getLocalDir());
		}

		ApplicationLink applicationLink = new ApplicationLink(configTO.getTransferSettings(), options.isShortUrl());

		if (configTO.getMasterKey() != null) {	
			String encryptedLinkStr = applicationLink.createEncryptedLink(configTO.getMasterKey());
			return new GenlinkOperationResult(encryptedLinkStr, true);
		}
		else {
			String plaintextLinkStr = applicationLink.createPlaintextLink();
			return new GenlinkOperationResult(plaintextLinkStr, false);
		}
	}
}
