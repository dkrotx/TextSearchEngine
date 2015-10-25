package org.bytesoft.tsengine;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * IndexingConfig - container for indexing configuration
 */
public class IndexingConfig {
    private long max_membuf;

    private Path rindex_path;
    private Path rindex_cat_path;
    private Path rindex_dict_path;
    private Path rindex_urls_path;
    private Path rindex_urls_idx_path;

    public IndexingConfig(String directory) {
        max_membuf = 512*(1<<20);

        rindex_path = Paths.get(directory, "rindex.bin");
        rindex_cat_path = Paths.get(directory, "rindex.cat");
        rindex_dict_path = Paths.get(directory, "rindex.dic");
        rindex_urls_path = Paths.get(directory, "urls.txt");
        rindex_urls_idx_path = Paths.get(directory, "urls.idx");

    }

    public Path GetRindexPath() { return rindex_path; }
    public Path GetRindexCatPath() { return rindex_cat_path; }
    public Path GetRindexDictPath() { return rindex_dict_path; }
    public Path GetUrlsPath() { return rindex_urls_path; }
    public Path GetUrlsIdxPath() { return rindex_urls_idx_path; }

    public long GetMaxMemBuf() {
        return max_membuf;
    }
}
