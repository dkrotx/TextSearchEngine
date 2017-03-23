package org.bytesoft.tsengine;

import org.bytesoft.tsengine.dict.CatalogRecord;
import org.bytesoft.tsengine.dict.CatalogWriter;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Write rindex block and it's catalog
 */
public class IdxBlockWriter implements Closeable, Flushable {
    DataOutputStream rindex_writer;
    CatalogWriter catalog_writer;

    public IdxBlockWriter(Path rindex_path, Path catalog_path) throws IOException {
        rindex_writer = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(rindex_path)));
        catalog_writer = new CatalogWriter(catalog_path);
    }

    public void write(long word_hash, IdxBlockEncoder block) throws IOException {
        long size = block.Write(rindex_writer);
        catalog_writer.write(new CatalogRecord(word_hash, (int)size));
    }

    @Override
    public void flush() throws IOException {
        rindex_writer.flush();
        catalog_writer.flush();
    }

    @Override
    public void close() throws IOException {
        rindex_writer.close();
        catalog_writer.close();
    }
}
