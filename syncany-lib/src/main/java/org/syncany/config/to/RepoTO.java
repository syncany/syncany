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
package org.syncany.config.to;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;
import org.syncany.util.StringUtil;

/**
 * The repo transfer object is used to create and load the repo file
 * from/to XML. The repo file identifies the repository with a unique
 * repo ID, and defines the chunking framework settings. It is
 * stored locally and on the remote storage. 
 * 
 * <p>It uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a> at simple.sourceforge.net
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
@Root(name = "repo", strict = false)
public class RepoTO {
	@Element(name = "repoid", required = true)
	private String repoIdEncoded;
	private byte[] repoId;

	@Element(name = "chunker", required = false)
	private ChunkerTO chunker;

	@Element(name = "multichunker", required = false)
	private MultiChunkerTO multiChunker;

	@ElementList(name = "transformers", required = false, entry = "transformer")
	private ArrayList<TransformerTO> transformers;

	public byte[] getRepoId() {
		return repoId;
	}

	public void setRepoId(byte[] repoId) {
		this.repoId = repoId;
	}

	@Persist
	public void prepare() {
		repoIdEncoded = (repoId != null) ? StringUtil.toHex(repoId) : null;
	}

	@Complete
	public void release() {
		repoIdEncoded = null;
	}

	@Commit
	public void commit() {
		repoId = (repoIdEncoded != null) ? StringUtil.fromHex(repoIdEncoded) : null;
	}

	public ChunkerTO getChunkerTO() {
		return chunker;
	}

	public void setChunkerTO(ChunkerTO chunker) {
		this.chunker = chunker;
	}

	public MultiChunkerTO getMultiChunker() {
		return multiChunker;
	}

	public void setMultiChunker(MultiChunkerTO multiChunker) {
		this.multiChunker = multiChunker;
	}

	public List<TransformerTO> getTransformers() {
		return transformers;
	}

	public void setTransformers(List<TransformerTO> transformers) {
		this.transformers = (transformers != null) ? new ArrayList<TransformerTO>(transformers) : null;
	}

	public static class ChunkerTO extends TypedPropertyListTO {
		// Nothing special about this
	}

	public static class MultiChunkerTO extends TypedPropertyListTO {
		// Nothing special about this
	}

	public static class TransformerTO extends TypedPropertyListTO {
		// Nothing special about this
	}
}
