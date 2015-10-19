package org.bytesoft.tsengine.idxblock;

import java.nio.ByteBuffer;

/**
 * This class deals with set of index blocks and designed
 * to be top-level reader of index blocks.
 */
public class IdxBlocksIterator {
    private IdxBlockDecoder[] idxblocks;
    private int ndocs_total = 0;
    private int cur_block_id = 0;
    private int cur_doc_id = -1;

    public IdxBlocksIterator(ByteBuffer[] areas) {
        idxblocks = new IdxBlockDecoder[areas.length];

        for (int i = 0; i < areas.length; i++) {
            idxblocks[i] = new IdxBlockDecoder(areas[i]);
            ndocs_total += idxblocks[i].GetDocsAmount();
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
        if (!idxblocks[cur_block_id].HasNext()) {
            if (cur_block_id < idxblocks.length)
                cur_block_id++;
        }

        cur_doc_id = idxblocks[cur_block_id].ReadNext();
        return cur_doc_id;
    }

    /**
     * Check there is more documents in posting list
     * @return true if {@code ReadNext()} is available
     */
    public boolean HasNext() {
        return cur_block_id < idxblocks.length - 1 ||
                idxblocks[cur_block_id].HasNext();
    }

    /**
     * @return total amount of documents in all blocks
     */
    public int GetDocsAmount() {
        return ndocs_total;
    }
}
