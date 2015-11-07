import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.encoders.VarByteDecoder;
import org.bytesoft.tsengine.encoders.VarByteEncoder;
import org.bytesoft.tsengine.idxblock.JumpTableBuilder;
import org.bytesoft.tsengine.idxblock.JumpTableConfig;
import org.bytesoft.tsengine.idxblock.JumpTableNavigator;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Test creation of Jump Tables and lookup over them
 */
public class JumpTableTest {
    int log_base(int value, int base) {
        return (int)Math.ceil(Math.log(value) / Math.log(base));
    }

    JumpTableNavigator makeNavigatorFromBuilder(JumpTableBuilder builder) throws IOException {
        ByteArrayOutputStream dynmem = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(dynmem);
        int nw = builder.write(out);

        assertEquals("Number of bytes written to dynamic memory != reported", dynmem.size(), nw);

        ByteBuffer res_mem = ByteBuffer.wrap(dynmem.toByteArray());
        return new JumpTableNavigator(res_mem);
    }

    @Test
    public void TestJumpTable() throws IntCompressor.TooLargeToCompressException,
            JumpTableBuilder.TooLargeBlockToJump,
            IOException
    {
        final int ndocs = 100000;
        int[] saved_offsets = new int[ndocs];
        VarByteEncoder enc = new VarByteEncoder();
        JumpTableConfig jt_cfg = new JumpTableConfig(2, 8);
        JumpTableBuilder builder = new JumpTableBuilder(jt_cfg);

        assertEquals("No levels at the beginning", 0, builder.getNumberOfLevels());
        int cur_offset = -1; // too low

        // We will fill jump tables in following way:
        // - each 2nd value goes to builder
        // - each 8th value of each jump table level makes new jump table
        for(int i = 0; i < ndocs; i++) {
            if (i != 0 && (i % jt_cfg.rindex_step) == 0) {
                cur_offset = enc.GetStoreSize();
                builder.addEntry(i, i, cur_offset);
            }

            saved_offsets[i] = cur_offset;
            enc.AddNumber(i);
        }

        final int expected_levels = log_base(ndocs / jt_cfg.rindex_step, jt_cfg.jump_table_step);
        assertEquals(expected_levels, builder.getNumberOfLevels());

        JumpTableNavigator nav = makeNavigatorFromBuilder(builder);
        assertEquals(expected_levels, nav.getNumberOfLevels());

        VarByteDecoder dec = new VarByteDecoder(ByteBuffer.wrap(enc.GetBytes()));

        JumpTableNavigator.JumpRequest req = new JumpTableNavigator.JumpRequest();

        {
            req.doc_id = 500;
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[500], req.offset);
            assertEquals(500, req.doc_no);
            assertEquals(500, req.doc_id);

            dec.position(req.offset);
            assertEquals("500 is even number, so we have exact offset to it", 500, dec.ExtractNumber());
        }

        {
            req.doc_id = 801;
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[801], req.offset);
            assertEquals(800, req.doc_no);
            assertEquals(800, req.doc_id);

