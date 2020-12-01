package org.wysko.util;
/*
 * BitmapEncoder, encodes any byte array, stream or file into a 24-bit bitmap (BMP).
 * Copyright (C) 2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Encodes any byte array, stream or file into a 24-bit bitmap (BMP).
 * 
 * The <code>encodeToBitmap()</code>-methods can be used to transform any binary data 
 * into a rectangular image and stored in the widely supported 24-bit bitmap
 * format. The method does not hide, encrypt or compress the source data in any
 * way. It merely prepends a bitmap header and transforms the payload as 
 * specified by the bitmap file format.
 * 
 * <p>The <code>decodeFromBitmap()</code>-methods retrieve the orginal payload from a 
 * previously encoded bitmap. A bitmap not encoded with this class cannot be read.
 * 
 * <p><b>Example:</b>
 * 
 * <pre>
 * // Encode file "/etc/hosts" into file "/tmp/hosts.bmp", and restore it to "/tmp/hosts-restored"
 * BitmapEncoder.encodeToBitmap(new File("/etc/hosts"), new File("/tmp/hosts.bmp"));
 * BitmapEncoder.decodeFromBitmap(new File("/tmp/hosts.bmp"), new File("/tmp/hosts-restored"));
 * 
 * // Encode 3 bytes into a bitmap file
 * BitmapEncoder.encodeToBitmap(new byte[] { 0x01, 0x02, 0x03 }, new File("/tmp/3-bytes.bmp"));
 * byte[] threebytes = BitmapEncoder.decodeFromBitmap(new File("/tmp/3-bytes.bmp"));
 * </pre>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @see http://blog.philippheckel.com/2013/03/02/java-encode-any-byte-array-stream-or-file-into-a-24-bit-bitmap-bmp/
 */
public class BitmapEncoder {        
    private static final int BMP_SIZE_HEADER               = 54;                    // total header length, 54 bytes
    private static final int BMP_SIZE_IMAGE_WIDTH          = 4;                     // size of image width field, 4 bytes
    private static final int BMP_SIZE_PAYLOAD_LENGTH       = 4;                     // size of 'horizontal resolution' field, here: payload length, 4 bytes
    private static final int BMP_SIZE_BMPUTIL_MAGIC        = 4;                     // size of 'vertical resolution' field, here: payload length, 4 bytes

    private static final int BMP_OFFSET_FILESIZE_BYTES     = 2;                     // offset of filesize field, 4 bytes
    private static final int BMP_OFFSET_IMAGE_WIDTH        = 18;                    // offset of image width field, 4 bytes
    private static final int BMP_OFFSET_IMAGE_HEIGHT       = 22;                    // offset of image height field, 4 bytes
    private static final int BMP_OFFSET_IMAGE_DATA_BYTES   = 34;                    // 4 bytes
    private static final int BMP_OFFSET_PAYLOAD_LENGTH     = 38;                    // 4 bytes
    private static final int BMP_OFFSET_BMPUTIL_MAGIC      = 42;                    // 4 bytes
    
    private static final byte UDEF                         = 0;                     // undefined value in bitmap header, to be overwritten by methods                              
    
    /**
     *  24-bit bitmap header
     *  @see http://www.fastgraph.com/help/bmp_header_format.html
     */
    private static final byte[] BMP_HEADER = new byte[] {
        /* 00 */ 0x42, 0x4d,                                                        // signature, "BM"
        /* 02 */ UDEF, UDEF, UDEF, UDEF,                                            // size in bytes, filled dynamically
        /* 06 */ 0x00, 0x00,                                                        // reserved, must be zero
        /* 08 */ 0x00, 0x00,                                                        // reserved, must be zero
        /* 10 */ 0x36, 0x00, 0x00, 0x00,                                            // offset to start of image data in bytes
        /* 14 */ 0x28, 0x00, 0x00, 0x00,                                            // size of BITMAPINFOHEADER structure, must be 40 (0x28)
        /* 18 */ UDEF, UDEF, UDEF, UDEF,                                            // image width in pixels, filled dynamically
        /* 22 */ UDEF, UDEF, UDEF, UDEF,                                            // image height in pixels, filled dynamically
        /* 26 */ 0x01, 0x00,                                                        // number of planes, must be 1
        /* 28 */ 0x18, 0x00,                                                        // number of bits per pixel (1, 4, 8, or 24) -> 24 = 0x18
        /* 30 */ 0x00, 0x00, 0x00, 0x00,                                            // compression type (0=none, 1=RLE-8, 2=RLE-4)
        /* 34 */ UDEF, UDEF, UDEF, UDEF,                                            // size of image data in bytes (including padding)
        /* 38 */ UDEF, UDEF, UDEF, UDEF,                                            // normally: horizontal resolution in pixels per meter (unreliable)
                                                                                    //  --> HERE: used to indicate the payload length
        /* 42 */ UDEF, UDEF, UDEF, UDEF,                                            // vertical resolution in pixels per meter (unreliable)
                                                                                    //  --> HERE: used to mark file as encoded bitmap, see BMPUTIL_MAGIC
        /* 46 */ 0x00, 0x00, 0x00, 0x00,                                            // number of colors in image, or zero
        /* 50 */ 0x00, 0x00, 0x00, 0x00,                                            // number of important colors, or zero
    };  
    
