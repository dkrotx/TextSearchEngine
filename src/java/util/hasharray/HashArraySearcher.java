package util.hasharray;

import java.io.IOException;
import java.io.RandomAccessFile;

final public class HashArraySearcher<K extends Number, V extends BinaryWritable> {
    RandomAccessFile file;
    BucketInfo[] buckets;
    NumericReader keyReader;
    long content_offset;

    static private class BucketInfo {
        public int size;
        public long offset;
    }

    private interface NumericReader {
        Number read(RandomAccessFile f) throws IOException;
    }

    private void readBuckets() throws IOException {
        int nbuckets = file.readInt();
        int cur_offset = 0;

        buckets = new BucketInfo[nbuckets];
        for (int i = 0; i < nbuckets; i++) {
            buckets[i].size = file.readInt();
            buckets[i].offset = cur_offset;

            cur_offset += buckets[i].size;
        }
    }

    private NumericReader makeKeyReader(Class<K> cls) {
        if (cls.isInstance(Long.TYPE)) {
            return new NumericReader() {
                public final Number read(RandomAccessFile f) throws IOException {
                    return f.readLong();
                }
            };
        }

        throw new RuntimeException("HashArraySearcher doesn't support " +
                cls.getCanonicalName() +
                " as key");
    }

    public HashArraySearcher(RandomAccessFile file, Class<K> keyClass) throws IOException {
        this.file = file;
        keyReader = makeKeyReader(keyClass);
        readBuckets();
        content_offset = file.getFilePointer();
    }

    public boolean bsearchInBucket(int bucket, K key, V res) throws IOException {
        file.seek(content_offset + res.SizeBytes());
    }

    public boolean getFirst(K key, V res) throws IOException {
        int bucket = key.intValue() % buckets.length;
        return bsearchInBucket(bucket, key, res);
    }
}
