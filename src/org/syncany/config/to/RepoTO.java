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
package org.syncany.config.to;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name="repo")
@Namespace(reference="http://syncany.org/repo/1")
public class RepoTO {
	@Element(name="repoid", required=true)
	private byte[] repoId;
	
	@Element(name="chunker", required=false)
	private ChunkerTO chunker;
	
	@Element(name="multichunker", required=false)
	private MultiChunkerTO multiChunker;
	
	@ElementList(name="transformers", required=false, entry="transformer")
	private List<TransformerTO> transformers;
	
	public byte[] getRepoId() {
		return repoId;
	}

	public void setRepoId(byte[] repoId) {
		this.repoId = repoId;
	}

	public ChunkerTO getChunker() {
		return chunker;
	}

	public void setChunker(ChunkerTO chunker) {
		this.chunker = chunker;
	}

	public MultiChunkerTO getMultichunker() {
		return multiChunker;
	}

	public void setMultiChunker(MultiChunkerTO multiChunker) {
		this.multiChunker = multiChunker;
	}

	public List<TransformerTO> getTransformerTOs() {
		return transformers;
	}

	public void setTransformers(List<TransformerTO> transformers) {
		this.transformers = transformers;
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
