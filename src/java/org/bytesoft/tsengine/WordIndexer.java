package org.bytesoft.tsengine;

import org.bytesoft.tsengine.encoders.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static util.hash.MurmurHash3.murmurhash3_x64;
import static util.bytes.Bytes.*;

/**
 * WordIndexer class
 * Perform indxation of words in given document id.
 */
public class WordIndexer {
    HashMap<Long, DeltaIntEncoder> words_buf = new HashMap<>();
    final int SIZE_WORD_ENTRY = 16;

    long acc_size = 0;

    private void addDocidToEncoder(DeltaIntEncoder comp, int docid)
    {
        try {
            long sz = comp.GetStoreSize();
            comp.AddNumber(docid);
            acc_size += comp.GetStoreSize() - sz;
        } catch (Exception e) {
        }
    }

    public void AddWord(String word, int docid) {
        long hash = murmurhash3_x64(word);
        DeltaIntEncoder comp = words_buf.get(hash);

        if (comp == null) {
            comp = new DeltaIntEncoder(new VarByteEncoder());
            words_buf.put(hash, comp);
            acc_size += SIZE_WORD_ENTRY;
        }

        addDocidToEncoder(comp, docid);
    }

    public long GetApproximateSize() {
        return acc_size;
    }

    /**
     * Flush encoded content in given stream
     * @param out output stream to write binary data
     */
    public void WriteAndFlush(OutputStream out) throws IOException {
        for(Map.Entry<Long, DeltaIntEncoder> entry: words_buf.entrySet()) {
            DeltaIntEncoder enc = entry.getValue();
            out.write(toBytes(entry.getKey()));
            out.write(toBytes(enc.GetStoreSize()));
            out.write(enc.GetBytes());
        }
        words_buf.clear();
    }
}
