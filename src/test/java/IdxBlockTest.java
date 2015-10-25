import org.bytesoft.tsengine.encoders.VarByteEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlockDecoder;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;


/**
 * Test if IdxBlockEncoder/Decoder and Iterator
 */
public class IdxBlockTest {
    @Test
    public void TestIndexBlock() throws Exception {
        IdxBlockEncoder enc = new IdxBlockEncoder(new VarByteEncoder());
        int[] postings = new int[] { 1, 55, 3733, 4000 };

        IdxBlockDecoder dec = new IdxBlockDecoder( IdxBlockUtils.makeIndexBlock(postings) );
        assertEquals(postings.length, dec.GetDocsAmount());

        int[] decoded  = new int[postings.length];

        for(int i = 0; i < decoded.length; i++) {
            assertTrue("decoding not finished yet", dec.HasNext());
            decoded[i] = dec.ReadNext();
            assertEquals(decoded[i], dec.GetCurrentDocID());
        }

        assertArrayEquals("decoded should match original", postings, decoded);
        assertFalse("decoding finished", dec.HasNext());
    }


    @Test
    public void TestIdxBlockSet() throws Exception {
        Random rand = new Random();

        final int nblocks = rand.nextInt(10) + 5;
        int[][] content = new int[nblocks][];
        int prev_docid = 0;
        ArrayList<Integer> all_postings = new ArrayList<>();

        // make random posting lists from increasing docIDs
        for(int i = 0; i < nblocks; i++) {
            int nd = rand.nextInt(10) + 10;
            content[i] = new int[nd];
            for (int j = 0; j < nd; j++) {
                content[i][j] = prev_docid + rand.nextInt(1000000) + 1;
                all_postings.add(content[i][j]);
                prev_docid = content[i][j];
            }
        }

        // encode this random postings
        ByteBuffer[] bufs = new ByteBuffer[nblocks];

        for (int i = 0; i < content.length; i++) {
            bufs[i] = IdxBlockUtils.makeIndexBlock(content[i]);
        }

        // walk through index blocks via iterator
        IdxBlocksIterator it = new IdxBlocksIterator(bufs);
        assertEquals(all_postings.size(), it.GetDocsAmount());
        assertEquals(IdxBlocksIterator.ALPHA_ID, it.GetCurrentDocID());

        ArrayList<Integer> decoded = new ArrayList<>();

        for (int i = 0; i < all_postings.size(); i++) {
            assertTrue("decoding not finished yet", it.HasNext());
            decoded.add(it.ReadNext());
            assertEquals("GetCurrentDocID() should be equal to last decoded docid",
                    (int)decoded.get(decoded.size() - 1), it.GetCurrentDocID());
        }

        assertArrayEquals("decoded should match original",
                all_postings.toArray(), decoded.toArray());

        assertFalse("decoding finished", it.HasNext());

        // going beyond the end of block
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.ReadNext());
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.GetCurrentDocID());

        // check what one more ReadNext() won't throw, and won't change the situation
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.ReadNext());
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.GetCurrentDocID());
        assertFalse(it.HasNext());
    }

    @Test
    public void TestGotoIdFunctionality() throws Exception {
        IdxBlocksIterator it = new IdxBlocksIterator(new ByteBuffer[] {
                IdxBlockUtils.makeIndexBlockV(1, 3, 5, 7, 8, 9),
                IdxBlockUtils.makeIndexBlockV(10)
        });

        assertEquals("0 does not exists, but 1 OK", 1, it.GotoDocID(0));
        assertEquals(5, it.GotoDocID(5));
        assertEquals("Second GotoID(same) shouldn't do anything", 5, it.GotoDocID(5));

        assertEquals("Can't go backward", 5, it.GotoDocID(3));
        assertEquals("Check reading after GotoDocID", 7, it.ReadNext());
        assertEquals(8, it.ReadNext());

        assertEquals(10, it.GotoDocID(10));
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.GotoDocID(11));

        assertFalse(it.HasNext());
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.ReadNext());
    }

    @Test
    public void TestGotoIdFunctionality2() throws Exception {
        IdxBlocksIterator it = new IdxBlocksIterator(new ByteBuffer[] {
                IdxBlockUtils.makeIndexBlockV(1, 3, 5, 7),
                IdxBlockUtils.makeIndexBlockV(10, 11),
                IdxBlockUtils.makeIndexBlockV(55, 70),
        });

        assertEquals("100 is greater than any", IdxBlocksIterator.OMEGA_ID, it.GotoDocID(100));

        assertFalse(it.HasNext());
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.ReadNext());
    }

    @Test
    public void TestGotoIdFunctionality3() throws Exception {
        IdxBlocksIterator it = new IdxBlocksIterator(new ByteBuffer[] {
                IdxBlockUtils.makeIndexBlockV(1, 3, 5, 7),
                IdxBlockUtils.makeIndexBlockV(10, 11)
        });

        assertEquals("Going to OMEGA_ID should be OK",
                IdxBlocksIterator.OMEGA_ID, it.GotoDocID(IdxBlocksIterator.OMEGA_ID));

        assertFalse(it.HasNext());
        assertEquals(IdxBlocksIterator.OMEGA_ID, it.ReadNext());
    }
}
