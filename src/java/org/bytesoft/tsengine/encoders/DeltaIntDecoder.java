package org.bytesoft.tsengine.encoders;

/**
 * DeltaIntDecoder - decode integer sequence
 * represented by differences instead of absolute numbers
 */
public class DeltaIntDecoder implements IntDecompressor {
    private IntDecompressor decoder;
    private int prev = 0;

    /**
     * Construct DeltaIntDecoder
     * @param decoder underlying decoder
     */
    public DeltaIntDecoder(IntDecompressor decoder) {
        this.decoder = decoder;
    }

    @Override
    public int ExtractNumber() {
        int res = prev + decoder.ExtractNumber();
        return (prev = res);
    }
}
