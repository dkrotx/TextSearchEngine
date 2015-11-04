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
    private int continuous_elems = 0;
    private int continuous_size = 0;
    private JumpTableFillPolicy jt_policy = new JumpTableFillPolicy.Empty();

    private static final int MAX_HEADER_SIZE = 4;
    private JumpTableBuilder jump_table = new JumpTableBuilder(new JumpTableFillPolicy.Empty());

    public IdxBlockEncoder(IntCompressor encoder) {
        main_encoder = encoder;
    }

    /**
     * Add document to index block
     * @param id document ID
     * @throws IntCompressor.TooLargeToCompressException if id is too large or negative
     */
    public void AddDocID(int id) throws IntCompressor.TooLargeToCompressException {
        try {
            if (jt_policy.enough(continuous_elems, continuous_size)) {
                jump_table.addEntry(id, main_encoder.GetStoreSize());
                continuous_elems = 0;
                continuous_size = 0;
            }
        }
        catch(JumpTableBuilder.TooLargeBlockToJump e) {
            throw new IntCompressor.TooLargeToCompressException(e.getMessage());
        }

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
        continuous_elems++;
    }

    private int writeHeader(DataOutputStream wr) throws IOException {
        int size = 0;

        try {
            VarByteEncoder vb_enc = new VarByteEncoder();
            vb_enc.AddNumber(ndocs);

            wr.write(vb_enc.GetBytes());
            size += vb_enc.GetBytes().length;
        }
        catch(IntCompressor.TooLargeToCompressException e) {
            IllegalStateException exception = new IllegalStateException("ndocs should be representable in VarByte");
            exception.addSuppressed(e);
            throw exception;
        }

        size += jump_table.write(wr);

        return size;
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
