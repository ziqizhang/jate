package org.apache.lucene.analysis.jate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class wraps OpenNLPTokenizer to support PoS pattern sequence
 * matching based candidate extraction at index-time.
 *
 * Created by zqz on 28/09/2015.
 * <p>
 * TODO: change this to jate.solr.TermCandidateFilterFactory---(ZZ: i disagree. this class is generic. anyone who does not need ATE can still use this class for other purposes so it should not be named specifically with 'TermXXX' In fact I think this should be moved to package org.apache.lucene.analysis.opennlp)
 */
public class OpenNLPRegexChunkerFactory extends MWEFilterFactory {

    private Map<String, Pattern[]> patterns = new HashMap<>();
    private String patternFile;

    /**
     * Initialize this factory via a set of key-value pairs.
     * TODO: check why run-time parameter "posTaggerClass" and "posTaggerModel" not being used
     * @param args, arguments for the filter, see example of jate_text_2_terms field type in JATE 2.0 solr schema.xml
     */
    public OpenNLPRegexChunkerFactory(Map<String, String> args) {
        super(args);

        patternFile = args.get("patterns");
        if (patternFile == null) {
            throw new IllegalArgumentException("Parameter 'patterns' for chunker is missing.");
        }
    }

    private void initPatterns(List<String> rawData, Map<String, Pattern[]> patterns) throws IOException {
        //is patternStr a file?

        Map<String, List<Pattern>> m = new HashMap<>();
        for (String lineStr : rawData) {
            if (lineStr.trim().length() == 0 || lineStr.startsWith("#"))
                continue;
            String[] parts = lineStr.split("\t", 2);
            List<Pattern> pats = m.get(parts[0]);
            if (pats == null)
                pats = new ArrayList<>();
            pats.add(Pattern.compile(parts[1]));
            m.put(parts[0], pats);
        }
        for (Map.Entry<String, List<Pattern>> en : m.entrySet()) {
            patterns.put(en.getKey(), en.getValue().toArray(new Pattern[0]));
        }

    }

    @Override
    public TokenStream create(TokenStream input) {
        return new OpenNLPRegexChunker(input, patterns, maxTokens,
                minTokens,
                maxCharLength, minCharLength,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        super.inform(loader);
        if (patternFile != null) {
            try {
                List<String> lines = getLines(loader, patternFile.trim());
                initPatterns(lines, patterns);
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Initiating ");
                sb.append(this.getClass().getName()).append(" failed due to patterns. Details:\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                throw new IllegalArgumentException(sb.toString());
            }
        }
    }


}
