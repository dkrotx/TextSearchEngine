package util;

import java.nio.ByteBuffer;

/**
 * Almost like ByteBuffer, but designed to read BitSet from ByteBuffer zero-copy
 */
public class BitBufferReader {
    ByteBuffer buf;
    int cur_byte_offset = 8;
    int cur_byte = 0;

    public BitBufferReader(ByteBuffer buf) {
        this.buf = buf;
    }

    public boolean readBit() {
        if (cur_byte_offset >= 8) {
            cur_byte = (int)buf.get() & 0xff;
            cur_byte_offset = 0;
        }

        return (cur_byte & (1 << cur_byte_offset++)) != 0;
    }

    public final boolean hasRemaining() {
        return cur_byte_offset < 8 || buf.hasRemaining();
    }
}