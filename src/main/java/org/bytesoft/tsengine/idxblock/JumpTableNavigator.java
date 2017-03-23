package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.VarByteDecoder;

import java.nio.ByteBuffer;

/**
 * Navigate over built jump table: use multiple levels of JT to lookup document id as fast as possible.
 */
public class JumpTableNavigator {

    final private class JumpTableReader {
        ByteBuffer area;
        boolean is_direct;
        static final int JUMP_TABLE_STARTS_WITH_BIGGER_VALUE = -1;

        final private class DocumentRecord {
            int doc_id;
            int doc_no;
            int offset;

            DocumentRecord() { invalidate(); }

            boolean valid() { return doc_id != -1; }

            void invalidate() {
                doc_id = -1;
            }

            void assign(DocumentRecord pair) {
                doc_id = pair.doc_id;
                doc_no = pair.doc_no;
                offset = pair.offset;
            }
        }

        DocumentRecord cur = new DocumentRecord();
        DocumentRecord next = new DocumentRecord();

        JumpTableReader(ByteBuffer area, boolean is_direct) {
            this.area = area;
            this.is_direct = is_direct;
            position(0);
        }

        void readRecord(DocumentRecord rec) {
            if (area.remaining() == 0)
                rec.invalidate();
            else {
                rec.doc_id = VarByteDecoder.ExtractNumberFromBuf(area);
                if (is_direct)
                    rec.doc_no = VarByteDecoder.ExtractNumberFromBuf(area);
                rec.offset = VarByteDecoder.ExtractNumberFromBuf(area);
            }
        }

        private void readNext() {
            cur.assign(next);
            readRecord(next);
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
            readRecord(cur);
            readRecord(next);
        }

        int getCurrentJumpDocID() { return cur.doc_id; }
        int getCurrentJumpDocNo() { return cur.doc_no; }
    }

    JumpTableReader[] tables;

    void initializeLevel(ByteBuffer mem, int level) {
        int size = VarByteDecoder.ExtractNumberFromBuf(mem);

        ByteBuffer sub_mem = mem.slice();
        sub_mem.limit(size);

        tables[level] = new JumpTableReader(sub_mem, level == 0);
        mem.position(mem.position() + size);
    }

    public JumpTableNavigator(ByteBuffer mem) {
        int levels = VarByteDecoder.ExtractNumberFromBuf(mem);
        tables = new JumpTableReader[levels];

        for (int i = 0; i < levels; i++) {
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
     * find nearest record to given doc_id
     * @param req: .doc_id requested document ID
     * @return true if result can be used
     */
    public boolean findNearestOffset(JumpRequest req) {
        if (empty())
            return false;

        int level = tables.length - 1;
        int offset = tables[level].findNearestOffset(req.doc_id);

        for(--level; level >= 0; level--) {
            if (offset != JumpTableReader.JUMP_TABLE_STARTS_WITH_BIGGER_VALUE) {
                tables[level].position(offset);
            }
            offset = tables[level].findNearestOffset(req.doc_id);
        }

        if (offset == JumpTableReader.JUMP_TABLE_STARTS_WITH_BIGGER_VALUE)
            return false;

        JumpTableReader jt0 = tables[0];

        req.doc_id = jt0.getCurrentJumpDocID();
        req.offset = offset;
        req.doc_no = jt0.getCurrentJumpDocNo();
        return true;
    }

    public static class JumpRequest {
        public int doc_id;
        public int doc_no;
        public int offset;

        public JumpRequest() {}

        public JumpRequest(int document_id) {
            doc_id = document_id;
        }
    }
}
