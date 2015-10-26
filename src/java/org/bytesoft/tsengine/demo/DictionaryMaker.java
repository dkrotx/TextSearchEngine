package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.IndexingConfig;
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
    private IndexingConfig cfg;

    private int estimateNumberOfEntries(Path raw_catalog) throws IOException {
        return (int)(Files.size(raw_catalog) / DICTIONARY_RECORD_SIZE);
    }

    public void writeDictionary() throws IOException {
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(cfg.GetRindexDictPath()))) {
            dictionary.Write(out);
        }
    }

    public void CreateDictionary() throws IOException {
        try (DataInputStream idx = new DataInputStream(Files.newInputStream(cfg.GetRindexCatPath()))) {
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

    public DictionaryMaker(String config_file) throws IOException, IndexingConfig.BadConfigFormat {
        cfg = new IndexingConfig(config_file);
        dictionary = new DictionaryWriter(estimateNumberOfEntries(cfg.GetRindexCatPath()));
    }

    public static void main(String[] args) throws IOException, IndexingConfig.BadConfigFormat {
        Getopt g = new Getopt("DictionaryMaker", args, "c:");
        int c;
        String config_file = null;

        while( (c = g.getopt()) != -1 ) {
            switch(c) {
                case 'c':
                    config_file = g.getOptarg();
                    break;
            }
        }

        if (config_file == null || g.getOptind() != args.length) {
            System.err.println("Usage: " + DictionaryMaker.class.getCanonicalName() + " -c config.file");
            System.exit(64);
        }

        DictionaryMaker dm = new DictionaryMaker(config_file);
        dm.CreateDictionary();
        dm.writeDictionary();
    }
}
