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
package org.syncany.config;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */ 
public class Encryption {	
    public static final String DEFAULT_CIPHER = "AES/CBC/PKCS5Padding";
    public static final int DEFAULT_KEYLENGTH = 128;

	private String password;
    private String cipherStr;
    private Integer keySize;
    
    public Encryption() {
        this("", DEFAULT_CIPHER, DEFAULT_KEYLENGTH); 
    }

    public Encryption(String password, String cipherStr, int keySize) {
        this.password = password;
        this.cipherStr = cipherStr;
        this.keySize = keySize;
    } 

    public String getCipherStr() {
        return cipherStr;
    }

    public void setCipherStr(String cipherStr) {
        this.cipherStr = cipherStr;
    }

    public Integer getKeySize() {
        return keySize;
    }

    public void setKeySize(Integer keySize) {
        this.keySize = keySize;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }    
}
