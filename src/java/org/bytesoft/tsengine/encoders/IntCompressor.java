package org.bytesoft.tsengine.encoders;

/**
 * Integer compression interface
 */
public interface IntCompressor {
    void AddNumber(int x) throws TooLargeToCompressException;
    byte[] GetBytes();
    int    GetStoreSize();

    boolean CanEncodeZero();

    class TooLargeToCompressException extends Exception {
        TooLargeToCompressException(String msg) {
            super(msg);
        }
    }
}
