package uk.ac.shef.dcs.oak.jate.core.extractor;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.JATEProperties;
import uk.ac.shef.dcs.oak.jate.model.Corpus;
import uk.ac.shef.dcs.oak.jate.model.Document;
import uk.ac.shef.dcs.oak.jate.util.control.Normalizer;
import uk.ac.shef.dcs.oak.jate.util.control.StopList;

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
        String[] words = applyCharacterReplacement(content, JATEProperties.TERM_CLEAN_PATTERN).split(" ");
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        for (String w : words) {
            String nw = w.trim();
            //
            nw = nw.toLowerCase();
            //if(_stoplist.isStopWord(nw)) continue;
            nw = _normaliser.normalize(nw).trim();

            //
            if (!containsLetter(nw) && !containsDigit(nw)) continue;
            if (nw.length() < _minCharsInWord) continue;

            if (_removeStop && (_stoplist.isStopWord(nw) || _stoplist.isStopWord(w.trim()))) continue;
            //String lemma = _normaliser.normalize(w.trim());
            //word should be treated separately to NP, as different forms of a word should be treated separately in counting
            if (nw.length() > 0) {
                Set<String> variants = result.get(nw);
                variants = variants == null ? new HashSet<String>() : variants;
                variants.add(w);
                result.put(nw, variants);
            }
/*			String lemma = _normaliser.normalize(w.trim());
			if (lemma.length()>0) {
				result.add(lemma);
			}*/
        }
        return result;
    }
}
