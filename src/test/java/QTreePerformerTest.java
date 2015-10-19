import org.bytesoft.tsengine.QTreePerformer;
import org.bytesoft.tsengine.encoders.TooLargeToCompressException;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.bytesoft.tsengine.qparse.ExprToken;
import org.bytesoft.tsengine.qparse.ExpressionTokenizer;
import org.bytesoft.tsengine.qparse.QueryTreeBuilder;
import org.bytesoft.tsengine.qparse.QueryTreeNode;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Test QTreePerformer functionality:
 * - create different index blocks for words
 * - create QTreePerformer from query tree and this blocks
 * - get documents from query tree
 */
public class QTreePerformerTest {


    class IdxBlockStorage implements QTreePerformer.Word2BlockConverter {
        TreeMap<String, ArrayList<ByteBuffer>> word2bufs = new TreeMap<>();

        @Override
        public IdxBlocksIterator GetIndexBlockIterator(String word) {
            ArrayList<ByteBuffer> bufs_orig = word2bufs.get(word);

            if (bufs_orig == null)
                return null;

            ByteBuffer[] bufs_copy = new ByteBuffer[bufs_orig.size()];
            for (int i = 0; i < bufs_orig.size(); i++)
                bufs_copy[i] = bufs_orig.get(i).slice();

            return new IdxBlocksIterator(bufs_copy);
        }

        public IdxBlockStorage
        AddWord(String word, int ... posting) throws TooLargeToCompressException, IOException {
            ArrayList<ByteBuffer> bufs = word2bufs.get(word);

            if (bufs == null) {
                bufs = new ArrayList<>();
                word2bufs.put(word, bufs);
            }

            bufs.add(IdxBlockUtils.makeIndexBlock(posting));
            return this;
        }
    }

    QueryTreeNode parseQuery(String query) throws ParseException {
        ExprToken[] tokens = ExpressionTokenizer.Tokenize(query);
        return QueryTreeBuilder.BuildExpressionTree(tokens);
    }

    QTreePerformer makeQTreePerformer(String query, IdxBlockStorage storage) throws Exception {
        return new QTreePerformer(parseQuery(query), storage);
    }

    void checkPerformerFinished(QTreePerformer performer) {
        assertEquals(IdxBlocksIterator.OMEGA_ID, performer.GetNextDocument());
        assertTrue(performer.Finished());
    }

    int[] arrayListToPrimitives(ArrayList<? extends Integer> list) {
        int[] res = new int[list.size()];
        int i = 0;

        for (int x: list) {
            res[i++] = x;
        }

        return res;
    }

    void assertPerformerResultEquals(QTreePerformer performer, int[] nums) {
        ArrayList<Integer> res = new ArrayList<>();

        while(!performer.Finished()) {
            int id = performer.GetNextDocument();
            if (id != IdxBlocksIterator.OMEGA_ID)
                res.add(id);
        }

        assertArrayEquals("Extracted numbers from performer have to match", nums, arrayListToPrimitives(res));
    }

    void assertPerformerResultEqualsV(QTreePerformer performer, int ... nums) {
        assertPerformerResultEquals(performer, nums);
    }

    @Test
    public void TestSingleTerm() throws Exception {
        QTreePerformer performer = makeQTreePerformer("dog", new IdxBlockStorage().AddWord("dog", 1, 3, 5, 6));

        assertFalse(performer.Finished());

        assertEquals(1, performer.GetNextDocument());
        assertEquals(3, performer.GetNextDocument());
        assertEquals(5, performer.GetNextDocument());
        assertEquals(6, performer.GetNextDocument());

        checkPerformerFinished(performer);
    }

    @Test
    public void TestSingleConjunction() throws Exception {
        QTreePerformer performer = makeQTreePerformer("dog & cat", new IdxBlockStorage()
                .AddWord("dog", 1, 3, 5, 6, 7, 8)
                .AddWord("cat", 1, 7)
        );

        assertFalse(performer.Finished());

        assertEquals(1, performer.GetNextDocument());
        assertEquals(7, performer.GetNextDocument());

        checkPerformerFinished(performer);
    }

    @Test
    public void TestMultipleConjunction() throws Exception {
        QTreePerformer performer = makeQTreePerformer("dog & cat & fish & turtle", new IdxBlockStorage()
                        .AddWord("dog", 1, 3, 5, 6, 7, 8, 14)
                        .AddWord("cat", 1, 5, 6, 8, 12)
                        .AddWord("fish", 3, 6, 7, 8, 10)
                        .AddWord("turtle", 5, 6, 7, 8, 9)
        );

        assertFalse(performer.Finished());

        assertEquals(6, performer.GetNextDocument());
        assertEquals(8, performer.GetNextDocument());

        checkPerformerFinished(performer);
    }

    @Test
    // If any node in conjunction doesn't exists, there should be empty set in output
    public void TestNullConjunction() throws Exception {
        QTreePerformer performer = makeQTreePerformer("dog & cat & fish", new IdxBlockStorage()
                        .AddWord("dog", 1, 3, 5, 6, 7, 8, 14)
                        .AddWord("cat", 1, 5, 6, 8, 12)
        );

        assertFalse("QTreePerformer can't be finished before any extraction", performer.Finished());
        checkPerformerFinished(performer);
    }

    @Test
    public void TestSingleDisjunction() throws Exception {
        QTreePerformer performer = makeQTreePerformer("dog | cat", new IdxBlockStorage()
                        .AddWord("dog", 1, 3, 5, 6)
                        .AddWord("cat", 2, 5, 6, 8, 12)
        );

        assertPerformerResultEqualsV(performer, 1, 2, 3, 5, 6, 8, 12);
        checkPerformerFinished(performer);
    }

    @Test
    // If doesn't matter if any node in disjunction doesn't exists
    public void TestNullDisjunction() throws Exception {
        QTreePerformer performer = makeQTreePerformer("dog | fish | cat", new IdxBlockStorage()
                        .AddWord("dog", 1, 3, 5, 6)
                        .AddWord("cat", 2, 5, 6, 8, 12)
        );

        assertPerformerResultEqualsV(performer, 1, 2, 3, 5, 6, 8, 12);
        checkPerformerFinished(performer);
    }

    @Test
    // Negation is virtual source of documents. And theoretically infinite.
    public void TestNegation() throws Exception {
        QTreePerformer performer = makeQTreePerformer("!dog", new IdxBlockStorage()
                        .AddWord("dog", 1, 3, 5, 8)
        );

        assertFalse(performer.Finished());

        assertEquals(0, performer.GetNextDocument());
        assertEquals(2, performer.GetNextDocument());
        assertEquals(4, performer.GetNextDocument());
        assertEquals(6, performer.GetNextDocument());
        assertEquals(7, performer.GetNextDocument());

        // test beyond posting list
        assertEquals(9, performer.GetNextDocument());

        // and far beyond ...
        for(int i = 0; i < 100; i++) {
            assertEquals(i + 10, performer.GetNextDocument());
            assertFalse("Negation is always infinite", performer.Finished());
        }
    }

    @Test
    // this sample starts from "0"
    public void TestNegation2() throws Exception {
        QTreePerformer performer = makeQTreePerformer("!dog", new IdxBlockStorage()
                        .AddWord("dog", 0, 1, 2, 3, 5, 7)
        );

        assertFalse(performer.Finished());

        assertEquals(4, performer.GetNextDocument());
        assertEquals(6, performer.GetNextDocument());


        // test beyond posting list
        assertEquals(8, performer.GetNextDocument());
    }
}
