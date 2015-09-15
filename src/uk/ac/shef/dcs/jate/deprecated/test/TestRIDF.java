package uk.ac.shef.dcs.jate.deprecated.test;

import net.didion.jwnl.JWNLException;
import uk.ac.shef.dcs.jate.deprecated.JATEException;
import uk.ac.shef.dcs.jate.deprecated.core.algorithm.*;
import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureBuilderCorpusTermFrequency;
import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.jate.deprecated.core.feature.TermVariantsUpdater;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndexBuilderMem;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.jate.deprecated.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.jate.deprecated.core.extractor.WordExtractor;
import uk.ac.shef.dcs.jate.deprecated.io.ResultWriter2File;
import uk.ac.shef.dcs.jate.deprecated.model.CorpusImpl;
import uk.ac.shef.dcs.jate.deprecated.model.Term;
import uk.ac.shef.dcs.jate.deprecated.util.control.Lemmatizer;
import uk.ac.shef.dcs.jate.deprecated.util.control.StopList;
import uk.ac.shef.dcs.jate.deprecated.util.counter.TermFreqCounter;
import uk.ac.shef.dcs.jate.deprecated.util.counter.WordCounter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 */
public class TestRIDF {
    private Map<Algorithm, AbstractFeatureWrapper> _algregistry = new HashMap<Algorithm, AbstractFeatureWrapper>();
	private static Logger _logger = Logger.getLogger(AlgorithmTester.class.getName());

	public void registerAlgorithm(Algorithm a, AbstractFeatureWrapper f) {
		_algregistry.put(a, f);
	}

	public void execute(GlobalIndexMem index) throws JATEException, IOException {
		_logger.info("Initializing outputter, loading NP mappings...");
		ResultWriter2File writer = new ResultWriter2File(index);
		if (_algregistry.size() == 0) throw new JATEException("No algorithm registered!");
		_logger.info("Running NP recognition...");

		/*.extractNP(c);*/
		for (Map.Entry<Algorithm, AbstractFeatureWrapper> en : _algregistry.entrySet()) {
			_logger.info("Running feature store builder and ATR..." + en.getKey().toString());
			Term[] result = en.getKey().execute(en.getValue());
			writer.output(result, en.getKey().toString() + ".txt");
		}
	}

	public static void main(String[] args) throws IOException, JATEException, JWNLException {

		String path_to_corpus= args[0];
		String path_to_output= args[1];
		
		//if (args.length < 2) {
		//	System.out.println("Usage: java TestRIDF [path_to_corpus] [path_to_output]");
		//} else {
			System.out.println("Started "+ TestRIDF.class+"at: " + new Date() + "... For detailed progress see log file jate.log.");

			//creates instances of required processors and resources

			//stop word list
			StopList stop = new StopList(true);

			//lemmatiser
			Lemmatizer lemmatizer = new Lemmatizer();

			//noun phrase extractor
			CandidateTermExtractor npextractor = new WordExtractor(stop, lemmatizer, true,2);
			//WordExtractor npextractor = new WordExtractor(stop, lemmatizer);

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
			
			//build a feature store required by the tfidf algorithm, using the processors instantiated above
			FeatureCorpusTermFrequency termCorpusFreq =
							new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termIndex);

			/*newly added for improving frequency count calculation: ends*/
			

			//build a feature store required by the tfidf algorithm, using the processors instantiated above
			/*FeatureCorpusTermFrequency termCorpusFreq =
					new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termDocIndex);
			*/
			AlgorithmTester tester = new AlgorithmTester();
			tester.registerAlgorithm(new RIDFAlgorithm(), new RIDFFeatureWrapper(termCorpusFreq));
			tester.execute(termDocIndex, path_to_output);
			System.out.println("Ended at: " + new Date());
		}
	}
//}
