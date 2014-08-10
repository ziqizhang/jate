package uk.ac.shef.dcs.jate.core.feature.indexer;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.jate.model.Corpus;

/**
 * Build an instance of GlobalIndex
 */
public interface GlobalIndexBuilder {

    public GlobalIndex build(Corpus c, CandidateTermExtractor extractor) throws JATEException;
}
