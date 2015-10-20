package org.bytesoft.tsengine.urls;

import java.io.*;

/**
 * Class for reading collection of URLs written in compact way.
 * It holds in memory only offsets, and read url_file itself only when {@code GetURL(id)} called.
 */
public class UrlsCollectionReader {
    private int[] offsets;
    private RandomAccessFile urls_file;

    private void loadIndex(File urls_idx_path) throws IOException {
        final int n = (int)(urls_idx_path.length() / (Integer.SIZE / Byte.SIZE));
        offsets = new int[n];

        try (DataInputStream idx_stream = new DataInputStream(new FileInputStream(urls_idx_path))) {
            for (int i = 0; i < n; i++)
                offsets[i] = idx_stream.readInt();
        }
    }

    /**
     * Constructs UrlsCollectionReader object
     * @param urls_path file to read URLs from
     * @param urls_idx_path file to load binary index of URLs
     */
    public UrlsCollectionReader(File urls_path, File urls_idx_path) throws IOException {
        urls_file = new RandomAccessFile(urls_path, "r");
        loadIndex(urls_idx_path);
    }

    private String getURLFromOffset(int offset) throws IOException {
        urls_file.seek(offset);
        return urls_file.readLine();
    }

    /**
     * Read URL by document ID
     * @param id zero-based document ID
     * @return String representing URL
     * @throws IOException on read error
     * @throws IndexOutOfBoundsException if id is too big
     */
    public String ReadURL(int id) throws IOException, IndexOutOfBoundsException {
        if (id < 0 || id >= offsets.length) {
            throw new IndexOutOfBoundsException("document id=" + id + " is too big");
        }

        return getURLFromOffset(offsets[id]);
    }

    public int GetURLsAmount() {
        return offsets.length;
    }
}
