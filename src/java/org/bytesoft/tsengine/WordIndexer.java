package org.bytesoft.tsengine;

import org.bytesoft.tsengine.encoders.Simple9Encoder;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * WordIndexer class
 * Perform indexation of words in given document id.
 */
public class WordIndexer {
    HashMap<Long, IdxBlockEncoder> words_buf = new HashMap<>();
    final int SIZE_WORD_ENTRY = 16;

    long acc_size = 0;

    private void addDocIDToEncoder(IdxBlockEncoder enc, int id)
    {
        try {
            long sz = enc.GetStoreSize();
            enc.AddDocID(id);
            acc_size += enc.GetStoreSize() - sz;
        } catch (Exception e) {
        }
    }

    public void AddWord(String word, int docid)
    {
        long hash = WordUtils.GetWordHash(word);
        IdxBlockEncoder comp = words_buf.get(hash);

        if (comp == null) {
            comp = new IdxBlockEncoder(new Simple9Encoder());
            words_buf.put(hash, comp);
            acc_size += SIZE_WORD_ENTRY;
        }

        addDocIDToEncoder(comp, docid);
    }


    public long GetApproximateSize() {
        return acc_size;
    }

    /**
     * Flush encoded content in given stream
     *
     * @param rindex reverse index file
     * @param catalog file to store word hashes and rindex-records size
     */
    public void WriteAndFlush(DataOutputStream rindex, DataOutputStream catalog) throws IOException {
        for(Map.Entry<Long, IdxBlockEncoder> entry: words_buf.entrySet()) {
            IdxBlockEncoder enc = entry.getValue();

            int size = (int)enc.Write(rindex);

            catalog.writeLong(entry.getKey());
            catalog.writeInt(size);
        }
        words_buf.clear();
        acc_size = 0;
    }
}
