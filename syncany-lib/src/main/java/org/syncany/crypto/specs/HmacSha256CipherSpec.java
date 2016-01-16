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
package org.syncany.crypto.specs;

import java.io.InputStream;
import java.io.OutputStream;

import org.syncany.crypto.CipherException;
import org.syncany.crypto.CipherSpec;

/**
 * @author pheckel
 *
 */
public class HmacSha256CipherSpec extends CipherSpec {
	public HmacSha256CipherSpec() {
		super(-1, "HmacSHA256", 256, -1, false);
	}
		
	@Override
	public OutputStream newCipherOutputStream(OutputStream underlyingOutputStream, byte[] secretKey, byte[] iv) throws CipherException {
		throw new RuntimeException("Cannot create cipher.");
	}

	@Override
	public InputStream newCipherInputStream(InputStream underlyingInputStream, byte[] secretKey, byte[] iv) throws CipherException {
		throw new RuntimeException("Cannot create cipher.");
	}
}
