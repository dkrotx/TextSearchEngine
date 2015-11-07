import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.idxblock.IdxBlockDecoder;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.bytesoft.tsengine.idxblock.JumpTableConfig;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test index block functionality with jump tables.
 */
public class IdxBlockWithJumpsTest {
    static EncodersFactory factory = new EncodersFactory();

    static {
        factory.SetCurrentEncoder(EncodersFactory.EncodingMethods.ELIAS_GAMMA);
        factory.SetCurrentDecoder(EncodersFactory.EncodingMethods.ELIAS_GAMMA);
    }

    int[] makeRandomDocList(int n, int start) {
        int[] docs = new int[n];
        int prev = start;
        Random rand = new Random();

        docs[0] = start;

        for(int i = 1; i < n; i++) {
            docs[i] = prev + rand.nextInt(10) + 1;
            prev = docs[i];
        }

        return docs;
    }

    private void checkSequentiallyEqual(IdxBlockDecoder dec, int[] orig_docs) {
        for (int i = 0; i < orig_docs.length; i++) {
            assertTrue(dec.HasNext());
            assertEquals("Document #" + i + " not equal to original", orig_docs[i], dec.ReadNext());
        }

        assertFalse(dec.HasNext());
    }

    private void checkIteratorSequentiallyEqual(IdxBlocksIterator it, int[] orig_docs) {
        for (int i = 0; i < orig_docs.length; i++) {
            assertTrue(it.HasNext());
            assertEquals("Document #" + i + " not equal to original", orig_docs[i], it.ReadNext());
        }

        assertFalse(it.HasNext());
    }


    private void checkGoToFunctionality(IdxBlockDecoder dec, int[] orig_docs) {
        int[] targets = {1_000, 10_000, 90_000};

        for (int i = 0; i < targets.length; i++) {
            assertEquals("Document #" + orig_docs[targets[i]] + " exists in doc-list",
                    orig_docs[targets[i]], dec.GotoDocID(orig_docs[targets[i]]));

            assertTrue(dec.HasNext());
            assertEquals("Document #" + orig_docs[targets[i]+1] + " is next after " + orig_docs[targets[i]],
                    orig_docs[targets[i]+1], dec.ReadNext());
        }

        // check boundary case
        assertEquals("Last document should be found", orig_docs[orig_docs.length - 1], dec.GotoDocID(orig_docs[orig_docs.length - 1]));
        assertFalse(dec.HasNext());
        assertEquals("Beyond the end", -1, dec.GotoDocID(orig_docs[orig_docs.length - 1] + 1));
        assertEquals("Beyond the end", -1, dec.GotoDocID(orig_docs[orig_docs.length - 1] + 100500));
        assertFalse(dec.HasNext());
    }

    private void checIteratorkGoToFunctionality(IdxBlocksIterator it, int[] orig_docs) {
        int[] targets = {1_000, 10_000, 90_000};

        for (int i = 0; i < targets.length; i++) {
            assertEquals("Document #" + orig_docs[targets[i]] + " exists in doc-list",
                    orig_docs[targets[i]], it.GotoDocID(orig_docs[targets[i]]));

            assertTrue(it.HasNext());
            assertEquals("Document #" + orig_docs[targets[i]+1] + " is next after " + orig_docs[targets[i]],
                    orig_docs[targets[i]+1], it.ReadNext());
        }

        // check boundary case
        assertEquals("Last document should be found", orig_docs[orig_docs.length - 1], it.GotoDocID(orig_docs[orig_docs.length - 1]));
        assertFalse(it.HasNext());
        assertEquals("Beyond the end", IdxBlocksIterator.OMEGA_ID, it.GotoDocID(orig_docs[orig_docs.length - 1] + 1));
        assertEquals("Beyond the end", IdxBlocksIterator.OMEGA_ID, it.GotoDocID(orig_docs[orig_docs.length - 1] + 100500));
        assertFalse(it.HasNext());
    }

    @Test
    public void TestFunctionality() throws IntCompressor.TooLargeToCompressException {
        int[] docs = makeRandomDocList(100_000, 0);
        JumpTableConfig jt_cfg = new JumpTableConfig(64, 8);

        IdxBlockEncoder enc = new IdxBlockEncoder(factory.MakeEncoder(), jt_cfg);

        for (int i = 0; i < docs.length; i++) {
            enc.AddDocID(docs[i]);
        }

        checkSequentiallyEqual(IdxBlockUtils.getIndexBlockDecoder(enc, factory, jt_cfg), docs);
        checkGoToFunctionality(IdxBlockUtils.getIndexBlockDecoder(enc, factory, jt_cfg), docs);
    }

    @Test
    public void TestIdxBlockIteratorGoTo() throws IntCompressor.TooLargeToCompressException {
        int[] docs1 = makeRandomDocList(50_000, 0);
        int[] docs2 = makeRandomDocList(50_000, docs1[docs1.length - 1] + 1);
        JumpTableConfig jt_cfg = new JumpTableConfig(64, 8);

        IdxBlockEncoder enc1 = new IdxBlockEncoder(factory.MakeEncoder(), jt_cfg);
        IdxBlockEncoder enc2 = new IdxBlockEncoder(factory.MakeEncoder(), jt_cfg);

        for(int d: docs1) {
            enc1.AddDocID(d);
        }
        for(int d: docs2) {
            enc2.AddDocID(d);
        }

        ByteBuffer mem1 = IdxBlockUtils.writeBlockToMemory(enc1);
        ByteBuffer mem2 = IdxBlockUtils.writeBlockToMemory(enc2);

        {
            int[] docs = new int[docs1.length + docs2.length];

            System.arraycopy(docs1, 0, docs, 0, docs1.length);
            System.arraycopy(docs2, 0, docs, docs1.length, docs2.length);

            checkIteratorSequentiallyEqual(new IdxBlocksIterator(new ByteBuffer[] { mem1.slice(), mem2.slice() },
                    factory, jt_cfg), docs);
            checIteratorkGoToFunctionality(new IdxBlocksIterator(new ByteBuffer[] { mem1.slice(), mem2.slice() },
                    factory, jt_cfg), docs);
        }
    }
}
