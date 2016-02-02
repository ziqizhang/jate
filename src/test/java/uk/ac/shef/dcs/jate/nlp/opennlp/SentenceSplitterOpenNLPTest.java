package uk.ac.shef.dcs.jate.nlp.opennlp;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.shef.dcs.jate.nlp.SentenceSplitter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SentenceSplitterOpenNLPTest {
    static String workingDir = System.getProperty("user.dir");
    Path EN_SENT_MODEL_FILE = Paths.get(workingDir, "src", "main", "resource", "opennlp-models", "en-sent.bin");

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
}
