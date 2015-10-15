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
 * Created by - on 14/10/2015.
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

    protected POSTagger posTagger;

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

        String[] firstTokenSentCtx = parseSentenceContextPayload(start.getAttribute(PayloadAttribute.class));
        String[] lastTokenSentCtx = parseSentenceContextPayload(end.getAttribute(PayloadAttribute.class));

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
        }

        chunkStart = -1;
        chunkEnd = -1;
        return added;
    }
    private String[] parseSentenceContextPayload(PayloadAttribute attribute){
        BytesRef bfTokenSentCtx = attribute != null ? attribute.getPayload() : null;
        String[] tokenSentCtx = bfTokenSentCtx == null ? null : SentenceContext.parseString(
                bfTokenSentCtx.utf8ToString()
        );
        return tokenSentCtx;
    }

    private void addSentenceContextPayload(String[] firstTokenSentCtx,
                                             String[] lastTokenSentCtx){
        if(firstTokenSentCtx!=null && lastTokenSentCtx!=null){

            addSentenceContext(sentenceContextAtt, firstTokenSentCtx[0],
                    lastTokenSentCtx[1], lastTokenSentCtx[2]);
        }
    }

    private boolean crossBoundary(String[] firstTokenSentCtx,
                                            String[] lastTokenSentCtx){
        if(firstTokenSentCtx!=null && lastTokenSentCtx!=null){
            return !firstTokenSentCtx[2].equals(lastTokenSentCtx[2]);
        }
        return false;
    }

    protected List<Span> prune(List<Span> chunks, List<String> toks) {
        Set<String> existing = new HashSet<>();
        Iterator<Span> it = chunks.iterator();
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
        chunks.addAll(modified);
        Collections.sort(chunks);

        //second pass check other restrictions
        it = chunks.iterator();
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
                    string.append(toks.get(i)).append(" ");
                }
                int chars = string.toString().trim().length();
                if ((maxCharLength != 0 && chars > maxCharLength) || (minCharLength != 0 && chars < minCharLength))
                    it.remove();
            }

            existing.add(lookup);
        }
        Collections.sort(chunks);
        return chunks;
    }

    /**
     *
     * @param start
     * @param end THIS IS EXCLUSIVE
     * @param tokens
     * @return
     */
    protected int[] clean(int start, int end, List<String> tokens) {
        int newStart = start, newEnd = end;
        if (removeLeadingStopwords) {
            String tok = tokens.get(newStart);
            if (stopWordsIgnoreCase)
                tok = tok.toLowerCase();
            if (stopWords.contains(tok)) {
                newStart++;
                if (newStart >= newEnd)
                    return null;
            }
        }

        if (removeTrailingStopwords) {
            String tok = tokens.get(newEnd-1);
            if (stopWordsIgnoreCase)
                tok = tok.toLowerCase();
            if (stopWords.contains(tok)) {
                newEnd--;
                if (newStart >= newEnd)
                    return null;
            }
        }

        if (removeLeadingSymbolicTokens) {
            String tok = tokens.get(newStart);
            String normalized = tok.replaceAll("[\\p{Punct}]", "");
            if (normalized.length() == 0) {
                newStart++;
                if (newStart >= newEnd)
                    return null;
            }
        }
        if (removeLeadingSymbolicTokens) {
            String tok = tokens.get(newEnd-1);
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

    protected List<String[]> createTags(List<String[]> sentences) {
        List<String[]> result = new ArrayList<>(sentences.size());
        for(String[] sent: sentences){
            result.add(assignPOS(sent));
        }
        return result;
    }

    protected String[] assignPOS(String[] words) {

        return posTagger.tag(words);
    }


    protected List<String[]> walkTokens() throws IOException {
        List<String[]> sentences = new ArrayList<>();

        List<String> sent = new ArrayList<>();
        String prevSent=null;
        while (input.incrementToken()) {
            CharTermAttribute textAtt = input.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAtt = input.getAttribute(OffsetAttribute.class);
            char[] buffer = textAtt.buffer();
            String word = new String(buffer, 0, offsetAtt.endOffset() - offsetAtt.startOffset());
            String sentId = parseTokenSentenceId(input.getAttribute(PayloadAttribute.class));
            if(prevSent==null) {
                prevSent = sentId;
                sent.add(word);
            }else if(!prevSent.equals(sentId)){
                sentences.add(sent.toArray(new String[0]));
                sent = new ArrayList<>();
            }else{
                sent.add(word);
            }

            AttributeSource attrs = input.cloneAttributes();
            tokenAttrs.add(attrs);
        }

        return sentences;
    }

    private String parseTokenSentenceId(PayloadAttribute attribute){
        if(attribute!=null){
            return SentenceContext.parseSentenceId(attribute.getPayload().utf8ToString());
        }
        return "";
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
