package org.bytesoft.tsengine.encoders;

public class TooLargeToCompressException extends Exception {
    TooLargeToCompressException(String msg) {
        super(msg);
    }
}