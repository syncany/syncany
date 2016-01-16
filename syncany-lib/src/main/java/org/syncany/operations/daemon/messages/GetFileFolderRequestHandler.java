/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.daemon.messages;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.ObjectId;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Assembler;
import org.syncany.operations.Downloader;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.FolderRequestHandler;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.util.StringUtil;

public class GetFileFolderRequestHandler extends FolderRequestHandler {
	private SqlDatabase localDatabase;
	private LocalEventBus eventBus;
	
	public GetFileFolderRequestHandler(Config config) {
		super(config);		
		
		this.localDatabase = new SqlDatabase(config);
		this.eventBus = LocalEventBus.getInstance();
	}

	@Override
	public Response handleRequest(FolderRequest request) {
		GetFileFolderRequest concreteRequest = (GetFileFolderRequest) request;
		
		try {
			FileHistoryId fileHistoryId = FileHistoryId.parseFileId(concreteRequest.getFileHistoryId());
			long version = concreteRequest.getVersion();

			FileVersion fileVersion = localDatabase.getFileVersion(fileHistoryId, version);
			FileContent fileContent = localDatabase.getFileContent(fileVersion.getChecksum(), true);
			Map<ChunkChecksum, MultiChunkId> multiChunks = localDatabase.getMultiChunkIdsByChecksums(fileContent.getChunks());

			TransferManager transferManager = config.getTransferPlugin().createTransferManager(config.getConnection(), config);
			Downloader downloader = new Downloader(config, transferManager);
			Assembler assembler = new Assembler(config, localDatabase);

			downloader.downloadAndDecryptMultiChunks(new HashSet<MultiChunkId>(multiChunks.values()));

			File tempFile = assembler.assembleToCache(fileVersion);
			String tempFileToken = StringUtil.toHex(ObjectId.secureRandomBytes(40));
			
			GetFileFolderResponse fileResponse = new GetFileFolderResponse(concreteRequest.getId(), concreteRequest.getRoot(), tempFileToken);
			GetFileFolderResponseInternal fileResponseInternal = new GetFileFolderResponseInternal(fileResponse, tempFile);

			eventBus.post(fileResponseInternal);
			return null;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot reassemble file.", e);
			return new BadRequestResponse(concreteRequest.getId(), "Cannot reassemble file.");
		}
	}
}
