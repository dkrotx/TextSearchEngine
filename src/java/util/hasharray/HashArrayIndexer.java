package util.hasharray;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public final class HashArrayIndexer<K extends Number, V extends BinaryWritable> {
    private ArrayList< KVPair<K, V> >[] buckets;
    private NumericWriter keyWriter = null;

    interface NumericWriter {
        void write(DataOutputStream out, Number x) throws IOException;
    }


    private NumericWriter getAppropriateKeyWriter(Class<K> keyClass) {
        if (keyClass.isInstance(Long.TYPE)) {
            return new NumericWriter() {
                public final void write(DataOutputStream out, Number x) throws IOException {
                    out.writeLong(x.longValue());
                }
            };
        }

        throw new RuntimeException("HashArrayIndexer doesn't support " +
                keyClass.getCanonicalName() +
                " as key");
    }

    private static class KVPair<K extends Number, V> implements Comparable< KVPair<K, V> > {
        public K key;
        public V value;

        KVPair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public int compareTo(KVPair<K, V> obj) {
            long long_a = key.longValue();
            long long_b = obj.key.longValue();

            if (long_a < long_b) return -1;
            if (long_b > long_a) return +1;
            return 0;
        }
    }

    public HashArrayIndexer(int nelems, Class<K> keyClass) {
        int nbuckets = Math.max(nelems / 1024, 1);
        buckets = new ArrayList[nbuckets];

        keyWriter = getAppropriateKeyWriter(keyClass);
    }

    public void add(K key, V value) {
        final int bucket = key.intValue() % buckets.length;
        buckets[bucket].add(new KVPair(key, value));
    }

    /**
     * write HashArray to file
     * @param out output stream of bytes
     * @throws IOException
     */
    public void write(DataOutputStream out) throws IOException {
        sortBuckets();

        writeHeader(out);
        writeBucketAddrs(out);
        writeBucketContent(out);
    }

    private void writeHeader(DataOutputStream out) throws IOException {
        out.writeInt(buckets.length);
    }

    private void writeBucketAddrs(DataOutputStream out) throws IOException {
        for(ArrayList< KVPair<K, V> > b: buckets) {
            out.writeInt(b.size());
        }
    }

    private void writeSingleBucket(DataOutputStream out,
                                   ArrayList< KVPair<K, V> > bucket) throws IOException
    {
        for (KVPair<K, V> item: bucket) {
            keyWriter.write(out, item.key);
            item.value.write(out);
        }
    }


    private void writeBucketContent(DataOutputStream out) throws IOException {
        for(ArrayList< KVPair<K, V> > b: buckets) {
            writeSingleBucket(out, b);
        }
    }

    private void sortBuckets() {
        for(ArrayList< KVPair<K, V> > b: buckets) {
            Collections.sort(b);
        }
    }
}
