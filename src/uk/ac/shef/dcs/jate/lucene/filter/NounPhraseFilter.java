package uk.ac.shef.dcs.jate.lucene.filter;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.namefind.RegexNameFinder;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class NounPhraseFilter extends MWEFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    private boolean first = true;
    private static String SENTENCE_BREAK = "[.?!]";
    // cloned attrs of all tokens
    private List<AttributeSource> tokenAttrs = new ArrayList<>();
    private Map<Integer, Integer> chunkSpans = new HashMap<>(); //start, end; end is exclusive
    private Map<Integer, String> chunkTypes = new HashMap<>();
    private int chunkStart = -1;
    private int chunkEnd = -1;
    private int tokenIdx = 0;

    private POSTagger posTagger;
    private Chunker npChunker;
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected NounPhraseFilter(TokenStream input,
                               POSTagger posTaggerOp,
                               Chunker npChunker,
                               int minTokens, int maxTokens,
                               int minCharLength, int maxCharLength,
                               boolean removeLeadingStopWords,
                               boolean removeTrailingStopwords,
                               boolean removeLeadingSymbolicTokens,
                               boolean removeTrailingSymbolicTokens,
                               Set<String> stopWords,
                               boolean stopWordsIgnoreCase) {
        super(input, minTokens, maxTokens,
                minCharLength, maxCharLength,
                removeLeadingStopWords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
        posTagger=posTaggerOp;
        this.npChunker=npChunker;
    }

    @Override
    public boolean incrementToken() throws IOException {
        return false;
    }
}
