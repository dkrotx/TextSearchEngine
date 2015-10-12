import org.bytesoft.tsengine.encoders.*;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Test all integer encoding method
 */
public class EncodersTest {

    byte[] encodeSingleNumber(int x, IntCompressor enc) throws TooLargeToCompressException {
        enc.AddNumber(x);
        return enc.GetBytes();
    }

    @Test
    public void TestVarByteEncoding() throws TooLargeToCompressException {
        assertArrayEquals(new byte[]{1}, encodeSingleNumber(1, new VarByteEncoder()));
        assertArrayEquals(new byte[]{127}, encodeSingleNumber(127, new VarByteEncoder()));
        assertArrayEquals(new byte[]{(byte)0b10000001, 0b00000000}, encodeSingleNumber(128, new VarByteEncoder()));
        assertArrayEquals(new byte[]{(byte) 0b10001001, (byte) 0b10111001, (byte) 0b11101011, (byte) 0b00011111},
                encodeSingleNumber(19821983, new VarByteEncoder()));
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
}
