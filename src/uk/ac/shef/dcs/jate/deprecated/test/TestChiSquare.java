package uk.ac.shef.dcs.jate.deprecated.test;

import java.io.IOException;
import java.util.Date;

import net.didion.jwnl.JWNLException;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.core.algorithm.ChiSquareAlgorithm;
import uk.ac.shef.dcs.jate.deprecated.core.algorithm.ChiSquareFeatureWrapper;
import uk.ac.shef.dcs.jate.deprecated.core.algorithm.FrequencyAlgorithm;
import uk.ac.shef.dcs.jate.deprecated.core.algorithm.FrequencyFeatureWrapper;
import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureBuilderCorpusTermFrequency;
import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.jate.deprecated.core.feature.TermVariantsUpdater;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndexBuilderMem;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.jate.deprecated.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.jate.deprecated.core.extractor.NGramExtractor;
import uk.ac.shef.dcs.jate.deprecated.model.CorpusImpl;
import uk.ac.shef.dcs.jate.deprecated.util.control.Lemmatizer;
import uk.ac.shef.dcs.jate.deprecated.util.control.StopList;
import uk.ac.shef.dcs.jate.deprecated.util.counter.TermFreqCounter;
import uk.ac.shef.dcs.jate.deprecated.util.counter.WordCounter;


public class TestChiSquare{

		public static void main(String[] args) throws IOException, JATEException, JWNLException {
		
			String path_to_corpus = args[0];
			String path_to_output = args[1];
	
			System.out.println("Started "+ TestChiSquare.class+"at: " + new Date() + "... For detailed progress see log file jate.log.");

			//creates instances of required processors and resources

			//stop word list
			StopList stop = new StopList(true);

			//lemmatizer
			Lemmatizer lemmatizer = new Lemmatizer();

			//noun phrase extractor
			CandidateTermExtractor npextractor = new NGramExtractor(stop, lemmatizer);
            //CandidateTermExtractor npextractor = new NounPhraseExtractorOpenNLP(stop, lemmatizer);
			
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
			/*newly added for improving frequency count calculation: ends*/
			
			FeatureCorpusTermFrequency termCorpusFreq =
							new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termIndex);
			
			//Firstly, run the Simple Frequency Algorithm. 
			AlgorithmTester tester = new AlgorithmTester();
			tester.registerAlgorithm(new FrequencyAlgorithm(), new FrequencyFeatureWrapper(termCorpusFreq));
			tester.execute(termIndex, path_to_output);			
			
			//Execute chiSquare Algorithm
			AlgorithmTester chiTester = new AlgorithmTester();
			chiTester.registerAlgorithm(new ChiSquareAlgorithm(args[0]), new ChiSquareFeatureWrapper(tester));
			chiTester.execute(termDocIndex, path_to_output);
			
			System.out.println("Ended at: " + new Date());
			
		}
	}