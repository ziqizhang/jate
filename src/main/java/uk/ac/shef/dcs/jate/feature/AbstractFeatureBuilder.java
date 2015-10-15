package uk.ac.shef.dcs.jate.feature;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        Terms ngramInfo = SolrUtil.getTermVector(properties.getSolrFieldnameJATENGramInfo(), solrIndexSearcher);

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
        return allWords;
    }


    protected Set<String> getUniqueTerms() throws JATEException, IOException {
        Terms terms =SolrUtil.getTermVector(properties.getSolrFieldnameJATECTerms(),solrIndexSearcher);

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
