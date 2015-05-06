/**
 * Provides classes perform data deduplication on files.
 * 
 * <p>The central classes of this package are {@link org.syncany.chunk.Chunker Chunker} to
 * break files into individual chunks, {@link org.syncany.chunk.MultiChunker MultiChunker} to combine chunks
 * into larger multichunks (containers), and {@link org.syncany.chunk.Transformer Transformer}
 * transform the multichunk data stream. For variable-size chunkers, the 
 * {@link org.syncany.chunk.Fingerprinter Fingerprinter} class is also important. 
 * 
 * <p>The {@link org.syncany.chunk.Deduper Deduper} combines the above classes and eases
 * their usage. 
 */
package org.syncany.chunk;