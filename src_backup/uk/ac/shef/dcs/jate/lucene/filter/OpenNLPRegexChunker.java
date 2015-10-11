package uk.ac.shef.dcs.jate.lucene.filter;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by zqz on 25/09/2015.
 */
public class OpenNLPRegexChunker extends TokenFilter {
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
    private int maxPhraseSize;
    private int minPhraseChar;
    private int maxPhraseChar;
    private List<String> leadingStopWords;
    private boolean stopWordsIgnoreCase = true;

    private POSTagger posTagger;
    private RegexNameFinder regexChunker;

    public OpenNLPRegexChunker(
            TokenStream input,
            POSTagger posTaggerOp,
            Map<String, Pattern[]> patterns,
            int maxPhraseSize,
            int maxPhraseChars,
            int minPhraseChars,
            List<String> leadingStopWords,
            boolean stopWordsIgnoreCase) {
        super(input);
        this.posTagger = posTaggerOp;
        regexChunker = new RegexNameFinder(patterns);
        this.maxPhraseSize = maxPhraseSize;
        this.maxPhraseChar = maxPhraseChars;
        this.minPhraseChar = minPhraseChars;
        this.leadingStopWords = leadingStopWords;
        this.stopWordsIgnoreCase = stopWordsIgnoreCase;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (first) {
            //gather all tokens from doc
            String[] words = walkTokens();
            if (words.length == 0) {
                return false;
            }
            //tagging
            String[] pos = createTags(words);
            //chunking
            Span[] chunks = regexChunker.find(pos);
            chunks = prune(chunks, words);
            for (Span sp : chunks) {
                chunkSpans.put(sp.getStart(), sp.getEnd());
                chunkTypes.put(sp.getStart(), sp.getType());
            }
            first = false;
            tokenIdx = 0;
        }

        if (tokenIdx == tokenAttrs.size()) {
            resetParams();
            return false;
        }

        if (chunkStart != -1 && tokenIdx == chunkEnd) {  //already found a new chunk and now we found its end
            AttributeSource start = tokenAttrs.get(chunkStart);
            AttributeSource end = tokenAttrs.get(chunkEnd - 1);
            StringBuilder phrase = new StringBuilder();
            for (int i = chunkStart; i <= chunkEnd - 1; i++) {
                phrase.append(tokenAttrs.get(i).getAttribute(CharTermAttribute.class).buffer()).append(" ");
            }
            termAtt.setEmpty().append(phrase.toString().trim());
            offsetAtt.setOffset(start.getAttribute(OffsetAttribute.class).startOffset(),
                    end.getAttribute(OffsetAttribute.class).endOffset());
            typeAtt.setType(chunkTypes.get(chunkStart));

            chunkStart = -1;
            chunkEnd = -1;
            //do not increment token index here because end span is exclusive
            return true;
        }
        if (chunkSpans.containsKey(tokenIdx)) { //at the beginning of a new chunk
            chunkStart = tokenIdx;
            chunkEnd = chunkSpans.get(tokenIdx);
            tokenIdx++;
            return true;
        } else { //a token that is not part of a chunk
            tokenIdx++;
            return true;
        }
    }

    private Span[] prune(Span[] chunks, String[] toks) {
        Set<String> existing = new HashSet<>();
        List<Span> list = new ArrayList<>(Arrays.asList(chunks));
        Iterator<Span> it = list.iterator();
        List<Span> modified = new ArrayList<>();
        //first pass remove leading stopwords
        if (leadingStopWords != null && leadingStopWords.size() > 0) {
            while (it.hasNext()) {
                Span span = it.next();
                int newStart = -1;
                for (int i = span.getStart(); i < span.getEnd(); i++) {
                    String tok = toks[i];
                    if (stopWordsIgnoreCase)
                        tok = tok.toLowerCase();
                    if (!leadingStopWords.contains(tok)) {
                        newStart = i;
                        break;
                    }
                }
                if (newStart == -1) {
                    it.remove();
                    continue;
                }
                if (newStart != span.getStart()) {
                    modified.add(new Span(newStart, span.getEnd(), span.getType(), span.getProb()));
                    it.remove();
                }
            }
        }
        list.addAll(modified);
        Collections.sort(list);

        //second pass check other restrictions
        it = list.iterator();
        while (it.hasNext()) {
            Span span = it.next();
            if (span.getEnd() - span.getStart() > maxPhraseSize) {
                it.remove();
                continue;
            }
            String lookup = span.getStart() + "," + span.getEnd();
            if (existing.contains(lookup)) {
                it.remove();
                continue;
            }

            if (maxPhraseChar != 0 || minPhraseChar != 0) {
                StringBuilder string = new StringBuilder();
                for (int i = span.getStart(); i < span.getEnd(); i++) {
                    string.append(toks[i]).append(" ");
                }
                int chars = string.toString().trim().length();
                if ((maxPhraseChar != 0 && chars > maxPhraseChar) || (minPhraseChar != 0 && chars < minPhraseChar))
                    it.remove();
            }
        }
        Collections.sort(list);
        return list.toArray(new Span[0]);
    }

    private void resetParams() {
        first = true;
        tokenIdx = 0;
        chunkStart = -1;
        chunkEnd = -1;
        chunkSpans.clear();
        chunkTypes.clear();
    }

    private String[] createTags(String[] words) {
        String[] appended = appendDot(words);
        return assignPOS(appended);
    }

    private String[] assignPOS(String[] words) {
        return posTagger.tag(words);
    }

    // Hack #1: taggers expect a sentence break as the final term.
    // This does not make it into the attribute set lists.
    private String[] appendDot(String[] words) {
        int nWords = words.length;
        String lastWord = words[nWords - 1];
        if (lastWord.length() != 1) {
            return words;
        }
        if (lastWord.matches(SENTENCE_BREAK)) {
            return words;
        }
        words = Arrays.copyOf(words, nWords + 1);
        words[nWords] = ".";
        return words;
    }

    private String[] walkTokens() throws IOException {
        List<String> wordList = new ArrayList<>();
        while (input.incrementToken()) {
            CharTermAttribute textAtt = input.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAtt = input.getAttribute(OffsetAttribute.class);
            char[] buffer = textAtt.buffer();
            String word = new String(buffer, 0, offsetAtt.endOffset() - offsetAtt.startOffset());
            wordList.add(word);
            AttributeSource attrs = input.cloneAttributes();
            tokenAttrs.add(attrs);
        }
        String[] words = new String[wordList.size()];
        for (int i = 0; i < words.length; i++) {
            words[i] = wordList.get(i);
        }
        return words;
    }

    @Override
    public final void end() throws IOException {
        super.end();
        clearAttributes();
        tokenAttrs.clear();
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        clearAttributes();
        resetParams();
    }

}
