package util.bytes;


public class Bytes {
    public static byte[] toBytes(long val) {
        byte[] out = new byte[8];

        for (int i = 7; i > 0; i--) {
            out[i] = (byte)val;
            val >>>= 8;
        }

        out[0] = (byte)val;
        return out;
    }

    public static byte[] toBytes(int val) {
        byte[] out = new byte[4];

        for (int i = 3; i > 0; i--) {
            out[i] = (byte)val;
            val >>>= 8;
        }

        out[0] = (byte)val;
        return out;
    }
}
