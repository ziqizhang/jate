package uk.ac.shef.dcs.jate.feature;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public abstract class AbstractFeatureBuilder {

    protected SolrIndexSearcher solrIndexSearcher;

    protected JATEProperties properties;

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
            throw new JATEException("Features are required on 'Words', however there are no single-token lexical units in the "+
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

        TermsEnum termsEnum = terms.iterator();
        Set<String> allTermCandidates = new HashSet<>();

        while (termsEnum.next() != null) {
            BytesRef t = termsEnum.term();
            if (t.length == 0)
                continue;
            allTermCandidates.add(t.utf8ToString());
        }
        return allTermCandidates;
    }




}
