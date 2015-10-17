package org.apache.lucene.analysis.jate;

import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.IOException;
import java.util.*;

/**
 */
public abstract class OpenNLPMWEFilter extends MWEFilter{

    protected final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    protected final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    protected final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    protected boolean first = true;
    protected static String SENTENCE_BREAK = "[.?!]";
    // cloned attrs of all tokens
    protected List<AttributeSource> tokenAttrs = new ArrayList<>();
    protected Map<Integer, Integer> chunkSpans = new HashMap<>(); //start, end; end is exclusive
    protected Map<Integer, String> chunkTypes = new HashMap<>();
    protected int chunkStart = -1;
    protected int chunkEnd = -1;
    protected int tokenIdx = 0;


    public OpenNLPMWEFilter(TokenStream input, int minTokens, int maxTokens,
                            int minCharLength, int maxCharLength,
                            boolean removeLeadingStopWords, boolean removeTrailingStopwords,
                            boolean removeLeadingSymbolicTokens, boolean removeTrailingSymbolicTokens,
                            Set<String> stopWords, boolean stopWordsIgnoreCase) {
        super(input, minTokens, maxTokens,
                minCharLength, maxCharLength,
                removeLeadingStopWords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }

    protected OpenNLPMWEFilter(TokenStream input) {
        super(input);
    }


    protected boolean addMWE(){
        AttributeSource start = tokenAttrs.get(chunkStart);
        AttributeSource end = tokenAttrs.get(chunkEnd - 1);

        SentenceContext firstTokenSentCtx = parseSentenceContextPayload(start.getAttribute(PayloadAttribute.class));
        SentenceContext lastTokenSentCtx = parseSentenceContextPayload(end.getAttribute(PayloadAttribute.class));

        boolean added=false;
        if(!crossBoundary(firstTokenSentCtx, lastTokenSentCtx)) {
            StringBuilder phrase = new StringBuilder();
            for (int i = chunkStart; i <= chunkEnd - 1; i++)
                phrase.append(tokenAttrs.get(i).getAttribute(CharTermAttribute.class).buffer()).append(" ");

            termAtt.setEmpty().append(phrase.toString().trim());
            offsetAtt.setOffset(start.getAttribute(OffsetAttribute.class).startOffset(),
                    end.getAttribute(OffsetAttribute.class).endOffset());
            typeAtt.setType(chunkTypes.get(chunkStart));
            addSentenceContextPayload(firstTokenSentCtx, lastTokenSentCtx);
            added=true;
            //System.out.println(phrase.toString().trim()+","+sentenceContextAtt.getPayload().utf8ToString());
        }

        chunkStart = -1;
        chunkEnd = -1;
        return added;
    }
    private SentenceContext parseSentenceContextPayload(PayloadAttribute attribute){
        BytesRef bfTokenSentCtx = attribute != null ? attribute.getPayload() : null;
        SentenceContext tokenSentCtx = bfTokenSentCtx == null ? null : new SentenceContext(
                bfTokenSentCtx.utf8ToString()
        );
        return tokenSentCtx;
    }

    private void addSentenceContextPayload(SentenceContext firstTokenSentCtx,
                                           SentenceContext lastTokenSentCtx){
        if(firstTokenSentCtx!=null && lastTokenSentCtx!=null){
            addSentenceContext(sentenceContext, firstTokenSentCtx.getFirstTokenIdx(),
                    lastTokenSentCtx.getLastTokenIdx(),
                    firstTokenSentCtx.getPosTag(),
                    lastTokenSentCtx.getSentenceId());
        }
    }

    private boolean crossBoundary(SentenceContext firstTokenSentCtx,
                                  SentenceContext lastTokenSentCtx){
        if(firstTokenSentCtx!=null && lastTokenSentCtx!=null){
            return !firstTokenSentCtx.getSentenceId().equals(lastTokenSentCtx.getSentenceId());
        }
        return false;
    }

    protected Span[] prune(Span[] chunks, String[] toks) {
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
            if (maxCharLength != 0 || minCharLength != 0) {
                StringBuilder string = new StringBuilder();
                for (int i = span.getStart(); i < span.getEnd(); i++) {
                    string.append(toks[i]).append(" ");
                }
                int chars = string.toString().trim().length();
                if ((maxCharLength != 0 && chars > maxCharLength) || (minCharLength != 0 && chars < minCharLength))
                    it.remove();
            }

            String lookup = span.getStart() + "," + span.getEnd();
            if (existing.contains(lookup)) {
                it.remove();
                continue;
            }

            existing.add(lookup);
        }
        Collections.sort(list);
        return list.toArray(new Span[0]);
    }

