package uk.ac.shef.dcs.jate.core.nlptools;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * A singleton class which controls creation and dispatches of OpenNLP tools
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class NLPToolsControllerOpenNLP {

	private static NLPToolsControllerOpenNLP _ref;
	private final POSTagger _posTagger;
	private final Chunker _npChunker;
	private final SentenceDetector _sentDetect;
	private final Tokenizer _tokenizer;
	private final String locale;

	public NLPToolsControllerOpenNLP() throws IOException {
            this("en");
	}
	
	public NLPToolsControllerOpenNLP(String locale) throws IOException {
	String basePath = JATEProperties.getInstance().getNLPPath() + "/" + locale;
        POSModel posModel = new POSModel(new FileInputStream(basePath + "-pos.bin"));
		_posTagger = new POSTaggerME(posModel);

        ChunkerModel chunkerModel = new ChunkerModel(new FileInputStream(basePath + "-chunker.bin"));
		_npChunker = new ChunkerME(chunkerModel);

        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(basePath + "-token.bin"));
		_tokenizer = new TokenizerME(tokenizerModel);

        SentenceModel sentModel = new SentenceModel(new FileInputStream(basePath + "-sent.bin"));
        _sentDetect = new SentenceDetectorME(sentModel);
        this.locale = locale;
	}
	
        public synchronized static NLPToolsControllerOpenNLP setInstance(NLPToolsControllerOpenNLP nlp) throws IOException {
            _ref = nlp;
            return _ref;
        }

	public synchronized static NLPToolsControllerOpenNLP getInstance() throws IOException {
	    return _ref;
	}

	public Object clone() throws CloneNotSupportedException {
      throw new CloneNotSupportedException();
   }

	public synchronized POSTagger getPosTagger() {
		return _posTagger;
	}

	public synchronized Chunker getPhraseChunker() {
		return _npChunker;
	}

	public synchronized SentenceDetector getSentenceSplitter() {
		return _sentDetect;
	}

	public synchronized Tokenizer getTokeniser() {
		return _tokenizer;
	}
	
	public String getLocale()
	{
	    return locale;
	}
}
