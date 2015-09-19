package uk.ac.shef.dcs.jate.v2.algorithm;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;
import uk.ac.shef.dcs.jate.v2.model.TermInfoType;

import java.util.Map;

/**
 * Created by zqz on 19/09/2015.
 */
public class TermProvenanceCollector implements TermInfoCollector {

    protected IndexReader indexReader;

    public TermProvenanceCollector(IndexReader indexReader){
        this.indexReader=indexReader;
    }

    @Override
    public Map<TermInfoType, TermInfo> collect(String term) {
        return null;
    }
}
