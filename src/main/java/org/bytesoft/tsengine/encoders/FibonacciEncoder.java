package org.bytesoft.tsengine.encoders;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * Fibonacci encoder - class for fibonacci bit encoding
 */
public class FibonacciEncoder implements IntCompressor {

    static int[] FIB_SEQUENCE;

    static {
        ArrayList<Integer> sequence = new ArrayList<>();

        sequence.add(1);
        sequence.add(1);

        for(;;)
        {
            long n = (long)sequence.get(sequence.size() - 2) + sequence.get(sequence.size() - 1);
            if (n > (long)Integer.MAX_VALUE)
                break;

            sequence.add((int)n);
        }

        FIB_SEQUENCE = new int[sequence.size() - 1];
        for (int i = 1; i < sequence.size(); i++)
            FIB_SEQUENCE[i - 1] = sequence.get(i);
    }

    BitSet storage = new BitSet();
    int bit_index = 0;

    private long bin2fib(int num) {
        int high = FIB_SEQUENCE.length - 1;

        for(int i = 0 ; i < FIB_SEQUENCE.length; i++)
            if (FIB_SEQUENCE[i] > num) {
                high = i - 1;
                break;
            }

        long result = 0;

        for(int i = high; i >= 0 && num != 0; i--) {
            if (num >= FIB_SEQUENCE[i]) {
                num -= FIB_SEQUENCE[i];
                result |= (1L << i);
            }
        }

        return result;
    }

    private void add_bit(boolean bit) {
        storage.set(bit_index++, bit);
    }

    /**
     * Add number to fibonacci-encoded buffer
     * @param num integer to encode
     * @throws TooLargeToCompressException only if num is zero or negative
     */
    public void AddNumber(int num) throws IntCompressor.TooLargeToCompressException {
        if (num <= 0) {
            throw new IntCompressor.TooLargeToCompressException("Fibonacci can't encode zero or negatives");
        }

        // write fibonacci number from lower bits_per_number, so '11' will be terminator
        for (long fibnum = bin2fib(num); fibnum != 0; fibnum >>= 1)
            add_bit((fibnum & 1) != 0);

        add_bit(true);
    }

    @Override
    public void flush() {
        while ((bit_index & 7) != 0)
            add_bit(false);
    }

    public byte[]  GetBytes() { return storage.toByteArray(); }

    @Override
    public int size() {
        return (bit_index >> 3) + (((bit_index & 7) == 0) ? 0 : 1);
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
