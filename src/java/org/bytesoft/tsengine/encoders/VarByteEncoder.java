package org.bytesoft.tsengine.encoders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * VarByteEncoder - class for variable byte encoding
 */
public class VarByteEncoder implements IntCompressor {
    ByteArrayOutputStream storage = new ByteArrayOutputStream();

    /**
     * Add number to varbyte buffer
     * @param num integer to encode
     */
    public void AddNumber(int num) throws TooLargeToCompressException {
        try {
            EncodeNumberToStream(num, storage);
        } catch (IOException e) {
            // actually, never happens
            throw new OutOfMemoryError("No space buffer in VarByteEncoder: " + e.getMessage());
        }
    }

    public static int EncodeNumberToStream(int num, OutputStream stream) throws TooLargeToCompressException, IOException {
        int bytes = 0;
        if (num < 0 || num >= (1 << 28)) {
            throw new TooLargeToCompressException("Number too large to compress with varbyte");
        }

        // encode from highest bits_per_number, it will be faster to decode then
        for (int shift = 21; shift > 0; shift -= 7) {
            if (num >= (1 << shift)) {
                stream.write((num & (0x7f << shift)) >> shift | 0x80);
                bytes++;
            }
        }

        stream.write(num & 0x7f);
        return bytes+1;
    }

    public int GetStoreSize() { return storage.size();  }
    public byte[]  GetBytes() { return storage.toByteArray(); }

    /**
     * Get maximum integer value to encode.
     * You can't encode {@code GetMaxEncodingInt()+1} without getting {@code TooLargeToCompressException}
     *
     * @return this limit value
     */
    public static int GetMaxEncodingInt() {
        return 1 << 28;
    }

    public boolean CanEncodeZero() {
        return true;
    }
}
