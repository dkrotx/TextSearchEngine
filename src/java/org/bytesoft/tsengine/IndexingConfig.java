package org.bytesoft.tsengine;

import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * IndexingConfig - container for indexing configuration
 */
public class IndexingConfig {

    private String directory;
    private Path rindex_path;
    private Path rindex_cat_path;
    private Path dict_path;
    private Path urls_path;
    private Path urls_idx_path;
    private Path info_path;

    private int jt_direct_step = Integer.MAX_VALUE;
    private int jt_indirect_step = Integer.MAX_VALUE;

    int lem_cache_capacity = 100000;

    // indexing config
    private EncodersFactory.EncodingMethods encoder;
    private long max_membuf;

    public static class BadConfigFormat extends Exception {
        public BadConfigFormat(String msg) { super(msg); }
    }

    private static Object getNecessaryField(JSONObject parent, String field) throws BadConfigFormat {
        Object ret = parent.get(field);

        if (ret == null)
            throw new BadConfigFormat("Can't find necessary field " + field);

        return ret;
    }


    private void readConfigFile(String path) throws IOException, BadConfigFormat {
        JSONObject root;

        try {
            Path file = Paths.get(path);
            String content = new String( Files.readAllBytes(file) );

            JSONParser parser = new JSONParser();
            root = (JSONObject) parser.parse(content);
        } catch (ParseException e) {
            BadConfigFormat new_e = new BadConfigFormat("Failed to parse config file: " + e);
            new_e.addSuppressed(e);
            throw new_e;
        }

        directory = (String)getNecessaryField(root, "directory");

        JSONObject idx_section = (JSONObject)root.get("indexer");
        if (idx_section == null)
            throw new BadConfigFormat("indexer is necessary section");

        max_membuf = (Long)getNecessaryField(idx_section, "mem_size_mb");
        max_membuf *= (1 << 20);

        lem_cache_capacity = ((Long)idx_section.getOrDefault("lem_cache_capacity", new Long(lem_cache_capacity))).intValue();

        String encoder_name = (String)getNecessaryField(idx_section, "encoder");
        encoder = EncodersFactory.GetEncoderByName(encoder_name);
        if (encoder == null)
            throw new BadConfigFormat("indexer.encoder \"" + encoder_name + "\" not known");

        JSONObject jt_section = (JSONObject)idx_section.get("jump_tables");
        if (jt_section != null) {
            jt_direct_step = ((Long)getNecessaryField(jt_section, "direct_step")).intValue();
            jt_indirect_step = ((Long)getNecessaryField(jt_section, "indirect_step")).intValue();
        }
    }

    private void makeDirectoryAbsolute(String cfg_file) {
        if (!Paths.get(directory).isAbsolute()) {
            Path cfg_path = Paths.get(cfg_file);
            Path dir_path = cfg_path.resolveSibling(directory);

            directory = dir_path.toAbsolutePath().toString();
        }
    }

    public IndexingConfig(String cfg_file) throws IOException, BadConfigFormat {
        readConfigFile(cfg_file);

        makeDirectoryAbsolute(cfg_file);

        rindex_path = Paths.get(directory, "rindex.bin");
        rindex_cat_path = Paths.get(directory, "rindex.cat");
        dict_path = Paths.get(directory, "rindex.dic");
        urls_path = Paths.get(directory, "urls.txt");
        urls_idx_path = Paths.get(directory, "urls.idx");
        info_path = Paths.get(directory, "index.info");
    }

    public Path GetRindexPath() { return rindex_path; }
    public Path GetRindexCatPath() { return rindex_cat_path; }
    public Path GetRindexDictPath() { return dict_path; }
    public Path GetUrlsPath() { return urls_path; }
    public Path GetUrlsIdxPath() { return urls_idx_path; }
    public Path GetInfoPath() { return info_path; }

    public long GetMaxMemBuf() {
        return max_membuf;
    }

    public EncodersFactory.EncodingMethods GetEncodingMethod() {
        return encoder;
    }

    public int GetLemCacheCapacity() {
        return lem_cache_capacity;
    }

    public int GetJumpTableDirectStep() { return jt_direct_step; }
    public int GetJumpTableIndirectStep() { return jt_indirect_step; }
}
