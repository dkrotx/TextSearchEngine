package org.bytesoft.tsengine;

import de.l3s.boilerpipe.extractors.*;
import de.l3s.boilerpipe.BoilerpipeProcessingException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

/**
 * HTML document indexer
 */
public class HtmlDocIndexer {
    private WordIndexer widx = new WordIndexer();
    private int doc_id = -1;
    private IndexingConfig cfg;

    public class HTMLParsingError extends Exception {
        HTMLParsingError(String msg) { super(msg); }
    }

    public HtmlDocIndexer(IndexingConfig cfg) {
        this.cfg = cfg;
    }

    public void AddDocumentWords(String[] words) {
        HashSet<String> uniq_words = new HashSet<>();

        for (String w: words) {
            uniq_words.add(w.toLowerCase());
        }

        doc_id++;
        for (String w: uniq_words) {
            widx.AddWord(w, doc_id);
        }
    }

    public long GetApproximateSize() {
        return widx.GetApproximateSize();
    }

    public void Flush(OutputStream out) throws IOException {
        widx.WriteAndFlush(out);
    }

    public void AddText(String text) {
        String[] words = text.split("\\s+");
        AddDocumentWords(words);
    }

    public void AddDocument(String html) throws HTMLParsingError {
        try {
            String text = DefaultExtractor.getInstance().getText(html);
            AddText(text);
        }
        catch (BoilerpipeProcessingException e) {
            throw new HTMLParsingError("Failed to parse HTML document" + e.getMessage());
        }
    }
}
