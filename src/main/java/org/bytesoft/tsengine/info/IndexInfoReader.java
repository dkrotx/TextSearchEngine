package org.bytesoft.tsengine.info;

import org.bytesoft.tsengine.IndexingConfig;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.idxblock.JumpTableConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

/**
 * Read information-file about index
 */
public class IndexInfoReader {
    private IndexingConfig cfg;

    private int ndocs = 0;
    private EncodersFactory.EncodingMethods encoder;
    private JumpTableConfig jt_config;

    public static class IndexInfoFormatError extends Exception {
        public IndexInfoFormatError(String msg) { super(msg); }
    }

    public IndexInfoReader(IndexingConfig cfg) throws IOException, IndexInfoFormatError {
        try (FileReader reader = new FileReader(cfg.GetInfoPath().toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(reader);
            ndocs = ((Long)root.get("ndocs")).intValue();

            String encoder_name = (String)root.get("encoder");
            encoder = EncodersFactory.GetEncoderByName(encoder_name);
            if (encoder == null)
                throw new IndexInfoFormatError("Bad encoder \"" + encoder + "\"");

            int jt_direct_step = ((Long)root.get("jt_direct")).intValue();
            int jt_indirect_step = ((Long)root.get("jt_indirect")).intValue();
            jt_config = new JumpTableConfig(jt_direct_step, jt_indirect_step);
        } catch(ParseException e) {
            throw new IndexInfoFormatError("failed to parse index info file: " + e);
        }
    }

    public int GetNumberOfDocs() {
        return ndocs;
    }
    public EncodersFactory.EncodingMethods GetEncodingMethod() {
        return encoder;
    }
    public JumpTableConfig GetJumpTableConfig() { return jt_config; }
}
