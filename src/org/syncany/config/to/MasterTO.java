/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.config.to;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;
import org.syncany.util.StringUtil;

@Root(name="master")
@Namespace(reference="http://syncany.org/master/1")
public class MasterTO {
	@Element(name = "salt", required = false)
	private String saltEncoded;
	private byte[] salt;
	
	public MasterTO() {
		// Required default constructor
	}
	
	public MasterTO(byte[] salt) {
		this.salt = salt;
	}

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

	@Persist
	public void prepare() {
		saltEncoded = (salt != null) ? StringUtil.toHex(salt) : null;
	}

	@Complete
	public void release() {
		saltEncoded = null;
	}
	
	@Commit
	public void commit() {
		salt = (saltEncoded != null) ? StringUtil.fromHex(saltEncoded) : null;
	}
}
