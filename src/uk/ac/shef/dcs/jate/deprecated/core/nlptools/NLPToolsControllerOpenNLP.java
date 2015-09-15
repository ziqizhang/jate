package uk.ac.shef.dcs.jate.deprecated.core.nlptools;

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
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * A singleton class which controls creation and dispatches of OpenNLP tools
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class NLPToolsControllerOpenNLP {

	private static NLPToolsControllerOpenNLP _ref;
	private POSTagger _posTagger;
	private Chunker _npChunker;
	private SentenceDetector _sentDetect;
	private Tokenizer _tokenizer;

	private NLPToolsControllerOpenNLP() throws IOException {
        POSModel posModel = new POSModel(new FileInputStream(JATEProperties.getInstance().getNLPPath()+"/en-pos-maxent.bin"));
		_posTagger = new POSTaggerME(posModel);

        ChunkerModel chunkerModel = new ChunkerModel(new FileInputStream(JATEProperties.getInstance().getNLPPath()+"/en-chunker.bin"));
		_npChunker = new ChunkerME(chunkerModel);

        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(JATEProperties.getInstance().getNLPPath()+"/en-token.bin"));
		_tokenizer = new TokenizerME(tokenizerModel);

        SentenceModel sentModel = new SentenceModel(new FileInputStream(JATEProperties.getInstance().getNLPPath()+"/en-sent.bin"));
        _sentDetect = new SentenceDetectorME(sentModel);


	}

	public static NLPToolsControllerOpenNLP getInstance() throws IOException {
		if(_ref ==null) _ref=new NLPToolsControllerOpenNLP();
		return _ref;
	}

	public Object clone() throws CloneNotSupportedException {
      throw new CloneNotSupportedException();
   }

	public POSTagger getPosTagger() {
		return _posTagger;
	}

	public Chunker getPhraseChunker() {
		return _npChunker;
	}

	public SentenceDetector getSentenceSplitter() {
		return _sentDetect;
	}

	public Tokenizer getTokeniser() {
		return _tokenizer;
	}
}
