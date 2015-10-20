package org.bytesoft.tsengine;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import org.bytesoft.tsengine.urls.UrlsCollectionWriter;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;

/**
 * HTML document indexer
 */
public class HtmlDocIndexer {
    private WordIndexer widx = new WordIndexer();
    private int doc_id = -1;
    private IndexingConfig cfg;

    private DataOutputStream rindex_writer;
    private DataOutputStream catalog_writer;
    private UrlsWriter urls_writer;

    private class UrlsWriter {
        UrlsCollectionWriter writer;
        OutputStreamWriter url_stream;
        DataOutputStream url_idx_stream;

        public UrlsWriter() throws IOException {
            url_stream = new OutputStreamWriter(new FileOutputStream(cfg.GetUrlsPath().toFile()));
            url_idx_stream = new DataOutputStream(new FileOutputStream(cfg.GetUrlsIdxPath().toFile()));
            writer = new UrlsCollectionWriter(url_stream, url_idx_stream);
        }

        public void Put(String url) throws IOException {
            writer.WriteURL(url);
        }

        public void Flush() throws IOException {
            url_stream.flush();
            url_idx_stream.flush();
        }
    }


    public class HTMLParsingError extends Exception {
        HTMLParsingError(String msg) { super(msg); }
    }

    public HtmlDocIndexer(IndexingConfig cfg) throws IOException {
        this.cfg = cfg;

        rindex_writer = new DataOutputStream(new FileOutputStream(cfg.GetRindexPath().toFile()));
        catalog_writer = new DataOutputStream(new FileOutputStream(cfg.GetRindexCatPath().toFile()));
        urls_writer = this.new UrlsWriter();
    }

    public void AddDocumentWords(String[] words) {
        HashSet<String> uniq_words = new HashSet<>();

        for (String w: words) {
            uniq_words.add(WordUtils.NormalizeWord(w));
        }

        doc_id++;
        for (String w: uniq_words) {
            widx.AddWord(w, doc_id);
        }
    }

    public long GetApproximateSize() {
        return widx.GetApproximateSize();
    }

    public void Flush() throws IOException {
        widx.WriteAndFlush(rindex_writer, catalog_writer);
        urls_writer.Flush();
    }

    public void AddText(String text) {
        String[] words = text.split("\\s+");
        AddDocumentWords(words);
    }

    public void AddDocument(String url, String html) throws HTMLParsingError, IOException {
        try {
            String text = DefaultExtractor.getInstance().getText(html);
            AddText(text);
            urls_writer.Put(url);
        }
        catch (BoilerpipeProcessingException e) {
            throw new HTMLParsingError("Failed to parse HTML document" + e.getMessage());
        }

        if (GetApproximateSize() >= cfg.GetMaxMemBuf()) {
            Flush();
        }
    }
}
