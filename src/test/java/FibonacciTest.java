import org.bytesoft.tsengine.encoders.FibonacciDecoder;
import org.bytesoft.tsengine.encoders.FibonacciEncoder;
import org.bytesoft.tsengine.encoders.TooLargeToCompressException;

import org.junit.*;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test Fibonacci encoding/decoding
 */
public class FibonacciTest {
    byte[] encodeSingleNumber(int x) throws TooLargeToCompressException {
        FibonacciEncoder enc = new FibonacciEncoder();
        enc.AddNumber(x);
        return enc.GetBytes();
    }

    int decodeSingleNUmber(byte[] bytes) {
        FibonacciDecoder dec = new FibonacciDecoder(ByteBuffer.wrap(bytes));
        return dec.ExtractNumber();
    }

    // 1  2  3  5  8  13  21  34 |  55  89
    //                           |  1  (1)

    @Test
    public void TestEncoding() throws Exception {
        assertArrayEquals(new byte[]{(byte) 0b00000011}, encodeSingleNumber(1));
        assertArrayEquals(new byte[]{(byte) 0b11010001}, encodeSingleNumber(30));
        assertArrayEquals(new byte[]{(byte) 0b10100100, (byte) 0b00000001}, encodeSingleNumber(50));
        assertArrayEquals(new byte[]{(byte) 0b00000000, (byte) 0b00000011}, encodeSingleNumber(55));
    }

    @Test
    public void TestDecoding() throws Exception {
        assertEquals(1, decodeSingleNUmber(new byte[]{(byte) 0b00000011}));
        assertEquals(30, decodeSingleNUmber(new byte[]{(byte) 0b11010001}));
        assertEquals(50, decodeSingleNUmber(new byte[]{(byte) 0b10100100, (byte) 0b00000001}));
        assertEquals(55, decodeSingleNUmber(new byte[]{(byte) 0b00000000, (byte) 0b00000011}));
    }

    @Test
    public void TestBoundary()
    {
        boolean caught = false;

        try {
            caught = false;
            FibonacciEncoder enc = new FibonacciEncoder();
            enc.AddNumber(0);
        }
        catch (TooLargeToCompressException e) {
            caught = true;
        }
        finally {
            assertTrue("Zeroes should not work in fibonacci", caught);
        }


        try {
            caught = false;
            FibonacciEncoder enc = new FibonacciEncoder();
            enc.AddNumber(-1);
        }
        catch (TooLargeToCompressException e) {
            caught = true;
        }
        finally {
            assertTrue("Negatives should not work in fibonacci", caught);
        }

        try {
            FibonacciEncoder enc = new FibonacciEncoder();
            enc.AddNumber(Integer.MAX_VALUE);
        }
        catch (TooLargeToCompressException e) {
            fail("max integer should be OK for fibonacci");
        }
    }

    @Test
    public void TestEncodingAndDecoding() throws TooLargeToCompressException {
        // test what arbitrary encoded data can be decoded
        int[] numbers = new int[1000];
        Random rand = new Random();
        final int NUM_LIMIT = FibonacciEncoder.GetMaxEncodingInt();

        for (int i = 0; i < numbers.length; i++)
            numbers[i] = rand.nextInt(NUM_LIMIT);

        FibonacciEncoder enc = new FibonacciEncoder();
        for(int x: numbers) {
            enc.AddNumber(x);
        }

        FibonacciDecoder dec = new FibonacciDecoder( ByteBuffer.wrap(enc.GetBytes()) );
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
            ByteBuffer buf = ByteBuffer.wrap( new byte[] { 1 } ); // has to be terminated with '11'
            FibonacciDecoder dec = new FibonacciDecoder(buf);
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
