package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.document.PlainDocumentExchange.PlainDocument;
import org.bytesoft.tsengine.HtmlDocIndexer;
import org.bytesoft.tsengine.IndexingConfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

/**
 * Demonstrates how to use HtmlDocIndexer
 */
class HtmlIndexerDemo {

    private HtmlDocIndexer idx;

    public HtmlIndexerDemo(String config_file) throws IOException, IndexingConfig.BadConfigFormat {
        IndexingConfig cfg = new IndexingConfig(config_file);
        idx = new HtmlDocIndexer(cfg);
    }

    public void ParseOneMoreFile(String path) throws IOException, HtmlDocIndexer.HTMLParsingError {
        Path file = Paths.get(path);
        String url = "file://" + file.toAbsolutePath().toString();
        String html = new String( Files.readAllBytes(file) );

        idx.AddDocument(url, html);
    }

    private static class Decompressor {
        Inflater inflater = new Inflater();
        byte[]   chunk_buf = new byte[4096];

        public String Decompress(byte[] data) throws DataFormatException, UnsupportedEncodingException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            inflater.reset();
            inflater.setInput(data);

            while (!inflater.finished()) {
                int nb = inflater.inflate(chunk_buf);
                buf.write(chunk_buf, 0, nb);
            }

            return new String(buf.toByteArray(), 0, buf.size(), "UTF-8");
        }
    }

    Decompressor decompressor = new Decompressor();

    public void IdxPackedFile(String path) throws
            IOException,
            HtmlDocIndexer.HTMLParsingError,
            DataFormatException,
            UnsupportedEncodingException
    {
        try(BufferedReader in = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = in.readLine()) != null) {
                String[] url_data = line.split("\\t");

                Base64.Decoder decoder = Base64.getDecoder();
                String html = decompressor.Decompress(decoder.decode(url_data[1]));

                idx.AddDocument(url_data[0], html);
            }
        }
    }

    public void IdxPackedPlainText(String path) throws
        IOException
    {
        try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
            for(;;) {
                PlainDocument doc = PlainDocument.parseDelimitedFrom(in);
                if (doc == null)
                    break;

                idx.AddPlainTextDocument(doc.getUrl(), doc.getContent().toStringUtf8());
            }
        }
    }

    public void Flush() throws IOException {
        idx.Flush();
    }

    public static void main(String[] args) throws Exception {
        Getopt g = new Getopt("HtmlIndexerDemo", args, "c:pt");
        int c;
        boolean packed_input = false;
        boolean text_input = false;
        String config_file = null;

        while( (c = g.getopt()) != -1 ) {
            switch(c) {
                case 'c':
                    config_file = g.getOptarg();
                    break;
                case 't':
                    text_input = true;
                    break;
                case 'p':
                    packed_input = true;
                    break;
            }
        }

        if (g.getOptind() == args.length || config_file == null) {
            System.err.println("Usage: " + HtmlIndexerDemo.class.getCanonicalName() + " -c config.file input1 [...]");
            System.exit(64);
        }

        HtmlIndexerDemo demo = null;
        try {
            demo = new HtmlIndexerDemo(config_file);
        }
        catch(IOException e) {
            System.err.println("Can't create index at " + config_file + ": " + e.getMessage());
            System.exit(1);
        }

        for(int i = g.getOptind(); i < args.length; i++) {
            if (packed_input) {
                demo.IdxPackedFile(args[i]);
            } else if (text_input) {
                demo.IdxPackedPlainText(args[i]);
            } else {
                try {
                    demo.ParseOneMoreFile(args[i]);
                } catch (HtmlDocIndexer.HTMLParsingError e) {
                    System.err.println("Error parsing input file " + args[i]);
                    // just continue to parse another files
                }
            }
        }

        demo.Flush();
    }
}
