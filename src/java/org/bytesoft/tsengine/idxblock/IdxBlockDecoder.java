package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.DeltaIntDecoder;
import org.bytesoft.tsengine.encoders.VarByteDecoder;

import java.nio.ByteBuffer;

/**
 * This class designed to read content of index block and iterate through it.
 * It reads header, and provides access to (compressed) posting lists.
 */
public class IdxBlockDecoder {
    private ByteBuffer block_buf;
    private DeltaIntDecoder docid_decoder;

    private int ndocs = -1;
    private int cur_docs_offset = 0;
    private int cur_docid = -1;

    public IdxBlockDecoder(ByteBuffer mem) {
        block_buf = mem;
        readHeader();

        // TODO: select decoder from header
        docid_decoder = new DeltaIntDecoder(new VarByteDecoder(mem.slice()));
    }

    /**
     * Get last retrieved document ID without changing buffer-position
     * @return document ID or -1 if no documents was retrieved
     */
    public int GetCurrentDocID() {
        return cur_docid;
    }

    /**
     * Extract next docid in posting list
     * @return docid (you may cache it, or use {@code GetCurrentDocID()}
     */
    public int ReadNext() {
        cur_docid = docid_decoder.ExtractNumber();
        cur_docs_offset++;
        return cur_docid;
    }

    /**
     * Check there is more documents in posting list
     */
    public boolean HasNext() {
        return cur_docs_offset < ndocs;
    }

    /**
     * Set position to documentID >= given id
     * @warning GotoDocID can only move forward (growing doc IDs)
     *
     * @param id upper bound of document to seek
     * @return current document id, which can be:
     *      -1 if no documents >= id
     *      == id if document {@code #id} is found
     *      >  id if id itself not found, but there are documents with greater id
     */
    public int GotoDocID(int id) {
        while (GetCurrentDocID() < id) {
            if (!HasNext())
                return -1;
            ReadNext();
        }

        return GetCurrentDocID();
    }

    /**
     * @return amount of documents decoded from header
     */
    public int GetDocsAmount() {
        return ndocs;
    }

    private void readHeader() {
        ndocs = VarByteDecoder.ExtractNumberFromBuf(block_buf);
    }
}
