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
import java.util.Date;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CloneClient implements Serializable {
    private static final long serialVersionUID = 17398420202020111L;

    private Long id;
    private Long profileId;
    private String machineName;
    private String userName;    
    private Date lastUpdate;
    private Date lastProfileUpdate;
    private Date lastImageUpdate;
    
    public CloneClient() {
        // Nothing.
      }

    public CloneClient(String machineName, long profileId) {
        this.machineName = machineName;
        this.id = (long) machineName.hashCode();
        this.profileId = profileId;
    }
    
    public CloneClient(String machineName) {
        this.machineName = machineName;
        this.id = (long) machineName.hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String name) {
        this.machineName = name;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getLastImageUpdate() {
        return lastImageUpdate;
    }

    public void setLastImageUpdate(Date lastImageUpdate) {
        this.lastImageUpdate = lastImageUpdate;
    }

    public Date getLastProfileUpdate() {
        return lastProfileUpdate;
    }

    public void setLastProfileUpdate(Date lastProfileUpdate) {
        this.lastProfileUpdate = lastProfileUpdate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneClient)) 
            return false;

        CloneClient other = (CloneClient) object;

        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CloneClient[id=" + id + ",name="+machineName+"]";
    }
    
    public void merge(){
    	Database.getInstance().addClient(this);
    	Database.getInstance().saveClients();
    }
}
