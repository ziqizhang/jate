package uk.ac.shef.dcs.jate.algorithm;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TermInfoCollector {

    protected LeafReader indexReader;
    protected String ngramInfoFieldname;
    protected String idFieldname;

    public TermInfoCollector(LeafReader indexReader, String ngramInfoFieldname,
                             String idFieldname) {
        this.indexReader = indexReader;
        this.ngramInfoFieldname=ngramInfoFieldname;
        this.idFieldname=idFieldname;
    }

    public TermInfo collect(String term) throws IOException {
        TermInfo info = new TermInfo();
        BytesRef luceneTerm =  new BytesRef(term.getBytes());
        //this gives documents in which the term is found, but no offset information can be retrieved
        PostingsEnum postings =
                MultiFields.getTermDocsEnum(indexReader, ngramInfoFieldname,luceneTerm);
        //now go through each document
        int docId = postings.nextDoc();
        while (docId != PostingsEnum.NO_MORE_DOCS) {
            //get the term vector for that document.
            TermsEnum it = indexReader.getTermVector(docId, ngramInfoFieldname).iterator();
            //find the term of interest
            it.seekExact(luceneTerm);
            //get its posting info. this will contain offset info
            PostingsEnum postingsInDoc = it.postings(null, PostingsEnum.OFFSETS);
            postingsInDoc.nextDoc();

            Document doc = indexReader.document(docId);
            String id = doc.get(idFieldname);
            JATEDocument jd = new JATEDocument(id);
            Set<int[]> offsets = new HashSet<>();
            int totalFreq = postingsInDoc.freq();
            for (int i = 0; i < totalFreq; i++) {
                postingsInDoc.nextPosition();
                offsets.add(new int[]{postingsInDoc.startOffset(), postingsInDoc.endOffset()});
            }
            info.getOffsets().put(jd, offsets);

            docId=postings.nextDoc();
        }

        return info;
    }
}
