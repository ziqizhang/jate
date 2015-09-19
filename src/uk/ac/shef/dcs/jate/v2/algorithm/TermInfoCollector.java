package uk.ac.shef.dcs.jate.v2.algorithm;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

/**
 *
 */
public class TermInfoCollector{

    protected IndexReader indexReader;

    public TermInfoCollector(IndexReader indexReader){
        this.indexReader=indexReader;
    }

    public TermInfo collect(String term){

        //todo
        return null;
    }
}
