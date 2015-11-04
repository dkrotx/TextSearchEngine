package org.bytesoft.tsengine.encoders;

import util.BitBufferReader;

import java.nio.ByteBuffer;

/**
 * EliasGammaDecoder - decode numbers compressed with Elias-Gamma encoding method
 */
public class EliasGammaDecoder implements IntDecompressor {
    private BitBufferReader bitbuf;

    public EliasGammaDecoder(ByteBuffer buf) {
        bitbuf = new BitBufferReader(buf);
    }

    @Override
    public int ExtractNumber() {
        int shifts = 0;

        while (bitbuf.readBit() == false)
            shifts++;

        int res = 1 << shifts;

        while (--shifts >= 0) {
            if (bitbuf.readBit())
                res |= 1 << shifts;
        }

        return res;
    }

    @Override
    public void Position(int new_position) {
        bitbuf.position(new_position);
    }

    @Override
    public boolean CanDecodeZero() { return false; }
}
