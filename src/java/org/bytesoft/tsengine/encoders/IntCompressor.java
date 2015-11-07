package org.bytesoft.tsengine.encoders;

/**
 * Integer compression interface
 */
public interface IntCompressor {
    void AddNumber(int x) throws TooLargeToCompressException;
    byte[] GetBytes();

    int  size();
    default int GetStoreSize() {
        return size();
    }

    boolean CanEncodeZero();
    void flush();

    class TooLargeToCompressException extends Exception {
        public TooLargeToCompressException(String msg) {
            super(msg);
        }
    }
}
