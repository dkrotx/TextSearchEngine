package org.bytesoft.tsengine.encoders;

import java.util.BitSet;

/**
 * EliasGammaEncoder - class Elias Gamma coding
 * Rule of encoding is simple:
 *   - write number in usual, binary form
 *   - prefix number with `amount_of_digits - 1` encoded unary.
 *
 * For example:
 *   1 => 1
 *   5 => 00101
 */
public class EliasGammaEncoder implements IntCompressor {
    BitSet storage   = new BitSet();
    int    bit_index = 0;

    private void add_bit(boolean bit) {
        storage.set(bit_index++, bit);
    }

    private static int count_bits(int num) {
        int n = 0;

        for (; num != 0; num >>= 1)
            n++;

        return n;
    }

    /**
     * Add number to elias bit-buffer
     * @param num integer to encode
     */
    public void AddNumber(int num) throws IntCompressor.TooLargeToCompressException {
        if (num <= 0 || num > GetMaxEncodingInt()) {
            throw new TooLargeToCompressException("Number too large to compress with Elias Gamma");
        }

        int nbits = count_bits(num);
        for (int i = 1; i < nbits; i++) {
            add_bit(false);
        }

        for (int i = nbits - 1; i >= 0; i--) {
            add_bit((num & (1 << i)) != 0);
        }
    }

    @Override
    public int size() {
        return (bit_index >> 3) + (((bit_index & 7) == 0) ? 0 : 1);
    }


    public byte[]  GetBytes() {
        byte[] bytes = storage.toByteArray();
        int    exact_size = size();

        if (bytes.length == exact_size)
            return bytes;

        // BitSet.toByteArray has "nice" bug-feature:
        // it silently ignores final zeroes.
        // So, if you encode "0xff, 0x00" (16 bits_per_number) you'll get only 1 byte!
        byte[] exact_bytes = new byte[exact_size];
        System.arraycopy(bytes, 0, exact_bytes, 0, bytes.length);

        return exact_bytes;
    }

    @Override
    public void flush() {
        while ((bit_index & 7) != 0)
            add_bit(true);
    }

    /**
     * Get maximum integer value to encode.
     * You can't encode {@code GetMaxEncodingInt()+1} without getting {@code TooLargeToCompressException}
     *
     * @return this limit value
     */
    public static int GetMaxEncodingInt() {
        return Integer.MAX_VALUE;
    }

    public boolean CanEncodeZero() {
        return false;
    }
}
