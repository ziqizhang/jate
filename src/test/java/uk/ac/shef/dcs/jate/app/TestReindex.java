package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.CopyField;
import org.apache.solr.search.*;
import org.apache.solr.search.grouping.distributed.command.QueryCommand;
import org.junit.Before;
import org.junit.Test;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jieg on 04/05/2016.
 */
public class TestReindex extends BaseEmbeddedSolrTest {
    private static Logger LOG = Logger.getLogger(AppATEGENIATest.class.getName());


    @Override
    protected void setSolrCoreName() {
        solrCoreName = "jateCore";
    }

    @Override
    protected void setReindex() {

    }

    @Before
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    public void testStringListFiltering() {
        String term1 = "bad experience";
        String term2 = "intermittent light";
        String term3 = "much material";
        String term4 = "recent inspection";
        String term5 = "west face";
        String term6 = "66th rail";
        String term7 = "metallurgical examination";
        String term8 = "hinge crack";
        String term9 = "electronic transfer";
        String term10 = "narrow face";

        List<String> candidateTerms = Arrays.asList(term1, term2, term3, term4, term5, term6);

        List<String> allFilteredTerms = Arrays.asList(term2, term5, term6, term7, term8, term9, term10);

//        List<String> filteredCandidateTerms = candidateTerms.parallelStream().
//                filter(allFilteredTerms::contains).
//                collect(Collectors.toList());
//        System.out.println(filteredCandidateTerms);

        List<Map<String, Double>> filteredCandidateTerms = new ArrayList<>();
        new HashMap<String, Double>().put("", 1.0);
        candidateTerms.parallelStream().forEach(candidateTerm -> {
            allFilteredTerms.parallelStream().forEach(filteredTerm -> {
                if (filteredTerm.equalsIgnoreCase(candidateTerm)) {
                    Map<String, Double> selectedTerm = new HashMap<String, Double>();
                    selectedTerm.put(filteredTerm, 2.0);
                    filteredCandidateTerms.add(selectedTerm);
                }
            });
        });
        System.out.println(filteredCandidateTerms.toString());
    }


    public void testLoadCandidates()throws JATEException, IOException {
        SolrCore core = server.getCoreContainer().getCore(solrCoreName);
        SolrIndexSearcher indexSearcher = core.getSearcher().get();
        for (int i=0; i<indexSearcher.maxDoc(); i++) {
            Document doc = indexSearcher.doc(i);

            System.out.println("================="+doc.getField("id")+"===================");

            Terms indexedCandidateTermsVectors = SolrUtil.getTermVector(i, "jate_cterms", indexSearcher);
            TermsEnum iterTerms = indexedCandidateTermsVectors.iterator();
            BytesRef text;
            while((text = iterTerms.next()) != null) {
                System.out.println(text.utf8ToString());
            }
        }

    }


    public void testReIndexing() throws JATEException {
        SolrCore core = server.getCoreContainer().getCore(solrCoreName);
        SolrIndexSearcher indexSearcher = core.getSearcher().get();

        try {

            Map<String,List<CopyField>> copyFields = core.getLatestSchema().getCopyFieldsMap();

            IndexWriter writerIn = core.getSolrCoreState().getIndexWriter(core).get();
            final float boost = 0;
            for (int i=0; i<indexSearcher.maxDoc(); i++) {
                Document doc = indexSearcher.doc(i);
//                writerIn.deleteDocuments(new Terms(doc.get));
//                writerIn.tryDeleteDocument(readerIn, i);
//                writerIn.addDocument(doc);
//                writerIn.updateDocument();
                writerIn.deleteDocuments(new Term("id",doc.get("id")));

                for (String sourceField : copyFields.keySet()) {
                    List<CopyField> copyFieldList = copyFields.get(sourceField);
                    for (CopyField copyField : copyFieldList) {
                        IndexableField jateField = copyField.getDestination().createField(doc.get(copyField.getSource().getName()), boost);
                        doc.add(jateField);
                    }
                }
                writerIn.addDocument(doc);
//                writerIn.updateDocument(new Term("id",doc.get("id")), doc);
            }
            writerIn.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
