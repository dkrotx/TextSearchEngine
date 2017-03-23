package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.encoders.VarByteEncoder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Build one or more jump table levels to quickly navigate in posting-list.
 * Final jump table structure is hierarchy of small jump tables. One above previous.
 */
public class JumpTableBuilder {
    ArrayList<JumpTableWriter> tables = new ArrayList<>();
    JumpTableConfig table_config;
    int entries_cnt = 0;

    int doc_id;
    int doc_no;

    public JumpTableBuilder(JumpTableConfig jt_cfg) {
        table_config = jt_cfg;
    }

    JumpTableWriter getLevelTable(int level) {
        if (level >= tables.size()) {
            tables.add(new JumpTableWriter());
            assert (level + 1 == tables.size());
        }

        return tables.get(level);
    }

    int getLevelStep(int level) {
        int step = table_config.jump_table_step;
        while(level-- > 0)
            step *= table_config.jump_table_step;

        return step;
    }

    // add entry to given level
    // recursive: if needed, add entry to next (upper) level
    void addIndirectReference(int offset, int level) throws TooLargeBlockToJump {
        JumpTableWriter writer = getLevelTable(level);

        if (entries_cnt != 0 && (entries_cnt % getLevelStep(level)) == 0) {
            // add indirect reference
            addIndirectReference(writer.size(), level + 1);
        }

        try {
            writer.addIndirect(doc_id, offset);
        } catch (IntCompressor.TooLargeToCompressException e) {
            throw new TooLargeBlockToJump(e.getMessage());
        }
    }

    void addDirectEntry(int offset) throws TooLargeBlockToJump {
        JumpTableWriter writer = getLevelTable(0);

        if (entries_cnt != 0 && (entries_cnt % table_config.jump_table_step) == 0) {
            addIndirectReference(writer.size(), 1);
        }

        try {
            writer.addDirect(doc_id, doc_no, offset);
            entries_cnt++;
        } catch (IntCompressor.TooLargeToCompressException e) {
            throw new TooLargeBlockToJump(e.getMessage());
        }
    }

    /**
     *
     * @param doc_id absolute document ID
     * @param doc_no document number in list
     * @param offset offset to this doc_id in (packed) storage
     * @throws TooLargeBlockToJump
     */
    public void addEntry(int doc_id, int doc_no, int offset) throws TooLargeBlockToJump {
        this.doc_id = doc_id;
        this.doc_no = doc_no;

        addDirectEntry(offset);
    }

    public int getNumberOfLevels() {
        return tables.size();
    }

    public int write(DataOutputStream out) throws IOException {
        int size = 0;

        try {
            size += VarByteEncoder.EncodeNumberToStream(tables.size(), out);

            for (JumpTableWriter writer : tables) {
                byte[] content = writer.body.GetBytes();

                size += VarByteEncoder.EncodeNumberToStream(content.length, out);
                out.write(content);
                size += content.length;
            }
        } catch (IntCompressor.TooLargeToCompressException e) {
            throw new IllegalStateException("JumpTableBuilder.write should not deal with huge jump tables!");
        }

        return size;
    }

    public class TooLargeBlockToJump extends Exception {
        public TooLargeBlockToJump(String msg) {
            super(msg);
        }
    }

    class JumpTableWriter {
        VarByteEncoder body = new VarByteEncoder();

        int size() { return body.GetStoreSize(); }

        void addIndirect(int doc_id, int offset) throws IntCompressor.TooLargeToCompressException {
            body.AddNumber(doc_id);
            body.AddNumber(offset);
        }

        void addDirect(int doc_id, int doc_no, int offset) throws IntCompressor.TooLargeToCompressException {
            body.AddNumber(doc_id);
            body.AddNumber(doc_no);
            body.AddNumber(offset);
        }
    }
}
