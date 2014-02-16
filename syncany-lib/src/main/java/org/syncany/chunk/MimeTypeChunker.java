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
package org.syncany.chunk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The mime type chunker uses the file mime type delegate the chunking to
 * either a <i>regular</i> chunker, or a <i>special</i> chunker. 
 * 
 * <p>The {@link #createChunks(File) createChunks()}-method uses the mime type of a file to 
 * determine which chunker is used. If the mime type matches a pattern in the list given in the constructor,
 * the special chunker is used. If not, the regular chunker is used.
 * 
 * <p>This is particularly useful to differentiate files that might change in a few bytes
 * from files that hardly change at all (or change entirely) -- like image or video files.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class MimeTypeChunker extends Chunker {
    private static final Logger logger = Logger.getLogger(MimeTypeChunker.class.getSimpleName());   

	private Chunker regularChunker;
	private Chunker specialChunker;
	private List<Pattern> specialChunkerMimeTypes;
	
	private Chunker delegatedChunker;
	
	/**
	 * Creates a new mime type chunker. 
	 * 
	 * <p>The special chunker is used only if the mime type matches any of the mime types patterns
	 * in the list. Otherwise, the regular chunker is used.
	 * 
	 * @param regularChunker The regular chunker is used if none of the mime types in the list matches  
	 * @param specialChunker The special chunker is used if any of the mime type patterns matches
	 * @param specialChunkerMimeTypes List of mime type regex patterns used to determine which chunker to use
	 * @throws Exception If the two chunkers do not use the same checksum algorithm
	 */
	public MimeTypeChunker(Chunker regularChunker, Chunker specialChunker, List<String> specialChunkerMimeTypes) throws Exception {
		if (!regularChunker.getChecksumAlgorithm().equals(specialChunker.getChecksumAlgorithm())) {
			throw new Exception("Regular and special chunkers must use the same checksum algorithm.");
		}
		
		this.regularChunker = regularChunker;
		this.specialChunker = specialChunker;
		this.specialChunkerMimeTypes = initMimeTypePatterns(specialChunkerMimeTypes);
		this.delegatedChunker = null;
	}

	@Override
	public ChunkEnumeration createChunks(File file) throws IOException {
		String mimeType = Files.probeContentType(Paths.get(file.getAbsolutePath()));
		
		for (Pattern mimeTypePattern : specialChunkerMimeTypes) {
			if (mimeType != null && mimeTypePattern.matcher(mimeType).matches()) {
				logger.log(Level.INFO, "File mime type: "+mimeType+", using SPECIAL chunker: "+file);
				delegatedChunker = specialChunker;	
				
				return delegatedChunker.createChunks(file);
			}
		}
				
		logger.log(Level.INFO, "File mime type: "+mimeType+", using regular chunker: "+file);
		delegatedChunker = regularChunker;

		return delegatedChunker.createChunks(file);
	}

	@Override
	public String toString() {
		return "FileTypeBased";
	}

	@Override
	public String getChecksumAlgorithm() {
		return regularChunker.getChecksumAlgorithm();
	}	
	
	private List<Pattern> initMimeTypePatterns(List<String> mimeTypePatternStrs) {
		ArrayList<Pattern> patternList = new ArrayList<Pattern>();
		
		for (String mimeTypePatternStr : mimeTypePatternStrs) {
			patternList.add(Pattern.compile(mimeTypePatternStr));
		}
		
		return patternList;
	}
}