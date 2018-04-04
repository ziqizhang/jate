package org.apache.lucene.analysis.jate;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenStream;

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
            boolean stripLeadingSymbolChars,
            boolean stripTrailingSymbolChars,
            boolean stripAllSymbolChars,
            Set<String> stopWords,
            boolean stopWordsIgnoreCase) {
        super(input, minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopWords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stripLeadingSymbolChars,
                stripTrailingSymbolChars,
                stripAllSymbolChars,
                stopWords, stopWordsIgnoreCase);
        regexChunker = new RegexNameFinder(patterns);
    }

    /**
     * This method does not process individual tokens. Instead, it operates as follows:
     *
     * - the first time it is called, the candidate list of phrases are extracted
     *
     * - then starting from the second time, each time this method is called to process a token,
     *   it in fact processes one phrase and emit that phrase
     *
     * - it does so until either 1) no more phrases remain; or 2) no more tokens remain in the document.
     *
     * Therefore, in theory, it is possible that not all PoS matched chunks will be emitted, if the number
     * of PoS matched chunks is larger than the number of tokens in the document
     *
     * @return
     * @throws IOException
     */
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
            chunks = regexChunker.find(pos);
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
        return true;


    }

}
