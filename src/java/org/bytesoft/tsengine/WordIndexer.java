package org.bytesoft.tsengine;

import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;

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
    EncodersFactory encoder_factory;

    long acc_size = 0;

    public WordIndexer(EncodersFactory encoder_factory) {
        this.encoder_factory = encoder_factory;
    }

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
            comp = new IdxBlockEncoder(encoder_factory.MakeEncoder());
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
     * @param writer reverse index writer
     */
    public void flushBuffered(IdxBlockWriter writer) throws IOException {
        for(Map.Entry<Long, IdxBlockEncoder> entry: words_buf.entrySet()) {
            writer.write(entry.getKey(), entry.getValue());
        }

        words_buf.clear();
        acc_size = 0;
    }
}
