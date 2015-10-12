package org.bytesoft.tsengine.encoders;


public class DeltaIntEncoder implements IntCompressor {
    IntCompressor comp;
    int prev_docid = 0;

    public DeltaIntEncoder(IntCompressor comp) {
        this.comp = comp;
    }

    @Override
    public void AddNumber(int x) throws TooLargeToCompressException {
        comp.AddNumber(x - prev_docid);
        prev_docid = x;
    }

    @Override
    public byte[] GetBytes() {
        return comp.GetBytes();
    }

    @Override
    public int GetStoreSize() {
        return comp.GetStoreSize();
    }
}