    /**
     *  Magic value to mark files created by this class
     */
    private static final byte[] BMPUTIL_MAGIC = new byte[] {
    	0x30, 0x32, 0x30, 0x35                                                      // mark used in 'vertical resolution' field to mark bmp file, "0205"
    };
  
    /**
     * Encodes any <code>File</code> into a 24-bit bitmap (BMP) and outputs the image
     * to another <code>File</code>.
     * 
     * <p>The method does not hide, encrypt or compress the source data in any
     * way. It merely prepends a bitmap header and transforms the payload as 
     * specified by the bitmap file format.
	 * 
     * @param srcFile File to be read pointing to the input data (payload)
     * @param destFile File to write the 24-bit bitmap file to
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */     
    public static void encodeToBitmap(File srcFile, File destFile) throws IOException {
    	encodeToBitmap(new FileInputStream(srcFile), srcFile.length(), new FileOutputStream(destFile));
    }
    
    /**
     * Encodes any <code>File</code> into a 24-bit bitmap (BMP) and outputs the image
     * to another <code>File</code>.
     * 
     * <p>The method does not hide, encrypt or compress the source data in any
     * way. It merely prepends a bitmap header and transforms the payload as 
     * specified by the bitmap file format.
	 * 
     * @param srcBytes Byte array of input data (payload)
     * @param destStream Stream to write the 24-bit bitmap file to
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */    
    public static void encodeToBitmap(byte[] srcBytes, File destFile) throws IOException {
    	encodeToBitmap(new ByteArrayInputStream(srcBytes), srcBytes.length, new FileOutputStream(destFile));
    }
    
    /**
     * Encodes a byte array into a 24-bit bitmap (BMP) and outputs the image
     * to an <code>OutputStream</code>.
     * 
     * <p>The method does not hide, encrypt or compress the source data in any
     * way. It merely prepends a bitmap header and transforms the payload as 
     * specified by the bitmap file format.
	 * 
     * @param srcBytes Byte array of input data (payload)
     * @param destStream Stream to write the 24-bit bitmap file to
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */
    public static void encodeToBitmap(byte[] srcBytes, OutputStream destStream) throws IOException {
    	encodeToBitmap(new ByteArrayInputStream(srcBytes), srcBytes.length, destStream);
    }
    
