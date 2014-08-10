package uk.ac.shef.dcs.jate.core.feature;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndex;
import uk.ac.shef.dcs.jate.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.jate.util.counter.WordCounter;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.Document;
import uk.ac.shef.dcs.jate.util.control.Normalizer;
import uk.ac.shef.dcs.jate.util.counter.TermFreqCounter;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A specific type of feature builder that builds an instance of FeatureCorpusTermFrequency from a GlobalIndex.
 * Counting of term frequency is <b>case-sensitive</b>. For each canonical term form, each of its variants (letter case,
 * inflections etc) are counted in the document.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public class FeatureBuilderCorpusTermFrequency extends AbstractFeatureBuilder {

	private static Logger _logger = Logger.getLogger(FeatureBuilderCorpusTermFrequency.class.getName());

	/**
	 * Creates an instance
	 * @param counter1 candidate term counter, counting distributions of candidate terms
	 * @param counter2 word counter, counting number of words in documents
	 * @param normaliser a normaliser for returning terms to their canonical forms
	 * over the corpus and add up to the total frequencies of the lemma.
	 */
	public FeatureBuilderCorpusTermFrequency(TermFreqCounter counter1, WordCounter counter2, Normalizer normaliser) {
		super(counter1, counter2, normaliser);
	}

	/**
	 * Build an instance of FeatureCorpusTermFrequency
	 * @param index the global resource index
	 * @return
	 * @throws uk.ac.shef.dcs.jate.JATEException
	 */
	public FeatureCorpusTermFrequency build(GlobalIndex index) throws JATEException {
		FeatureCorpusTermFrequency _feature = new FeatureCorpusTermFrequency(index);
		if (index.getTermsCanonical().size() == 0 || index.getDocuments().size() == 0) throw new
                JATEException("No resource indexed!");

		_logger.info("About to build FeatureCorpusTermFrequency...");
		int totalCorpusTermFreq = 0;
		//for (Document d : index.getDocuments()) {
		//	_logger.info("For Document " + d);
			//String context = CandidateTermExtractor.applyCharacterReplacement(d.getContent(), JATEProperties.TERM_CLEAN_PATTERN);
		/*File file = new File ("C:\\Users\\aniya_agarwal\\Desktop\\reqtsent\\sample.txt");
		Document doc=null;
		try {
			doc = new DocumentImpl(file.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		*/
		
		
		for (Document d : index.getDocuments()) {
			_logger.info("For Document " + d);
			String context = CandidateTermExtractor.applyCharacterReplacement(d.getContent(), JATEProperties.TERM_CLEAN_PATTERN);
			
			totalCorpusTermFreq += _wordCounter.countWords(d);

			Set<String> candidates = index.retrieveTermsCanonicalInDoc(d);
            //counting by single-threaded termcounter
			for (String np : candidates) {
				//Set<String> va = index.retrieveVariantsOfTermCanonical(np);
				int freq = _termFreqCounter.count(context, index.retrieveVariantsOfTermCanonical(np));
				_feature.addToTermFreq(np, freq);
			}
		}
		_feature.setTotalCorpusTermFreq(totalCorpusTermFreq);

		return _feature;
	}

	
}
