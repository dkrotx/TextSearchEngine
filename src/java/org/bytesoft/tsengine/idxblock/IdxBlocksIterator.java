package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.EncodersFactory;

import java.nio.ByteBuffer;

/**
 * This class deals with set of index blocks and designed
 * to be top-level reader of index blocks.
 */
public class IdxBlocksIterator {
    private IdxBlockDecoder[] blocks;
    private int docs_total = 0;
    private int cur_block_id = 0;
    private int cur_doc_id = ALPHA_ID;

    /**
     * starting document ID (before first {@code ReadNext()})
     */
    static public final int ALPHA_ID = -1;

    /**
     * finishing document ID ({@code HasNext() == false && ReadNext() == OMEGA_ID})
     */
    static public final int OMEGA_ID = -2;

    /**
     * Create block iterator from given ByteBuffer areas with jump tables
     * @param areas distinct index blocks to decode
     * @param decoder_factory decoder maker
     * @param jt_cfg configuration of Jump Tables
     */
    public IdxBlocksIterator(ByteBuffer[] areas, EncodersFactory decoder_factory, JumpTableConfig jt_cfg) {
        blocks = new IdxBlockDecoder[areas.length];

        for (int i = 0; i < areas.length; i++) {
            blocks[i] = new IdxBlockDecoder(areas[i], decoder_factory, jt_cfg);
            docs_total += blocks[i].GetDocsAmount();
        }
    }

    /**
     * Create block iterator from given ByteBuffer areas without jump tables
     * @param areas distinct index blocks to decode
     * @param decoder_factory decoder maker
     */
    public IdxBlocksIterator(ByteBuffer[] areas, EncodersFactory decoder_factory) {
        this(areas, decoder_factory, JumpTableConfig.makeEmptyJumpTable());
    }

    /**
     * Get last retrieved document ID without changing buffer-position
     * @return document ID or -1 if no documents was retrieved
     */
    public int GetCurrentDocID() {
        return cur_doc_id;
    }

    /**
     * Extract next docid in posting list
     * @return docid (you may cache it, or use {@code GetCurrentDocID()}
     */
    public int ReadNext() {
        if (cur_doc_id == OMEGA_ID)
            return OMEGA_ID;

        if (!blocks[cur_block_id].HasNext()) {
            if (!select_next_block())
                return (cur_doc_id = OMEGA_ID);
        }

        cur_doc_id = blocks[cur_block_id].ReadNext();
        return cur_doc_id;
    }

    private boolean select_next_block() {
        if (cur_block_id+1 < blocks.length) {
            cur_block_id++;
            return true;
        }
        return false;
    }

    /**
     * Set position to documentID >= given id
     * @warning GotoDocID can only move forward (growing doc IDs)
     *
     * @param id upper bound of document to seek
     * @return current document id, which can be:
     *      OMEGA_ID if no documents >= id
     *      == id if document {@code #id} is found
     *      >  id if id itself not found, but there are documents with greater id
     */
    public int GotoDocID(int id) {
        if (cur_doc_id == OMEGA_ID) {
            return OMEGA_ID;
        }
        if (id == OMEGA_ID) {
            return (cur_doc_id = OMEGA_ID);
        }

        do {
            int found_id = blocks[cur_block_id].GotoDocID(id);
            if (found_id != -1) {
                return (cur_doc_id = found_id);
            }
        } while(select_next_block());

        return (cur_doc_id = OMEGA_ID);
    }

    /**
     * Check there is more documents in posting list
     * @return true if {@code ReadNext()} is available
     */
    public boolean HasNext() {
        return cur_doc_id != OMEGA_ID && (cur_block_id +1 < blocks.length || blocks[cur_block_id].HasNext());
    }

    /**
     * @return total amount of documents in all blocks
     */
    public int GetDocsAmount() {
        return docs_total;
    }
}
