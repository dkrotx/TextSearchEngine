package org.bytesoft.tsengine.dict;

import util.lang.ExceptionalIterator;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read index catalog file
 */
public class CatalogReader implements AutoCloseable {
    long file_size;
    Path path;

    public CatalogReader(Path path) throws IOException {
        file_size = Files.size(path);
        this.path = path;
    }

    private CatalogRecord readRecord(DataInputStream in) throws IOException {
        long word_hash = in.readLong();
        int size = in.readInt();

        return new CatalogRecord(word_hash, size);
    }

    @Override
    public void close() throws IOException {
        // Nothing occupied by current implementation.
        // But it's handy to use in try-with-resources block
    }

    /**
     * Load all catalog entries in memory
     * @return all catalog entries
     * @throws IOException
     */
    public CatalogRecord[] entries() throws IOException {
        final int n = numberOfEntries();
        CatalogRecord[] records = new CatalogRecord[n];

        try(DataInputStream cat = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            for(int i = 0; i < n; i++)
                records[i] = readRecord(cat);
        }

        return records;
    }

    public int numberOfEntries() throws IOException {
        return (int)(file_size / (CatalogRecord.SIZE / Byte.SIZE));
    }

    public ExceptionalIterator<CatalogRecord, IOException> iterator() throws IOException {
        return new ExceptionalIterator<CatalogRecord, IOException>()
        {
            final int amount = numberOfEntries();
            DataInputStream cat = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)));
            int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < amount;
            }

            @Override
            public CatalogRecord next() throws IOException {
                currentIndex++;
                return readRecord(cat);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove method not implemented for CatalogReader");
            }

            @Override
            public void close() throws IOException { cat.close(); }
        };
    }
}
