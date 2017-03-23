package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.encoders.IntDecompressor;
import org.bytesoft.tsengine.encoders.VarByteDecoder;

import java.nio.ByteBuffer;

/**
 * This class designed to read content of index block and iterate through it.
 * It reads header, and provides access to (compressed) posting lists.
 */
public class IdxBlockDecoder {
    private ByteBuffer block_buf;
    private IntDecompressor decoder;

    private int ndocs = -1;
    private int cur_docs_offset = 0;
    private int cur_docid = -1;
    private int docs_in_page;
    private int cur_page_rest;
    private JumpTableNavigator jump_table;

    private static final ThreadLocal<JumpTableNavigator.JumpRequest> jt_request =
            new ThreadLocal<JumpTableNavigator.JumpRequest>() {
                @Override protected JumpTableNavigator.JumpRequest initialValue() {
                    return new JumpTableNavigator.JumpRequest();
                }
            };

    public IdxBlockDecoder(ByteBuffer mem, EncodersFactory decoder_factory) {
        this(mem, decoder_factory, JumpTableConfig.makeEmptyJumpTable());
    }

    private void resetPage() {
        cur_page_rest = docs_in_page;
    }

    public IdxBlockDecoder(ByteBuffer mem, EncodersFactory decoder_factory, JumpTableConfig jt_cfg) {
        block_buf = mem;
        readHeader();
        docs_in_page = jt_cfg.rindex_step;
        resetPage();
        decoder = decoder_factory.MakeDecoder(block_buf.slice());
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
        if (cur_docid == -1)
            cur_docid = decoder.ExtractNumber() - 1;
        else {
            if (cur_page_rest == 0) {
                decoder.align();
                resetPage();
            }

            int delta = decoder.ExtractNumber();
            if (decoder.CanDecodeZero())
                delta++;

            cur_docid += delta;
        }

        cur_docs_offset++;
        cur_page_rest--;
        return cur_docid;
    }

    /**
     * Check there is more documents in posting list
     */
    public boolean HasNext() {
        return cur_docs_offset < ndocs;
    }


    private void jump(int id) {
        JumpTableNavigator.JumpRequest req = jt_request.get();
        req.doc_id = id;

        if (jump_table.findNearestOffset(req)) {
            cur_docs_offset = req.doc_no;
            decoder.position(req.offset);
            resetPage();

            // we should skip this document since this is not delta, but absolute value
            decoder.ExtractNumber();
            cur_docid = req.doc_id;
            cur_docs_offset++;
            cur_page_rest--;
        }
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
        if (HasNext() && id - GetCurrentDocID() > (long)docs_in_page*2) {
            jump(id);
        }

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
        jump_table = new JumpTableNavigator(block_buf);
    }
}
