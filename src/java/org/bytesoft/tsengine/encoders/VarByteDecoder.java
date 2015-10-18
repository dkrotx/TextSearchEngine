package org.bytesoft.tsengine.encoders;

import java.nio.ByteBuffer;

/**
 * VarByteDecoder - decode compressed array of integers
 */
public class VarByteDecoder implements IntDecompressor {
    private ByteBuffer buf;

    public VarByteDecoder(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public int ExtractNumber() {
        byte b   = buf.get();
        int  res = b & 0x7f;

        while (b < 0) {
            b = buf.get();
            res <<= 7;
            res |= b & 0x7f;
        }

        return res;
    }
}
