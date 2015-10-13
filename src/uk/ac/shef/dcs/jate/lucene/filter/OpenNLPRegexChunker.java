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
public class OpenNLPRegexChunker extends MWEFilter {
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
    private RegexNameFinder regexChunker;

    public OpenNLPRegexChunker(
            TokenStream input,
            POSTagger posTaggerOp,
            Map<String, Pattern[]> patterns,
            int maxTokens,
            int minTokens,
            int maxCharLength,
            int minCharLength,
            boolean removeLeadingStopWords,
            boolean removeTrailingStopwords,
            boolean removeLeadingSymbolicTokens,
            boolean removeTrailingSymbolicTokens,
            Set<String> stopWords,
            boolean stopWordsIgnoreCase) {
        super(input, minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopWords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
        this.posTagger = posTaggerOp;
        regexChunker = new RegexNameFinder(patterns);
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
        //first pass remove stopwords
        if (removeLeadingStopwords || removeTrailingStopwords || removeLeadingSymbolicTokens || removeTrailingSymbolicTokens) {
            while (it.hasNext()) {
                Span span = it.next();
                int[] newspan = clean(span.getStart(), span.getEnd(), toks);
                if(newspan==null){
                    it.remove();
                    continue;
                }

                if (newspan[0] != span.getStart()||newspan[1]!=span.getEnd()) {
                    modified.add(new Span(newspan[0], newspan[1], span.getType(), span.getProb()));
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
            if (span.getEnd() - span.getStart() > maxTokens) {
                it.remove();
                continue;
            }
            if (span.getEnd() - span.getStart() < minTokens) {
                it.remove();
                continue;
            }
            String lookup = span.getStart() + "," + span.getEnd();
            if (existing.contains(lookup)) {
                it.remove();
                continue;
            }

            if (maxCharLength != 0 || minCharLength != 0) {
                StringBuilder string = new StringBuilder();
                for (int i = span.getStart(); i < span.getEnd(); i++) {
                    string.append(toks[i]).append(" ");
                }
                int chars = string.toString().trim().length();
                if ((maxCharLength != 0 && chars > maxCharLength) || (minCharLength != 0 && chars < minCharLength))
                    it.remove();
            }
        }
        Collections.sort(list);
        return list.toArray(new Span[0]);
    }

    private int[] clean(int start, int end, String[] tokens) {
        int newStart = start, newEnd = end;
        if (removeLeadingStopwords) {
            String tok = tokens[newStart];
            if (stopWordsIgnoreCase)
                tok = tok.toLowerCase();
            if (stopWords.contains(tok)) {
                newStart++;
                if (newStart > newEnd)
                    return null;
            }
        }

        if (removeTrailingStopwords) {
            String tok = tokens[newEnd];
            if (stopWordsIgnoreCase)
                tok = tok.toLowerCase();
            if (stopWords.contains(tok)) {
                newEnd--;
                if (newStart > newEnd)
                    return null;
            }
        }

        if (removeLeadingSymbolicTokens) {
            String tok = tokens[newStart];
            String normalized = tok.replaceAll("[\\p{Punct}]", "");
            if (normalized.length() == 0) {
                newStart++;
                if (newStart > newEnd)
                    return null;
            }
        }
        if (removeLeadingSymbolicTokens) {
            String tok = tokens[newEnd];
            String normalized = tok.replaceAll("[\\p{Punct}]", "");
            if (normalized.length() == 0) {
                newEnd--;
                if (newStart > newEnd)
                    return null;
            }
        }

        if(newEnd==end && newStart==start)
            return new int[]{newStart, newEnd};
        else
            return clean(newStart, newEnd, tokens);
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
