package uk.ac.shef.dcs.jate.nlp.opennlp;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import uk.ac.shef.dcs.jate.nlp.SentenceSplitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public SentenceSplitterOpenNLP(File modelFile) throws IOException {
        LOG.info("Initializing OpenNLP sentence splitter...");
        sentenceDetector = new SentenceDetectorME(new SentenceModel(modelFile));
    }

    public List<int[]> split(String text) {
        Span[] offsets = sentenceDetector.sentPosDetect(text);
        List<int[]> rs = new ArrayList<>();
        for (Span s : offsets) {
            rs.add(new int[]{s.getStart(), s.getEnd()});
        }
        return rs;
    }
}
