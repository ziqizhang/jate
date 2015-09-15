package uk.ac.shef.dcs.jate.deprecated.core.feature;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndex;
import uk.ac.shef.dcs.jate.deprecated.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.jate.deprecated.model.Document;
import uk.ac.shef.dcs.jate.deprecated.util.control.Normalizer;
import uk.ac.shef.dcs.jate.deprecated.util.counter.TermFreqCounter;
import uk.ac.shef.dcs.jate.deprecated.util.counter.WordCounter;

import java.util.Set;
import java.util.logging.Logger;

/**
 * A specific type of feature builder that builds an instance of FeatureDocumentTermFrequency from a GlobalIndex.
 * Counting of term frequency is <b>case-sensitive</b>. For each canonical term form, each of its variants (letter case,
 * inflections etc) are counted in the document.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class FeatureBuilderDocumentTermFrequency extends AbstractFeatureBuilder {

	private static Logger _logger = Logger.getLogger(FeatureBuilderDocumentTermFrequency.class.getName());

	/**
	 * Creates an instance
	 * @param counter1 candidate term counter, counting distributions of candidate terms
	 * @param counter2 word counter, counting number of words in documents
	 * @param normaliser a normaliser for normalising terms to their canonical forms
	 * over the corpus and add up to the total frequencies of the lemma.
	 */
	public FeatureBuilderDocumentTermFrequency(TermFreqCounter counter1, WordCounter counter2, Normalizer normaliser) {
		super(counter1, counter2, normaliser);
	}

	/**
	 * Build an instance of FeatureCorpusTermFrequency
	 * @param index the global resource index
	 * @return
	 * @throws uk.ac.shef.dcs.jate.v2.JATEException
	 */
	public FeatureDocumentTermFrequency build(GlobalIndex index) throws JATEException {
		FeatureDocumentTermFrequency _feature = new FeatureDocumentTermFrequency(index);
		if (index.getTermsCanonical().size() == 0 || index.getDocuments().size() == 0) throw new
                JATEException("No resource indexed!");

		_logger.info("About to build FeatureDocumentTermFrequency...");

		int totalTermFreq = 0;
		for (Document d : index.getDocuments()) {
			_logger.info("For document " + d);
			totalTermFreq += _wordCounter.countWords(d);
            String context = CandidateTermExtractor.applyCharacterReplacement(
                    d.getContent(), JATEProperties.TERM_CLEAN_PATTERN
            );

			Set<String> candidates = index.retrieveTermsCanonicalInDoc(d);
			for (String np : candidates) {
				//term freq
				int tfreq = _termFreqCounter.count(context, index.retrieveVariantsOfTermCanonical(np));
				_feature.addToTermFreqInDoc(np, d, tfreq);
			}
		}
		_feature.setTotalCorpusTermFreq(totalTermFreq);
		return _feature;
	}
}
