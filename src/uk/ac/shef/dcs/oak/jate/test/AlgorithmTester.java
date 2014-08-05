package uk.ac.shef.dcs.oak.jate.test;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.core.algorithm.*;
import uk.ac.shef.dcs.oak.jate.core.feature.*;
import uk.ac.shef.dcs.oak.jate.core.feature.indexer.GlobalIndex;
import uk.ac.shef.dcs.oak.jate.core.feature.indexer.GlobalIndexBuilderMem;
import uk.ac.shef.dcs.oak.jate.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.oak.jate.core.extractor.CandidateTermExtractor;
import uk.ac.shef.dcs.oak.jate.core.extractor.NounPhraseExtractorOpenNLP;
import uk.ac.shef.dcs.oak.jate.core.extractor.WordExtractor;
import uk.ac.shef.dcs.oak.jate.io.ResultWriter2File;
import uk.ac.shef.dcs.oak.jate.model.CorpusImpl;
import uk.ac.shef.dcs.oak.jate.model.Term;
import uk.ac.shef.dcs.oak.jate.util.control.Lemmatizer;
import uk.ac.shef.dcs.oak.jate.util.control.StopList;
import uk.ac.shef.dcs.oak.jate.util.counter.WordCounter;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * An example class that test all algorithms
 */
public class AlgorithmTester {

	private Map<Algorithm, AbstractFeatureWrapper> _algregistry = new HashMap<Algorithm, AbstractFeatureWrapper>();
	private static Logger _logger = Logger.getLogger(AlgorithmTester.class.getName());
	
	//NC Value code modification begins
	
	private Term[] result;
	private GlobalIndex _index;
	
	
	//NC Value code modification ends
	
	public void registerAlgorithm(Algorithm a, AbstractFeatureWrapper f) {
		_algregistry.put(a, f);
	}

	
	public void execute(GlobalIndex index, String outFolder) throws JATEException, IOException {
		//NCValue code modification begins
		
		_index=index;
		
		//NCValue code modification ends
		
		ResultWriter2File writer = new ResultWriter2File(index);
        if (_algregistry.size() == 0) throw new JATEException("No algorithm registered!");
        _logger.info("Running NP recognition...");

        /*.extractNP(c);*/
        for (Map.Entry<Algorithm, AbstractFeatureWrapper> en : _algregistry.entrySet()) {
            _logger.info("Running feature store builder and ATR..." + en.getKey().toString());
            result = en.getKey().execute(en.getValue()); 
            writer.output(result, outFolder + File.separator + en.getKey().toString() + ".txt");   
        }
      
	}
	
	/*NC Value code modification begins*/
    
	public Term[] getTerms(){
		return result;
	}
	
	public GlobalIndex getIndex(){
		return _index;
	}
	
    /*NC Value code modification ends*/
	
	
	/*modified code begins :RAKE*/
	
	public void execute(String outFolder) throws JATEException, IOException {
		ResultWriter2File writer = new ResultWriter2File();
        if (_algregistry.size() == 0) throw new JATEException("No algorithm registered!");
        _logger.info("Running NP recognition...");

        /*.extractNP(c);*/
        for (Map.Entry<Algorithm, AbstractFeatureWrapper> en : _algregistry.entrySet()) {
            _logger.info("Running feature store builder and ATR..." + en.getKey().toString());
            Term[] result = en.getKey().execute(en.getValue());
            writer.output(result, outFolder + File.separator + en.getKey().toString() + ".txt");
        }
	}
	
	/*Modified code ends :RAKE */
	
	
	
	

