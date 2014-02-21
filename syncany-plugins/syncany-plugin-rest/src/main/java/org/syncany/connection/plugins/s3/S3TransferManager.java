/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins.s3;

import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.StorageBucket;
import org.syncany.connection.plugins.rest.RestTransferManager;

/**
 *
 * @author Philipp C. Heckel
 */
public class S3TransferManager extends RestTransferManager {
   public S3TransferManager(S3Connection connection) {
        super(connection);
    }

    @Override
    public S3Connection getConnection() {
        return (S3Connection) super.getConnection();
    }
    
    @Override
    protected RestStorageService createService() throws ServiceException {
        return new RestS3Service(getConnection().getCredentials());
    }

    @Override
    protected StorageBucket createBucket() {
        return new S3Bucket(getConnection().getBucket(), getConnection().getLocation());
    }
}
