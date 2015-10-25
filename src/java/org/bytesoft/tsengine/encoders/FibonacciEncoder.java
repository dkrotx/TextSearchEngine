package org.bytesoft.tsengine.encoders;

import java.util.BitSet;

/**
 * Fibonacci encoder - class for fibonacci bit encoding
 */
public class FibonacciEncoder implements IntCompressor {
    BitSet storage = new BitSet();
    int bit_index = 0;

    private long bin2fib(int num) {
        int high = FibonacciSequence.FIB_SEQUENCE.length - 1;

        for(int i = 0 ; i < FibonacciSequence.FIB_SEQUENCE.length; i++)
            if (FibonacciSequence.FIB_SEQUENCE[i] > num) {
                high = i - 1;
                break;
            }

        long result = 0;

        for(int i = high; i >= 0 && num != 0; i--) {
            if (num >= FibonacciSequence.FIB_SEQUENCE[i]) {
                num -= FibonacciSequence.FIB_SEQUENCE[i];
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
    public void AddNumber(int num) throws TooLargeToCompressException {
        if (num <= 0) {
            throw new TooLargeToCompressException("Fibonacci can't encode zero or negatives");
        }

        // write fibonacci number from lower bits_per_number, so '11' will be terminator
        for (long fibnum = bin2fib(num); fibnum != 0; fibnum >>= 1)
            add_bit((fibnum & 1) != 0);

        add_bit(true);
    }

    public int GetStoreSize() { return (bit_index >> 3) + 1;  }
    public byte[]  GetBytes() { return storage.toByteArray(); }

    /**
     * Get maximum integer value to encode.
     * You can't encode {@code GetMaxEncodingInt()+1} without getting {@code TooLargeToCompressException}
     *
     * @return this limit value
     */
    public static int GetMaxEncodingInt() {
        return Integer.MAX_VALUE;
    }
}
