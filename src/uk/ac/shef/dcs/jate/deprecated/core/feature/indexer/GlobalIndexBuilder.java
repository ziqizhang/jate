package uk.ac.shef.dcs.jate.deprecated.core.feature.indexer;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.jate.deprecated.model.Corpus;

/**
 * Build an instance of GlobalIndex
 */
@Deprecated
public interface GlobalIndexBuilder {

    public GlobalIndex build(Corpus c, CandidateTermExtractor extractor) throws JATEException;
}