	public static void main(String[] args) {
		if (args.length < 3) System.out.println("Usage: java AlgorithmTester [corpus_path] [reference_corpus_path] [output_folder]");
		else {
			try {
				System.out.println(new Date());
                //##########################################################
                //#         Step 1. Extract candidate terms/words from     #
                //#         documents, and index the terms/words, docs     #
                //#         and their relations (occur-in, containing)     #
                //##########################################################

                //stop words and lemmatizer are used for processing the extraction of candidate terms
				StopList stop = new StopList(true);
				Lemmatizer lemmatizer = new Lemmatizer();

                //Three CandidateTermExtractor are implemented:
                //1. An OpenNLP noun phrase extractor that extracts noun phrases as candidate terms
				CandidateTermExtractor npextractor = new NounPhraseExtractorOpenNLP(stop, lemmatizer);
                //2. A generic N-gram extractor that extracts n(default is 5, see the property file) grams
                //CandidateTermExtractor npextractor = new NGramExtractor(stop, lemmatizer);
                //3. A word extractor that extracts single words as candidate terms.
                //CandidateTermExtractor wordextractor = new WordExtractor(stop, lemmatizer);

                //This instance of WordExtractor is needed to build word frequency data, which are required by some algorithms
				CandidateTermExtractor wordextractor = new WordExtractor(stop, lemmatizer, false, 1);

                GlobalIndexBuilderMem builder = new GlobalIndexBuilderMem();
				GlobalIndexMem wordDocIndex = builder.build(new CorpusImpl(args[0]), wordextractor);
				GlobalIndexMem termDocIndex = builder.build(new CorpusImpl(args[0]), npextractor);

                //Optionally, you can save the index data as HSQL databases on file system
               // GlobalIndexWriterHSQL.persist(wordDocIndex, "D:/work/JATR_SDK/jate_googlecode/test/output/worddb");
               // GlobalIndexWriterHSQL.persist(termDocIndex, "D:/work/JATR_SDK/jate_googlecode/test/output/termdb");


                //##########################################################
                //#         Step 2. Build various statistical features     #
                //#         used by term extraction algorithms. This will  #
                //#         need the indexes built above, and counting the #
                //#         frequencies of terms                           #
                //##########################################################

                //A WordCounter instance is required to count number of words in corpora/documents
                WordCounter wordcounter = new WordCounter();

                //Next we need to count frequencies of candidate terms. This is a computational extensive process
                // and can take long for large corpus.
                //
                // There are two ways of doing this, the first is to use multi-thread
                // counting process, by which the corpus is split into sections and several threads are run in parallel
                // for counting; the second is a single thread option. The first will use more memory and CPU but faster
                // on large corpus.

                /* #1 Due to use of multi-threading, this can significantly occupy your CPU and memory resources. It is
                 better to use this way on dedicated server machines, and only for very large corpus
                * */
                FeatureCorpusTermFrequency wordFreq =
                        new FeatureBuilderCorpusTermFrequencyMultiThread(wordcounter, lemmatizer).build(wordDocIndex);
				FeatureDocumentTermFrequency termDocFreq =
                        new FeatureBuilderDocumentTermFrequencyMultiThread(wordcounter, lemmatizer).build(termDocIndex);
				FeatureTermNest termNest =
                        new FeatureBuilderTermNestMultiThread().build(termDocIndex);
				FeatureRefCorpusTermFrequency bncRef =
						new FeatureBuilderRefCorpusTermFrequency(args[1]).build(null);
				FeatureCorpusTermFrequency termCorpusFreq =
                        new FeatureBuilderCorpusTermFrequencyMultiThread(wordcounter, lemmatizer).build(termDocIndex);

                /* #2 */
                /*
                TermFreqCounter npcounter = new TermFreqCounter();
                FeatureCorpusTermFrequency wordFreq =
						new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(wordDocIndex);
				FeatureDocumentTermFrequency termDocFreq =
						new FeatureBuilderDocumentTermFrequency(npcounter, wordcounter, lemmatizer).build(termDocIndex);
				FeatureTermNest termNest =
						new FeatureBuilderTermNest().build(termDocIndex);
				FeatureRefCorpusTermFrequency bncRef =
						new FeatureBuilderRefCorpusTermFrequency(args[1]).build(null);
				FeatureCorpusTermFrequency termCorpusFreq =
						new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termDocIndex);
                */


                //##########################################################
                //#         Step 3. For each algorithm you want to test    #
                //#         create an instance of the algorithm class,     #
                //#         and also an instance of its feature wrapper.   #
                //##########################################################
				AlgorithmTester tester = new AlgorithmTester();
				tester.registerAlgorithm(new TFIDFAlgorithm(), new TFIDFFeatureWrapper(termCorpusFreq));
				//tester.registerAlgorithm(new GlossExAlgorithm(), new GlossExFeatureWrapper(termCorpusFreq, wordFreq, bncRef));
				//tester.registerAlgorithm(new WeirdnessAlgorithm(), new WeirdnessFeatureWrapper(wordFreq, termCorpusFreq, bncRef));
				tester.registerAlgorithm(new CValueAlgorithm(), new CValueFeatureWrapper(termCorpusFreq, termNest));
			//	tester.registerAlgorithm(new TermExAlgorithm(), new TermExFeatureWrapper(termDocFreq, wordFreq, bncRef));
                tester.registerAlgorithm(new RIDFAlgorithm(), new RIDFFeatureWrapper(termCorpusFreq));
                tester.registerAlgorithm(new AverageCorpusTFAlgorithm(), new AverageCorpusTFFeatureWrapper(termCorpusFreq));
                tester.registerAlgorithm(new FrequencyAlgorithm(), new FrequencyFeatureWrapper(termCorpusFreq));

				tester.execute(termDocIndex, args[2]);
				System.out.println(new Date());

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
