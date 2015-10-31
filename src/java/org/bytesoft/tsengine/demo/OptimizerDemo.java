package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.IndexOptimizer;
import org.bytesoft.tsengine.IndexingConfig;
import org.bytesoft.tsengine.info.IndexInfoReader;

import java.io.IOException;

class OptimizerDemo {
    IndexingConfig cfg;
    IndexOptimizer optimizer;

    public OptimizerDemo(String config_file) throws
            IOException,
            IndexingConfig.BadConfigFormat,
            IndexInfoReader.IndexInfoFormatError
    {
        cfg = new IndexingConfig(config_file);
        optimizer = new IndexOptimizer(cfg);
    }

    public void optimize() throws IOException, IndexOptimizer.IndexConvertionError {
        optimizer.optimize();
    }

    public static void main(String[] args) throws Exception {
        Getopt g = new Getopt(OptimizerDemo.class.getCanonicalName(), args, "c:");
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
            System.err.println("Usage: " + OptimizerDemo.class.getCanonicalName() + " -c config.file");
            System.exit(64);
        }

        OptimizerDemo demo = new OptimizerDemo(config_file);
        demo.optimize();
    }
}