    /**
     * Encodes an <code>InputStream</code> into a 24-bit bitmap (BMP) and outputs the image
     * as byte array.
	 *
     * <p>The method does not hide, encrypt or compress the source data in any
     * way. It merely prepends a bitmap header and transforms the payload as 
     * specified by the bitmap file format.
	 * 
     * @param srcStream Stream of input data (payload)
     * @param srcStreamLength Length of the input data stream (in bytes)
     * @param destStream Stream to write the 24-bit bitmap file to
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */
    public static void encodeToBitmap(InputStream srcStream, long srcStreamLength, OutputStream destStream) throws IOException {	
        if (srcStreamLength > Integer.MAX_VALUE) {
            throw new IOException("File too big; max. "+Integer.MAX_VALUE+" bytes supported.");
        }

        // CALCULATE HEADER FIELDS
        
        int imageWidth = (int) Math.ceil(Math.sqrt((double) srcStreamLength / 3));  // Image width, sqrt(payload/3), divided by 3 because of RGB
        int imageHeight = (int) Math.ceil((double) srcStreamLength                  // Image height, payload / image width / 3
        		/ (double) imageWidth / 3);
        
        int rowPadding = 4 - (imageWidth*3 % 4);                                    // Padding at row end, row length must be divisible by 4
        int filesizeBytes = imageWidth*imageHeight*3 + imageHeight*rowPadding       // Total bitmap size, RGB-data + padding-data + header
        		+ BMP_SIZE_HEADER;

        int imageBytesWithPadding = filesizeBytes - BMP_SIZE_HEADER;                // Image bytes without header (incl. padding), required field in header 
        int payloadPadding = (int) (imageWidth*imageHeight*3 - srcStreamLength);	// Padding at the end of the image, to make it rectangular 	

        // System.out.println("payloadLength = "+srcStreamLength);
        // System.out.println("imageWidth = sqrt(payloadLength/3) = "+imageWidth);
        // System.out.println("imageHeight = payloadLength / imageWidth / 3 = "+imageHeight);
        // System.out.println("rowPadding = "+rowPadding);
        // System.out.println("filesizeBytes = imageWidth*imageHeight*3 + imageHeight*rowPadding + BMP_SIZE_HEADER = "+filesizeBytes);	
        // System.out.println("imageBytesWithPadding = filesizeBytes - BMP_SIZE_HEADER = "+imageBytesWithPadding);
        // System.out.println("payloadPadding = imageWidth*imageHeight*3 - payloadLength = "+payloadPadding);
        

        // WRITE FIELDS TO HEADER
        
        byte[] header = BMP_HEADER.clone();                                         // Clone bitmap header template, and overwrite with fields

        writeIntLE(header, BMP_OFFSET_FILESIZE_BYTES, filesizeBytes);
        writeIntLE(header, BMP_OFFSET_IMAGE_WIDTH, imageWidth);
        writeIntLE(header, BMP_OFFSET_IMAGE_HEIGHT, imageHeight);
        writeIntLE(header, BMP_OFFSET_IMAGE_DATA_BYTES, imageBytesWithPadding);
        writeIntLE(header, BMP_OFFSET_PAYLOAD_LENGTH, (int) srcStreamLength);   
        
        System.arraycopy(BMPUTIL_MAGIC, 0, header,                                  // Copy magic number to header 
        		BMP_OFFSET_BMPUTIL_MAGIC, BMPUTIL_MAGIC.length);

        
        // WRITE TO STREAM
        
        // Add payload 
        destStream.write(header, 0, header.length);

        // Write other lines (regular width)
        byte[] row = new byte[imageWidth*3];
        int read;

        while ((read = srcStream.read(row)) != -1) {
        	destStream.write(row, 0, read);                                         // Write payload
        	destStream.write(new byte[rowPadding]);                                 // Write padding
        }

        // Write payload padding
        destStream.write(new byte[payloadPadding]);

        srcStream.close();
        destStream.close();
    }       
    
    /**
     * Decodes a 24-bit bitmap previously encoded by this class from 
     * an <code>InputStream</code> to a byte array.
     * 
     * <p>The method can only read bitmaps that were encoded using one of the 
     * <code>encodeToBitmap()</code> methods. It will throw an exception if
     * any other bitmaps are read.
     * 
     * @param srcStream Stream of input data (24-bit bitmap)
     * @return The original data read from the bitmap (payload)
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */
    public static byte[] decodeFromBitmap(InputStream srcStream) throws IOException {
    	ByteArrayOutputStream destStream = new ByteArrayOutputStream();
    	decodeFromBitmap(srcStream, destStream);
    	
    	return destStream.toByteArray();
    }

    /**
     * Decodes a 24-bit bitmap previously encoded by this class from 
     * a <code>File</code> to a byte array.
     * 
     * <p>The method can only read bitmaps that were encoded using one of the 
     * <code>encodeToBitmap()</code> methods. It will throw an exception if
     * any other bitmaps are read.
     * 
     * @param srcFile File to be read pointing to the input data (24-bit bitmap)
     * @return The original data read from the bitmap (payload)
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */
    public static byte[] decodeFromBitmap(File srcFile) throws IOException {
    	ByteArrayOutputStream destStream = new ByteArrayOutputStream();
    	decodeFromBitmap(new FileInputStream(srcFile), destStream);
    	
    	return destStream.toByteArray();    	
    }
    
    /**
     * Decodes a 24-bit bitmap previously encoded by this class from 
     * a <code>File</code> to another <code>File</code>.
     * 
     * <p>The method can only read bitmaps that were encoded using one of the 
     * <code>encodeToBitmap()</code> methods. It will throw an exception if
     * any other bitmaps are read.
     * 
     * @param srcFile File to be read pointing to the input data (24-bit bitmap)
     * @param destFile File to be written the original data to (payload)  
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */    
    public static void decodeFromBitmap(File srcFile, File destFile) throws IOException {
    	decodeFromBitmap(new FileInputStream(srcFile), new FileOutputStream(destFile));
    }
    