            dec.position(req.offset);
            assertEquals("801 is odd number, so we have exact offset to 800", 800, dec.ExtractNumber());
            assertEquals(801, dec.ExtractNumber());
        }

        // try bigger value
        {
            req.doc_id = 80001;
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[80001], req.offset);
            assertEquals(80000, req.doc_no);
            assertEquals(80000, req.doc_id);

            dec.position(req.offset);
            assertEquals("80001 is odd number, so we have exact offset to 800", 80000, dec.ExtractNumber());
            assertEquals(80001, dec.ExtractNumber());
        }
    }

    @Test
    public void TestBoundaryCases() throws IntCompressor.TooLargeToCompressException,
            JumpTableBuilder.TooLargeBlockToJump,
            IOException
    {
        final int ndocs = 100000;
        int[] saved_offsets = new int[ndocs];
        VarByteEncoder enc = new VarByteEncoder();
        JumpTableConfig jt_cfg = new JumpTableConfig(128, 8);
        JumpTableBuilder builder = new JumpTableBuilder(jt_cfg);

        int cur_offset = -1; // too low

        // We will fill jump tables in following way:
        // - each 128 th value goes to builder
        // - each 8th value of each jump table level makes new jump table
        for(int i = 0; i < ndocs; i++) {
            if (i != 0 && (i % jt_cfg.rindex_step) == 0) {
                cur_offset = enc.GetStoreSize();
                builder.addEntry(i, i, cur_offset);
            }

            saved_offsets[i] = cur_offset;
            enc.AddNumber(i);
        }

        final int expected_levels = log_base(ndocs / jt_cfg.rindex_step, jt_cfg.jump_table_step);
        assertEquals(expected_levels, builder.getNumberOfLevels());

        JumpTableNavigator nav = makeNavigatorFromBuilder(builder);
        assertEquals(expected_levels, nav.getNumberOfLevels());

        {
            assertFalse("Too small for any JT", nav.findNearestOffset(new JumpTableNavigator.JumpRequest(0)));
            assertFalse("Too small for any JT", nav.findNearestOffset(new JumpTableNavigator.JumpRequest(100)));
            assertFalse("Too small for any JT", nav.findNearestOffset(new JumpTableNavigator.JumpRequest(127)));

            JumpTableNavigator.JumpRequest req = new JumpTableNavigator.JumpRequest(128);
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[128], req.offset);
        }

        {
            JumpTableNavigator.JumpRequest req = new JumpTableNavigator.JumpRequest(ndocs - 1);

            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[ndocs - 1], req.offset);

            req.doc_id = ndocs + 1;
            assertTrue(nav.findNearestOffset(req));
            assertEquals("Beyond saved offsets", saved_offsets[ndocs - 1], req.offset);

            req.doc_id = ndocs + 100500;
            assertTrue(nav.findNearestOffset(req));
            assertEquals("Beyond saved offsets", saved_offsets[ndocs - 1], req.offset);
        }
    }

    @Test
    public void TestMovesOnlyForward() throws IntCompressor.TooLargeToCompressException,
            JumpTableBuilder.TooLargeBlockToJump,
            IOException
    {
        final int ndocs = 10000;
        int[] saved_offsets = new int[ndocs];
        VarByteEncoder enc = new VarByteEncoder();
        JumpTableConfig jt_cfg = new JumpTableConfig(2, 8);
        JumpTableBuilder builder = new JumpTableBuilder(jt_cfg);

        int cur_offset = -1; // too low

        // We will fill jump tables in following way:
        // - each 2nd value goes to builder
        // - each 8th value of each jump table level makes new jump table
        for(int i = 0; i < ndocs; i++) {
            if (i != 0 && (i % jt_cfg.rindex_step) == 0) {
                cur_offset = enc.GetStoreSize();
                builder.addEntry(i, i, cur_offset);
            }

            saved_offsets[i] = cur_offset;
            enc.AddNumber(i);
        }

        final int expected_levels = log_base(ndocs / jt_cfg.rindex_step, jt_cfg.jump_table_step);
        assertEquals(expected_levels, builder.getNumberOfLevels());

        JumpTableNavigator nav = makeNavigatorFromBuilder(builder);
        assertEquals(expected_levels, nav.getNumberOfLevels());

        JumpTableNavigator.JumpRequest req = new JumpTableNavigator.JumpRequest();
        {
            req.doc_id = 500;
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[500], req.offset);
            assertEquals(500, req.doc_id);
            assertEquals(500, req.doc_no);
        }

        {
            req.doc_id = 100;
            assertFalse("JumpTableNavigator shouldn't move back", nav.findNearestOffset(req));
        }

        // btw, setting to same value one more time should not change anything
        {
            req.doc_id = 500;
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[500], req.offset);
            assertEquals(500, req.doc_id);
            assertEquals(500, req.doc_no);
        }

        // Moving within same jump table entry should not change anything too
        {
            req.doc_id = 501;
            assertTrue(nav.findNearestOffset(req));
            assertEquals(saved_offsets[500], req.offset);
            assertEquals(500, req.doc_id);
            assertEquals(500, req.doc_no);
        }
    }

    @Test
    public void TestEmptyJumpTables() throws IOException {
        JumpTableConfig jt_cfg = new JumpTableConfig(2, 8);
        JumpTableBuilder builder = new JumpTableBuilder(jt_cfg);

        ByteArrayOutputStream storage = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(storage);
        assertEquals(0, builder.getNumberOfLevels());
        builder.write(stream);
        assertEquals("There should be only 0 as level number", 1, storage.size());

        JumpTableNavigator nav = new JumpTableNavigator(ByteBuffer.wrap(storage.toByteArray()));
        assertEquals(0, nav.getNumberOfLevels());
    }
}
