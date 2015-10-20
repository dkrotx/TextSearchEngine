package org.bytesoft.tsengine.urls;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Class for writing collection of URLs in compact way.
 * It writes urls and their offsets, so it's fast to retrieve URL
 * by document ID with {@code UrlsCollectionReader}
 */
public class UrlsCollectionWriter {
    private OutputStreamWriter url_stream;
    private DataOutputStream url_idx_stream;
    private int offset = 0;

    /**
     * Constructs UrlsCollectionWriter object
     * @param url_stream stream to write URLs ('\n'-separated)
     * @param url_idx_stream stream to write binary index of URLs
     */
    public UrlsCollectionWriter(OutputStreamWriter url_stream, DataOutputStream url_idx_stream) {
        this.url_stream = url_stream;
        this.url_idx_stream = url_idx_stream;
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
