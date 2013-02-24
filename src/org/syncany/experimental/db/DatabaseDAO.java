package org.syncany.experimental.db;

/**
 * Len             Description
 * -------------------------------------------------------                             
 * // Preamble
 * 7 byte          'Syncany' 
 * 1 byte           0x01 (Database format version)
 *   
 * // Chunks
 * 1 int           Number of chunks (= n)  
 *   // If n > 0
 *     // For each chunk (n times)
 *     1 byte      Checksum length (= m)
 *     m byte      Checksum
 * 
 * // Multichunks
 * 1 int           Number of multichunks (= n)
 *   // If n > 0
 *     // For each multichunk (n times)
 *     1 byte      Checksum length (= m)
 *     m byte      Checksum
 *     1 short     Number of chunks in multichunk (= p)
 *     // If p > 0
 *       // For each chunk (p times)
 *       m byte    Chunk checksum
 * 
 * // Content
 * 1 int           Number of contents (= n)
 *   // If n > 0
 *     // For each content (n times)
 *     1 byte      Checksum length (= m)
 *     m byte      Checksum
 *     1 int       Content size
 *     1 short     Number of chunks in content (= p)
 *     // If p > 0
 *       // For each chunk (p times)
 *       m byte    Chunk checksum
 * 
 * // File Histories
 * 1 int           Number of file histories (= n)
 *   // If n > 0
 *     // For each file history (n times)
 *     1 long      File ID
 *     
 * // File Versions
 * 1 int           Number of file versions (= n)
 *   // If n > 0
 *     // For each file version (n times)
 *     1 long      File ID (of file history)
 *     1 long      File version (of this file)
 *     // If is empty or directory
 *       1 byte    0x00 (empty marker)
 *     // If is file and not empty
 *       1 byte    0x01 (non-empty-marker)
 *       1 byte    Content checksum size (= m)
 *       m byte    Content checksum
 *     // Endif
 *     1 short     Path length (= p)
 *     p byte      Path
 *     1 short     Name length (= q)
 *     q byte      Name 
 */  
public class DatabaseDAO {

}
