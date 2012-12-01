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
package org.syncany.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Encodes any files into a 24-bit bitmap (BMP).
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class BitmapUtil {    
    private static final byte UDEF = 0x00;    
    
    private static final int BMP_HEADER_LENGTH = 54;
    private static final int BMP_OFFSET_FILESIZE_BYTES = 2; // 4 bytes;
    private static final int BMP_OFFSET_IMAGE_WIDTH = 18; // 4 bytes
    private static final int BMP_OFFSET_IMAGE_HEIGHT = 22; // 4 bytes
    private static final int BMP_OFFSET_IMAGE_DATA_BYTES = 34; // 4 bytes
    private static final int BMP_OFFSET_PAYLOAD_LENGTH = 38; // 4 bytes
    
    // http://www.fastgraph.com/help/bmp_header_format.html
    private static final byte[] BMP_HEADER = new byte[] {
        /* 00 */ 0x42, 0x4d,             // signature
        /* 02 */ UDEF, UDEF, UDEF, UDEF, // size in bytes, filled dynamically
        /* 06 */ 0x00, 0x00,             // reserved, must be zero
        /* 08 */ 0x00, 0x00,             // reserved, must be zero
        /* 10 */ 0x36, 0x00, 0x00, 0x00, // offset to start of image data in bytes
        /* 14 */ 0x28, 0x00, 0x00, 0x00, // size of BITMAPINFOHEADER structure, must be 40 (0x28)
        /* 18 */ UDEF, UDEF, UDEF, UDEF, // image width in pixels, filled dynamically
        /* 22 */ UDEF, UDEF, UDEF, UDEF, // image height in pixels, filled dynamically
        /* 26 */ 0x01, 0x00,             // number of planes, must be 1
        /* 28 */ 0x18, 0x00,             // number of bits per pixel (1, 4, 8, or 24) -> 24 = 0x18
        /* 30 */ 0x00, 0x00, 0x00, 0x00, // compression type (0=none, 1=RLE-8, 2=RLE-4)
        /* 34 */ UDEF, UDEF, UDEF, UDEF, // size of image data in bytes (including padding)
        /* 38 */ UDEF, UDEF, UDEF, UDEF, // normally: horizontal resolution in pixels per meter (unreliable)
                                         // HERE: used to indicate the payload length
        /* 42 */ 0x00, 0x00, 0x00, 0x00, // vertical resolution in pixels per meter (unreliable)
        /* 46 */ 0x00, 0x00, 0x00, 0x00, // number of colors in image, or zero
        /* 50 */ 0x00, 0x00, 0x00, 0x00, // number of important colors, or zero
    };       
    
    public static void main(String[] args) throws Exception {
        encodeToBitmap(new File("/home/pheckel/Desktop/Philipp's PC/Steno/testfile.txt"), 
            new File("/home/pheckel/Desktop/Philipp's PC/Steno/testfile.bmp"));

        decodeFromBitmap(new File("/home/pheckel/Desktop/Philipp's PC/Steno/testfile.bmp"),
            new File("/home/pheckel/Desktop/Philipp's PC/Steno/testfile-decoded.txt"));
    }
    
    public static void decodeFromBitmap(File srcFile, File dstFile) throws Exception {
        // NOTE: For some reason, decoding from a non-FileInputStream does not
        //       always work properly. Do not create a method 
        //       decodeFromBitmap(InputStream, ...)

        InputStream is = new FileInputStream(srcFile);

        // Read BMP width from header
        is.skip(BMP_OFFSET_IMAGE_WIDTH);

        byte[] imageWidthBytes = new byte[4];
        is.read(imageWidthBytes);
        int imageWidth = toIntLE(imageWidthBytes);

        // Row length must be divisible by 4; calculate padding
        int linePadding = 4 - (imageWidth*3 % 4);

        // Skip to the 'horizontal resolution' field
        is.skip(16);
        byte[] payloadLengthBytes = new byte[4];
        is.read(payloadLengthBytes);
        int payloadLength = toIntLE(payloadLengthBytes);

        is.skip(12);//BMP_HEADER_LENGTH - OFFSET_IMAGE_WIDTH - 4);	

        OutputStream os = new FileOutputStream(dstFile);

        byte[] row = new byte[imageWidth*3];	
        int read; 
        int restOfPayload = payloadLength;

        while ((read = is.read(row)) != -1) {
            if (restOfPayload >= read) {
                os.write(row, 0, read);
                is.skip(linePadding); // skip padding

                restOfPayload -= read;
            }
            else {
                os.write(row, 0, restOfPayload);
                break;
            }
        }

        is.close();
        os.close();	
    }
    
    public static void encodeToBitmap(File srcFile, File dstFile) throws Exception {	
        if (srcFile.length() > Integer.MAX_VALUE) {
            throw new RuntimeException("File too big; max. "+Integer.MAX_VALUE+" bytes supported.");
        }

        int payloadLength = (int) srcFile.length();
        int pixelWidth = (int) Math.ceil(Math.sqrt((double) payloadLength / 3));
        int pixelHeight = (int) Math.ceil((double) payloadLength / (double) pixelWidth / 3 /* RGB */);
        int linePadding = 4 - (pixelWidth*3 % 4); // row length must be divisible by 4; calculate padding

        int filesizeBytes = 
            pixelWidth*pixelHeight*3 /* RGB */ + 
            pixelHeight*linePadding /* padding */ + 
            BMP_HEADER_LENGTH /* BMP header*/;

        int imgBytesWithPadding = filesizeBytes - BMP_HEADER_LENGTH;
        int payloadPadding = pixelWidth*pixelHeight*3 - payloadLength;		

        /*System.out.println("payload = "+payloadLength);
        System.out.println("pixel width  = sqrt(payload/3) = "+pixelWidth);
        System.out.println("pixel height = payload / width / 3 = "+pixelHeight);
        System.out.println("padding per line = "+linePadding);
        System.out.println("imgbytes w/ padding = "+imgBytesWithPadding);
        System.out.println("filesize total = "+filesizeBytes);	
        System.out.println("padding = "+payloadPadding);*/

        byte[] header = BMP_HEADER.clone();

        writeIntLE(header, BMP_OFFSET_FILESIZE_BYTES, filesizeBytes);
        writeIntLE(header, BMP_OFFSET_IMAGE_WIDTH, pixelWidth);
        writeIntLE(header, BMP_OFFSET_IMAGE_HEIGHT, pixelHeight);
        writeIntLE(header, BMP_OFFSET_IMAGE_DATA_BYTES, imgBytesWithPadding);
        writeIntLE(header, BMP_OFFSET_PAYLOAD_LENGTH, payloadLength);

        // Add payload 
        InputStream is = new FileInputStream(srcFile);
        FileOutputStream os = new FileOutputStream(dstFile);

        os.write(header, 0, header.length);

        // Write first row [4 bytes, width-4 bytes]
        //os.write(toByteArrayLE(payloadLength));

        // Write other lines (regular width)
        byte[] row = new byte[pixelWidth*3];
        int read;

        while ((read = is.read(row)) != -1) {
            os.write(row, 0, read);
            os.write(new byte[linePadding]); // padding
        }

        // Write payload padding
        os.write(new byte[payloadPadding]);

        is.close();
        os.close();
    }
    
    // little endian (least significant left)
    private static void writeIntLE(byte[] bytes, int startoffset, int value) {
        bytes[startoffset] = (byte)(value);
        bytes[startoffset+1] = (byte)(value >>> 8);
        bytes[startoffset+2] = (byte)(value >>> 16);
        bytes[startoffset+3] = (byte)(value >>> 24);
    }
    
    // little endian (least significant left)
    
    
    // little endian (least significant left)
    private static int toIntLE(byte[] value) {
        return ((value[3]   & 0xff) << 24) |
            ((value[2] & 0xff) << 16) |
            ((value[1] & 0xff) << 8) |
            (value[0] & 0xff);
    }    
        
}
