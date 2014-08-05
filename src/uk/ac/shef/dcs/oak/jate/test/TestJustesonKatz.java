package uk.ac.shef.dcs.oak.jate.test;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.core.algorithm.JustesonAlgorithm;
import uk.ac.shef.dcs.oak.jate.core.feature.FeatureBuilderCorpusTermFrequency;
import uk.ac.shef.dcs.oak.jate.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.oak.jate.core.feature.TermVariantsUpdater;
import uk.ac.shef.dcs.oak.jate.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.oak.jate.core.extractor.NounPhraseExtractorOpenNLP;
import uk.ac.shef.dcs.oak.jate.model.CorpusImpl;
import uk.ac.shef.dcs.oak.jate.util.control.Lemmatizer;
import uk.ac.shef.dcs.oak.jate.util.control.StopList;
import uk.ac.shef.dcs.oak.jate.util.counter.TermFreqCounter;
import uk.ac.shef.dcs.oak.jate.util.counter.WordCounter;
import uk.ac.shef.dcs.oak.jate.core.algorithm.FrequencyFeatureWrapper;
import uk.ac.shef.dcs.oak.jate.core.feature.indexer.GlobalIndexBuilderMem;
import uk.ac.shef.dcs.oak.jate.core.extractor.CandidateTermExtractor;
import java.io.IOException;
import java.util.Date;

public class TestJustesonKatz {
	public static void main(String[] args) throws IOException, JATEException {


        String path_to_corpus = args[0];
        String path_to_output = args[1];

		//String path_to_corpus= JATEProperties.getInstance().getCorpusPath();
		//String path_to_output= JATEProperties.getInstance().getResultPath();

		System.out.println("Started "+ TestJustesonKatz.class+"at: " + new Date() + "... For detailed progress see log file jate.log.");

		//creates instances of required processors and resources

		//stop word list
		StopList stop = new StopList(true);
	//lemmatiser
		Lemmatizer lemmatizer = new Lemmatizer();

		//noun phrase extractor
		CandidateTermExtractor npextractor = new NounPhraseExtractorOpenNLP(stop, lemmatizer);
		//CandidateTermExtractor npextractor = new NGramExtractor(stop, lemmatizer);
		//counters
		TermFreqCounter npcounter = new TermFreqCounter();
		WordCounter wordcounter = new WordCounter();

		//create global resource index builder, which indexes global resources, such as documents and terms and their
		//relations
		GlobalIndexBuilderMem builder = new GlobalIndexBuilderMem();
		//build the global resource index
		GlobalIndexMem termDocIndex = builder.build(new CorpusImpl(path_to_corpus), npextractor);
		
		/*newly added for improving frequency count calculation: begins*/
		
		
		TermVariantsUpdater update = new TermVariantsUpdater(termDocIndex, stop, lemmatizer);
		 
		
		GlobalIndexMem termIndex = update.updateVariants();
		
		FeatureCorpusTermFrequency termCorpusFreq =
						new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termIndex);
			
		
		/*newly added for improving frequency count calculation: ends*/

		//build a feature store required by the tfidf algorithm, using the processors instantiated above
		//		FeatureCorpusTermFrequency termCorpusFreq =
		//			new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termDocIndex);
					

		AlgorithmTester tester = new AlgorithmTester();
		tester.registerAlgorithm(new JustesonAlgorithm(), new FrequencyFeatureWrapper(termCorpusFreq));
		
		tester.execute(termDocIndex, path_to_output);
		System.out.println("Ended at: " + new Date());
	}
}