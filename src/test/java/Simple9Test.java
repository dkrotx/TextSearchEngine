import org.bytesoft.tsengine.encoders.*;
import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test Simple9 encoding/decoding
 */
public class Simple9Test {
    byte[] encodeSingleNumber(int x) throws TooLargeToCompressException {
        Simple9Encoder enc = new Simple9Encoder();
        enc.AddNumber(x);
        return enc.GetBytes();
    }

    byte[] encodeNumbers(int ... number) throws TooLargeToCompressException {
        Simple9Encoder enc = new Simple9Encoder();

        for (int x: number)
            enc.AddNumber(x);

        return enc.GetBytes();
    }

    int[] extractNumbers(int n, byte[] bytes) {
        Simple9Decoder dec = new Simple9Decoder(ByteBuffer.wrap(bytes));
        int[] res = new int[n];

        for (int i = 0; i < n; i++)
            res[i] = dec.ExtractNumber();

        return res;
    }


    byte[] makeByteArray(int ... val) {
        byte[] bytes = new byte[val.length];

        for (int i = 0; i < val.length; i++)
            bytes[i] = (byte)val[i];

        return bytes;
    }

    @Test
    public void TestEncoding() throws Exception {
        assertArrayEquals(makeByteArray(0x80, 0, 0, 0), encodeSingleNumber(0));

        assertArrayEquals(makeByteArray(0x80, 0, 0, 1), encodeSingleNumber(1));
        assertArrayEquals(makeByteArray(0x80, 0, 0, 5), encodeSingleNumber(5));
        assertArrayEquals(makeByteArray(0x80, 0, 0, 15), encodeSingleNumber(15));
        assertArrayEquals(makeByteArray(0x80, 0, 0, 30), encodeSingleNumber(30));
        assertArrayEquals(makeByteArray(0x80, 0, 0, 155), encodeSingleNumber(155));
        assertArrayEquals(makeByteArray(0x80, 0, 1, 54), encodeSingleNumber(310));
    }

    @Test
    public void TestPackEncoding() throws Exception {
        // encoding a single number isn't so interesting in Simple9
        // so test with several numbers

        assertArrayEquals(makeByteArray(0x70, 0, 0b01000000, 0b00000001), encodeNumbers(1, 1));
        assertArrayEquals(makeByteArray(0b0011_0001, 0b00010001, 0b00010001, 0b00010001), encodeNumbers(1, 1, 1, 1, 1, 1, 1));

        assertArrayEquals(makeByteArray(0b0011_0001, 0b00010001, 0b00100001, 0b00010001), encodeNumbers(1, 1, 1, 2, 1, 1, 1));
        assertArrayEquals(makeByteArray(0b0011_0001, 0b01010001, 0b00100001, 0b00010001), encodeNumbers(1, 1, 1, 2, 1, 5, 1));

        assertArrayEquals(makeByteArray(0b0011_0001, 0b01010001, 0b00100001, 0b10000001), encodeNumbers(1, 8, 1, 2, 1, 5, 1));
    }

    @Test
    public void TestDecoding() throws Exception {
        assertArrayEquals(new int[]{1}, extractNumbers(1, makeByteArray(0x80, 0, 0b00000000, 0b00000001)));
        assertArrayEquals(new int[]{1, 1}, extractNumbers(2, makeByteArray(0x70, 0, 0b01000000, 0b00000001)));
        assertArrayEquals(new int[] {1, 1, 1, 1, 1, 1, 1}, extractNumbers(7, makeByteArray(0b0011_0001, 0b00010001, 0b00010001, 0b00010001)));
        assertArrayEquals(new int[] {1, 1, 1, 2, 1, 1, 1}, extractNumbers(7, makeByteArray(0b0011_0001, 0b00010001, 0b00100001, 0b00010001)));
        assertArrayEquals(new int[] {1, 1, 1, 2, 1, 5, 1}, extractNumbers(7, makeByteArray(0b0011_0001, 0b01010001, 0b00100001, 0b00010001)));
        assertArrayEquals(new int[] {1, 8, 1, 2, 1, 5, 1}, extractNumbers(7, makeByteArray(0b0011_0001, 0b01010001, 0b00100001, 0b10000001)));

    }

    @Test
    public void TestBoundary()
    {
        boolean caught = false;

        try {
            caught = false;
            Simple9Encoder enc = new Simple9Encoder();
            enc.AddNumber(0x40000000);
        }
        catch (TooLargeToCompressException e) {
            caught = true;
        }
        finally {
            assertTrue("Too big for simple9", caught);
        }


        try {
            caught = false;
            Simple9Encoder enc = new Simple9Encoder();
            enc.AddNumber(-1);
        }
        catch (TooLargeToCompressException e) {
            caught = true;
        }
        finally {
            assertTrue("Negatives should not work in simple9", caught);
        }

        try {
            Simple9Encoder enc = new Simple9Encoder();
            enc.AddNumber(0);
        }
        catch (TooLargeToCompressException e) {
            fail("0 should be OK for simple9");
        }
    }

    @Test
    public void TestEncodingAndDecoding() throws TooLargeToCompressException {
        // test what arbitrary encoded data can be decoded
        int[] numbers = new int[1000];
        Random rand = new Random();
        final int NUM_LIMIT = Simple9Encoder.GetMaxEncodingInt();

        for (int i = 0; i < numbers.length; i++)
            numbers[i] = rand.nextInt(NUM_LIMIT);

        Simple9Encoder enc = new Simple9Encoder();
        for(int x: numbers) {
            enc.AddNumber(x);
        }

        Simple9Decoder dec = new Simple9Decoder( ByteBuffer.wrap(enc.GetBytes()) );
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
            ByteBuffer buf = ByteBuffer.wrap( new byte[] { 0 } ); // there should be 'long'-aligned buffer
            Simple9Decoder dec = new Simple9Decoder(buf);
            dec.ExtractNumber();
        }
        catch(BufferUnderflowException e) {
            caught = true;
        }
        finally {
            assertTrue("BufferUnderflowException should be caught!", caught);
        }
    }
}
