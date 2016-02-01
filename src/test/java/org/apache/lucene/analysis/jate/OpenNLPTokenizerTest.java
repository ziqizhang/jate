package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.FilesystemResourceLoader;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpenNLPTokenizerTest {

    String workingDir = System.getProperty("user.dir");

    OpenNLPTokenizer tokenizer;

    @Before
    public void setup() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("sentenceModel", workingDir+"\\testdata\\solr-testbed\\testCore\\conf\\en-sent.bin");
        args.put("tokenizerModel", workingDir+"\\testdata\\solr-testbed\\testCore\\conf\\en-token.bin");

        OpenNLPTokenizerFactory openNLPTokenizerFactory  = new OpenNLPTokenizerFactory(args);

        openNLPTokenizerFactory.inform(new FilesystemResourceLoader(Paths.get(new File(workingDir).toURI())));

        tokenizer = (OpenNLPTokenizer)openNLPTokenizerFactory.create(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
    }

    @After
    public void tearDown() throws IOException {

    }
}
