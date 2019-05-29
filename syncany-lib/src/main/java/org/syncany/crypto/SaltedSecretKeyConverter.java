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
package org.syncany.crypto;

import javax.crypto.spec.SecretKeySpec;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.syncany.util.StringUtil;

/**
 * Converter to properly encode a {@link SaltedSecretKey} when writing 
 * an XML. Salt and key are serialized as attributes.
 * 
 * @author Christian Roth (christian.roth@port17.de)
 */
public class SaltedSecretKeyConverter implements Converter<SaltedSecretKey> {
	public SaltedSecretKey read(InputNode node) throws Exception {
		byte[] saltBytes = StringUtil.fromHex(node.getAttribute("salt").getValue());
		byte[] keyBytes = StringUtil.fromHex(node.getAttribute("key").getValue());

		return new SaltedSecretKey(new SecretKeySpec(keyBytes, CipherParams.MASTER_KEY_DERIVATION_FUNCTION), saltBytes);
	}

	public void write(OutputNode node, SaltedSecretKey saltedSecretKey) {
		node.setAttribute("salt", StringUtil.toHex(saltedSecretKey.getSalt()));
		node.setAttribute("key", StringUtil.toHex(saltedSecretKey.getEncoded()));
	}
}