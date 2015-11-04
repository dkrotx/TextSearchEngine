package org.bytesoft.tsengine.idxblock;

import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.encoders.VarByteEncoder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Build one or more jump table levels to quickly navigate in posting-list.
 * Final jump table structure is hierarchy of small jump tables. One above previous.
 * <p>
 * |posting-list-page||posting-list-page||posting-list-page|
 * |JT0||JT0|JT0|...
 * |JT1|
 */
public class JumpTableBuilder {
    ArrayList<WriterAndInfo> tables = new ArrayList<>();
    JumpTableFillPolicy fill_policy;

    public JumpTableBuilder(JumpTableFillPolicy fill_policy) {
        this.fill_policy = fill_policy;
    }

    WriterAndInfo getLevelTable(int level) {
        if (level >= tables.size()) {
            tables.add(new WriterAndInfo());
            assert (level + 1 == tables.size());
        }

        return tables.get(level);
    }

    // add entry to given level
    // recursive: if needed, add entry to next (upper) level
    void addEntryToLevel(int abs_docid, int offset, int level) throws TooLargeBlockToJump {
        WriterAndInfo wi = getLevelTable(level);
        JumpTableWriter jt_writer = wi.writer;

        if (fill_policy.enough(wi.continuous_elems, wi.continuous_size)) {
            // add indirect reference
            addEntryToLevel(abs_docid, jt_writer.size(), level + 1);

            wi.continuous_elems = 0;
            wi.continuous_size = 0;
        }

        try {
            wi.continuous_size += jt_writer.addEntry(abs_docid, offset);
            wi.continuous_elems++;
        } catch (IntCompressor.TooLargeToCompressException e) {
            throw new TooLargeBlockToJump(e.getMessage());
        }
    }

    public void addEntry(int abs_docid, int offset) throws TooLargeBlockToJump {
        addEntryToLevel(abs_docid, offset, 0);
    }

    public int getNumberOfLevels() {
        return tables.size();
    }

    public int write(DataOutputStream out) throws IOException {
        int size = 0;

        try {
            size += VarByteEncoder.EncodeNumberToStream(tables.size(), out);

            for (WriterAndInfo wi : tables) {
                JumpTableWriter jt_writer = wi.writer;
                byte[] content = jt_writer.jtbody.GetBytes();

                size += VarByteEncoder.EncodeNumberToStream(content.length, out);
                out.write(content);
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
        VarByteEncoder jtbody = new VarByteEncoder();

        int addEntry(int abs_docid, int offset) throws IntCompressor.TooLargeToCompressException {
            int size_before = jtbody.GetStoreSize();

            jtbody.AddNumber(abs_docid);
            jtbody.AddNumber(offset);

            return jtbody.GetStoreSize() - size_before;
        }

        int size() { return jtbody.GetStoreSize(); }
    }

    class WriterAndInfo {
        JumpTableWriter writer = new JumpTableWriter();
        int continuous_elems = 0;
        int continuous_size = 0;
    }
}
