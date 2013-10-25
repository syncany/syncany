/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;

/**
 *
 * @author pheckel
 */
public class CipherHmacOutputStream_UNUSED extends FilterOutputStream {
    // the cipher engine to use to process stream data
    private Cipher cipher;

    // the underlying output stream
    private OutputStream output;

    /* the buffer holding one byte of incoming data */
    private byte[] ibuffer = new byte[1];

    // the buffer holding data ready to be written out
    private byte[] obuffer;
    
    private Mac mac;
    private byte[] iv;

    /**
     *
     * Constructs a CipherOutputStream from an OutputStream and a
     * Cipher.
     * <br>Note: if the specified output stream or cipher is
     * null, a NullPointerException may be thrown later when
     * they are used.
     *
     * @param os  the OutputStream object
     * @param c   an initialized Cipher object
     */
    public CipherHmacOutputStream_UNUSED(OutputStream os, Cipher c, Mac m, byte[] iv) {
        super(os);
        output = os;
        cipher = c;
        mac = m; 
        this.iv = iv; 
    };
   
    /**
     * Writes the specified byte to this output stream.
     *
     * @param      b   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs.
     * @since      JCE1.2
     */
    public void write(int b) throws IOException {
        ibuffer[0] = (byte) b;
        obuffer = cipher.update(ibuffer, 0, 1);
        if (obuffer != null) {
        	mac.update(obuffer);
            output.write(obuffer);
            obuffer = null;
        }
    };

    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * to this output stream.
     * <p>
     * The <code>write</code> method of
     * <code>CipherOutputStream</code> calls the <code>write</code>
     * method of three arguments with the three arguments
     * <code>b</code>, <code>0</code>, and <code>b.length</code>.
     *
     * @param      b   the data.
     * @exception  NullPointerException if <code>b</code> is null.
     * @exception  IOException  if an I/O error occurs.
     * @see        javax.crypto.CipherOutputStream#write(byte[], int, int)
     * @since JCE1.2
     */
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     * @since      JCE1.2
     */
    public void write(byte b[], int off, int len) throws IOException {
        obuffer = cipher.update(b, off, len);
        if (obuffer != null) {
        	mac.update(obuffer);
            output.write(obuffer);
            obuffer = null;
        }
    }

    /**
     * Flushes this output stream by forcing any buffered output bytes
     * that have already been processed by the encapsulated cipher object
     * to be written out.
     *
     * <p>Any bytes buffered by the encapsulated cipher
     * and waiting to be processed by it will not be written out. For example,
     * if the encapsulated cipher is a block cipher, and the total number of
     * bytes written using one of the <code>write</code> methods is less than
     * the cipher's block size, no bytes will be written out.
     *
     * @exception  IOException  if an I/O error occurs.
     * @since      JCE1.2
     */
    public void flush() throws IOException {
        if (obuffer != null) {
        	mac.update(obuffer);
            output.write(obuffer);
            obuffer = null;
        }
        output.flush();
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream.
     * <p>
     * This method invokes the <code>doFinal</code> method of the encapsulated
     * cipher object, which causes any bytes buffered by the encapsulated
     * cipher to be processed. The result is written out by calling the
     * <code>flush</code> method of this output stream.
     * <p>
     * This method resets the encapsulated cipher object to its initial state
     * and calls the <code>close</code> method of the underlying output
     * stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @since      JCE1.2
     */
    public void close() throws IOException {
        try {
            obuffer = cipher.doFinal();
        } catch (IllegalBlockSizeException e) {
            obuffer = null;
        } catch (BadPaddingException e) {
            obuffer = null;
        }
        try {
            flush();
        } catch (IOException ignored) {}
        
        writeHmac();
        
        out.close();
    }	
    
    private void writeHmac() throws IOException {
		try {
			// Include IV & algorithm details in HMAC
			mac.update(iv);
			mac.update(cipher.getAlgorithm().getBytes("UTF8"));
			mac.update(cipher.getParameters().getEncoded());
			
			// Write HMAC
			byte[] macBytes = mac.doFinal();
			out.write(macBytes);
		}
		catch (Exception e) {
			throw new IOException(e);
		}		
	}
}