    /**
     *
     * @param start
     * @param end THIS IS EXCLUSIVE
     * @param tokens
     * @return
     */
    protected int[] clean(int start, int end, String[] tokens) {
        int newStart = start, newEnd = end;
        if (removeLeadingStopwords) {
            String tok = tokens[newStart];
            if (stopWordsIgnoreCase)
                tok = tok.toLowerCase();
            if (stopWords.contains(tok)) {
                newStart++;
                if (newStart >= newEnd)
                    return null;
            }
        }

        if (removeTrailingStopwords) {
            String tok = tokens[newEnd-1];
            if (stopWordsIgnoreCase)
                tok = tok.toLowerCase();
            if (stopWords.contains(tok)) {
                newEnd--;
                if (newStart >= newEnd)
                    return null;
            }
        }

        if (removeLeadingSymbolicTokens) {
            String tok = tokens[newStart];
            String normalized = tok.replaceAll("[\\p{Punct}]", "");
            if (normalized.length() == 0) {
                newStart++;
                if (newStart >= newEnd)
                    return null;
            }
        }
        if (removeLeadingSymbolicTokens) {
            String tok = tokens[newEnd-1];
            String normalized = tok.replaceAll("[\\p{Punct}]", "");
            if (normalized.length() == 0) {
                newEnd--;
                if (newStart >= newEnd)
                    return null;
            }
        }

        if(newEnd==end && newStart==start)
            return new int[]{newStart, newEnd};
        else if(newEnd-newStart==1)
            return new int[]{newStart, newEnd};
        else
            return clean(newStart, newEnd, tokens);
    }

    protected void resetParams() {
        first = true;
        tokenIdx = 0;
        chunkStart = -1;
        chunkEnd = -1;
        chunkSpans.clear();
        chunkTypes.clear();
    }


    protected String[][] walkTokens() throws IOException {
        List<String> wordList = new ArrayList<>();
        List<String> posList = new ArrayList<>();
        while (input.incrementToken()) {
            CharTermAttribute textAtt = input.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAtt = input.getAttribute(OffsetAttribute.class);
            char[] buffer = textAtt.buffer();
            String word = new String(buffer, 0, offsetAtt.endOffset() - offsetAtt.startOffset());
            wordList.add(word);
            PayloadAttribute posAtt = input.getAttribute(PayloadAttribute.class);
            if(posAtt!=null){
                posList.add(new SentenceContext(posAtt.getPayload().utf8ToString()).getPosTag());
            }
            AttributeSource attrs = input.cloneAttributes();
            tokenAttrs.add(attrs);
        }
        if(wordList.size()!=posList.size()){
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(" requires both token and token POS. Tokens=").append(wordList.size())
                    .append(", POS=").append(posList.size()).append(", and they are inconsistent.")
                    .append(" Have you enabled POS tagging in your Solr analyzer chain?");
            throw new IOException(sb.toString());
        }
        String[] words = new String[wordList.size()];
        String[] pos = new String[posList.size()];
        for (int i = 0; i < words.length; i++) {
            words[i] = wordList.get(i);
            pos[i]=posList.get(i);
        }
        return new String[][]{words,pos};
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
