package org.bytesoft.tsengine.demo;

import org.bytesoft.tsengine.IndexingConfig;
import org.bytesoft.tsengine.QTreePerformer;
import org.bytesoft.tsengine.WordUtils;
import org.bytesoft.tsengine.dict.DictRecord;
import org.bytesoft.tsengine.dict.DictionarySearcher;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.bytesoft.tsengine.qparse.ExprToken;
import org.bytesoft.tsengine.qparse.ExpressionTokenizer;
import org.bytesoft.tsengine.qparse.QueryTreeBuilder;
import org.bytesoft.tsengine.qparse.QueryTreeNode;
import org.bytesoft.tsengine.urls.UrlsCollectionReader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.EnumSet;

/**
 * This class demonstrates how to use searching in TextSearchEngine.
 */
class SearcherDemo {
    private IndexingConfig cfg;

    DictionarySearcher dictionary;
    FileChannel rindex_file;
    MappedByteBuffer rindex_mem;
    UrlsCollectionReader urls_reader;

    class Word2BlockTransformer implements QTreePerformer.Word2BlockConverter {

        private ByteBuffer getMemoryByDictRecord(DictRecord rec) {
            rindex_mem.position(rec.offset);
            ByteBuffer area = rindex_mem.slice();
            area.limit(rec.size);

            return area;
        }

        @Override
        public IdxBlocksIterator GetIndexBlockIterator(String word) {
            long hash = WordUtils.GetWordHash(WordUtils.GetWordFirstForm(word));
            IdxBlocksIterator blocks_iter = null;

            try {
                DictRecord[] records = dictionary.GetAll(hash);

                if (records != null) {
                    ByteBuffer[] areas = new ByteBuffer[records.length];
                    for (int i = 0; i < records.length; i++)
                        areas[i] = getMemoryByDictRecord(records[i]);

                    blocks_iter = new IdxBlocksIterator(areas);
                }
            }
            catch(IOException e)
            {}

            return blocks_iter;
        }
    }

    private void openReverseIndex() throws IOException {
        Path path = cfg.GetRindexPath();

        rindex_file = FileChannel.open(path, EnumSet.of(StandardOpenOption.READ));
        rindex_mem  = rindex_file.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path));
    }

    private void openDictionary() throws IOException {
        Path path = cfg.GetRindexDictPath();
        dictionary = new DictionarySearcher(new RandomAccessFile(path.toFile(), "r"));
    }

    private void openUrlsCollection() throws IOException {
        Path path = cfg.GetUrlsPath();
        Path path_idx = cfg.GetUrlsIdxPath();

        urls_reader = new UrlsCollectionReader(path.toFile(), path_idx.toFile());
    }

    private void printMatchedDocument(int id) throws IOException {
        System.out.printf("%4d  %s\n", id, urls_reader.ReadURL(id));
    }

    public SearcherDemo(IndexingConfig cfg) throws IOException {
        this.cfg = cfg;

        openReverseIndex();
        openDictionary();
        openUrlsCollection();
    }

    private QueryTreeNode parseQuery(String query) throws ParseException {
        ExprToken[] tokens = ExpressionTokenizer.Tokenize(query);
        return QueryTreeBuilder.BuildExpressionTree(tokens);
    }

    public void PerformRequest(String query) throws ParseException, IOException {
        try {
            Word2BlockTransformer w2block = this.new Word2BlockTransformer();
            QueryTreeNode qtree = parseQuery(query);
            QTreePerformer performer = new QTreePerformer(qtree, w2block);

            while (!performer.Finished()) {
                int id = performer.GetNextDocument();
                if (id >= urls_reader.GetURLsAmount() || id == IdxBlocksIterator.OMEGA_ID)
                    break;

                printMatchedDocument(id);
            }
        }
        catch(QTreePerformer.UnsupportedQTreeOperator e) {
            System.err.println("Should not happen: " + e);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: " + SearcherDemo.class.getCanonicalName() + " path/to/database QUERY");
            System.exit(64);
        }

        String directory = args[0];
        SearcherDemo searcher = new SearcherDemo(new IndexingConfig(directory));

        try {
            searcher.PerformRequest(args[1]);
        } catch (ParseException e) {
            System.err.println("Exception while parsing query: "+ e);
        }
    }
}