    /**
     * Decodes a 24-bit bitmap previously encoded by this class from 
     * an <code>InputStream</code> to an <code>OutputStream</code>.
     * 
     * <p>The method can only read bitmaps that were encoded using one of the 
     * <code>encodeToBitmap()</code> methods. It will throw an exception if
     * any other bitmaps are read.
     * 
     * @param srcStream Stream to be read pointing to the input data (24-bit bitmap)
     * @param destStream Stream to be written the original data to (payload)  
     * @throws IOException Thrown if the input/output stream cannot be read/written
     */        
    public static void decodeFromBitmap(InputStream srcStream, OutputStream destStream) throws IOException {
        // READ HEADER
    	long bytesRead = 0;
        
        // Skip ahead & read 'image width' field
        bytesRead += srcStream.skip(BMP_OFFSET_IMAGE_WIDTH - bytesRead);            // Stores the image width, needed for row length and 
        byte[] imageWidthBytes = new byte[BMP_SIZE_IMAGE_WIDTH];                    // row padding calculation
        bytesRead += srcStream.read(imageWidthBytes);
        int imageWidth = toIntLE(imageWidthBytes);                                  

        // Skip ahead & read 'horizontal resolution' field 
        bytesRead += srcStream.skip(BMP_OFFSET_PAYLOAD_LENGTH - bytesRead);         // Stores the payload length, needed for read-loop below
        byte[] payloadLengthBytes = new byte[BMP_SIZE_PAYLOAD_LENGTH];
        bytesRead += srcStream.read(payloadLengthBytes);
        int payloadLength = toIntLE(payloadLengthBytes);
        
        // Skip ahead & read 'vertical resolution' field            
        bytesRead += srcStream.skip(BMP_OFFSET_BMPUTIL_MAGIC - bytesRead);          // Stores the magic field, needed to check if the image was
        byte[] magicFieldBytes = new byte[BMP_SIZE_BMPUTIL_MAGIC];                  // previously encoded using this class
        bytesRead += srcStream.read(magicFieldBytes);
        
        if (!Arrays.equals(BMPUTIL_MAGIC, magicFieldBytes)) {
        	throw new IOException("Given bitmap does not contain encoded binary data.");
        }

        // Skip ahead to the actual payload
        bytesRead += srcStream.skip(BMP_SIZE_HEADER - bytesRead);                   // Skip the rest of the header 	
                
        // READ PAYLOAD
        if (payloadLength > 0) {
            int rowPaddingLength = 4 - (imageWidth*3 % 4);                          // Padding at row end, row length must be divisible by 4 
        	int rowLength = imageWidth*3;                                           // Row width is 3x the image width (RGB coding)
        
            // System.out.println("payloadLength = "+payloadLength);
            // System.out.println("imageWidth = "+imageWidth);
            // System.out.println("rowPaddingLength = "+rowPaddingLength);
            // System.out.println("rowLength = "+rowLength);        	
        	
	        byte[] row = new byte[rowLength];
	        int read; 
	        int restOfPayload = payloadLength;
	
	        while ((read = srcStream.read(row)) != -1) {
	            if (restOfPayload >= read) {
	                destStream.write(row, 0, read);                                 // Write to output stream
	                srcStream.skip(rowPaddingLength);                               // Skip bitmap padding (if any)
	
	                restOfPayload -= read;
	            }
	            else {                                                                     
	            	destStream.write(row, 0, restOfPayload);                        // Write the last bytes
	                break;
	            }
	        }
        }
        
        srcStream.close();
        destStream.close();	
    }
    
    /**
     *  Write an integer to a byte array (as little endian) at a
     *  specific offset. 
     */
    private static void writeIntLE(byte[] bytes, int startoffset, int value) {
        bytes[startoffset] = (byte)(value);
        bytes[startoffset+1] = (byte)(value >>> 8);
        bytes[startoffset+2] = (byte)(value >>> 16);
        bytes[startoffset+3] = (byte)(value >>> 24);
    }
    
    /**
     * Read an integer value from a 4-byte array (as little endian).
     */
    private static int toIntLE(byte[] value) {
        return ((value[3] & 0xff) << 24) |
            ((value[2] & 0xff) << 16) |
            ((value[1] & 0xff) << 8) |
            (value[0] & 0xff);
    }  
    
    /**
     * Encode/decode a bitmap from the command line.
     * 
     * <p><b>Syntax:</b><br />
     * <tt>BitmapEncoder encode SRCFILE DESTFILE.bmp<br />
     * <tt>BitmapEncoder decode SRCFILE.bmp DESTFILE
     */
    public static void main(String[] args) throws IOException  {
    	if (args.length >= 2 && "encode".equals(args[0])) {
    		BitmapEncoder.encodeToBitmap(new File(args[1]), new File(args[2]));
    	}
    	else if (args.length >= 2 && "decode".equals(args[0])) {
    		BitmapEncoder.decodeFromBitmap(new File(args[1]), new File(args[2]));
    	}
    	else {
    		System.out.println("Usage: BitmapEncoder encode SRCFILE DESTFILE.bmp");
    		System.out.println("       BitmapEncoder decode SRCFILE.bmp DESTFILE");
    	}
    }
}