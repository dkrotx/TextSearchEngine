package org.bytesoft.tsengine.encoders;

/**
 * VarByteEncoder - class for variable byte encoding
 */
public class VarByteEncoder implements IntCompressor {
    CompactByteBuffer storage = new CompactByteBuffer();

    /**
     * Add number to varbyte buffer
     * @param num integer to encode
     */
    public void AddNumber(int num) throws TooLargeToCompressException {
        if (num < 0 || num >= (1 << 28)) {
            throw new TooLargeToCompressException("Number too large to compress with varbyte");
        }

        // encode from highest bits, it will be faster to decode then
        for (int shift = 21; shift > 0; shift -= 7) {
            if (num >= (1 << shift))
                storage.AddByte((num & (0x7f << shift)) >> shift | 0x80);
        }

        storage.AddByte(num & 0x7f);
    }

    public int GetStoreSize() { return storage.GetSize();  }
    public byte[]  GetBytes() { return storage.GetBytes(); }
}
