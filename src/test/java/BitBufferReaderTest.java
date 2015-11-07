import org.junit.Test;
import util.BitBufferReader;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Test BitBufferReader class
 */
public class BitBufferReaderTest {
    @Test
    public void SimpleTest() {
        BitBufferReader bitbuf = new BitBufferReader(ByteBuffer.wrap(new byte[]{0b00000001}));

        assertTrue(bitbuf.hasRemaining());

        assertTrue(bitbuf.readBit());
        assertTrue(bitbuf.hasRemaining());

        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());

        assertFalse(bitbuf.hasRemaining());
    }

    @Test
    public void TestEmpty() {
        BitBufferReader bitbuf = new BitBufferReader(ByteBuffer.wrap(new byte[0]));
        assertFalse(bitbuf.hasRemaining());
    }

    @Test
    public void TestBigNumbers() {
        byte[] original = new byte[]{ (byte)0xff, 0x55, 0x27, 0x10};
        byte[] extracted = new byte[original.length];

        BitBufferReader bitbuf = new BitBufferReader(ByteBuffer.wrap(original));

        for (int i = 0; i < original.length; i++) {
            int num = 0;
            for (int ibit = 0; ibit < 8; ibit++) {
                assertTrue(bitbuf.hasRemaining());
                if (bitbuf.readBit())
                    num |= 1 << ibit;
            }

            extracted[i] = (byte)num;
        }

        assertArrayEquals("Exctracted array should be the same", original, extracted);
        assertFalse(bitbuf.hasRemaining());
    }

    @Test
    public void TestAlign() {
        byte[] original = new byte[]{ (byte)0xff, 0x00, (byte)0xff};

        BitBufferReader bitbuf = new BitBufferReader(ByteBuffer.wrap(original));
        // 0xff
        assertTrue(bitbuf.readBit());
        assertTrue(bitbuf.readBit());

        assertTrue(bitbuf.hasRemaining());
        bitbuf.alignByte();
        assertTrue(bitbuf.hasRemaining());
        // 0x00
        assertFalse(bitbuf.readBit());
        assertFalse(bitbuf.readBit());

        assertTrue(bitbuf.hasRemaining());
        bitbuf.alignByte();
        assertTrue(bitbuf.hasRemaining());
        // 0xff
        assertTrue(bitbuf.readBit());
        assertTrue(bitbuf.readBit());

        assertTrue(bitbuf.hasRemaining());
        bitbuf.alignByte();
        assertFalse("butbuf should be finished by align()", bitbuf.hasRemaining());
    }
}
