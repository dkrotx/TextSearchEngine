import org.bytesoft.tsengine.encoders.TooLargeToCompressException;
import org.bytesoft.tsengine.encoders.VarByteDecoder;
import org.bytesoft.tsengine.encoders.VarByteEncoder;
import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test all integer encoding method
 */
public class VarByteTest {

    byte[] encodeSingleNumber(int x) throws TooLargeToCompressException {
        VarByteEncoder enc = new VarByteEncoder();
        enc.AddNumber(x);
        return enc.GetBytes();
    }

    private VarByteDecoder getDecoder(int[] vals) {
        ByteBuffer buf = ByteBuffer.allocate(vals.length);
        for (int i: vals) {
            buf.put((byte)(i & 0xff));
        }

        buf.rewind();

        return new VarByteDecoder(buf);
    }

    private VarByteDecoder getDecoder(byte[] vals) {
        return new VarByteDecoder(ByteBuffer.wrap(vals));
    }

    @Test
    public void TestVarByteEncoding() throws TooLargeToCompressException {
        assertArrayEquals(new byte[]{1}, encodeSingleNumber(1));
        assertArrayEquals(new byte[]{127}, encodeSingleNumber(127));
        assertArrayEquals(new byte[]{(byte)0b10000001, 0b00000000}, encodeSingleNumber(128));
        assertArrayEquals(new byte[]{(byte) 0b10001001, (byte) 0b10111001, (byte) 0b11101011, (byte) 0b00011111},
                encodeSingleNumber(19821983));
    }

    @Test
    public void TestVarByteBoundary() {
        try {
            VarByteEncoder enc = new VarByteEncoder();
            enc.AddNumber(0);
        }
        catch (TooLargeToCompressException e) {
            fail("0 should be OK for varbyte");
        }

        try {
            VarByteEncoder enc = new VarByteEncoder();
            enc.AddNumber(1<<28 - 1);
        }
        catch (TooLargeToCompressException e) {
            fail("268435455 is OK for varbyte");
        }

        try {
            VarByteEncoder enc = new VarByteEncoder();
            enc.AddNumber(-17);
            fail("Negative numbers should not work in varbyte");
        }
        catch (TooLargeToCompressException e) {
        }

        try {
            VarByteEncoder enc = new VarByteEncoder();
            enc.AddNumber(1<<28);
            fail("268435456+ should not work in varbyte");
        }
        catch (TooLargeToCompressException e) {
        }
    }

    @Test
    public void TestSingleNumberDecoding() {
        assertEquals(0, getDecoder(new int[]{ 0 }).ExtractNumber());
        assertEquals(1, getDecoder(new int[]{ 1 }).ExtractNumber());
        assertEquals(127, getDecoder(new int[]{ 127 }).ExtractNumber());

        assertEquals(128, getDecoder(new int[]{ 0b10000001, 0b00000000 }).ExtractNumber());
        assertEquals(19821983,
                getDecoder(new int[]{ 0b10001001, 0b10111001, 0b11101011, 0b00011111 }).ExtractNumber());
    }

    @Test
    public void TestMultipleNumberDecoding() {
        VarByteDecoder dec = getDecoder( new int[] {
                        0b10000001, 0b00000000, /* 128 */
                        0b01111111, /* 127 */
                        0b10001001, 0b10111001, 0b11101011, 0b00011111, /* 19821983 */
                        0b00000000, /* 0 */
                        0b00000111, /* 7 */
                }
        );

        assertEquals(128, dec.ExtractNumber());
        assertEquals(127, dec.ExtractNumber());
        assertEquals(19821983, dec.ExtractNumber());
        assertEquals(0, dec.ExtractNumber());
        assertEquals(7, dec.ExtractNumber());
    }

    @Test
    public void TestEncodingAndDecoding() throws TooLargeToCompressException {
        // test what arbitrary encoded data can be decoded
        int[] numbers = new int[1000];
        Random rand = new Random();
        final int NUM_LIMIT = VarByteEncoder.GetMaxEncodingInt();

        for (int i = 0; i < numbers.length; i++)
            numbers[i] = rand.nextInt(NUM_LIMIT);

        VarByteEncoder enc = new VarByteEncoder();
        for(int x: numbers) {
            enc.AddNumber(x);
        }

        VarByteDecoder dec = getDecoder(enc.GetBytes());
        int[] decoded = new int[numbers.length];

        for(int i = 0; i < numbers.length; i++)
            decoded[i] = dec.ExtractNumber();

        assertArrayEquals("Extracted array should be equal to original (random) one",
                numbers,
                decoded);
    }

    @Test
    public void TestException() {
        boolean caught = false;

        try {
            VarByteDecoder dec = getDecoder(new int[]{0x80, 0x80});
            dec.ExtractNumber();
        }
        catch(BufferUnderflowException e) {
            caught = true;
        }
        finally {
            if (!caught)
                fail("BufferUnderflowException didn't caught!");
        }
    }
}
