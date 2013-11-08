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
