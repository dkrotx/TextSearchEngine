package org.bytesoft.tsengine;

/**
 * IndexingConfig - container for indexing configuration
 */
public class IndexingConfig {
    private long max_membuf;

    public IndexingConfig() {
        max_membuf = 16* (1<<20);
    }

    public long GetMaxMemBuf() {
        return max_membuf;
    }
}
