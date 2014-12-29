/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.VectorClock;

/**
 * The file content data access object (DAO) writes and queries the SQL database for information
 * on {@link FileContent}s. It translates the relational data in the <i>filecontent</i> table to
 * Java objects.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileContentSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(FileContentSqlDao.class.getSimpleName());
	
	public FileContentSqlDao(Connection connection) {
		super(connection);
	}

	/**
	 * Writes a list of {@link FileContent}s to the database using <tt>INSERT</tt>s and the given connection.
	 * It fills two tables, the <i>filecontent</i> table ({@link FileContent}) and the <i>filecontent_chunk</i> 
	 * table ({@link ChunkChecksum}).
	 * 
	 * <p>To do the latter (write chunk references), this method calls
	 * {@link #writeFileContentChunkRefs(Connection, FileContent) writeFileContentChunkRefs()} for every 
	 * {@link FileContent}. 
	 * 
	 * <p><b>Note:</b> This method executes, but does not commit the queries.
	 * 
	 * @param connection The connection used to execute the statements
	 * @param databaseVersionId 
	 * @param fileContents List of {@link FileContent}s to be inserted in the database
	 * @throws SQLException If the SQL statement fails
	 */
	public void writeFileContents(Connection connection, long databaseVersionId, Collection<FileContent> fileContents) throws SQLException {
		for (FileContent fileContent : fileContents) {
			PreparedStatement preparedStatement = getStatement(connection, "filecontent.insert.all.writeFileContents.sql");

			preparedStatement.setString(1, fileContent.getChecksum().toString());
			preparedStatement.setLong(2, databaseVersionId);
			preparedStatement.setLong(3, fileContent.getSize());
			
			preparedStatement.executeUpdate();
			preparedStatement.close();	
			
			// Write chunk references
			writeFileContentChunkRefs(connection, fileContent);			
		}
	}
	
	private void writeFileContentChunkRefs(Connection connection, FileContent fileContent) throws SQLException {
		PreparedStatement preparedStatement = getStatement(connection, "filecontent.insert.all.writeFileContentChunkRefs.sql");
		int order = 0;
		
		for (ChunkChecksum chunkChecksum : fileContent.getChunks()) {
			preparedStatement.setString(1, fileContent.getChecksum().toString());
			preparedStatement.setString(2, chunkChecksum.toString());
			preparedStatement.setInt(3, order);

			preparedStatement.addBatch();
			
			order++;				
		}
		
		preparedStatement.executeBatch();
		preparedStatement.close();
	}

	/**
	 * Removes unreferenced {@link FileContent}s from the database table <i>filecontent</i>,
	 * as well as the corresponding chunk references (list of {@link ChunkChecksum}s) from the
	 * table <i>filecontent_chunk</i>. 
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the query.
	 * 
	 * @throws SQLException If the SQL statement fails
	 */
	public void removeUnreferencedFileContents() throws SQLException {
		// Note: Chunk references (filcontent_chunk) must be removed first, because
		//       of the foreign key constraints. 
		
		removeUnreferencedFileContentChunkRefs();
		removeUnreferencedFileContentsInt();
	}
	
	private void removeUnreferencedFileContentsInt() throws SQLException {
		PreparedStatement preparedStatement = getStatement("filecontent.delete.all.removeUnreferencedFileContents.sql");
		preparedStatement.executeUpdate();	
		preparedStatement.close();
	}
	
	private void removeUnreferencedFileContentChunkRefs() throws SQLException {
		PreparedStatement preparedStatement = getStatement("filecontent.delete.all.removeUnreferencedFileContentRefs.sql");
		preparedStatement.executeUpdate();	
		preparedStatement.close();
	}
	
	/**
	 * Queries the database for a particular {@link FileContent}, either with or without the
	 * corresponding chunk references (list of {@link ChunkChecksum}). 
	 * 
	 * @param fileChecksum {@link FileContent}-identifying file checksum
	 * @param includeChunkChecksums If <tt>true</tt>, the resulting {@link FileContent} will contain its chunk references  
	 * @return Returns a {@link FileContent} either with or without chunk references, or <tt>null</tt> if it does not exist. 
	 */
	public FileContent getFileContent(FileChecksum fileChecksum, boolean includeChunkChecksums) {
		if (fileChecksum == null) {
			return null;
		}
		else if (includeChunkChecksums) {
			return getFileContentWithChunkChecksums(fileChecksum);			
		}
		else {
			return getFileContentWithoutChunkChecksums(fileChecksum);			
		}
	}

	/**
	 * Queries the SQL database for all {@link FileContent}s that <b>originally appeared</b> in the
	 * database version identified by the given vector clock.
	 * 
	 * <p><b>Note:</b> This method does <b>not</b> select all the file contents that are referenced
	 * in the database version. In particular, it <b>does not return</b> file contents that appeared
	 * in previous other database versions.
	 * 
	 * @param vectorClock Vector clock that identifies the database version
	 * @return Returns all {@link FileContent}s that originally belong to a database version
	 */
	public Map<FileChecksum, FileContent> getFileContents(VectorClock vectorClock) {
		try (PreparedStatement preparedStatement = getStatement("filecontent.select.master.getFileContentsWithChunkChecksumsForDatabaseVersion.sql")) {
			preparedStatement.setString(1, vectorClock.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createFileContents(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private FileContent getFileContentWithoutChunkChecksums(FileChecksum fileChecksum) {
		try (PreparedStatement preparedStatement = getStatement("filecontent.select.all.getFileContentByChecksumWithoutChunkChecksums.sql")) {
			preparedStatement.setString(1, fileChecksum.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					FileContent fileContent = new FileContent();
	
					fileContent.setChecksum(FileChecksum.parseFileChecksum(resultSet.getString("checksum")));
					fileContent.setSize(resultSet.getLong("size"));
	
					return fileContent;
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private FileContent getFileContentWithChunkChecksums(FileChecksum fileChecksum) {
		try (PreparedStatement preparedStatement = getStatement("filecontent.select.all.getFileContentByChecksumWithChunkChecksums.sql")) {
			preparedStatement.setString(1, fileChecksum.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				FileContent fileContent = null;
				
				while (resultSet.next()) {
					if (fileContent == null) {
						fileContent = new FileContent();
						
						fileContent.setChecksum(FileChecksum.parseFileChecksum(resultSet.getString("checksum")));
						fileContent.setSize(resultSet.getLong("size"));
					}
					
					// Add chunk references
					ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum"));
					fileContent.addChunk(chunkChecksum);
				}
	
				return fileContent;
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<FileChecksum, FileContent> createFileContents(ResultSet resultSet) throws SQLException {
		Map<FileChecksum, FileContent> fileContents = new HashMap<FileChecksum, FileContent>();	
		FileChecksum currentFileChecksum = null;
		
		while (resultSet.next()) {		
			FileChecksum fileChecksum = FileChecksum.parseFileChecksum(resultSet.getString("checksum"));
			FileContent fileContent = null;
			
			if (currentFileChecksum != null && currentFileChecksum.equals(fileChecksum)) {
				fileContent = fileContents.get(fileChecksum);	
			}
			else {
				fileContent = new FileContent();
				
				fileContent.setChecksum(fileChecksum);
				fileContent.setSize(resultSet.getLong("size"));
			}
			
			ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(resultSet.getString("chunk_checksum"));
			fileContent.addChunk(chunkChecksum);

			fileContents.put(fileChecksum, fileContent); 
			currentFileChecksum = fileChecksum;
		}
		
		return fileContents;
	}

	/**
	 * no commit
	 */
	public void updateDirtyFileContentsNewDatabaseId(long newDatabaseVersionId) {
		try (PreparedStatement preparedStatement = getStatement("filecontent.update.dirty.updateDirtyFileContentsNewDatabaseId.sql")) {
			preparedStatement.setLong(1, newDatabaseVersionId);
			preparedStatement.executeUpdate();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
}
