package org.bytesoft.tsengine.dict;


import java.io.IOException;
import java.io.RandomAccessFile;

public final class DictionarySearcher {
    private RandomAccessFile file;
    BucketInfo[] buckets;
    final static private int KVSIZE = 8 + DictRecord.SIZE;

    private static final class BucketInfo {
        public long offset;
        public int  nelems;
    }

    private void readBucketsAddrs() throws IOException {
        int nbuckets = file.readInt();
        buckets = new BucketInfo[nbuckets];

        for (int i = 0; i < nbuckets; i++) {
            buckets[i] = new BucketInfo();
            buckets[i].nelems = file.readInt();
        }

        long offs = file.getFilePointer();

        for(BucketInfo b: buckets) {
            b.offset = offs;
            offs += b.nelems * KVSIZE;
        }
    }

    public DictionarySearcher(RandomAccessFile file) throws IOException {
        this.file = file;
        readBucketsAddrs();
    }

    public DictRecord GetFirst(long key) throws IOException {
        return bsearchSingle(key, bucketByKey(key));
    }

    public DictRecord[] GetAll(long key) throws IOException {
        return bsearchMulty(key, bucketByKey(key));
    }

    public boolean Exists(long key) throws IOException {
        return bsearchCheckExists(key, bucketByKey(key));
    }

    private BucketInfo bucketByKey(long key) {
        return buckets[(int)(key % buckets.length)];
    }

    private DictRecord bsearchSingle(long key, BucketInfo bucket) throws IOException {
        int low = 0;
        int high = bucket.nelems;

        while (low < high) {
            int i = (low + high) / 2;

            file.seek(bucket.offset + i*KVSIZE);
            long k = file.readLong();

            if (key < k)
                high = i;
            else if (key > k)
                low = i + 1;
            else {
                return new DictRecord(file.readLong(), file.readInt());
            }
        }

        return null;
    }

    private DictRecord[] bsearchMulty(long key, BucketInfo bucket) throws IOException {
        int low = 0;
        int high = bucket.nelems;

        while (low < high) {
            int i = (low + high) / 2;

            file.seek(bucket.offset + i*KVSIZE);
            long k = file.readLong();

            if (key < k)
                high = i;
            else if (key > k)
                low = i + 1;
            else {
                int lbound, rbound;

                for (lbound = i - 1; lbound >= 0; lbound--) {
                    file.seek(bucket.offset + lbound*KVSIZE);
                    if (file.readLong() != key)
                        break;
                }


                for (rbound = i + 1; rbound < bucket.nelems; rbound++) {
                    file.seek(bucket.offset + rbound*KVSIZE);
                    if (file.readLong() != key)
                        break;
                }

                final int key_size = Long.SIZE / Byte.SIZE;

                DictRecord[] res = new DictRecord[rbound - (lbound + 1)];
                int out_index = 0;

                for (i = lbound + 1; i < rbound; i++) {
                    file.seek(bucket.offset + i*KVSIZE + key_size);
                    res[out_index++] = new DictRecord(file.readLong(), file.readInt());
                }

                return res;
            }
        }

        return null;
    }

    private boolean bsearchCheckExists(long key, BucketInfo bucket) throws IOException {
        int low = 0;
        int high = bucket.nelems;

        while (low < high) {
            int i = (low + high) / 2;

            file.seek(bucket.offset + i*KVSIZE);
            long k = file.readLong();

            if (key < k)
                high = i;
            else if (key > k)
                low = i + 1;
            else
                return true;
        }

        return false;
    }


}
