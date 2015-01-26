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

import javax.crypto.spec.SecretKeySpec;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.filter.Filter;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.transform.Matcher;
import org.simpleframework.xml.transform.Transform;
import org.syncany.crypto.CipherParams;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.ObjectId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock;
import org.syncany.util.StringUtil;

/**
 * @author pheckel
 *
 */
public class XmlSerializer {
	private static Serializer serializer;

	public static Serializer getInstance() {
		if (serializer == null) {
			serializer = createSerializer();
		}
		
		return serializer;
	}
	
	public static Serializer createSerializer() {
		return new Persister(createDefaultStrategy(), createDefaultMatcher());
	}

	private static Strategy createDefaultStrategy() {
		try {
			Registry registry = new Registry();
			registry.bind(VectorClock.class, new VectorClockConverter());
			
			return new RegistryStrategy(registry);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	private static Matcher createDefaultMatcher() {
		return new ObjectIdMatcher();
	}
	
	private static class ObjectIdMatcher implements Matcher {
		private FileChecksumTransform fileChecksumTransform;
		private FileHistoryIdTransform fileHistoryIdTransform;
		
		public ObjectIdMatcher() {
			this.fileChecksumTransform = new FileChecksumTransform();
			this.fileHistoryIdTransform = new FileHistoryIdTransform();
		}
		
		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Transform match(Class type) throws Exception {
			if (type.isAssignableFrom(FileChecksum.class)) {
				return fileChecksumTransform;
			}
			else if (type.isAssignableFrom(FileHistoryId.class)) {
				return fileHistoryIdTransform;
			}
			else {
				return null;
			}
		}		
	}

	private static class FileChecksumTransform implements Transform<FileChecksum> {
		@Override
		public FileChecksum read(String value) throws Exception {
			return FileChecksum.parseFileChecksum(value);
		}

		@Override
		public String write(FileChecksum value) throws Exception {
			return value.toString();
		}
	}
	
	private static class FileHistoryIdTransform implements Transform<FileHistoryId> {
		@Override
		public FileHistoryId read(String value) throws Exception {
			return FileHistoryId.parseFileId(value);
		}

		@Override
		public String write(FileHistoryId value) throws Exception {
			return value.toString();
		}
	}

	private static class VectorClockConverter implements Converter<VectorClock> {
		@Override
		public VectorClock read(InputNode node) throws Exception {
			return VectorClock.parseVectorClock(node.getValue());
		}

		@Override
		public void write(OutputNode node, VectorClock value) throws Exception {
			node.setValue(value.toString());
		}
	}

	/**
	 * Converter to properly encode a {@link SaltedSecretKey} when writing 
	 * an XML. Salt and key are serialized as attributes.
	 * 
	 * @author Christian Roth <christian.roth@port17.de>
	 */
	public static class SaltedSecretKeyConverter implements Converter<SaltedSecretKey> {
		public SaltedSecretKey read(InputNode node) throws Exception {
			byte[] saltBytes = StringUtil.fromHex(node.getAttribute("salt").getValue());
			byte[] keyBytes = StringUtil.fromHex(node.getAttribute("key").getValue());

			return new SaltedSecretKey(new SecretKeySpec(keyBytes, CipherParams.MASTER_KEY_DERIVATION_FUNCTION), saltBytes);
		}

		public void write(OutputNode node, SaltedSecretKey saltedSecretKey) {
			node.setAttribute("salt", StringUtil.toHex(saltedSecretKey.getSalt()));
			node.setAttribute("key", StringUtil.toHex(saltedSecretKey.getEncoded()));
		}
	}
}
