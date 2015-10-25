import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.encoders.VarByteEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Helper static methods for pretty and compact unit tests
 */
public class IdxBlockUtils {
    static public ByteBuffer makeIndexBlock(int[] postings) throws IntCompressor.TooLargeToCompressException, IOException {
        IdxBlockEncoder enc = new IdxBlockEncoder(new VarByteEncoder());

        for(int id: postings) {
            enc.AddDocID(id);
        }

        ByteArrayOutputStream membuf = new ByteArrayOutputStream();
        enc.Write(new DataOutputStream(membuf));

        return ByteBuffer.wrap(membuf.toByteArray());
    }

    static public ByteBuffer makeIndexBlockV(int ... posting) throws IntCompressor.TooLargeToCompressException, IOException {
        return makeIndexBlock(posting);
    }
}
