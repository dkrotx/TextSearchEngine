package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.IndexingConfig;
import org.bytesoft.tsengine.dict.CatalogReader;
import org.bytesoft.tsengine.dict.CatalogRecord;
import org.bytesoft.tsengine.dict.DictRecord;
import org.bytesoft.tsengine.dict.DictionaryWriter;
import util.lang.ExceptionalIterator;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Build dictionary for given inverted index.
 */
class DictionaryMaker {
    private IndexingConfig cfg;
    private CatalogReader catalog;
    private DictionaryWriter dictionary;

    public DictionaryMaker(String config_file) throws IOException, IndexingConfig.BadConfigFormat {
        cfg = new IndexingConfig(config_file);
        catalog = new CatalogReader(cfg.GetRindexCatPath());
        dictionary = new DictionaryWriter(catalog.numberOfEntries());
    }

    public void CreateDictionary() throws IOException {
        fillDictionary();
        writeDictionary();
    }

    private void fillDictionary() throws IOException {
        try (ExceptionalIterator<CatalogRecord, IOException> it = catalog.iterator()) {
            int offset = 0;

            while (it.hasNext()) {
                CatalogRecord record = it.next();
                dictionary.Add(record.word_hash, new DictRecord(offset, record.size));
                offset += record.size;
            }
        }
    }

    private void writeDictionary() throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(cfg.GetRindexDictPath())))) {
            dictionary.Write(out);
        }
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
    }
}
