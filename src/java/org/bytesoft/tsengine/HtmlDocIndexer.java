package org.bytesoft.tsengine;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.info.IndexInfoWriter;
import org.bytesoft.tsengine.text.TextTokenizer;
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
    private WordIndexer word_indexer;
    private int doc_id = -1;
    private IndexingConfig cfg;

    private DataOutputStream rindex_writer;
    private DataOutputStream catalog_writer;
    private UrlsWriter urls_writer;
    private TextTokenizer text_tokenizer = new TextTokenizer();

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

        EncodersFactory enc_factory = new EncodersFactory();
        enc_factory.SetCurrentEncoder(cfg.GetEncodingMethod());

        word_indexer = new WordIndexer(enc_factory);
        rindex_writer = new DataOutputStream(new FileOutputStream(cfg.GetRindexPath().toFile()));
        catalog_writer = new DataOutputStream(new FileOutputStream(cfg.GetRindexCatPath().toFile()));
        urls_writer = this.new UrlsWriter();
    }

    public void AddDocumentWords() {
        HashSet<String> uniq_words = new HashSet<>();

        while(text_tokenizer.hasNextToken()) {
            String word = text_tokenizer.getNextToken();
            uniq_words.add(WordUtils.GetWordFirstForm(word));
        }

        doc_id++;
        for (String w: uniq_words) {
            word_indexer.AddWord(w, doc_id);
        }
    }

    public long GetApproximateSize() {
        return word_indexer.GetApproximateSize();
    }

    public void Flush() throws IOException {
        word_indexer.WriteAndFlush(rindex_writer, catalog_writer);
        urls_writer.Flush();

        rindex_writer.flush();
        catalog_writer.flush();
    }

    public void AddText(String text) {
        text_tokenizer.TokenizeText(text);
        AddDocumentWords();
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
            System.out.println("Flush");
            Flush();
        }
    }

    public void WriteIndexInfo() throws IOException {
        IndexInfoWriter info = new IndexInfoWriter(cfg);

        info.SetNumberOfDocs(doc_id+1);
        info.Write();
    }
}
