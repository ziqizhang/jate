package uk.ac.shef.dcs.jate.v2.lucene.filter;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zqz on 25/09/2015.
 */
public class OpenNLPRegexChunker extends TokenFilter {
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected OpenNLPRegexChunker(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        return false;
    }

    public static void main(String[] args) {
        Map<String, Pattern[]> patterns = new HashMap<>();
        patterns.put("NP",new Pattern[]{Pattern.compile("(NN)+")});
        RegexNameFinder rnf = new RegexNameFinder(patterns);
        String[] tokPOS = new String[]{"DT","NNS","NN","DT","NN","OT"};
        Span[] rs=rnf.find(tokPOS);
        System.out.println(rs);
    }
}
