package org.bytesoft.tsengine.dict;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Write rindex catalog - address book of invert index
 */
public class CatalogWriter implements AutoCloseable {
    DataOutputStream out;

    public CatalogWriter(Path cat_path) throws IOException {
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cat_path.toFile())));
    }

    public void write(CatalogRecord rec) throws IOException {
        out.writeLong(rec.word_hash);
        out.writeInt(rec.size);
    }

    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
