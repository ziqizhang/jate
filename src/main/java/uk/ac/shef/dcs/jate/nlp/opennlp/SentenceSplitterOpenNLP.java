package uk.ac.shef.dcs.jate.nlp.opennlp;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.nlp.SentenceSplitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SentenceSplitterOpenNLP implements SentenceSplitter {
	private static Logger LOG = Logger.getLogger(SentenceSplitterOpenNLP.class.getName());
	protected SentenceDetector sentenceDetector;

	public SentenceSplitterOpenNLP(InputStream model) throws IOException {
		LOG.info("Initializing OpenNLP sentence splitter...");
		sentenceDetector = new SentenceDetectorME(new SentenceModel(model));
	}

	public SentenceSplitterOpenNLP(String modelFile) throws IOException {
		LOG.info("Initializing OpenNLP sentence splitter...");
		sentenceDetector = new SentenceDetectorME(new SentenceModel(new FileInputStream(modelFile)));
	}

	public List<int[]> split(String text) {
		Span[] offsets = sentenceDetector.sentPosDetect(text);
		List<int[]> rs = new ArrayList<>();
		for (Span s : offsets) {
			rs.add(new int[] { s.getStart(), s.getEnd() });
		}
		return rs;
	}

	public static void testOpenNLPSentenceSplitter() throws JATEException {
		try {
			Path currentRelativePath = Paths.get("");
			String prjRoot = currentRelativePath.toAbsolutePath().toString();

			SentenceSplitter sentSplitter = new SentenceSplitterOpenNLP(
					prjRoot + File.separator + "resource" + File.separator + "en-sent.bin");
			List<int[]> sentSize = sentSplitter.split(
					"P4 . Vocabulary learning means learning the words and their limitations , probability of occurrences , and syntactic behavior around them , Swartz & Yazdani -LRB- 1992 -RRB- . Answers 7 and 10 are examples of bypassing strategies i.e. ; the use of a different verb or another sentence structure as a means for avoiding relative clauses .Children , who now go to french schools , often switch back to English for their leisure activities because of the scarcity of options open to them .When the children were asked about the main subject in the picture , the answers were acceptable in standard French , showing that they had no problems in using relative clauses with qui .");
			System.out.println("should have 3 sentences" + sentSize.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new JATEException("sentence splitter model failed to be loaded!");
		}
	}

	public static void main(String[] args) throws JATEException {
		testOpenNLPSentenceSplitter();
	}
}
