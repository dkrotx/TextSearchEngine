package org.bytesoft.tsengine;

import org.bytesoft.tsengine.dict.CatalogReader;
import org.bytesoft.tsengine.dict.CatalogRecord;
import org.bytesoft.tsengine.encoders.EncodersFactory;
import org.bytesoft.tsengine.encoders.IntCompressor;
import org.bytesoft.tsengine.idxblock.IdxBlockEncoder;
import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.bytesoft.tsengine.idxblock.JumpTableConfig;
import org.bytesoft.tsengine.info.IndexInfoReader;
import org.bytesoft.tsengine.info.IndexInfoWriter;
import org.bytesoft.tsengine.urls.UrlsCollectionReader;
import org.bytesoft.tsengine.urls.UrlsCollectionWriter;
import sun.nio.ch.DirectBuffer;
import util.lang.ExceptionalIterator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;


/**
 * This class provides ability to optimize index storage:
 * - Glue several records from catalog in one (to achieve smaller dictionary)
 * - Glue several index blocks in one (to "linearize" IO of decompression)
 */
public class IndexOptimizer implements Closeable {
    class Database implements Closeable {
        IndexingConfig cfg;
        FileChannel index;
        IndexInfoReader info;
        MappedByteBuffer index_mem = null;

        int start_doc;
        EncodersFactory decoder = new EncodersFactory();
        JumpTableConfig jt_cfg;

        Database(IndexingConfig cfg, int start_doc) throws IOException, IndexInfoReader.IndexInfoFormatError {
            this.cfg = cfg;
            this.start_doc = start_doc;
            index = FileChannel.open(cfg.GetRindexPath(), EnumSet.of(StandardOpenOption.READ));
            info = new IndexInfoReader(cfg);

            decoder.SetCurrentDecoder(info.GetEncodingMethod());
            jt_cfg = info.GetJumpTableConfig();
        }

        int getDocumentsAmount() {
            return info.GetNumberOfDocs();
        }

        void mmap() throws IOException {
            index_mem = index.map(FileChannel.MapMode.READ_ONLY, 0, index.size());
        }

        void munmap() {
            if (index_mem != null) {
                ((DirectBuffer) index_mem).cleaner().clean();
                index_mem = null;
            }
        }

        ByteBuffer getIndexArea(int offset, int size) {
            index_mem.position(offset);
            ByteBuffer area = index_mem.slice();
            area.limit(size);
            return area;
        }


        @Override
        public void close() throws IOException {
            munmap();
            index.close();
        }
    }

    Database[] src_dbs;
    ArrayList<AbsoluteCatalogRecord> records = new ArrayList<>();

    IndexingConfig dst_config;
    IdxBlockWriter out_index_writer;
    EncodersFactory out_encoder = new EncodersFactory();
    JumpTableConfig out_jt_cfg;

    public IndexOptimizer(IndexingConfig dst_config, IndexingConfig[] src_configs) throws IOException,
            IndexInfoReader.IndexInfoFormatError {
        int total_docs = 0;
        src_dbs = new Database[src_configs.length];

        for (int i = 0; i < src_configs.length; i++) {
            src_dbs[i] = new Database(src_configs[i], total_docs);
            src_dbs[i].mmap();
            total_docs += src_dbs[i].getDocumentsAmount();
        }

        this.dst_config = dst_config;
        out_jt_cfg = JumpTableConfig.fromIndexingConfig(dst_config);
        out_encoder.SetCurrentEncoder(dst_config.GetEncodingMethod());
    }

    public class IndexConvertionError extends Exception {
        public IndexConvertionError(String msg) { super(msg); }
    }

    final class AbsoluteCatalogRecord {
        long word_hash;
        int offset;
        int size;
        short file_id;

        AbsoluteCatalogRecord(int file_id, int offset, long word_hash, int size) {
            this.file_id = (short)file_id;
            this.word_hash = word_hash;
            this.offset = offset;
            this.size = size;
        }
    }

    private void readSingleCatalog(int i) throws IOException {
        CatalogReader cat = new CatalogReader(src_dbs[i].cfg.GetRindexCatPath());

        try(ExceptionalIterator<CatalogRecord, IOException> it = cat.iterator()) {
            int offset = 0;
            while (it.hasNext()) {
                CatalogRecord item = it.next();
                records.add(new AbsoluteCatalogRecord(i, offset, item.word_hash, item.size));
                offset += item.size;
            }
        }

        cat.close();
    }

    private int estimateSrcCatalogsSize() throws IOException {
        int summ = 0;

        for(Database db: src_dbs) {
            try(CatalogReader cat = new CatalogReader(db.cfg.GetRindexCatPath())) {
                summ += cat.numberOfEntries();
            }
        }

        return summ;
    }

    private void readCatalogs() throws IOException {
        records.ensureCapacity(estimateSrcCatalogsSize());
        for(int i = 0; i < src_dbs.length; i++)
            readSingleCatalog(i);
    }

    private void openDstIndex() throws IOException {
        out_index_writer = new IdxBlockWriter(dst_config.GetRindexPath(), dst_config.GetRindexCatPath());
    }

