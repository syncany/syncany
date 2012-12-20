/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.util.chunk2.chunking;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 *
 * 
 * @see http://www.garykessler.net/library/file_sigs.html
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ContentBasedChunker extends Chunker {
    public static final String DEFAULT_DIGEST_ALG = "SHA1";
    
    // Partially from: http://www.xinotes.org/notes/note/378/
    private static final int BUFFER_SIZE = 4096;
    
    private Chunker defaultSmallChunker;    
    private Chunker defaultMediumChunker;  
    private Chunker defaultBigChunker;  
    private MessageDigest digest;
    
    
    public ContentBasedChunker(Chunker defaultChunker) {
        this(defaultChunker, defaultChunker, defaultChunker, DEFAULT_DIGEST_ALG);
    }
    
    public ContentBasedChunker(Chunker defaultSmallChunker, Chunker defaultMediumChunker) {
        this(defaultSmallChunker, defaultMediumChunker, defaultMediumChunker, DEFAULT_DIGEST_ALG);
    }
    public ContentBasedChunker(Chunker defaultSmallChunker, Chunker defaultMediumChunker, Chunker defaultBigChunker) {
        this(defaultSmallChunker, defaultMediumChunker, defaultBigChunker, DEFAULT_DIGEST_ALG);
    }
    
    public ContentBasedChunker(Chunker defaultSmallChunker, Chunker defaultMediumChunker,  Chunker defaultBigChunker, String digestAlg) {
        this.defaultSmallChunker = defaultSmallChunker;
        this.defaultMediumChunker = defaultMediumChunker;
        this.defaultBigChunker = defaultBigChunker;
        
        try {
            this.digest = MessageDigest.getInstance(digestAlg);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }           
    }
  
    @Override
    public Enumeration<Chunk> createChunks(InputStream in) throws IOException {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in);
        }
 
        // Identify file by signature
        in.mark(BUFFER_SIZE);

        byte[] buffer = new byte[BUFFER_SIZE];
        int read = in.read(buffer, 0, BUFFER_SIZE);

        in.reset();

        /*if (isMP3(buffer, read)) {
            return new Mp3ChunkEnumeration(in, digest);
        }
        else*/ if (isJPGExif(buffer, read)) {
            return defaultMediumChunker.createChunks(in);
        }
        else if (isVideo(buffer, read)) {
            return defaultBigChunker.createChunks(in);
        }
        else if (isOfficeFile(buffer, read)) { // must be before ZIP!
            return defaultSmallChunker.createChunks(in);
        }
        /*else if (isZip(buffer, read)) {
            //return defaultMediumChunker.createChunks(in);
            return new ZipChunkEnumeration(in, digest);
        } */       
        else {
            return defaultSmallChunker.createChunks(in);
        }            
    }

    private boolean isOfficeFile(byte[] buffer, int size) {    
        return 
            /* DOC, DOT, PPS, PPT, XLA, XLS, WIZ 	 
               From: http://www.garykessler.net/library/file_sigs.html */
            matchesSignature(new int[] { 0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1 }, buffer, size) ||
            
            /* DOCX, XLSX, PPTX 
               From: http://www.garykessler.net/library/file_sigs.html */
            matchesSignature(new int[] { 0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00 }, buffer, size) ||
            
            /* Open Office documents: ODF/ODS/.. - ZIP archives with special extra signature
               "PK.." [26 random bytes] "mimetypeapplication/vnd.oasis.opendocument" (self-identified) */ 
            matchesSignature(new int[] { 
                /*  0 */ 0x50, 0x4B, 0x03, 0x04, // = ZIP archvie
                /*  4 */ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
                /* 30 */ 0x6D, 0x69, 0x6D, 0x65, 0x74, 0x79, 0x70, 0x65, 0x61, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F,
                         0x6E, 0x2F, 0x76, 0x6E, 0x64, 0x2E, 0x6F, 0x61, 0x73, 0x69, 0x73, 0x2E, 0x6F, 0x70, 0x65, 0x6E,
                         0x64, 0x6F, 0x63, 0x75, 0x6D, 0x65, 0x6E, 0x74 }, buffer, size);        
    }

    private boolean isPDFFile(byte[] buffer, int size) {
        return matchesSignature(new int[] { 0x25, 0x50, 0x44, 0x46 }, buffer, size);
    }
    
    private boolean isZip(byte[] buffer, int size) {
        return matchesSignature(new int[] { 0x50, 0x4B, 0x03, 0x04 }, buffer, size);
    }

    private boolean isJPGExif(byte[] buffer, int size) {
        return matchesSignature(new int[] { 0xFF, 0xD8, 0xFF }, buffer, size);
        
        //    /* JFIF  */ matchesSignature(new int[] { 0xFF, 0xD8, 0xFF, 0xE0, -1, -1, 0x4A, 0x46, 0x49, 0x46, 0x00 }, buffer, size) ||
        //    /* Exif  */ matchesSignature(new int[] { 0xFF, 0xD8, 0xFF, 0xE1, -1, -1, 0x45, 0x78, 0x69, 0x66, 0x00 }, buffer, size) ||
        //    /* SPIFF */ matchesSignature(new int[] { 0xFF, 0xD8, 0xFF, 0xE8, -1, -1, 0x53, 0x50, 0x49, 0x46, 0x46, 0x00 }, buffer, size);
    }    
    
    private boolean isVideo(byte[] buffer, int size) {
        return
            /* MKV 
               From: http://www.garykessler.net/library/file_sigs.html */
            matchesSignature(new int[] { 0x1A, 0x45, 0xDF, 0xA3, 0x93, 0x42, 0x82, 0x88, 0x6D, 0x61, 0x74, 0x72, 0x6F, 0x73, 0x6B, 0x61 }, buffer, size) ||
            
            /* AVI 
               From: http://www.garykessler.net/library/file_sigs.html */
            matchesSignature(new int[] { 0x52, 0x49, 0x46, 0x46,   -1,   -1,   -1,   -1, 0x41, 0x56, 0x49, 0x20, 0x4C, 0x49, 0x53, 0x54 }, buffer, size) ||

            /* MPG, MPEG
               From: http://www.garykessler.net/library/file_sigs.html */
            (matchesSignature(new int[] { 0x00, 0x00, 0x01 /*, Bx*/ }, buffer, size) 
                && size >= 4 && (buffer[3] & 0xff) >> 4 == 0xB) ||      
                
            /* MPG, VOB
               From: http://www.garykessler.net/library/file_sigs.html */
            matchesSignature(new int[] { 0x00, 0x00, 0x01, 0xBA }, buffer, size);
    }
    

    
    private static boolean isMP3(byte[] buffer, int size) {
        return matchesSignature(new int[] { 0x49, 0x44, 0x33 }, buffer, size);
    }        

    private static boolean matchesSignature(int[] signature, byte[] buffer, int size) {
        if (size < signature.length) {
            return false;
        }

        boolean b = true;
        for (int i = 0; i < signature.length; i++) {
            // -1 is a joker-byte (= anything!)
            if (signature[i] == -1) {
                continue;
            }
            
            if (signature[i] != (0x00ff & buffer[i])) {
                b = false;
                break;
            }
        }
        
        return b;
    }

    @Override
    public String toString() {
        return "CBC-...";
    }
}

