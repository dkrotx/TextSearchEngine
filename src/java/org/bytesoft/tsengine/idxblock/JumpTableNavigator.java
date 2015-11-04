package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.VarByteDecoder;

import java.nio.ByteBuffer;

/**
 * Navigate over built jump table: use multiple levels of JT to lookup document id as fast as possible.
 */
public class JumpTableNavigator {

    final private class JumpTableReader {
        ByteBuffer area;
        static final int JUMP_TABLE_STARTS_WITH_BIGGER_VALUE = -1;

        final private class DocumentOffsetPair {
            int doc_id;
            int offset;

            DocumentOffsetPair() { invalidate(); }

            boolean valid() { return doc_id != -1; }

            void invalidate() {
                doc_id = -1;
                offset = -1;
            }

            void read(ByteBuffer buf) {
                if (buf.remaining() == 0)
                    invalidate();
                else {
                    doc_id = VarByteDecoder.ExtractNumberFromBuf(buf);
                    offset = VarByteDecoder.ExtractNumberFromBuf(buf);
                }
            }

            void assign(DocumentOffsetPair pair) {
                doc_id = pair.doc_id;
                offset = pair.offset;
            }
        }

        DocumentOffsetPair cur = new DocumentOffsetPair();
        DocumentOffsetPair next = new DocumentOffsetPair();

        JumpTableReader(ByteBuffer area) {
            this.area = area;
            position(0);
        }

        private void readNext() {
            cur.assign(next);
            next.read(area);
        }

        int findNearestOffset(int doc_id) {
            if (!cur.valid() || cur.doc_id > doc_id)
                return JUMP_TABLE_STARTS_WITH_BIGGER_VALUE;

            while(next.valid() && doc_id >= next.doc_id) {
                readNext();
            }

            return cur.offset;
        }

        void position(int offset) {
            area.position(offset);
            cur.read(area);
            next.read(area);
        }
    }

    JumpTableReader[] tables;

    void initializeLevel(ByteBuffer mem, int level) {
        int size = VarByteDecoder.ExtractNumberFromBuf(mem);

        ByteBuffer sub_mem = mem.slice();
        sub_mem.limit(size);

        tables[level] = new JumpTableReader(sub_mem);
        mem.position(mem.position() + size);
    }

    public JumpTableNavigator(ByteBuffer mem) {
        int nlevels = VarByteDecoder.ExtractNumberFromBuf(mem);
        tables = new JumpTableReader[nlevels];

        for (int i = 0; i < nlevels; i++) {
            initializeLevel(mem, i);
        }
    }

    public int getNumberOfLevels() {
        return tables.length;
    }

    public boolean empty() {
        return tables.length == 0;
    }

    /**
     * find neareast offset to given doc_id
     * @param doc_id document ID
     * @return nearest offset to doc_id or -1 if can't help
     */
    public int findNearestOffset(int doc_id) {
        if (empty())
            return -1;

        int level = tables.length - 1;
        int offset = tables[level].findNearestOffset(doc_id);

        for(--level; level >= 0; level--) {
            if (offset != JumpTableReader.JUMP_TABLE_STARTS_WITH_BIGGER_VALUE) {
                tables[level].position(offset);
            }
            offset = tables[level].findNearestOffset(doc_id);
        }

        return offset;
    }
}
