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
package org.syncany.experimental.db;

import java.io.Serializable;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ContentChunk implements Serializable {    
    private long id;
    private ChunkEntry chunk;
    private int orderNumber;
    private Content content;
    
    public ContentChunk() {
    }

    public ContentChunk(Content content, ChunkEntry chunk, int orderNumber) {
        this.content = content;
        this.chunk = chunk;
        this.orderNumber = orderNumber;
    }

    public ChunkEntry getChunk() {
        return chunk;
    }

    public Content getContent() {
        return content;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public long getId() {
        return id;
    }       
}
