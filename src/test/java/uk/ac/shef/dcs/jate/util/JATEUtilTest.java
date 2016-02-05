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
}
