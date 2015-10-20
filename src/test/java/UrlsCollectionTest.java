import org.bytesoft.tsengine.urls.UrlsCollectionReader;
import org.bytesoft.tsengine.urls.UrlsCollectionWriter;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class UrlsCollectionTest {
    @Test
    public void SimpleUrlsTest() throws IOException {
        String[] urls = {
                "http://www.mail.ru/",
                "http://habrahabr.ru/company/metrotek/blog/261003/",
                "http://www.improgrammer.net/10-articles-every-programmer-must-read/"
        };

        File tmp_file_urls = File.createTempFile("tsengine-urls", ".txt");
        File tmp_file_idx = File.createTempFile("tsengine-urls", ".idx");

        try (
                OutputStreamWriter urls_stream =
                     new OutputStreamWriter(new FileOutputStream(tmp_file_urls));
                DataOutputStream idx_stream =
                        new DataOutputStream(new FileOutputStream(tmp_file_idx))
        ) {

            UrlsCollectionWriter writer = new UrlsCollectionWriter(urls_stream, idx_stream);
            for(String u: urls)
                writer.WriteURL(u);
        }

        UrlsCollectionReader reader = new UrlsCollectionReader(tmp_file_urls, tmp_file_idx);

        assertEquals("We wrote " + urls.length + " URLs", urls.length, reader.GetURLsAmount());
        for(int i = urls.length - 1; i >= 0; i--)
            assertEquals(urls[i], reader.ReadURL(i));

        // check too big ReadURL() situation

        boolean caught = false;
        try {
            reader.ReadURL(urls.length /* wrong since 0-based */);
        }
        catch(IndexOutOfBoundsException e) {
            caught = true;
        }
        finally {
            if (!caught)
                fail("Reading of URL #" + urls.length + " is beyond the bounds");
        }
    }
}
