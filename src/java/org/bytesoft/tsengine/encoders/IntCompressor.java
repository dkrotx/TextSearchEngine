package org.bytesoft.tsengine.encoders;

/**
 * Integer compression interface
 */
public interface IntCompressor {
    void AddNumber(int x) throws TooLargeToCompressException;
    default void AddNumbers(int[] arr) throws TooLargeToCompressException {
        for (int x: arr)
            AddNumber(x);
    }

    byte[] GetBytes();
    int    GetStoreSize();
}
