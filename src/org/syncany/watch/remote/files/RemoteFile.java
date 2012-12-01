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
package org.syncany.watch.remote.files;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class RemoteFile {
    private String name;
    private long size;
    private Object source;

    protected RemoteFile() {
        // Fressen.
    }

    public RemoteFile(RemoteFile remoteFile) {
        this(remoteFile.getName(), remoteFile.getSize(), remoteFile.getSource());
    }

    public RemoteFile(String name) {
        this(name, 0, null);
    }

    public RemoteFile(String name, long size) {
        this(name, size, null);
    }

    public RemoteFile(String name, long size, Object source) {
        this.name = name;
        this.size = size;
        this.source = source;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return RemoteFile.class.getSimpleName()
            +"[name="+name+", source="+source+"]";
    }
}
