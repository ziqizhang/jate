package org.apache.lucene.analysis.jate;

import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import org.apache.lucene.util.AttributeSource;
import uk.ac.shef.dcs.jate.nlp.Chunker;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.IOException;
import java.util.*;
/**
 *
 */
public class OpenNLPNounPhraseFilter extends OpenNLPMWEFilter {

    private Chunker npChunker;
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected OpenNLPNounPhraseFilter(TokenStream input,
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
            String[] tags = npChunker.chunk(words, pos);
            Span[] chunks=createSpan(tags);
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
            boolean added=addMWE();
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

    private Span[] createSpan(String[] tags) {
        int start=-1;
        List<Span> result =new ArrayList<>();
        for(int i=0; i<tags.length;i++){
            if(tags[i].equalsIgnoreCase(npChunker.getContinueTag())){
                //do nothing
            }
            else{
                if(start!=-1){
                    result.add(new Span(start, i, "NP"));
                    if(tags[i].equalsIgnoreCase(npChunker.getStartTag()))
                        start=i;
                    else
                        start=-1;
                }else if(tags[i].equalsIgnoreCase(npChunker.getStartTag())){
                    start=i;
                }
            }
        }
        if(start!=-1){
            result.add(new Span(start, tags.length,"NP"));
        }
        return result.toArray(new Span[0]);
    }


}
