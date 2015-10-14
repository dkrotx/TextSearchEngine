package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.HtmlDocIndexer;
import org.bytesoft.tsengine.IndexingConfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Demonstrates how to use HtmlDocIndexer
 * Usage: class -o path/to/output.file input1 [...]
 */
public class HtmlIndexerDemo {

    private HtmlDocIndexer idx;
    private DataOutputStream idxfile;

    public HtmlIndexerDemo(String out_path) throws FileNotFoundException {
        IndexingConfig cfg = new IndexingConfig();
        idx = new HtmlDocIndexer(cfg);
        idxfile = new DataOutputStream(new FileOutputStream(out_path));
    }

    public void ParseOneMoreFile(String path) throws IOException, HtmlDocIndexer.HTMLParsingError {
        String html = new String( Files.readAllBytes(Paths.get(path)) );
        idx.AddDocument(html);
    }

    public void FlushFiles() throws IOException {
        idx.Flush(idxfile);
        idxfile.close();
    }

    public static void main(String[] args) {
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

        if (g.getOptind() == args.length || ofile == null) {
            System.err.println("Usage: " + HtmlIndexerDemo.class.getCanonicalName() + " -o path/to/output.file input1 [...]");
            System.exit(64);
        }

        HtmlIndexerDemo demo = null;
        try {
            demo = new HtmlIndexerDemo(ofile);
        }
        catch(FileNotFoundException e) {
            System.err.println("Can't open output file " + ofile + ": " + e.getMessage());
        }

        try {
            for(int i = g.getOptind(); i < args.length; i++) {
                try {
                    demo.ParseOneMoreFile(args[i]);
                }
                catch(HtmlDocIndexer.HTMLParsingError e) {
                    System.err.println("Error parsing input file " + args[i]);
                    // just continue to parse another files
                }
            }

            demo.FlushFiles();
        }
        catch(IOException e) {
            System.err.println("IO-error while parsing HTML files");
            System.exit(1);
        }
    }
}
