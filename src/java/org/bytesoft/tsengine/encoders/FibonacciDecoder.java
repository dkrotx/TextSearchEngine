package org.bytesoft.tsengine.encoders;

import util.BitBufferReader;

import java.nio.ByteBuffer;

/**
 * FibonacciDecoder - decode numbers compressed with fibonacci encoding method
 */
public class FibonacciDecoder implements IntDecompressor {
    private BitBufferReader bitbuf;

    public FibonacciDecoder(ByteBuffer buf) {
        bitbuf = new BitBufferReader(buf);
    }

    @Override
    public int ExtractNumber() {
        int res = 0;
        int idx = 0;

        for(boolean prev = false, bit = bitbuf.readBit();
            !bit || !prev;
            prev = bit, bit = bitbuf.readBit())
        {
            if (bit)
                res += FibonacciEncoder.FIB_SEQUENCE[idx];

            idx++;
        }

        return res;
    }
}
