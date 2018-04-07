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
            String[][] wordsAndPOS = walkTokens();
            //gather all tokens from doc
            String[] words = wordsAndPOS[0];
            if (words.length == 0) {
                return false;
            }
            //tagging
            String[] pos = wordsAndPOS[1];
            String[] tags = npChunker.chunk(words, pos);
            chunks=createSpan(tags);
            chunks = prune(chunks, words);
            Arrays.sort(chunks, (t1, t2) -> {
                if (t1.getStart()< t2.getStart())
                    return 0;
                else if (t1.getStart()>t2.getStart())
                    return 1;
                else{
                    return Integer.compare(t1.getEnd(), t2.getEnd());
                }
            });
            first = false;
            currentSpanIndex=0;
        }

        if (currentSpanIndex == chunks.length) {
            resetParams();
            return false;
        }

        Span chunk = chunks[currentSpanIndex];
        boolean success=addMWE(chunk.getStart(), chunk.getEnd(), chunk.getType());
        while (!success && currentSpanIndex<chunks.length){
            chunk = chunks[currentSpanIndex];
            success=addMWE(chunk.getStart(), chunk.getEnd(), chunk.getType());
        }

        if (currentSpanIndex<chunks.length)
            return true;
        return false;

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
