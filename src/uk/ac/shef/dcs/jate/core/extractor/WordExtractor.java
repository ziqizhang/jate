package uk.ac.shef.dcs.jate.core.extractor;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.Corpus;
import uk.ac.shef.dcs.jate.model.Document;
import uk.ac.shef.dcs.jate.util.control.Normalizer;
import uk.ac.shef.dcs.jate.util.control.StopList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts words from texts. Words will be normalized to reduce inflections . Characters that do not match the pattern
 * [a-zA-A\-] are replaced by whitespaces.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public class WordExtractor extends CandidateTermExtractor {

    private boolean _removeStop = true;
    private int _minCharsInWord = 2;

    /**
     * Creates an instance with specified stopwords list and normaliser.
     * By default, stopwords are ignored; words that have less than 2 characters
     * are ignored.
     *
     * @param stop       a list of words which are unlikely to occur in a domain specific candidate term
     * @param normaliser an instance of a Normalizer which returns candidate term to canonical form
     */
    public WordExtractor(StopList stop, Normalizer normaliser) {
        _stoplist = stop;
        _normaliser = normaliser;
    }

    /**
     * Creates an instance with specified stopwords list and normaliser.
     *
     * @param stop       a list of words which are unlikely to occur in a domain specific candidate term
     * @param normaliser an instance of a Normalizer which returns candidate term to canonical form
     * @param removeStop whether stop words should be ignored in the extracted words
     * @param minCharsInWord words that contain less than this number of characters (non-white space) are ignored in the
     * extracted words
     */
    public WordExtractor(StopList stop, Normalizer normaliser, boolean removeStop, int minCharsInWord) {
        _stoplist = stop;
        _normaliser = normaliser;
        _removeStop=removeStop;
        _minCharsInWord=minCharsInWord;
    }

    public WordExtractor() {
    }

    public Map<String, Set<String>> extract(Corpus c) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        for (Document d : c) {
            for (Map.Entry<String, Set<String>> e : extract(d).entrySet()) {
                Set<String> variants = res.get(e.getKey());
                variants = variants == null ? new HashSet<String>() : variants;
                variants.addAll(e.getValue());
                res.put(e.getKey(), variants);
            }
        }

        return res;
    }

    public Map<String, Set<String>> extract(Document d) throws JATEException {
        return extract(d.getContent());
    }

    public Map<String, Set<String>> extract(String content) throws JATEException {
        final String[] words = applyCharacterReplacement(content, JATEProperties.TERM_CLEAN_PATTERN).split(" ");
        final Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        for (String w : words) {
            
            String originalWord = Normalizer.basicNormalize(w);
            String nw = _normaliser.normalize(originalWord);
            if (nw.length() < _minCharsInWord) {
                continue;
            }

            if (_removeStop && (_stoplist.isStopWord(originalWord) || _stoplist.isStopWord(nw))) {
                continue;
            }

            Set<String> variants = result.get(originalWord);
            variants = variants == null ? new HashSet<String>() : variants;
            if (!w.equals(originalWord)) {
                variants.add(w);
            }
            result.put(nw, variants);
        }
        return result;
    }
}
