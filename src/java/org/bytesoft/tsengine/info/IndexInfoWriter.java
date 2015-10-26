package org.bytesoft.tsengine.info;

import org.bytesoft.tsengine.IndexingConfig;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.json.simple.JSONObject;

import java.io.*;

/**
 * Write information-file about index
 */
public class IndexInfoWriter {
    IndexingConfig cfg;
    int ndocs = 0;

    public IndexInfoWriter(IndexingConfig cfg) {
        this.cfg = cfg;
    }

    public void SetNumberOfDocs(int n) {
        ndocs = n;
    }

    public void Write() throws IOException {
        try (FileWriter wr = new FileWriter(cfg.GetInfoPath().toFile())) {
            JSONObject obj = new JSONObject();

            obj.put("ndocs", new Integer(ndocs));
            obj.put("encoder", EncodersFactory.GetEncoderNameByID(cfg.GetEncodingMethod()));

            obj.writeJSONString(wr);
        }
    }
}
