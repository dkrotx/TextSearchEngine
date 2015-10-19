import org.bytesoft.tsengine.encoders.TooLargeToCompressException;
import org.bytesoft.tsengine.idxblock.IdxBlockDecoder;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.junit.*;
import static org.junit.Assert.*;


import org.bytesoft.tsengine.encoders.DeltaIntEncoder;
import org.bytesoft.tsengine.encoders.VarByteEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;


/**
 * Test if IdxBlockEncoder/Decoder and Iterator
 */
public class IdxBlockTest {
    @Test
    public void TestIndexBlock() throws IOException, TooLargeToCompressException {
        IdxBlockEncoder enc = new IdxBlockEncoder(new DeltaIntEncoder(new VarByteEncoder()));
        int[] postings = new int[] { 1, 55, 3733, 4000 };
        int[] decoded  = new int[postings.length];

        for(int id: postings) {
            enc.AddDocID(id);
        }

        ByteArrayOutputStream membuf = new ByteArrayOutputStream();
        enc.Write(new DataOutputStream(membuf));

        IdxBlockDecoder dec = new IdxBlockDecoder( ByteBuffer.wrap(membuf.toByteArray()) );

        assertEquals(postings.length, dec.GetDocsAmount());
        for(int i = 0; i < decoded.length; i++) {
            assertTrue("decoding not finished yet", dec.HasNext());
            decoded[i] = dec.ReadNext();
            assertEquals(decoded[i], dec.GetCurrentDocID());
        }

        assertArrayEquals("decoded should match original", postings, decoded);
        assertFalse("decoding finished", dec.HasNext());
    }

    private ByteBuffer writeIndexBlock(int[] postings) throws TooLargeToCompressException, IOException {
        IdxBlockEncoder enc = new IdxBlockEncoder(new DeltaIntEncoder(new VarByteEncoder()));

        for(int id: postings) {
            enc.AddDocID(id);
        }

        ByteArrayOutputStream membuf = new ByteArrayOutputStream();
        enc.Write(new DataOutputStream(membuf));

        return ByteBuffer.wrap(membuf.toByteArray());
    }

    @Test
    public void TestIdxBlockSet() throws TooLargeToCompressException, IOException {
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
            bufs[i] = writeIndexBlock(content[i]);
        }

        // walk through index blocks via iterator
        IdxBlocksIterator it = new IdxBlocksIterator(bufs);
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
    }
}
