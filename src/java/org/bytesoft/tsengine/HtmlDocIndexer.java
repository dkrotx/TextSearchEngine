package org.bytesoft.tsengine;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.info.IndexInfoWriter;
import org.bytesoft.tsengine.text.TextTokenizer;
import org.bytesoft.tsengine.urls.UrlsCollectionWriter;

import java.io.*;
import java.util.HashSet;

/**
 * HTML document indexer
 */
public class HtmlDocIndexer {
    private WordIndexer word_indexer;
    private int doc_id = -1;
    private IndexingConfig cfg;

    private IdxBlockWriter index_writer;

    private UrlsCollectionWriter urls_writer;
    private TextTokenizer text_tokenizer = new TextTokenizer();
    private LemmatizerCache lem_cache;


    public class HTMLParsingError extends Exception {
        HTMLParsingError(String msg) { super(msg); }
    }

    public HtmlDocIndexer(IndexingConfig cfg) throws IOException {
        this.cfg = cfg;

        word_indexer = new WordIndexer(cfg);
        index_writer = new IdxBlockWriter(cfg.GetRindexPath(), cfg.GetRindexCatPath());
        urls_writer = new UrlsCollectionWriter(cfg);
        lem_cache = new LemmatizerCache(cfg.GetLemCacheCapacity());
    }

    private void addDocumentWords() {
        HashSet<String> uniq_words = new HashSet<>();

        while(text_tokenizer.hasNextToken()) {
            String word = text_tokenizer.getNextToken();
            if (word.length() > 1)
                uniq_words.add(lem_cache.GetFirstForm(WordUtils.NormalizeWord(word)));
        }

        doc_id++;
        for (String w: uniq_words) {
            word_indexer.AddWord(w, doc_id);
        }
    }

    public long GetApproximateSize() {
        return word_indexer.GetApproximateSize();
    }

    public void AddDocument(String url, String html) throws HTMLParsingError, IOException {
        try {
            String text = DefaultExtractor.getInstance().getText(html);
            AddPlainTextDocument(url, text);
        }
        catch (BoilerpipeProcessingException e) {
            throw new HTMLParsingError("Failed to parse HTML document" + e.getMessage());
        }
    }

    public void AddPlainTextDocument(String url, String text) throws IOException {
        addText(text);
        urls_writer.WriteURL(url);

        if (GetApproximateSize() >= cfg.GetMaxMemBuf()) {
            Flush();
        }
    }

    private void addText(String text) {
        text_tokenizer.TokenizeText(text);
        addDocumentWords();
    }

    public void Flush() throws IOException {
        word_indexer.flushBuffered(index_writer);
        urls_writer.flush();
        index_writer.flush();

        writeIndexInfo();
    }

    private void writeIndexInfo() throws IOException {
        IndexInfoWriter info = new IndexInfoWriter(cfg);

        info.SetNumberOfDocs(doc_id+1);
        info.Write();
    }
}
