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
 * Created by zqz on 25/09/2015.
 */
//todo consider sentence boundary
public class OpenNLPRegexChunker extends OpenNLPMWEFilter {

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
            //gather all tokens from doc
            List<String[]> sentences = walkTokens();
            if (sentences.size() == 0) {
                return false;
            }
            //tagging
            List<String[]> pos = createTags(sentences);
            //chunking
            List<Span> chunks = new ArrayList<>();
            List<String> words = new ArrayList<>();

            for(int s=0; s<sentences.size(); s++){
                String[] ws = sentences.get(s);
                words.addAll(Arrays.asList(ws));
                String[] poss = pos.get(s);
                Span[] cs = regexChunker.find(poss);
                if(cs.length>0)
                    chunks.addAll(Arrays.asList(cs));
            }

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

}
