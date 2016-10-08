package org.apache.lucene.analysis.jate;

import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenStream;

import uk.ac.shef.dcs.jate.nlp.Chunker;

import java.io.IOException;
import java.util.*;
/**
 * Generate candidate terms using the OpenNLP noun phrase chunker
 */
public final class OpenNLPNounPhraseFilter extends OpenNLPMWEFilter {

    private Chunker npChunker;
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected OpenNLPNounPhraseFilter(TokenStream input,
                                      Chunker npChunker,
                                      int minTokens, int maxTokens,
                                      int minCharLength, int maxCharLength,
                                      boolean removeLeadingStopWords,
                                      boolean removeTrailingStopwords,
                                      boolean removeLeadingSymbolicTokens,
                                      boolean removeTrailingSymbolicTokens,
                                      boolean stripLeadingSymbolChars,
                                      boolean stripTrailingSymbolChars,
                                      boolean stripAllSymbolChars,
                                      Set<String> stopWords,
                                      boolean stopWordsIgnoreCase) {
        super(input, minTokens, maxTokens,
                minCharLength, maxCharLength,
                removeLeadingStopWords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stripLeadingSymbolChars,
                stripTrailingSymbolChars,
                stripAllSymbolChars,
                stopWords, stopWordsIgnoreCase);
        this.npChunker=npChunker;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (first) {
            //gather all tokens from doc
            String[][] wordsAndPos = walkTokens();
            String[] words = wordsAndPos[0];
            if (words.length == 0) {
                return false;
            }
            //tagging
            String[] pos = wordsAndPos[1];
            //chunking
            String[] tags = npChunker.chunk(words, pos);
            Span[] chunks=createSpan(tags);
            chunks = prune(chunks, words);
            for (Span sp : chunks) {
                List<Integer> ends = chunkSpans.get(sp.getStart());
                if(ends==null)
                    ends=new ArrayList<>();
                ends.add(sp.getEnd());

                chunkSpans.put(sp.getStart(), ends);
                chunkTypes.put(sp.getStart(), sp.getType());
            }
            first = false;
            tokenIdx = 0;
        }

        if (tokenIdx == tokenAttrs.size()) {
            resetParams();
            return false;
        }

        if (chunkStart != -1 && chunkEnds.contains(tokenIdx)) {  //already found a new chunk and now we found its end
            addMWE(tokenIdx);
            
            //do not increment token index here because end span is exclusive
            //tokenIdx++;
            return true;
        }
        if (chunkSpans.containsKey(tokenIdx)) { //at the beginning of a new chunk
            chunkStart = tokenIdx;
            chunkEnds = chunkSpans.get(tokenIdx);
            tokenIdx = chunkEnds.get(0); //set tokenIdx to be the next end index for the beginning index
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
