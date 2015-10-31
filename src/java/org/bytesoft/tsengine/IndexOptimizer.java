package org.bytesoft.tsengine;

import org.bytesoft.tsengine.dict.CatalogReader;
import org.bytesoft.tsengine.dict.CatalogRecord;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.bytesoft.tsengine.info.IndexInfoReader;
import util.lang.ExceptionalIterator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class provides ability to optimize index storage:
 * - Glue several records from catalog in one (to achieve smaller dictionary)
 * - Glue several index blocks in one (to "linearize" IO of decompression)
 */
public class IndexOptimizer implements Closeable {
    IndexingConfig cfg;
    FileChannel rindex_file;
    MappedByteBuffer rindex_mem;
    ArrayList<AbsoluteCatalogRecord> records;

    IdxBlockWriter new_rindex_writer;
    Path new_rindex_path;
    Path new_catalog_path;

    EncodersFactory encoder_factory = new EncodersFactory();

    public IndexOptimizer(IndexingConfig cfg) throws IOException, IndexInfoReader.IndexInfoFormatError {
        this.cfg = cfg;
        IndexInfoReader info_reader = new IndexInfoReader(cfg);
        encoder_factory.SetCurrentDecoder(info_reader.GetEncodingMethod());
        encoder_factory.SetCurrentEncoder(info_reader.GetEncodingMethod());
    }

    public class IndexConvertionError extends Exception {
        public IndexConvertionError(String msg) { super(msg); }
    }

    final class AbsoluteCatalogRecord {
        long word_hash;
        int offset;
        int size;

        AbsoluteCatalogRecord(int offset, long word_hash, int size) {
            this.word_hash = word_hash;
            this.offset = offset;
            this.size = size;
        }
    }

    private void readCatalog() throws IOException {
        CatalogReader cat = new CatalogReader(cfg.GetRindexCatPath());
        records = new ArrayList<>();

        records.ensureCapacity(cat.numberOfEntries());

        try(ExceptionalIterator<CatalogRecord, IOException> it = cat.iterator()) {
            int offset = 0;
            while (it.hasNext()) {
                CatalogRecord item = it.next();
                records.add(new AbsoluteCatalogRecord(offset, item.word_hash, item.size));
                offset += item.size;
            }
        }
    }

    private void openInvertedIndex() throws IOException {
        Path idx_path = cfg.GetRindexPath();
        rindex_file = FileChannel.open(idx_path, EnumSet.of(StandardOpenOption.READ));
        rindex_mem  = rindex_file.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(idx_path));

        new_rindex_path = Paths.get(idx_path.toAbsolutePath() + ".new");
        new_catalog_path = Paths.get(cfg.GetRindexCatPath() + ".new");
        new_rindex_writer = new IdxBlockWriter(new_rindex_path, new_catalog_path);
    }

    @Override
    public void close() throws IOException {
        rindex_file.close();
        new_rindex_writer.close();
    }

    private void replaceOriginFiles() throws IOException {
        close();

        Files.move(new_rindex_path, cfg.GetRindexPath(), REPLACE_EXISTING);
        Files.move(new_catalog_path, cfg.GetRindexCatPath(), REPLACE_EXISTING);
    }

    private ByteBuffer getInvertedIndexArea(AbsoluteCatalogRecord rec) {
        rindex_mem.position(rec.offset);
        ByteBuffer area = rindex_mem.slice();
        area.limit(rec.size);

        return area;
    }

    private int getNextWordRecord(int from) {
        long word_hash = records.get(from).word_hash;

        for(int i = from + 1; i < records.size(); i++)
            if (records.get(i).word_hash != word_hash)
                return i;

        return records.size();
    }

    private void rewriteIndex() throws IOException, IntCompressor.TooLargeToCompressException {
        int start = 0;

        while (start < records.size()) {
            long cur_word_hash = records.get(start).word_hash;
            int end = getNextWordRecord(start);
            ByteBuffer[] areas = new ByteBuffer[end - start];

            for(int i = 0; i < areas.length; i++) {
                areas[i] = getInvertedIndexArea(records.get(i + start));
            }

            IdxBlocksIterator block_reader = new IdxBlocksIterator(areas, encoder_factory);
            IdxBlockEncoder single = mergeIndexBlocks(block_reader);

            new_rindex_writer.write(cur_word_hash, single);

            start = end;
        }
    }

    private IdxBlockEncoder mergeIndexBlocks(IdxBlocksIterator block_reader)
            throws IntCompressor.TooLargeToCompressException {
        IdxBlockEncoder single = new IdxBlockEncoder(encoder_factory.MakeEncoder());
        while (block_reader.HasNext()) {
            single.AddDocID(block_reader.ReadNext());
        }

        return single;
    }

    public void optimize() throws IOException, IndexConvertionError {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("read catalog records");
        readCatalog();
        System.out.println("sort catalog records");
        Collections.sort(records, new Comparator<AbsoluteCatalogRecord>() {
            @Override
            public int compare(AbsoluteCatalogRecord o1, AbsoluteCatalogRecord o2) {
                return o1.word_hash < o2.word_hash ? -1 : (o1.word_hash > o2.word_hash ? 1 : 0);
            }
        });

        System.out.println("open inverted index");
        openInvertedIndex();
        try {
            System.out.println("rewrite inverted index");
            rewriteIndex();
        } catch(IntCompressor.TooLargeToCompressException e) {
            String src = encoder_factory.GetCurrentDecoderName();
            String dst = encoder_factory.GetCurrentEncoderName();

            close();
            throw new IndexConvertionError("Failed to convert index encoding from " +
                    src + " to " + dst + ": " + e.getMessage());
        }

        System.out.println("replace origin files");
        replaceOriginFiles();
    }
}
