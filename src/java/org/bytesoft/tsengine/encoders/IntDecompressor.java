package org.bytesoft.tsengine.encoders;

/**
 * IntDecompressor - interface for interaction with
 * compressed ints.
 */
public interface IntDecompressor {
    int ExtractNumber();
    void Position(int new_position);
    boolean CanDecodeZero();
}
