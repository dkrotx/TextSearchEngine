package org.bytesoft.tsengine;

import static util.hash.MurmurHash3.murmurhash3_x64;

/**
 * Static utils for
 */
public final class WordUtils {
    private WordUtils() {}

    static public String NormalizeWord(String word) {
        return word.toLowerCase();
    }

    static public long GetWordHash(String word) {
        return murmurhash3_x64(word);
    }
}
