package org.bytesoft.tsengine.dict;

/**
 * catalog record - address of rindex' entry
 */
public class CatalogRecord {
    public long word_hash;
    public int size;

    public static int SIZE = Long.SIZE + Integer.SIZE;

    public CatalogRecord(long word_hash, int size) {
        this.word_hash = word_hash;
        this.size = size;
    }
}
