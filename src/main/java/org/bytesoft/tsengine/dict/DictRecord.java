package org.bytesoft.tsengine.dict;


final public class DictRecord implements Comparable<DictRecord> {
    public int offset;
    public int  size;

    static public final int SIZE = 4 + 4;

    public DictRecord(int offset, int size) {
        this.offset = offset;
        this.size = size;
    }

    @Override
    public boolean equals(Object obj) {
        DictRecord o = (DictRecord)obj;
        return offset == o.offset && size == o.size;
    }

    @Override
    public String toString() {
        return "DictRecord(offset=" + offset + ", size=" + size + ")";
    }

    @Override
    public int compareTo(DictRecord o) {
        return (offset < o.offset) ? -1 : (offset > o.offset ? 1 : 0);
    }
}
