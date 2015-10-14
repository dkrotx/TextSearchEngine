package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.dict.DictRecord;
import org.bytesoft.tsengine.dict.DictionaryWriter;

import java.io.*;

/**
 * Build dictionary for given inverted index.
 */
class DictionaryMaker {

    private void skipIndexBlock(DataInput file) throws IOException {
        // each record is [WORD_HASH:8][BLOCK_SIZE:4][BLOCK_DATA:BLOCK_SIZE]
        file.skipBytes(Long.SIZE / Byte.SIZE);
        file.skipBytes(file.readInt());
    }

    private int estimateNumberOfEntries(String path) throws IOException {
        int n = 0;

        try (RandomAccessFile idx = new RandomAccessFile(path, "r")) {
            long file_size = idx.length();

            while(idx.getFilePointer() < file_size) {
                skipIndexBlock(idx);
                n++;
            }
        }

        return n;
    }


    public void HandleIndexFile(String path, String dict_path) throws IOException {
        int nrec = estimateNumberOfEntries(path);

        DictionaryWriter dict = new DictionaryWriter(nrec);
        long offset = 0;

        try (DataInputStream idx = new DataInputStream(new FileInputStream(path))) {
            try {
                for(;;) {
                    long whash = idx.readLong();
                    int block_size = idx.readInt();

                    dict.Add(whash, new DictRecord(offset, block_size));
                    offset += block_size;
                    idx.skipBytes(block_size);
                }
            }
            catch(EOFException e) {}

            idx.close();
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(dict_path))) {
            dict.Write(out);
            out.close();
        }
    }

    public static void main(String[] args) throws IOException {
        Getopt g = new Getopt("HtmlIndexerDemo", args, "o:");
        int c;
        String ofile = null;

        while( (c = g.getopt()) != -1 ) {
            switch(c) {
                case 'o':
                    ofile = g.getOptarg();
                    break;
            }
        }

        final int nargs = args.length - g.getOptind();

        if (nargs != 1 || ofile == null) {
            System.err.println("Usage: " + HtmlIndexerDemo.class.getCanonicalName() + " -o path/to/output.dic rindex");
            System.exit(64);
        }

        DictionaryMaker dm = new DictionaryMaker();
        dm.HandleIndexFile(args[g.getOptind()], ofile);
    }
}
