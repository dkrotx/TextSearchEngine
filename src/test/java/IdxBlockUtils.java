import org.bytesoft.tsengine.encoders.EliasGammaEncoder;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.idxblock.IdxBlockDecoder;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;
import org.bytesoft.tsengine.idxblock.JumpTableConfig;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Helper static methods for pretty and compact unit tests
 */
public class IdxBlockUtils {
    static public ByteBuffer makeIndexBlock(int[] postings) throws IntCompressor.TooLargeToCompressException, IOException {
        IdxBlockEncoder enc = new IdxBlockEncoder(new EliasGammaEncoder());

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

    static public ByteBuffer writeBlockToMemory(IdxBlockEncoder enc) {
        try {
            ByteArrayOutputStream membuf = new ByteArrayOutputStream();
            enc.Write(new DataOutputStream(membuf));
            return ByteBuffer.wrap(membuf.toByteArray());
        }
        catch(IOException e) {
            throw new OutOfMemoryError("not enough memory to write index block");
        }
    }

    static public IdxBlockDecoder getIndexBlockDecoder(IdxBlockEncoder enc,
                                                       EncodersFactory dec_factory, JumpTableConfig jt_cfg)
    {
        return new IdxBlockDecoder(writeBlockToMemory(enc), dec_factory, jt_cfg);
    }
}
