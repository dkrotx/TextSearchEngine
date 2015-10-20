package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.DeltaIntEncoder;
import org.bytesoft.tsengine.encoders.TooLargeToCompressException;
import org.bytesoft.tsengine.encoders.VarByteEncoder;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * This class implements index-block encoding using
 * integer encoder for posting list, and helper things like header.
 */
public class IdxBlockEncoder {
    private int ndocs = 0;
    private DeltaIntEncoder main_encoder;

    private static final int MAX_HEADER_SIZE = 4;

    public IdxBlockEncoder(DeltaIntEncoder encoder) {
        main_encoder = encoder;
    }

    public void AddDocID(int id) throws TooLargeToCompressException {
        main_encoder.AddNumber(id);
        ndocs++;
    }

    private int writeHeader(DataOutputStream wr) throws IOException {
        try {
            VarByteEncoder vb_enc = new VarByteEncoder();
            vb_enc.AddNumber(ndocs);

            wr.write(vb_enc.GetBytes());
            return vb_enc.GetBytes().length;
        }
        catch(TooLargeToCompressException e) {
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
