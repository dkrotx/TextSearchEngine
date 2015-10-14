package org.bytesoft.tsengine.dict;


import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

final public class DictionaryWriter {
    ArrayList<KVPair>[] buckets;

    static private final class KVPair implements Comparable<KVPair>  {
        public long key;
        public DictRecord value;

        public KVPair(long key, DictRecord value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(KVPair o) {
            // stabilize sort by `value`
            return key < o.key ? -1 : (key > o.key ? 1 : value.compareTo(o.value));
        }
    }

    public DictionaryWriter(int nelems) {
        int nbuckets = Math.max(nelems * DictRecord.SIZE / 4096, 1);
        buckets = new ArrayList[nbuckets];

        for(int i = 0; i < nbuckets; i++)
            buckets[i] = new ArrayList<KVPair>();
    }

    public void Add(long key, DictRecord entry) {
        int bucket = (int)(Math.abs(key) % buckets.length);
        buckets[bucket].add( new KVPair(key, entry) );
    }

    private void writeBucketsAddrs(DataOutputStream out) throws IOException {
        out.writeInt(buckets.length);
        for(ArrayList<KVPair> b: buckets) {
            out.writeInt(b.size());
        }
    }

    private void writeSingleBucket(ArrayList<KVPair> bucket,
                                   DataOutputStream out) throws IOException
    {
        for(KVPair kv: bucket) {
            out.writeLong(kv.key);
            out.writeLong(kv.value.offset);
            out.writeInt(kv.value.size);
        }
    }

    private void writeBucketsContent(DataOutputStream out) throws IOException {
        for(ArrayList<KVPair> b: buckets) {
            writeSingleBucket(b, out);
        }
    }

    private void sortBuckets() {
        for(ArrayList<KVPair> b: buckets) {
            Collections.sort(b);
        }
    }

    public void Write(DataOutputStream out) throws IOException {
        sortBuckets();
        writeBucketsAddrs(out);
        writeBucketsContent(out);
    }
}
