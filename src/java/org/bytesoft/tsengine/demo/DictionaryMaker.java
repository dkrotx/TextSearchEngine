package org.bytesoft.tsengine.demo;

import org.bytesoft.tsengine.dict.DictRecord;
import org.bytesoft.tsengine.dict.DictionaryWriter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Build dictionary for given inverted index.
 */
class DictionaryMaker {

    private static final int DICTIONARY_RECORD_SIZE = 8 + 4;
    private Path src_catalog_path;
    private Path dictionary_path;
    private DictionaryWriter dictionary;

    private int estimateNumberOfEntries(Path raw_catalog) throws IOException {
        return (int)(Files.size(raw_catalog) / DICTIONARY_RECORD_SIZE);
    }

    public void writeDictionary() throws IOException {
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(dictionary_path))) {
            dictionary.Write(out);
        }
    }

    public void CreateDictionary() throws IOException {
        try (DataInputStream idx = new DataInputStream(Files.newInputStream(src_catalog_path))) {
            try {
                int offset = 0;

                for(;;) {
                    long word_hash = idx.readLong();
                    int block_size = idx.readInt();

                    dictionary.Add(word_hash, new DictRecord(offset, block_size));
                    offset += block_size;
                }
            }
            catch(EOFException e) {}
        }
    }

    public DictionaryMaker(String index_directory) throws IOException {
        src_catalog_path = Paths.get(index_directory, "rindex.cat");
        dictionary_path = Paths.get(index_directory, "rindex.dic");

        dictionary = new DictionaryWriter(estimateNumberOfEntries(src_catalog_path));
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: " + HtmlIndexerDemo.class.getCanonicalName() + " path/to/rindex");
            System.exit(64);
        }

        DictionaryMaker dm = new DictionaryMaker(args[0]);
        dm.CreateDictionary();
        dm.writeDictionary();
    }
}
