package uk.ac.shef.dcs.oak.jate.core.extractor;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.JATEProperties;
import uk.ac.shef.dcs.oak.jate.model.Corpus;
import uk.ac.shef.dcs.oak.jate.model.Document;
import uk.ac.shef.dcs.oak.jate.util.control.Normalizer;
import uk.ac.shef.dcs.oak.jate.util.control.StopList;
import uk.ac.shef.dcs.oak.jate.core.nlptools.NLPToolsControllerOpenNLP;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * An NGram extractor that extracts n-grams from texts. By default n=5. change this by resetting the property
 * jate.system.term.maxwords in the property file
 */
public class NGramExtractor extends CandidateTermExtractor {

    private static Logger _logger = Logger.getLogger(NGramExtractor.class.getName());


    public NGramExtractor(StopList stop, Normalizer normaliser) throws IOException {
        _stoplist = stop;
        _normaliser = normaliser;
    }

    @Override
    public Map<String, Set<String>> extract(Corpus c) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        for (Document d : c) {
            _logger.info("Extracting candidate NGram... From Document " + d);
            for (Map.Entry<String, Set<String>> e : extract(d).entrySet()) {
                Set<String> variants = res.get(e.getKey());
                variants = variants == null ? new HashSet<String>() : variants;
                variants.addAll(e.getValue());
                res.put(e.getKey(), variants);
            }
        }
        return res;
    }

    @Override
    public Map<String, Set<String>> extract(Document d) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        try {
            for (String s : NLPToolsControllerOpenNLP.getInstance().getSentenceSplitter().sentDetect(d.getContent())) {
                for (Map.Entry<String, Set<String>> e : extract(s).entrySet()) {
                    Set<String> variants = res.get(e.getKey());
                    variants = variants == null ? new HashSet<String>() : variants;
                    variants.addAll(e.getValue());
                    res.put(e.getKey(), variants);
                }
            }
        } catch (IOException e) {
            throw new JATEException(e);
        }
        return res;
    }

    @Override
    public Map<String, Set<String>> extract(String content) throws JATEException {
        Map<String, Set<String>> nouns = new HashMap<String, Set<String>>();
        try {
            String[] tokens = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(content);
            List<String> candidates = getNGram(tokens, JATEProperties.getInstance().getMaxMultipleWords(), true);
            for (String c : candidates) {
                c = applyCharacterReplacement(c, JATEProperties.TERM_CLEAN_PATTERN);
                String[] e = applySplitList(c);

                for (String str : e) {
                    String stopremoved = applyTrimStopwords(str, _stoplist,_normaliser);
                    if (stopremoved == null) continue;
                    String original = stopremoved;
                    str = _normaliser.normalize(stopremoved.toLowerCase()).trim();

                    String[] nelements = str.split("\\s+");
                    if (nelements.length < 1 || nelements.length >
                            Integer.valueOf(JATEProperties.getInstance().getMaxMultipleWords()))
                        continue;
                    if (JATEProperties.getInstance().isIgnoringDigits() &&
                            containsDigit(str))
                        continue;
                    if (!containsLetter(str)) continue;
                    if (!hasReasonableNumChars(str)) continue;

                    if (c.toLowerCase().indexOf(str) != -1) {
                        Set<String> variants = nouns.get(str);
                        variants = variants == null ? new HashSet<String>() : variants;
                        variants.add(original);
                        nouns.put(str, variants);
                    }
                }
            }
        } catch (IOException wte) {
            throw new JATEException(wte);
        }

        return nouns;
    }


    private List<String> getNGram(String[] tokens, int n, boolean includeSmaller) {
        List<Integer[]> ng = new ArrayList<Integer[]>();

        int begin = 0;
        int end = begin + n - 1>=tokens.length?tokens.length-1:begin + n - 1;
        Integer positions[] = new Integer[2];

        while (end < tokens.length) {
            positions = new Integer[2];
            positions[0] = begin;
            positions[1] = end;
            ng.add(positions);

            if (includeSmaller) {
                int smallEnd = end - 1;
                while (smallEnd >= begin) {
                    positions = new Integer[2];
                    positions[0] = begin;
                    positions[1] = smallEnd;
                    ng.add(positions);
                    smallEnd--;
                }
            }
            begin++;
            end++;
        }
        if (includeSmaller) {
            while (begin != end) {
                int smallEnd = end - 1;
                while (smallEnd >= begin &&smallEnd<tokens.length) {
                    positions = new Integer[2];
                    positions[0] = begin;
                    positions[1] = smallEnd;
                    ng.add(positions);
                    smallEnd--;
                }
                begin++;
            }

        }

        List<String> ngrams = new ArrayList<String>();
        for(Integer[] offsets: ng){
            String ngram = "";
            for(int i = offsets[0]; i<=offsets[1]; i++){
                ngram = ngram+tokens[i]+" ";
            }
            ngrams.add(ngram.trim());
        }

        return ngrams;
    }
}
