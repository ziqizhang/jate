package uk.ac.shef.dcs.jate.feature;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public abstract class AbstractFeatureBuilder {

    protected SolrIndexSearcher solrIndexSearcher;

    protected JATEProperties properties;

    /**
     * setting SEQUENTIAL_THRESHOLD (or MAX_TASKS_PER_WORKER) to a good-in-practice value is a trade-off.
     * The documentation for the ForkJoin framework suggests creating parallel subtasks until
     * the number of basic computation steps is somewhere over 100 and less than 10,000.
     *
     * The exact number is not crucial provided you avoid extremes.
     *
     * @see <a href="http://homes.cs.washington.edu/~djg/teachingMaterials/grossmanSPAC_forkJoinFramework.html"/>
     * @see <a href="http://stackoverflow.com/questions/19925820/fork-join-collecting-results/19926423#19926423"/>
     */
    protected final static int MIN_SEQUENTIAL_THRESHOLD = 100;
    protected final static int MAX_SEQUENTIAL_THRESHOLD = 10000;

    public AbstractFeatureBuilder(SolrIndexSearcher solrIndexSearcher, JATEProperties properties){
        this.solrIndexSearcher=solrIndexSearcher;
        this.properties=properties;
    }

    public abstract AbstractFeature build() throws JATEException;


    protected Set<String> getUniqueWords() throws JATEException, IOException {
        Terms ngramInfo = SolrUtil.getTermVector(properties.getSolrFieldNameJATENGramInfo(), solrIndexSearcher);

        TermsEnum termsEnum = ngramInfo.iterator();
        Set<String> allWords = new HashSet<>();

        while (termsEnum.next() != null) {
            BytesRef t = termsEnum.term();
            if (t.length == 0)
                continue;
            String termStr=t.utf8ToString();
            if(!termStr.contains(" "))
                allWords.add(termStr);
        }
        if(allWords.size()==0)
            throw new JATEException("MWEMetadata are required on 'Words', however there are no single-token lexical units in the "+
            properties.getSolrFieldNameJATENGramInfo()+" field. Check to see if your analyzer pipeline outputs uni-grams");
        return allWords;
    }


    /**
     * Retrieve term candidates from solr field
     *      see @code {uk.ac.shef.dcs.jate.JATEProperties.PROPERTY_SOLR_FIELD_CONTENT_TERMS}
     *
     * The method assumes that the term candidates are extracted at index-time and stored in pre-configured field
     *
     * @return Set, a set of term candidate surface form
     * @throws JATEException
     * @throws IOException
     */
    protected Set<String> getUniqueTerms() throws JATEException, IOException {
        Terms terms =SolrUtil.getTermVector(properties.getSolrFieldNameJATECTerms(),solrIndexSearcher);

        //>>>>>>>>>
        /*TermsEnum source = terms.iterator();
        String term = //"thrownawayorusedjustforelementarystatistical profile";
        "l hierar hy";
        //"ordertoavoidadependencyofthebaselineresultontherandom";

                if (source.seekExact(new BytesRef(term.getBytes("UTF-8")))) {
                    PostingsEnum docEnum = source.postings(null);
                    int doc = 0;
                    while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        int tfid = docEnum.freq();  //tf in document

                    }

                } else {

                }*/
        //>>>>>>>>>

        TermsEnum termsEnum = terms.iterator();
        Set<String> allTermCandidates = new HashSet<>();

        while (termsEnum.next() != null) {
            BytesRef t = termsEnum.term();
            if (t.length == 0)
                continue;
            allTermCandidates.add(t.utf8ToString());

            if(t.utf8ToString().equals("l hierar hy"))
                System.out.println();
        }
        return allTermCandidates;
    }




}
