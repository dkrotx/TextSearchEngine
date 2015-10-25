import org.bytesoft.tsengine.encoders.*;
import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test Elias-Gamma encoding/decoding
 */
public class EliasGammaTest {
    byte[] encodeSingleNumber(int x) throws IntCompressor.TooLargeToCompressException {
        EliasGammaEncoder enc = new EliasGammaEncoder();
        enc.AddNumber(x);
        return enc.GetBytes();
    }

    int decodeSingleNUmber(byte[] bytes) {
        EliasGammaDecoder dec = new EliasGammaDecoder(ByteBuffer.wrap(bytes));
        return dec.ExtractNumber();
    }


    @Test
    public void TestEncoding() throws Exception {
        assertArrayEquals(new byte[]{(byte) 0b00000001}, encodeSingleNumber(1));
        assertArrayEquals(new byte[]{(byte) 0b000101_00}, encodeSingleNumber(5));
        assertArrayEquals(new byte[]{(byte) 0b01111_000}, encodeSingleNumber(15));
        assertArrayEquals(new byte[]{(byte) 0b1111_0000, (byte)0b00000000}, encodeSingleNumber(30));
        assertArrayEquals(new byte[]{(byte) 0b011_00000, 0b00000111}, encodeSingleNumber(55));
    }

    @Test
    public void TestDecoding() throws Exception {
        assertEquals(1, decodeSingleNUmber(new byte[]{(byte) 0b00000001}));
        assertEquals(5, decodeSingleNUmber(new byte[]{(byte) 0b000101_00}));
        assertEquals(15, decodeSingleNUmber(new byte[]{(byte) 0b01111_000}));
        assertEquals(30, decodeSingleNUmber(new byte[]{(byte) 0b1111_0000, (byte) 0b00000000}));
        assertEquals(55, decodeSingleNUmber(new byte[]{(byte) 0b011_00000, (byte) 0b00000111}));
    }

    @Test
    public void TestBoundary()
    {
        boolean caught = false;

        try {
            caught = false;
            EliasGammaEncoder enc = new EliasGammaEncoder();
            enc.AddNumber(0);
        }
        catch (IntCompressor.TooLargeToCompressException e) {
            caught = true;
        }
        finally {
            assertTrue("Zeroes should not work in Elias gamma", caught);
        }


        try {
            caught = false;
            EliasGammaEncoder enc = new EliasGammaEncoder();
            enc.AddNumber(-1);
        }
        catch (IntCompressor.TooLargeToCompressException e) {
            caught = true;
        }
        finally {
            assertTrue("Negatives should not work in Elias gamma", caught);
        }

        try {
            EliasGammaEncoder enc = new EliasGammaEncoder();
            enc.AddNumber(Integer.MAX_VALUE);
        }
        catch (IntCompressor.TooLargeToCompressException e) {
            fail("max integer should be OK for fibonacci");
        }
    }

    @Test
    public void TestEncodingAndDecoding() throws IntCompressor.TooLargeToCompressException {
        // test what arbitrary encoded data can be decoded
        int[] numbers = new int[1000];
        Random rand = new Random();
        final int NUM_LIMIT = EliasGammaEncoder.GetMaxEncodingInt();

        for (int i = 0; i < numbers.length; i++)
            numbers[i] = rand.nextInt(NUM_LIMIT);

        EliasGammaEncoder enc = new EliasGammaEncoder();
        for(int x: numbers) {
            enc.AddNumber(x);
        }

        EliasGammaDecoder dec = new EliasGammaDecoder( ByteBuffer.wrap(enc.GetBytes()) );
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
            ByteBuffer buf = ByteBuffer.wrap( new byte[] { 0 } ); // elias expect '1' as delimiter
            EliasGammaDecoder dec = new EliasGammaDecoder(buf);
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
