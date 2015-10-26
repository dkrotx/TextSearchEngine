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
     * Create block iterator from given ByteBuffer areas.
     * @param areas distinct index blocks to decode
     */
    public IdxBlocksIterator(ByteBuffer[] areas, EncodersFactory decoder_factory) {
        blocks = new IdxBlockDecoder[areas.length];

        for (int i = 0; i < areas.length; i++) {
            blocks[i] = new IdxBlockDecoder(areas[i], decoder_factory);
            docs_total += blocks[i].GetDocsAmount();
        }
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
            if (cur_block_id + 1 < blocks.length)
                cur_block_id++;
            else {
                return (cur_doc_id = OMEGA_ID);
            }
        }

        cur_doc_id = blocks[cur_block_id].ReadNext();
        return cur_doc_id;
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
        if (id == OMEGA_ID) {
            cur_doc_id = OMEGA_ID;
        }
        else {
            while (GetCurrentDocID() < id && ReadNext() != OMEGA_ID) {
            }
        }

        return GetCurrentDocID();
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
