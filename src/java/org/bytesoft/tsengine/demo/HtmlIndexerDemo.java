package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.HtmlDocIndexer;
import org.bytesoft.tsengine.IndexingConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demonstrates how to use HtmlDocIndexer
 */
class HtmlIndexerDemo {

    private HtmlDocIndexer idx;

    public HtmlIndexerDemo(String out_path) throws IOException {
        IndexingConfig cfg = new IndexingConfig(out_path);
        idx = new HtmlDocIndexer(cfg);
    }

    public void ParseOneMoreFile(String path) throws IOException, HtmlDocIndexer.HTMLParsingError {
        Path file = Paths.get(path);
        String url = "file://" + file.toAbsolutePath().toString();
        String html = new String( Files.readAllBytes(file) );

        idx.AddDocument(url, html);
    }

    public void Flush() throws IOException {
        idx.Flush();
    }

    public static void main(String[] args) throws IOException {
        Getopt g = new Getopt("HtmlIndexerDemo", args, "o:");
        int c;
        String index_directory = null;

        while( (c = g.getopt()) != -1 ) {
            switch(c) {
                case 'o':
                    index_directory = g.getOptarg();
                    break;
            }
        }

        if (g.getOptind() == args.length || index_directory == null) {
            System.err.println("Usage: " + HtmlIndexerDemo.class.getCanonicalName() + " -o path/to/out_directory input1 [...]");
            System.exit(64);
        }

        HtmlIndexerDemo demo = null;
        try {
            demo = new HtmlIndexerDemo(index_directory);
        }
        catch(IOException e) {
            System.err.println("Can't create index at " + index_directory + ": " + e.getMessage());
            System.exit(1);
        }

        for(int i = g.getOptind(); i < args.length; i++) {
            try {
                demo.ParseOneMoreFile(args[i]);
            }
            catch(HtmlDocIndexer.HTMLParsingError e) {
                System.err.println("Error parsing input file " + args[i]);
                // just continue to parse another files
            }
        }

        demo.Flush();
    }
}
