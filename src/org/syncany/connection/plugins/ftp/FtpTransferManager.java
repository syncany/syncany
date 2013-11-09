/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;

/**
 *
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpTransferManager extends AbstractTransferManager { 
    private static final Logger logger = Logger.getLogger(FtpTransferManager.class.getSimpleName());
    
    private static final int CONNECT_RETRY_COUNT = 3;    
    private static final int TIMEOUT_DEFAULT = 5000;
    private static final int TIMEOUT_CONNECT = 5000;
    private static final int TIMEOUT_DATA = 5000;
    
    private FTPClient ftp;
    
    private String repoPath;
    private String dataPath;

    public FtpTransferManager(FtpConnection connection) {
        super(connection);
        
        this.ftp = new FTPClient();
        this.repoPath = connection.getPath();
        this.dataPath = connection.getPath()+"/data";
    } 
 
    @Override
    public FtpConnection getConnection() {
        return (FtpConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageException {
        for (int i=0; i<CONNECT_RETRY_COUNT; i++) {
            try {
                if (ftp.isConnected()) {
                    return;
                }

                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "FTP client connecting to {0}:{1} ...", new Object[]{getConnection().getHostname(), getConnection().getPort()});
                }

                ftp.setConnectTimeout(TIMEOUT_CONNECT);
                ftp.setDataTimeout(TIMEOUT_DATA);	
                //ftp.setControlKeepAliveReplyTimeout(TIMEOUT_CONTROL_REPLY);
                ftp.setDefaultTimeout(TIMEOUT_DEFAULT);

                ftp.connect(getConnection().getHostname(), getConnection().getPort());
                ftp.login(getConnection().getUsername(), getConnection().getPassword());
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!            
                
                return;
            }
            catch (Exception ex) {                
                if (i == CONNECT_RETRY_COUNT-1) {
                	logger.log(Level.WARNING, "FTP client connection failed. Retrying failed.", ex);                    
                    throw new StorageException(ex);
                }
                else {
                	logger.log(Level.WARNING, "FTP client connection failed. Retrying "+(i+1)+"/"+CONNECT_RETRY_COUNT+" ...", ex);                                    	
                }
            }                        
        }      
    }

    @Override
    public void disconnect() {
        try {
            ftp.logout();
            ftp.disconnect();
        }
        catch (Exception ex) {
            
        }
    } 
    
    @Override
    public void init() throws StorageException {
    	connect();
    	
    	try {
			ftp.mkd(dataPath);
		}
    	catch (IOException e) {
    		throw new StorageException("Cannot create data directory: "+dataPath, e);
		}
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();
        
        String remotePath = getRemoteFilePath(remoteFile);
        
        try {
            // Download file
            File tempFile = createTempFile(localFile.getName());
            OutputStream tempFOS = new FileOutputStream(tempFile);

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "FTP: Downloading {0} to temp file {1}", new Object[]{remotePath, tempFile});            
            }
            
            ftp.retrieveFile(remotePath, tempFOS);

            tempFOS.close();

            // Move file
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[]{tempFile, localFile});            
            }            
            
            localFile.delete();      
            FileUtils.moveFile(tempFile, localFile);            
            tempFile.delete();
        }
        catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while downloading file "+remoteFile.getName(), ex);
            throw new StorageException(ex);
        }
    }

    private String getRemoteFilePath(RemoteFile remoteFile) {
    	if (remoteFile instanceof MultiChunkRemoteFile) {
    		return dataPath+"/"+remoteFile.getName();
    	}
    	else {
    		return repoPath+"/"+remoteFile.getName();
    	}
	}

	@Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();

        String remotePath = getRemoteFilePath(remoteFile);
        String tempRemotePath = getConnection().getPath()+"/temp-"+remoteFile.getName();

        try {
            // Upload to temp file
            InputStream fileFIS = new FileInputStream(localFile);

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "FTP: Uploading {0} to temp file {1}", new Object[]{localFile, tempRemotePath});            
            }
            
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!            
            
            if (!ftp.storeFile(tempRemotePath, fileFIS)) {
                throw new IOException("Error uploading file "+remoteFile.getName());
            }

            fileFIS.close();

            // Move
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[]{tempRemotePath, remotePath});            
            }    
            
            ftp.rename(tempRemotePath, remotePath);
        }
        catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not upload file "+localFile+" to "+remoteFile.getName(), ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();

        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();
            FTPFile[] ftpFiles = ftp.listFiles(getConnection().getPath()+"/");

            for (FTPFile f : ftpFiles) {
                files.put(f.getName(), new RemoteFile(f.getName(), f));
            }

            return files;
        }
        catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to list FTP directory.", ex);
            throw new StorageException(ex);
        }
    }    

	@Override
	public Map<String, RemoteFile> list(final String namePrefix) throws StorageException {
		connect();

        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();
            FTPFile[] ftpFiles = ftp.listFiles(getConnection().getPath()+"/", new FTPFileFilter() {				
				@Override
				public boolean accept(FTPFile file) {
					return file.getName().startsWith(namePrefix);
				}
			});

            for (FTPFile f : ftpFiles) {
                files.put(f.getName(), new RemoteFile(f.getName(), f));
            }

            return files;
        }
        catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to list FTP directory.", ex);
            throw new StorageException(ex);
        }
	}

    @Override
    public boolean delete(RemoteFile remoteFile) throws StorageException {
        connect();
        
        String remotePath = getRemoteFilePath(remoteFile);

        try {
            return ftp.deleteFile(remotePath);
        }
        catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not delete file "+remoteFile.getName(), ex);
            throw new StorageException(ex);
        }
    }
}
