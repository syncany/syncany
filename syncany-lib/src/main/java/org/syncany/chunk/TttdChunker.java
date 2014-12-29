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
package org.syncany.chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TTTD chunker is an implementation of the Two Threshold Two Divisor (TTTD)
 * chunking method based on the paper of Kave Eshghi and Hsiu Khuern Tang, 2005.
 *
 * <p>The class implements a content-based {@link Chunker}, i.e. it determines breakpoints
 * on the content rather than on the offset. It uses a fingerprinting algorithm to
 * calculate window fingerprints and determine a breakpoint. The algorithm given in the
 * constructor is instantiated to an implementation of a {@link Fingerprinter}.
 *
 * <p>The TTTD chunking method makes sure that it does not produce chunks smaller
 * than a certain threshold. To do so, it ignores chunk boundaries until a minimum
 * size is reached. Even though this negatively affects the duplicate detection
 * (because bigger chunks are created), chunk boundaries are still "natural" --
 * because they are based on the underlying data.
 *
 * <p>To handle chunks that exceed a certain maximum size, TTTD applies two techniques.
 * It defines the two divisors <i>D</i> (regular divisor) and <i>D'</i> (backup divisor)
 * with <i>D</i> &gt; <i>D'</i>. Because <i>D'</i> is smaller than <i>D</i>, it is more
 * likely to find a breakpoint.
 *
 * <p>If <i>D</i> does not find a chunk boundary before the maximum chunk size is reached,
 * the backup breakpoint found by <i>D'</i> is used. If <i>D</i> also does not find any
 * breakpoints, TTTD simply cuts the chunk at the maximum chunk size. TTTD hence guarantees
 * to emit chunks with a minimum and maximum size.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @see <a href="http://www.hpl.hp.com/techreports/2005/HPL-2005-30R1.html">Original TTTD paper: A framework for analyzing and improving content-based chunking algorithms</a>,
 *      2005, Kave Eshghi and Hsiu Khuern Tang
 */
public class TttdChunker extends Chunker {
	private static final Logger logger = Logger.getLogger(TttdChunker.class.getSimpleName());

	public static final int DEFAULT_WINDOW_SIZE = 48; // like LBFS
	public static final String DEFAULT_DIGEST_ALG = "SHA1";
	public static final String DEFAULT_FINGERPRINT_ALG = "Adler32";

	private int Tmin;
	private int Tmax;
	private int D;
	private int Ddash;
	private int windowSize;
	private String checksumAlgorithm;
	private String fingerprintAlgorithm;
	private String name;

	public TttdChunker(int Tmin, int Tmax, int D, int Ddash, int windowSize) {
		this(Tmin, Tmax, D, Ddash, windowSize, DEFAULT_DIGEST_ALG, DEFAULT_FINGERPRINT_ALG);
	}

	public TttdChunker(int Tmin, int Tmax, int D, int Ddash, int windowSize, String digestAlg) {
		this(Tmin, Tmax, D, Ddash, windowSize, digestAlg, DEFAULT_FINGERPRINT_ALG);
	}

	public TttdChunker(int avgChunkSize) {
		this(avgChunkSize, DEFAULT_WINDOW_SIZE, DEFAULT_DIGEST_ALG, DEFAULT_FINGERPRINT_ALG);
	}

	/**
	 * Infer the optimal values for avgChunkSize from the orginal paper's optimal (measured) values.
	 * LBFS: avg. chunk size = 1015 bytes --> Tmin = 460, Tmax = 2800, D = 540, Ddash = 270
	 */
	public TttdChunker(int avgChunkSize, int windowSize, String digestAlg, String fingerprintAlg) {
		this(
				/* Tmin */(int) Math.round(460.0 * avgChunkSize / 1015.0),
				/* Tmax */(int) Math.round(2800.0 * avgChunkSize / 1015.0),
				/*   D  */(int) Math.round(540.0 * avgChunkSize / 1015.0),
				/*   D  */(int) Math.round(270.0 * avgChunkSize / 1015.0),
				/* rest */windowSize, digestAlg, fingerprintAlg, "TTTD-" + avgChunkSize + "-" + digestAlg + "-" + fingerprintAlg);
	}

	public TttdChunker(int Tmin, int Tmax, int D, int Ddash, int windowSize, String digestAlg, String fingerprintAlg) {
		this(Tmin, Tmax, D, Ddash, windowSize, digestAlg, fingerprintAlg, "TTTD-" + Tmin + "-" + Tmax + "-" + D + "-" + Ddash + "-" + digestAlg + "-"
				+ fingerprintAlg);
	}

