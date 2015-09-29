package uk.ac.shef.dcs.jate.algorithm;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TermInfoCollector {

    protected IndexReader indexReader;
    protected String ngramInfoFieldname;
    protected String idFieldname;

    public TermInfoCollector(IndexReader indexReader) {
        this.indexReader = indexReader;
    }

    public TermInfo collect(String term) throws IOException {
        TermInfo info = new TermInfo();

        PostingsEnum postings =
                MultiFields.getTermDocsEnum(indexReader, ngramInfoFieldname, new BytesRef(term.getBytes()));

        int docId = postings.nextDoc();
        while (docId != PostingsEnum.NO_MORE_DOCS) {
            Document doc = indexReader.document(docId);
            String id = doc.get(idFieldname);
            JATEDocument jd = new JATEDocument(id);
            Set<int[]> offsets = new HashSet<>();
            int totalFreq = postings.freq();
            for (int i = 0; i < totalFreq; i++) {
                postings.nextPosition();
                offsets.add(new int[]{postings.startOffset(), postings.endOffset()});
            }
            info.getOffsets().put(jd, offsets);

        }

        return info;
    }
}
