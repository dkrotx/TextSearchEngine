package org.bytesoft.tsengine.encoders;

import java.nio.ByteBuffer;

/**
 * Simple9Decoder - decode byte buffer compressed by simple9
 */
public class Simple9Decoder implements IntDecompressor {
    private ByteBuffer buf;
    int[] unpacked = new int[28];
    int   unpacked_offset = 0;
    int   unpacked_size = 0;

    public Simple9Decoder(ByteBuffer buf) {
        this.buf = buf;
    }

    private void extract_next() {
        int value = buf.getInt();

        Simple9Encoder.variant v = Simple9Encoder.variants[value >>> 28];
        int mask = (1 << v.bits_per_number) - 1;

        for (int i = 0; i < v.amount; i++) {
            unpacked[i] = value & mask;
            value >>= v.bits_per_number;
        }

        unpacked_size = v.amount;
        unpacked_offset = 0;
    }

    @Override
    public int ExtractNumber() {
        if (unpacked_offset >= unpacked_size)
            extract_next();

        return unpacked[unpacked_offset++];
    }

    @Override
    public void Position(int new_position) {
        buf.position(new_position);
        unpacked_size = 0;
    }

    @Override
    public boolean CanDecodeZero() { return true; }
}
