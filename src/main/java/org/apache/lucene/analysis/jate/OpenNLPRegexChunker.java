package org.apache.lucene.analysis.jate;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.AttributeSource;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Generates candidate terms based on PoS regex
 */
public final class OpenNLPRegexChunker extends OpenNLPMWEFilter {

    private RegexNameFinder regexChunker;

    public OpenNLPRegexChunker(
            TokenStream input,
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
        regexChunker = new RegexNameFinder(patterns);
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
            //chunking
            Span[] chunks = regexChunker.find(pos);
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
            boolean added=addMWE(tokenIdx);
            //do not increment token index here because end span is exclusive
            //tokenIdx++;
            return true;
        }
        if (chunkSpans.containsKey(tokenIdx)) { //found a new chunk, the current tokindex is the beginning of the chunk
            chunkStart = tokenIdx;
            chunkEnds = chunkSpans.get(tokenIdx);
            tokenIdx = chunkEnds.get(0); //set tokenIdx to be the next end index for the beginning index
            return true;
        } else { //a token that is not part of a chunk
            tokenIdx++;
            return true;
        }
    }

}
