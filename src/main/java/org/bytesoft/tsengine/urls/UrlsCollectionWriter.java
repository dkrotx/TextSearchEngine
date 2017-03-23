package org.bytesoft.tsengine.urls;

import org.bytesoft.tsengine.IndexingConfig;

import java.io.*;

/**
 * Class for writing collection of URLs in compact way.
 * It writes urls and their offsets, so it's fast to retrieve URL
 * by document ID with {@code UrlsCollectionReader}
 */
public class UrlsCollectionWriter implements AutoCloseable, Flushable {
    private OutputStreamWriter url_stream;
    private DataOutputStream url_idx_stream;
    private int offset = 0;

    public UrlsCollectionWriter(IndexingConfig cfg) throws IOException {
        url_stream = new OutputStreamWriter(new BufferedOutputStream(
                new FileOutputStream(cfg.GetUrlsPath().toFile())));

        url_idx_stream = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(cfg.GetUrlsIdxPath().toFile()))
        );
    }

    public UrlsCollectionWriter(OutputStreamWriter urls_stream, DataOutputStream url_idx_stream)
            throws IOException {
        this.url_stream = urls_stream;
        this.url_idx_stream = url_idx_stream;
    }

    @Override
    public void close() throws IOException {
        url_stream.close();
        url_idx_stream.close();
    }

    @Override
    public void flush() throws IOException {
        url_stream.flush();
        url_idx_stream.flush();
    }

    /**
     * Write given document URL to collection
     * @param url URL to write
     * @throws IOException if IO-error due write
     */
    public void WriteURL(String url) throws IOException {
        url_idx_stream.writeInt(offset);

        url_stream.write(url);
        url_stream.write('\n');

        offset += url.getBytes().length + 1;
    }
}
