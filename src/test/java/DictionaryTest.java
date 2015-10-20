import org.bytesoft.tsengine.dict.DictRecord;
import org.bytesoft.tsengine.dict.DictionarySearcher;
import org.bytesoft.tsengine.dict.DictionaryWriter;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;


public class DictionaryTest {
    @Test
    public void checkSimpleSearch() throws IOException {
        DictionaryWriter dic = new DictionaryWriter(4);

        dic.Add(1, new DictRecord(10, 10));
        dic.Add(3, new DictRecord(30, 30));
        dic.Add(7, new DictRecord(70, 70));
        dic.Add(8, new DictRecord(80, 80));


        File tmpfile = File.createTempFile("tsengine-dict", ".tst");
        tmpfile.deleteOnExit();

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tmpfile))) {
            dic.Write(out);
        }

        try (RandomAccessFile in = new RandomAccessFile(tmpfile, "r")) {
            DictionarySearcher srch = new DictionarySearcher(in);

            assertTrue(srch.Exists(1));
            assertTrue(srch.Exists(7));
            assertTrue(srch.Exists(3));
            assertTrue(srch.Exists(8));

            assertFalse(srch.Exists(0));
            assertFalse(srch.Exists(2));
            assertFalse(srch.Exists(10));

            assertEquals(new DictRecord(10, 10), srch.GetFirst(1));
            assertEquals(new DictRecord(80, 80), srch.GetFirst(8));

            assertEquals(null, srch.GetFirst(2));
            assertEquals(null, srch.GetFirst(1000));
        }
    }

    @Test
    public void checkMultyValue() throws IOException {
        DictionaryWriter dic = new DictionaryWriter(6);

        dic.Add(0, new DictRecord(0, 0));
        dic.Add(1, new DictRecord(100, 10));
        dic.Add(3, new DictRecord(300, 30));
        dic.Add(1, new DictRecord(400, 40));
        dic.Add(7, new DictRecord(700, 70));
        dic.Add(8, new DictRecord(800, 80));
        dic.Add(1, new DictRecord(900, 90));

        File tmpfile = File.createTempFile("tsengine-dict", ".tst");
        tmpfile.deleteOnExit();

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tmpfile))) {
            dic.Write(out);
        }

        try (RandomAccessFile in = new RandomAccessFile(tmpfile, "r")) {
            DictionarySearcher srch = new DictionarySearcher(in);

            assertTrue(srch.Exists(1));

            DictRecord[] expected = {
                    new DictRecord(100, 10),
                    new DictRecord(400, 40),
                    new DictRecord(900, 90)
            };

            assertArrayEquals("there should be 3 entries of '1'", expected, srch.GetAll(1));
        }
    }
}
