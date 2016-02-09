package uk.ac.shef.dcs.jate.util;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class JATEUtilTest {
    static String workingDir = System.getProperty("user.dir");

    @Test
    public void cleanTextTest() {
        String text = "P r e v i o u s Efforts, C H A T - 8 0 , P R A T - 8 9 and HSQL Trondheim " +
                "is a small city with a university and 140000 inhabitants.";

        String cleanedText = JATEUtil.cleanText(text);

        Assert.assertTrue(("Previous Efforts, CHAT - 8 0 , PRAT - 8 9 and HSQL Trondheim is " +
                "a small city with a university and 140000 inhabitants.").equals(cleanedText));
    }

    @Test
    public void loadDocumentText() throws FileNotFoundException, JATEException {
        Path cleanedXMLDoc = Paths.get(workingDir, "src", "test", "resource", "eval", "A00-1001_cln.xml");
        JATEDocument jateDocument = JATEUtil.loadACLRDTECDocument(new FileInputStream(cleanedXMLDoc.toFile()));
        assert jateDocument.getId().equals("A00-1001");
        assert jateDocument.getContent() != null;
        assert jateDocument.getContent().length() > 200;
    }

    @Test
    public void loadDocumentText2() throws FileNotFoundException, JATEException {
        //check parsing error
        Path cleanedXMLDoc = Paths.get(workingDir, "src", "test", "resource", "eval", "E06-2023_cln.xml");
        JATEDocument jateDocument = JATEUtil.loadACLRDTECDocument(new FileInputStream(cleanedXMLDoc.toFile()));
        assert jateDocument.getId().equals("E06-2023");
        assert jateDocument.getContent() != null;

        String paragraph0 = "According to linguistic theory, morphemes are considered to be the smallest " +
                "meaning-bearing elements of a language. However, no adequate language-independent definition of " +
                "the word as a unit has been agreed upon. If effective methods can be devised for the unsupervised " +
                "discovery of morphemes, they could aid the formulation of a linguistic theory of morphology for a " +
                "new language. The utilization of morphemes as basic representational units in a statistical " +
                "language model instead of words seems a promising course [Creutz, 2004].";

        assert jateDocument.getContent().contains(paragraph0);

        String paragraph1 = "Many natural language processing tasks, including parsing, semantic modeling, " +
                "information retrieval, and machine translation, frequently require a morphological analysis " +
                "of the language at hand. The task of a morphological analyzer is to identify the lexeme, " +
                "citation form, or inflection class of surface word forms in a language. It seems that even " +
                "approximate automated morphological analysis would be beneficial for many NL applications " +
                "dealing with large vocabularies (e.g. text retrieval applications).";

        assert jateDocument.getContent().contains(paragraph1);

        String paragraph2 = "[Monson 2004] presents a framework for unsupervised induction of natural language " +
                "morphology, wherein candidate suffixes are grouped into candidate inflection classes, which are " +
                "then placed in a lattice structure. With similar arranged inflection classes placed near one " +
                "candidate in the lattice, it proposes this structure to be an ideal search space in which to " +
                "isolate the true inflection classes of a language. [Schone and Jurafsky 2000] presents an " +
                "unsupervised model in which knowledge-free distributional cues are combined orthography-based " +
                "with information automatically extracted from semantic word co-occurrence patterns " +
                "in the input corpus.";

        assert jateDocument.getContent().contains(paragraph2);
    }

    @Test
    public void testLoadACLRDTECDocument() throws FileNotFoundException, JATEException {
        Path cleanedXMLDoc = Paths.get(workingDir, "src", "test", "resource", "eval", "E06-2023_cln.xml");
        JATEDocument jateDocument = JATEUtil.loadACLRDTECDocument(new FileInputStream(cleanedXMLDoc.toFile()));

        assert jateDocument != null;
        assert jateDocument.getId() != null;
        assert jateDocument.getId().equals("P06-1139");
        assert jateDocument.getContent() != null;

        //ignore reference title
        Assert.assertEquals(-1,jateDocument.getContent().indexOf("generation.Introduction"));
        //ignore reference title
        Assert.assertEquals(-1, jateDocument.getContent().indexOf("5.0.Fast"));
        //ignore reference title
        Assert.assertEquals(-1, jateDocument.getContent().indexOf("Fast decoding and optimal"));
        Assert.assertEquals(-1, jateDocument.getContent().indexOf("generation.Statistical"));
    }
}
