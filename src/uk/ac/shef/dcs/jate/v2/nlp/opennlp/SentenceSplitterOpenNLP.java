package uk.ac.shef.dcs.jate.v2.nlp.opennlp;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import uk.ac.shef.dcs.jate.v2.nlp.SentenceSplitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zqz on 24/09/2015.
 */
public class SentenceSplitterOpenNLP implements SentenceSplitter {
    protected SentenceDetector sentenceDetector;

    public SentenceSplitterOpenNLP(String modelFile) throws IOException {
        sentenceDetector= new SentenceDetectorME(new SentenceModel(new FileInputStream(modelFile)));
    }

    public List<int[]> split(String text){
        Span[] offsets = sentenceDetector.sentPosDetect(text);
        List<int[]> rs  =new ArrayList<>();
        for(Span s: offsets){
            rs.add(new int[]{s.getStart(), s.getEnd()});
        }
        return rs;
    }

}
