package org.bytesoft.tsengine.encoders;

import util.bytes.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Simple9 encoding method.
 * This is buffered encoding method -
 *   it's much easier to encode full array to simple9 because it's stateful.
 *
 */
public class Simple9Encoder implements IntCompressor {
    ArrayList<Integer> numbers = new ArrayList<>();
    static final int INTEGER_MEMORY_SIZE = 8;
    static final int BODY_BITS = 28;

    static final class variant {
        int code;
        int bits_per_number;
        int amount;

        public variant(int code, int bits_per_number) {
            this.code = code;
            this.bits_per_number = bits_per_number;
            amount = BODY_BITS / bits_per_number;
        }
    }

    static final variant[] variants = {
            new variant(0, 1), new variant(1, 2), new variant(2, 3),
            new variant(3, 4), new variant(4, 5), new variant(5, 7),
            new variant(6, 9), new variant(7, 14), new variant(8, 28)
    };

    ByteArrayOutputStream storage = new ByteArrayOutputStream();


    private static int count_bits(int num) {
        int n = 0;

        for (; num != 0; num >>= 1)
            n++;

        return n;
    }

    private variant get_variant(int offset) {
        int rest = numbers.size() - offset;

        for(variant v: variants) {
            if (rest < v.amount)
                continue;

            boolean matches = true;

            for (int i = 0; i < v.amount; i++)
                if (count_bits(numbers.get(offset + i)) > v.bits_per_number) {
                    matches = false;
                    break;
                }

            if (matches)
                return v;
        }

        return variants[variants.length - 1];
    }

    private int encode_by_variant(int offset, variant v) {
        int res = v.code << 28;
        int n = Math.min(v.amount, numbers.size() - offset);

        for(int i = 0; i < n; i++)
            res |= ( numbers.get(offset + i) << v.bits_per_number*i );

        return res;
    }

    @Override
    public void AddNumber(int x) throws TooLargeToCompressException {
        if (x < 0 || x > GetMaxEncodingInt())
            throw new TooLargeToCompressException("Number too large to compress with Simple9");

        numbers.add(x);
    }

    @Override
    public byte[] GetBytes() {
        if (!numbers.isEmpty())
            encodeBuffered();

        return storage.toByteArray();
    }

    private void encodeBuffered() {
        try {
            int offset = 0;
            while (offset < numbers.size()) {
                variant v = get_variant(offset);
                int val = encode_by_variant(offset, v);
                storage.write(Bytes.toBytes(val));
                offset += v.amount;
            }

        } catch (IOException e) {
            throw new OutOfMemoryError("No memory to encode simple9 buffer");
        }

        numbers.clear();
    }

    @Override
    public void flush() {
        encodeBuffered();
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public int GetStoreSize() {
        final int buffered_size = numbers.size()*INTEGER_MEMORY_SIZE + 4;
        return storage.size() + buffered_size;
    }

    /**
     * Get maximum integer value to encode.
     * You can't encode {@code GetMaxEncodingInt()+1} without getting {@code TooLargeToCompressException}
     *
     * @return this limit value
     */
    public static int GetMaxEncodingInt() {
        return 0x0fffffff;
    }

    @Override
    public boolean CanEncodeZero() {
        return true;
    }
}
