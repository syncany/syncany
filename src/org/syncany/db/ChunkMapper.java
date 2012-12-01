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

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ChunkMapper implements Serializable {    
    /**
	 * 
	 */
	private static final long serialVersionUID = 4966325666285186277L;
	
    private CloneChunk chunk;
    private int orderNumber;
    private CloneFile file;
    
    public ChunkMapper() {
    }

    public ChunkMapper(CloneFile file, CloneChunk chunk, int orderNumber) {
        this.file = file;
        this.chunk = chunk;
        this.orderNumber = orderNumber;
    }

    public CloneChunk getChunk() {
        return chunk;
    }

    public void setChunk(CloneChunk chunk) {
        this.chunk = chunk;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public CloneFile getFile() {
        return file;
    }

    public void setFile(CloneFile file) {
        this.file = file;
    }

}
