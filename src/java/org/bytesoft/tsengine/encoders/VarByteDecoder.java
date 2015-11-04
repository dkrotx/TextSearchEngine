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
    public void Position(int new_position) {
        buf.position(new_position);
    }

    @Override
    public int ExtractNumber() {
        return ExtractNumberFromBuf(buf);
    }

    public static int ExtractNumberFromBuf(ByteBuffer buf) {
        byte b   = buf.get();
        int  res = b & 0x7f;

        while (b < 0) {
            b = buf.get();
            res = (res << 7) | (b & 0x7f);
        }

        return res;
    }

    @Override
    public boolean CanDecodeZero() { return true; }
}
