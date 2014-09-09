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
package org.syncany.plugins;

import org.syncany.config.to.ConfigTO;
import org.syncany.plugins.annotations.Option;
import org.syncany.plugins.transfer.TransferSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Roth <christian.roth@port17.de>
 * @version 0.0.1
 */

public class TransferSettingsHelper {

	private static final Logger logger = Logger.getLogger(TransferSettingsHelper.class.getSimpleName());
	private static final Type[] SUPPORTED_TYPES = { Integer.TYPE, Boolean.TYPE, String.class };

	public static <T extends TransferSettings> T restore(ConfigTO.ConnectionTO connection, Class<T> transferSettingsClass) throws StorageException {

		if (!TransferSettingsHelper.isClassValid(transferSettingsClass)) {
			throw new StorageException("Invalid TransferSettings class: Unsupported field types found.");
		}

		T transferSettings;
		try {
			transferSettings = transferSettingsClass.newInstance();

			for (Field f : transferSettings.getClass().getDeclaredFields()) {
				if (!f.isAnnotationPresent(Option.class)) {
					continue;
				}
				restoreFieldFromConnection(transferSettings, f, connection);
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to recreate plugin settings: " + e.getMessage());
			throw new StorageException("Unable to recreate plugin settings: " + e.getMessage());
		}

		if (!validate(connection, transferSettings)) {
			throw new StorageException("Unable to recreate plugin settings: Verification failed");
		}

		return transferSettings;

	}

  public static <T> boolean validate(ConfigTO.ConnectionTO connectionTo, T transferSettings) {

    try {
      for (Field f : transferSettings.getClass().getDeclaredFields()) {
        if (f.isAnnotationPresent(Option.class)) {
          Option annotation = f.getAnnotation(Option.class);
          if (annotation.mandatory() && f.get(transferSettings) == null) {
            logger.log(Level.WARNING, "Missing mandatory transfersettings field {0}#{1}", new Object[] {
              transferSettings.getClass().getSimpleName(), f.getName() });
            return false;
          }
        }
      }
      return true;
    }
    catch (IllegalAccessException e) {
      logger.log(Level.SEVERE, "Unable to validate if field is correctly set: " + e.getMessage());
      return false;
    }

  }

  public static <T extends TransferSettings> boolean isClassValid(Class<T> transferSettingsClass) {

    for (Field f : transferSettingsClass.getDeclaredFields()) {
      if (f.isAnnotationPresent(Option.class) && !Arrays.asList(SUPPORTED_TYPES).contains(f.getType())) {
        return false;
      }
    }
    return true;

  }

	private static <T> void restoreFieldFromConnection(T transferSettings, Field f, ConfigTO.ConnectionTO connection) throws Exception {

		f.setAccessible(true);
		final Option annotation = f.getAnnotation(Option.class);
		final Type fieldType = f.getType();
		String fieldValue = connection.getSettings().get(annotation.name().equals(Option.FIELDNAME) ? f.getName() : annotation.name());

		if (fieldValue != null) {
			if (fieldType == Integer.TYPE) {
				f.setInt(transferSettings, Integer.parseInt(fieldValue));
			}
			else if (fieldType == Boolean.TYPE) {
				f.setBoolean(transferSettings, Boolean.parseBoolean(fieldValue));
			}
			else if (fieldType == String.class) {
				f.set(transferSettings, fieldValue);
			}
      else {
        throw new StorageException("Invalid TransferSettings class: Unsupported field types found.");
      }
		}

	}

}