    @Override
    public void close() throws IOException {
        for(Database db: src_dbs) {
            db.close();
        }
        out_index_writer.close();
    }


    private int getNextWordRecord(int from) {
        long word_hash = records.get(from).word_hash;

        for(int i = from + 1; i < records.size(); i++)
            if (records.get(i).word_hash != word_hash)
                return i;

        return records.size();
    }

    class CrossFileIterator {
        class IteratorOffsetPair {
            IdxBlocksIterator it;
            Database db_from;
        }
        ArrayList<IteratorOffsetPair> local_blocks = new ArrayList<>();
        Iterator<IteratorOffsetPair> cross_it;
        IteratorOffsetPair current;

        CrossFileIterator(int start, int end) {
            int local_start, local_end;

            for (local_start = start; local_start < end; local_start = local_end) {
                short cur_file = records.get(local_start).file_id;

                local_end = local_start + 1;
                while (local_end < end && records.get(local_end).file_id == cur_file)
                    local_end++;

                Database db = src_dbs[cur_file];
                IteratorOffsetPair pair = new IteratorOffsetPair();
                pair.it = getLocalIterator(db, local_start, local_end);
                pair.db_from = db;

                local_blocks.add(pair);
            }

            cross_it = local_blocks.iterator();
            current = cross_it.next();
        }

        private IdxBlocksIterator getLocalIterator(Database db, int start, int end) {
            ByteBuffer[] areas = new ByteBuffer[end-start];

            for(int i = 0; i < areas.length; i++) {
                AbsoluteCatalogRecord rec = records.get(i + start);
                areas[i] = db.getIndexArea(rec.offset, rec.size);
            }

            return new IdxBlocksIterator(areas, db.decoder, db.jt_cfg);
        }

        /* don't need hasNext() since OMEGA_ID */

        int next() {
            int id = current.it.ReadNext();

            if (id == IdxBlocksIterator.OMEGA_ID) {
                if (!cross_it.hasNext())
                    return IdxBlocksIterator.OMEGA_ID;

                current = cross_it.next();
                id = current.it.ReadNext();
                assert(id != IdxBlocksIterator.OMEGA_ID); // always has 1 document at least
            }

            return id + current.db_from.start_doc;
        }
    } /* CrossFileIterator */

    private void writeDstIndex() throws IOException, IndexConvertionError {
        int word_start, word_end;

        for (word_start = 0; word_start < records.size(); word_start = word_end) {
            long word_hash = records.get(word_start).word_hash;
            word_end = getNextWordRecord(word_start);

            CrossFileIterator cfi = this.new CrossFileIterator(word_start, word_end);

            IdxBlockEncoder single;
            try {
                single = mergeIndexBlocks(cfi);
            } catch(IntCompressor.TooLargeToCompressException e) {
                throw new IndexConvertionError("failed to convert document IDs from source index to destination" +
                        e.getMessage());
            }

            out_index_writer.write(word_hash, single);
        }
    }

    private IdxBlockEncoder mergeIndexBlocks(CrossFileIterator it)
            throws IntCompressor.TooLargeToCompressException {
        IdxBlockEncoder single = new IdxBlockEncoder(out_encoder.MakeEncoder(), out_jt_cfg);
        int doc_id;

        while ((doc_id = it.next()) != IdxBlocksIterator.OMEGA_ID) {
            single.AddDocID(doc_id);
        }

        return single;
    }

    private void writeDstUrls() throws IOException {
        try(UrlsCollectionWriter urls_writer = new UrlsCollectionWriter(dst_config)) {
            for(Database db: src_dbs) {
                try (UrlsCollectionReader urls_reader = new UrlsCollectionReader(db.cfg)) {
                    ExceptionalIterator<String, IOException> it = urls_reader.iterator();
                    while(it.hasNext())
                        urls_writer.WriteURL(it.next());
                }
            }
        }
    }

    private void writeIndexInfo() throws IOException {
        Database last_db = src_dbs[src_dbs.length - 1];
        int total_docs = last_db.start_doc + last_db.getDocumentsAmount();

        IndexInfoWriter wr = new IndexInfoWriter(dst_config);
        wr.SetNumberOfDocs(total_docs);
        wr.Write();
    }

    private void sortCatalog() {
        /* order by word_hash, file, offset */
        Collections.sort(records, new Comparator<AbsoluteCatalogRecord>() {
            @Override
            public int compare(AbsoluteCatalogRecord o1, AbsoluteCatalogRecord o2) {
                if (o1.word_hash < o2.word_hash)
                    return -1;
                if (o1.word_hash > o2.word_hash)
                    return 1;

                if (o1.file_id < o2.file_id)
                    return -1;
                if (o1.file_id > o2.file_id)
                    return 1;

                return (o1.offset < o2.offset) ? -1 : 1;
            }
        });
    }

    public void optimize() throws IOException, IndexConvertionError {
        readCatalogs();
        sortCatalog();

        openDstIndex();
        writeDstIndex();
        writeDstUrls();

        writeIndexInfo();

        close();
    }
}
