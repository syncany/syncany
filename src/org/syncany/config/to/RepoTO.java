package org.syncany.config.to;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name="storage")
@Namespace(reference="http://syncany.org/repo/1")
public class RepoTO {
	@Element(name="chunker")
	private ChunkerTO chunker;
	
	@Element(name="multichunker")
	private MultichunkerTO multichunker;
	
	@ElementList(name="transformers", entry="transformer")
	private List<TransformerTO> transformers;
	
	public ChunkerTO getChunker() {
		return chunker;
	}

	public void setChunker(ChunkerTO chunker) {
		this.chunker = chunker;
	}

	public MultichunkerTO getMultichunker() {
		return multichunker;
	}

	public void setMultichunker(MultichunkerTO multichunker) {
		this.multichunker = multichunker;
	}

	public List<TransformerTO> getTransformers() {
		return transformers;
	}

	public void setTransformers(List<TransformerTO> transformers) {
		this.transformers = transformers;
	}

	public static class ChunkerTO extends TypedPropertyListTO {
		// Nothing special about this
	}
	
	public static class MultichunkerTO extends TypedPropertyListTO {
		// Nothing special about this
	}
	
	public static class TransformerTO extends TypedPropertyListTO {
		// Nothing special about this
	} 
}
