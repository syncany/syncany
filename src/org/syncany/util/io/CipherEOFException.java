package org.syncany.util.io;

import java.io.EOFException;

public class CipherEOFException extends EOFException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1377543073822530998L;

	public CipherEOFException(String msg){
		super(msg);
	}

}
