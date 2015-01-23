/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.util;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.syncany.crypto.SaltedSecretKey;
import com.github.zafarkhaja.semver.Version;

/**
 * Converter to properly encode a {@link SaltedSecretKey} when writing
 * an XML. Salt and key are serialized as attributes.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
public class SemVerConverter implements Converter<Version> {
	public Version read(InputNode node) throws Exception {
		return Version.valueOf(node.getValue());
	}

	public void write(OutputNode node, Version version) {
		node.setValue(version.toString());
	}
}