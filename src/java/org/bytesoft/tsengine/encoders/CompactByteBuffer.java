package org.bytesoft.tsengine.encoders;

import java.io.ByteArrayOutputStream;

/**
 * Compact byte buffer
 * Use memory exactly for storing needed bytes
 */
public class CompactByteBuffer {
    private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

    public void AddByte(int b) {
        byteStream.write(b);
    }

    public byte[] GetBytes() {
        return byteStream.toByteArray();
    }

    final public int GetSize() {
        return byteStream.size();
    }

}
