package org.bytesoft.tsengine.demo;

import gnu.getopt.Getopt;
import org.bytesoft.tsengine.IndexOptimizer;
import org.bytesoft.tsengine.IndexingConfig;
import org.bytesoft.tsengine.info.IndexInfoReader;

import java.io.IOException;
import java.util.Arrays;

class OptimizerDemo {
    IndexOptimizer optimizer;

    public OptimizerDemo(String dst_config_file, String[] src_config_files) throws
            IOException,
            IndexingConfig.BadConfigFormat,
            IndexInfoReader.IndexInfoFormatError
    {
        IndexingConfig dst_config = new IndexingConfig(dst_config_file);
        IndexingConfig[] src_configs = new IndexingConfig[src_config_files.length];

        for (int i = 0; i < src_config_files.length; i++)
            src_configs[i] = new IndexingConfig(src_config_files[i]);

        optimizer = new IndexOptimizer(dst_config, src_configs);
    }

    public void optimize() throws IOException, IndexOptimizer.IndexConvertionError {
        optimizer.optimize();
    }

    public static void main(String[] args) throws Exception {
        Getopt g = new Getopt(OptimizerDemo.class.getCanonicalName(), args, "c:");
        int c;
        String dst_config = null;

        while( (c = g.getopt()) != -1 ) {
            switch(c) {
                case 'c':
                    dst_config = g.getOptarg();
                    break;
            }
        }

        if (dst_config == null || g.getOptind() == args.length) {
            System.err.println("Usage: " + OptimizerDemo.class.getCanonicalName() + " -c result.conf SRC_CONF [...]");
            System.exit(64);
        }

        OptimizerDemo demo = new OptimizerDemo(dst_config, Arrays.copyOfRange(args, g.getOptind(), args.length));
        demo.optimize();
    }
}
