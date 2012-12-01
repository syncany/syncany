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
package org.syncany.db;

import java.io.Serializable;
import java.util.Arrays;
import org.syncany.util.StringUtil;

/**
 * Represents the chunk of a file.
 * 
 * @author Philipp C. Heckel
 */
public class CloneChunk implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 4792444121554015874L;
    
    private long dbId;
    private byte[] metaId;
    private byte[] checksum;

    public CloneChunk() {
        // Nothing.
    }
    
    public CloneChunk(byte[] checksum) {
        this();
        this.checksum = checksum;
    }   

    public byte[] getMetaId() {
        return metaId;
    }

    public void setMetaId(byte[] metaId) {
        this.metaId = metaId;
    }

    public byte[] getChecksum() {
        return checksum;
    }
    
    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }
    
    public String getIdStr()  {
        return encodeIdStr(metaId, checksum);
    }

    public String getFileName() {
        return getFileName(getMetaId(), getChecksum());
    }
    
    public static String getFileName(String idStr) {
        byte[] checksum = CloneChunk.decodeChecksum(idStr);
        byte[] metaId = CloneChunk.decodeMetaId(idStr);
        
        return getFileName(metaId, checksum);
    }    
    
    public static String getFileName(byte[] metaId, byte[] checksum) {
        return String.format("chunk-%s", encodeIdStr(metaId, checksum));
    }
    
    public static String encodeIdStr(byte[] checksum) {
        return encodeIdStr(null, checksum);
    }
    
    public static String encodeIdStr(byte[] metaId, byte[] checksum) {
        if (metaId == null) {
            if (checksum == null) {
                return null;
            }
            return String.format("%s", StringUtil.toHex(checksum)).toLowerCase();
        }
        else if (checksum == null) {
            return String.format("%s", StringUtil.toHex(metaId)).toLowerCase();
        }
        else {
            return String.format("%s-%s", 
                    StringUtil.toHex(metaId), 
                    StringUtil.toHex(checksum)).toLowerCase();
        }       
    }
    
    public static byte[] decodeChecksum(String idStr) {
        int dashpos = idStr.indexOf("-");
        return (dashpos == -1) ? StringUtil.fromHex(idStr.toUpperCase()) : StringUtil.fromHex(idStr.toUpperCase().substring(dashpos+1));
    }
    
    public static byte[] decodeMetaId(String idStr) {
        int dashpos = idStr.indexOf("-");
        return (dashpos == -1) ? null : StringUtil.fromHex(idStr.toUpperCase().substring(0, dashpos));
    }    
    
    public static String getMetaIdStr(String idStr) {
        int dashpos = idStr.indexOf("-");
        return (dashpos == -1) ? null : idStr.substring(0, dashpos);
    }
    
    public static String getChecksumStr(String idStr) {
        int dashpos = idStr.indexOf("-");
        return (dashpos == -1) ? idStr : idStr.substring(dashpos+1);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CloneChunk other = (CloneChunk) obj;
        if (!Arrays.equals(this.metaId, other.metaId)) {
            return false;
        }
        if (!Arrays.equals(this.checksum, other.checksum)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Arrays.hashCode(this.metaId);
        hash = 67 * hash + Arrays.hashCode(this.checksum);
        return hash;
    }

    @Override
    public String toString() {
        return "CloneChunk[dbId="+dbId+", metaId="+encodeIdStr(metaId)+", checksum="+encodeIdStr(checksum) + "]";
    }
}
