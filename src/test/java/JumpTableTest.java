import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.encoders.VarByteDecoder;
import org.bytesoft.tsengine.encoders.VarByteEncoder;
import org.bytesoft.tsengine.idxblock.JumpTableBuilder;
import org.bytesoft.tsengine.idxblock.JumpTableFillPolicy;
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
        builder.write(out);

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
        JumpTableBuilder builder = new JumpTableBuilder(new JumpTableFillPolicy.ByNumberOfEntries(8));

        assertEquals("No levels at the beginning", 0, builder.getNumberOfLevels());
        int cur_offset = -1; // too low

        // We will fill jump tables in following way:
        // - each 2nd value goes to builder
        // - each 8th value of each jump table level makes new jump table
        for(int i = 0; i < ndocs; i++) {
            if (i != 0 && (i % 2) == 0) {
                cur_offset = enc.GetStoreSize();
                builder.addEntry(i, cur_offset);
            }

            saved_offsets[i] = cur_offset;
            enc.AddNumber(i);
        }

        assertEquals(log_base(ndocs/2, 8), builder.getNumberOfLevels());

        JumpTableNavigator nav = makeNavigatorFromBuilder(builder);
        assertEquals(log_base(ndocs/2, 8), nav.getNumberOfLevels());

        VarByteDecoder dec = new VarByteDecoder(ByteBuffer.wrap(enc.GetBytes()));

        {
            int offset = nav.findNearestOffset(500);
            assertEquals(saved_offsets[500], offset);

            dec.Position(offset);
            assertEquals("500 is even number, so we have exact offset to it", 500, dec.ExtractNumber());
        }

        {
            int offset = nav.findNearestOffset(801);
            assertEquals(saved_offsets[801], offset);

            dec.Position(offset);
            assertEquals("801 is odd number, so we have exact offset to 800", 800, dec.ExtractNumber());
            assertEquals(801, dec.ExtractNumber());
        }

        // try bigger value
        {
            int offset = nav.findNearestOffset(80001);
            assertEquals(saved_offsets[80001], offset);

            dec.Position(offset);
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
        JumpTableBuilder builder = new JumpTableBuilder(new JumpTableFillPolicy.ByNumberOfEntries(8));

        int cur_offset = -1; // too low

        // We will fill jump tables in following way:
        // - each 128 th value goes to builder
        // - each 8th value of each jump table level makes new jump table
        for(int i = 0; i < ndocs; i++) {
            if (i != 0 && (i % 128) == 0) {
                cur_offset = enc.GetStoreSize();
                builder.addEntry(i, cur_offset);
            }

            saved_offsets[i] = cur_offset;
            enc.AddNumber(i);
        }

        assertEquals(log_base(ndocs/128, 8), builder.getNumberOfLevels());

        JumpTableNavigator nav = makeNavigatorFromBuilder(builder);
        assertEquals(log_base(ndocs/128, 8), nav.getNumberOfLevels());

        {
            assertEquals("Too small for any JT", -1, nav.findNearestOffset(0));
            assertEquals("Too small for any JT", -1, nav.findNearestOffset(100));
            assertEquals("Too small for any JT", -1, nav.findNearestOffset(127));

            assertEquals(saved_offsets[128], nav.findNearestOffset(128));
        }

        {
            assertEquals(saved_offsets[ndocs - 1], nav.findNearestOffset(ndocs - 1));
            assertEquals("Beyond saved offsets", saved_offsets[ndocs - 1], nav.findNearestOffset(ndocs + 1));
            assertEquals("Beyond saved offsets", saved_offsets[ndocs - 1], nav.findNearestOffset(ndocs + 100500));
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
        JumpTableBuilder builder = new JumpTableBuilder(new JumpTableFillPolicy.ByNumberOfEntries(8));

        int cur_offset = -1; // too low

        // We will fill jump tables in following way:
        // - each 2nd value goes to builder
        // - each 8th value of each jump table level makes new jump table
        for(int i = 0; i < ndocs; i++) {
            if (i != 0 && (i % 2) == 0) {
                cur_offset = enc.GetStoreSize();
                builder.addEntry(i, cur_offset);
            }

            saved_offsets[i] = cur_offset;
            enc.AddNumber(i);
        }

        assertEquals(log_base(ndocs/2, 8), builder.getNumberOfLevels());

        JumpTableNavigator nav = makeNavigatorFromBuilder(builder);
        assertEquals(log_base(ndocs/2, 8), nav.getNumberOfLevels());

        {
            int offset = nav.findNearestOffset(500);
            assertEquals(saved_offsets[500], offset);
        }

        {
            int offset = nav.findNearestOffset(100);
            assertEquals("JumpTableNavigator shouldn't move back", -1, offset);
        }

        // btw, setting to same value one more time should not change anything
        {
            int offset = nav.findNearestOffset(500);
            assertEquals(saved_offsets[500], offset);

            offset = nav.findNearestOffset(500);
            assertEquals(saved_offsets[500], offset);
        }

        // Moving within same jump table entry should not change anithing too
        {
            int offset = nav.findNearestOffset(501);
            assertEquals(saved_offsets[501], offset);
            assertEquals(saved_offsets[500], saved_offsets[501]);
        }
    }
}
