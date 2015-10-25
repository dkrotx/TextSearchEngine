package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.encoders.VarByteEncoder;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * This class implements index-block encoding using
 * integer encoder for posting list, and helper things like header.
 */
public class IdxBlockEncoder {
    private int prev_doc_id = -1;
    private int ndocs = 0;
    private IntCompressor main_encoder;

    private static final int MAX_HEADER_SIZE = 4;

    public IdxBlockEncoder(IntCompressor encoder) {
        main_encoder = encoder;
    }

    /**
     * Add document to index block
     * @param id document ID
     * @throws IntCompressor.TooLargeToCompressException if id is too large or negative
     */
    public void AddDocID(int id) throws IntCompressor.TooLargeToCompressException {
        if (ndocs == 0) {
            // Since not all encoders may encode zero, it's better to
            // encode +1 to first document
            main_encoder.AddNumber(id + 1);
        }
        else {
            int delta = id - prev_doc_id;

            assert(delta >= 1);

            // Some encoders may encode zeroes. So, to save this code-position (and index space)
            // use delta-1 to encode (delta is always >= 1)
            if (main_encoder.CanEncodeZero())
                main_encoder.AddNumber(delta - 1);
            else
                main_encoder.AddNumber(delta);
        }

        prev_doc_id = id;
        ndocs++;
    }

    private int writeHeader(DataOutputStream wr) throws IOException {
        try {
            VarByteEncoder vb_enc = new VarByteEncoder();
            vb_enc.AddNumber(ndocs);

            wr.write(vb_enc.GetBytes());
            return vb_enc.GetBytes().length;
        }
        catch(IntCompressor.TooLargeToCompressException e) {
            IllegalStateException exception = new IllegalStateException("ndocs should be representable in VarByte");
            exception.addSuppressed(e);
            throw exception;
        }
    }

    public long Write(DataOutputStream wr) throws IOException {
        long nbytes = writeHeader(wr);
        byte[] body = main_encoder.GetBytes();

        wr.write(body);
        return nbytes + body.length;
    }

    public long GetStoreSize() {
        return main_encoder.GetStoreSize() + MAX_HEADER_SIZE;
    }
}
