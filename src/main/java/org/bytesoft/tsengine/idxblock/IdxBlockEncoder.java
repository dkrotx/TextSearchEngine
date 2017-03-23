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
    private JumpTableConfig jump_table_cfg;
    private JumpTableBuilder jump_table;


    public IdxBlockEncoder(IntCompressor encoder) {
        this(encoder, JumpTableConfig.makeEmptyJumpTable());
    }

    public IdxBlockEncoder(IntCompressor encoder, JumpTableConfig jt_config) {
        main_encoder = encoder;
        jump_table_cfg = jt_config;
        jump_table = new JumpTableBuilder(jump_table_cfg);
    }

    /**
     * Add document to index block
     * @param id document ID
     * @throws IntCompressor.TooLargeToCompressException if id is too large or negative
     */
    public void AddDocID(int id) throws IntCompressor.TooLargeToCompressException {
        try {
            if (ndocs != 0 && (ndocs % jump_table_cfg.rindex_step) == 0) {
                main_encoder.flush();
                jump_table.addEntry(id, ndocs, main_encoder.size());
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
        return main_encoder.GetStoreSize() + 16 /* average overhead */;
    }
}
