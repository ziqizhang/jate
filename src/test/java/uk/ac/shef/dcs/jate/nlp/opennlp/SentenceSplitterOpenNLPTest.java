package uk.ac.shef.dcs.jate.nlp.opennlp;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;
import uk.ac.shef.dcs.jate.nlp.SentenceSplitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SentenceSplitterOpenNLPTest {
    static String workingDir = System.getProperty("user.dir");
    Path EN_SENT_MODEL_FILE = Paths.get(workingDir, "src", "test", "resource", "opennlp-models", "en-sent.bin");

    @Test
    public void testOpenNLPSentenceSplitter() throws IOException {
        SentenceSplitter sentSplitter = new SentenceSplitterOpenNLP(EN_SENT_MODEL_FILE.toFile());

        String text = "P4 . Vocabulary learning means learning the words and their limitations , " +
                "probability of occurrences , and syntactic behavior around them , Swartz & Yazdani -LRB- 1992 " +
                "-RRB- . Answers 7 and 10 are examples of bypassing strategies i.e. ; the use of a different verb " +
                "or another sentence structure as a means for avoiding relative clauses .Children , who now go " +
                "to french schools , often switch back to English for their leisure activities because of the " +
                "scarcity of options open to them .When the children were asked about the main subject in the " +
                "picture , the answers were acceptable in standard French , showing that they had no problems " +
                "in using relative clauses with qui .";
        List<int[]> sentSize = sentSplitter.split(text);

        Assert.assertNotNull(sentSize);
        Assert.assertEquals("Should split the text into total 3 sentences.", 3, sentSize.size());
    }

    @Test
    public void testOpenNLPPOSTagger() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Path EN_POS_MODEL_FILE = Paths.get(workingDir, "src", "test", "resource", "opennlp-models", "en-pos-maxent.bin");
        Path patternFile = Paths.get(workingDir, "src", "test", "resource", "eval", "ACL_RD-TEC", "aclrdtec.patterns");

        FileInputStream fis = new FileInputStream(EN_POS_MODEL_FILE.toFile());
        FileInputStream pis = new FileInputStream(patternFile.toFile());

        String sentence = "SEMI-LITERAL Each audio file has been processed to produce a semi-literal " +
                "transcription that was then aligned with recognition output generated in the " +
                "process of creating semi-literal transcriptions . " +
                "The portions of the audio corresponding to matching segments were used for acoustic adaptation training .";

        POSTagger posTagger = InstanceCreator.createPOSTagger("uk.ac.shef.dcs.jate.nlp.opennlp.POSTaggerOpenNLP",
                fis);
        String[] tokens = sentence.split(" ");
        String[] posTags = posTagger.tag(tokens);

        assert tokens.length == posTags.length;
        assert "NN".equals(posTags[posTags.length - 2]);
        assert "NN".equals(posTags[posTags.length - 3]);
        assert "JJ".equals(posTags[posTags.length - 4]);

        List<String> lines = WordlistLoader.getLines(pis, StandardCharsets.UTF_8);

        RegexNameFinder regexChunker = new RegexNameFinder(initPatterns(lines));
        Span[] chunks = regexChunker.find(posTags);

        List<String> candidateTerms = new ArrayList<>();
        for (Span span : chunks) {
            StringBuffer candidateTerm = new StringBuffer();
            for (int index=span.getStart(); index < span.getEnd(); index++ ) {
                candidateTerm.append(tokens[index]+ " ");
            }
            System.out.println(candidateTerm.toString().trim());
            candidateTerms.add(candidateTerm.toString().trim());
        }

        assert candidateTerms.size() == 15;
        assert "training".equals(candidateTerms.get(7));
        assert "audio".equals(candidateTerms.get(0));
        assert "audio file".equals(candidateTerms.get(8));
        assert "training".equals(candidateTerms.get(7));
        assert "acoustic adaptation training".equals(candidateTerms.get(14));
        assert "semi-literal transcriptions".equals(candidateTerms.get(13));

    }

    private Map<String, Pattern[]> initPatterns(List<String> rawData) throws IOException {
        //is patternStr a file?
        Map<String, Pattern[]> patterns = new HashMap<>();

        Map<String, List<Pattern>> m = new HashMap<>();
        for (String lineStr : rawData) {
            if (lineStr.trim().length() == 0 || lineStr.startsWith("#"))
                continue;
            String[] parts = lineStr.split("\t", 2);
            List<Pattern> pats = m.get(parts[0]);
            if (pats == null)
                pats = new ArrayList<>();
            pats.add(Pattern.compile(parts[1]));
            m.put(parts[0], pats);
        }
        for (Map.Entry<String, List<Pattern>> en : m.entrySet()) {
            patterns.put(en.getKey(), en.getValue().toArray(new Pattern[0]));
        }

        return patterns;

    }
}
