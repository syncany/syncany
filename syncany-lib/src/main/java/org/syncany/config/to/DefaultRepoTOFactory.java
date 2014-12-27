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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.GzipTransformer;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.CipherSpec;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

/**
 * This class produces {@link RepoTO}s with some sensible defaults for the Chunkers and
 * MultiChunkers. The transformers are configurable, namely whether or not compression is used
 * and how it is encrypted.
 * 
 * @author Pim Otte <otte.pim@gmail.com>
 */
public class DefaultRepoTOFactory implements RepoTOFactory {
	private ChunkerTO chunkerTO;
	private MultiChunkerTO multiChunkerTO;
	private List<TransformerTO> transformersTO;

	public DefaultRepoTOFactory(boolean gzipEnabled, List<CipherSpec> cipherSpecs) {
		chunkerTO = getDefaultChunkerTO();
		multiChunkerTO = getDefaultMultiChunkerTO();
		transformersTO = getTransformersTO(gzipEnabled, cipherSpecs);
	}

	public RepoTO createRepoTO() {
		return createRepoTO(chunkerTO, multiChunkerTO, transformersTO);
	}

	public List<TransformerTO> getTransformersTO(boolean gzipEnabled, List<CipherSpec> cipherSpecs) {
		List<TransformerTO> transformersTO = new ArrayList<TransformerTO>();

		if (gzipEnabled) {
			transformersTO.add(getGzipTransformerTO());
		}

		if (cipherSpecs.size() > 0) {
			TransformerTO cipherTransformerTO = getCipherTransformerTO(cipherSpecs);
			transformersTO.add(cipherTransformerTO);
		}

		return transformersTO;
	}

	public RepoTO createRepoTO(ChunkerTO chunkerTO, MultiChunkerTO multiChunkerTO, List<TransformerTO> transformersTO) {
		// Make transfer object
		RepoTO repoTO = new RepoTO();

		// Create random repo identifier
		byte[] newRepoId = new byte[32];
		new SecureRandom().nextBytes(newRepoId);

		repoTO.setRepoId(newRepoId);

		// Add to repo transfer object
		repoTO.setChunkerTO(chunkerTO);
		repoTO.setMultiChunker(multiChunkerTO);
		repoTO.setTransformers(transformersTO);

		return repoTO;
	}

	protected ChunkerTO getDefaultChunkerTO() {
		ChunkerTO chunkerTO = new ChunkerTO();

		chunkerTO.setType(FixedChunker.TYPE);
		chunkerTO.setSettings(new HashMap<String, String>());
		chunkerTO.getSettings().put(Chunker.PROPERTY_SIZE, "16");

		return chunkerTO;
	}

	protected MultiChunkerTO getDefaultMultiChunkerTO() {
		MultiChunkerTO multichunkerTO = new MultiChunkerTO();

		multichunkerTO.setType(ZipMultiChunker.TYPE);
		multichunkerTO.setSettings(new HashMap<String, String>());
		multichunkerTO.getSettings().put(MultiChunker.PROPERTY_SIZE, "4096");

		return multichunkerTO;
	}

	protected TransformerTO getGzipTransformerTO() {
		TransformerTO gzipTransformerTO = new TransformerTO();
		gzipTransformerTO.setType(GzipTransformer.TYPE);

		return gzipTransformerTO;
	}

	protected TransformerTO getCipherTransformerTO(List<CipherSpec> cipherSpec) {
		String cipherSuitesIdStr = StringUtil.join(cipherSpec, ",", new StringJoinListener<CipherSpec>() {
			@Override
			public String getString(CipherSpec cipherSpec) {
				return "" + cipherSpec.getId();
			}
		});

		Map<String, String> cipherTransformerSettings = new HashMap<String, String>();
		cipherTransformerSettings.put(CipherTransformer.PROPERTY_CIPHER_SPECS, cipherSuitesIdStr);
		// Note: Property 'password' is added dynamically by CommandLineClient

		TransformerTO cipherTransformerTO = new TransformerTO();
		cipherTransformerTO.setType(CipherTransformer.TYPE);
		cipherTransformerTO.setSettings(cipherTransformerSettings);

		return cipherTransformerTO;
	}
}