	private TttdChunker(int Tmin, int Tmax, int D, int Ddash, int windowSize, String digestAlg, String fingerprintAlg, String name) {
		this.Tmin = Tmin;
		this.Tmax = Tmax;
		this.D = D;
		this.Ddash = Ddash;
		this.windowSize = windowSize;
		this.checksumAlgorithm = digestAlg;
		this.fingerprintAlgorithm = fingerprintAlg;
		this.name = name;

		if (windowSize > Tmin) {
			throw new IllegalArgumentException("Window size must be smaller than Tmin.");
		}
	}

	@Override
	public ChunkEnumeration createChunks(File file) throws IOException {
		return new TTTDEnumeration(new FileInputStream(file));
	}

	@Override
	public String getChecksumAlgorithm() {
		return checksumAlgorithm;
	}

	@Override
	public String toString() {
		return name;
	}

	public class TTTDEnumeration implements ChunkEnumeration {
		private InputStream in;
		private boolean closed;
		private byte[] c;
		private int clen;
		private int cpos;

		private MessageDigest chunkDigest;
		private MessageDigest fileDigest;
		private Fingerprinter fingerprinter;

		public TTTDEnumeration(InputStream in) throws IOException {
			this.in = in;
			this.closed = false;
			this.c = new byte[8192];
			this.clen = -1;
			this.cpos = -1;

			try {
				fingerprinter = Fingerprinter.getInstance(fingerprintAlgorithm);
				chunkDigest = MessageDigest.getInstance(checksumAlgorithm);
				fileDigest = MessageDigest.getInstance(checksumAlgorithm);

				fileDigest.reset();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean hasMoreElements() {
			return !closed;
		}

		@Override
		public Chunk nextElement() {
			if (closed) {
				return null;
			}

			chunkDigest.reset();
			fingerprinter.reset();

			try {
				int backupBreak = 0;
				int breakpoint = -1;

				byte[] buf = new byte[Tmax];
				int bufpos = -1;

				while (bufpos < buf.length - 1) {
					if (cpos == -1 || cpos == clen - 1) {
						cpos = -1;
						clen = readFromInputStreamFixed(c, in);//in.read(c);

						if (clen == -1) {
							break;
						}

						fileDigest.update(c, 0, clen);
					}

					bufpos++;
					cpos++;
					buf[bufpos] = c[cpos];

					if (bufpos < Tmin) {
						continue;
					}
					else if (bufpos == Tmin) {
						fingerprinter.check(buf, bufpos - windowSize, windowSize);
					}
					else {
						fingerprinter.roll(buf[bufpos]);
					}

					int hash = fingerprinter.getValue();

					// The value of r (right side) plays no role! #39  
					if ((hash % Ddash) == Ddash - 1) {
						backupBreak = bufpos;
					}

					if ((hash % D) == D - 1) {
						breakpoint = bufpos;
						break;
					}

					if (bufpos < Tmax) {
						continue;
					}

					if (backupBreak != 0) {
						breakpoint = backupBreak;
						break;
					}
					else {
						breakpoint = bufpos;
						break;
					}
				}

				// Close if this was the last bytes
				if (clen == -1) {
					in.close();
					closed = true;
				}

				// EOF as breakpoint
				if (breakpoint == -1) {
					breakpoint = bufpos;
				}

				// Increase breakpoint
				breakpoint++;

				// Create chunk
				chunkDigest.update(buf, 0, breakpoint);

				byte[] chunkChecksum = chunkDigest.digest();
				byte[] chunkContents = buf;
				int chunkSize = breakpoint;
				byte[] fileChecksum = (clen == -1) ? fileDigest.digest() : null;

				return new Chunk(chunkChecksum, chunkContents, chunkSize, fileChecksum);
			}
			catch (IOException ex) {
				logger.log(Level.SEVERE, "Error while retrieving next chunk.", ex);
				return null;
			}
		}

		@Override
		public void close() {
			try {
				in.close();
			}
			catch (IOException e) {
				logger.log(Level.INFO, "Error while closing", e);
			}
		}

		/**
		 * Fixes the read errors occurring with Cipher streams in the standard
		 * Java read implementation.
		 */
		private int readFromInputStreamFixed(byte[] readToBuffer, InputStream inputStream) throws IOException {
			int bytesRead = 0;

			while (bytesRead < readToBuffer.length) {
				int byteRead = inputStream.read();

				if (byteRead == -1) {
					return (bytesRead != 0) ? bytesRead : -1;
				}

				readToBuffer[bytesRead] = (byte) byteRead;
				bytesRead++;
			}

			return (bytesRead != 0) ? bytesRead : -1;
		}
	}
}
