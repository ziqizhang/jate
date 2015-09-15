package uk.ac.shef.dcs.jate.deprecated.core.extractor;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;
import uk.ac.shef.dcs.jate.deprecated.core.nlptools.NLPToolsControllerOpenNLP;
import uk.ac.shef.dcs.jate.deprecated.model.Corpus;
import uk.ac.shef.dcs.jate.deprecated.model.Document;
import uk.ac.shef.dcs.jate.deprecated.util.control.Normalizer;
import uk.ac.shef.dcs.jate.deprecated.util.control.StopList;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zqz
 * Date: 01/08/14
 * Time: 15:45
 * To change this template use File | Settings | File Templates.
 */
public class VerbPhraseExtractorOpenNLP extends CandidateTermExtractor {
    private static Logger _logger = Logger.getLogger(VerbPhraseExtractorOpenNLP.class.getName());

    /**
     * Creates an instance with specified stopwords list and norm
     *
     * @param stop
     * @param normaliser
     * @throws java.io.IOException
     */
    public VerbPhraseExtractorOpenNLP(StopList stop, Normalizer normaliser) throws IOException {
        _stoplist = stop;
        _normaliser = normaliser;

    }

    @Override
    public Map<String, Set<String>> extract(Corpus c) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        for (Document d : c) {
            _logger.info("Extracting candidate NP... From Document " + d);
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
        //  System.out.println(content+ "||" );
        Map<String, Set<String>> vps = new HashMap<String, Set<String>>();
        try {
            String[] tokens = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(content);
            String[] pos = NLPToolsControllerOpenNLP.getInstance().getPosTagger().tag(tokens);
            String[] candidates = chunkVPs(tokens, pos);
            for (String c : candidates) {
                c = applyCharacterReplacement(c, JATEProperties.TERM_CLEAN_PATTERN);
                String[] e = applySplitList(c);

                for (String str : e) {
                    String stopremoved = applyTrimStopwords(str, _stoplist, _normaliser,false,true);
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
                        //System.out.print(original+"|");

                        Set<String> variants = vps.get(str);
                        variants = variants == null ? new HashSet<String>() : variants;
                        variants.add(original);
                        vps.put(str, variants);
                    }
                }
            }
            //  System.out.println("\n");
        } catch (IOException wte) {
            throw new JATEException(wte);
        }

        return vps;
    }


    //todo: change this
    private String[] chunkVPs(String[] tokens, String[] pos) throws IOException {
        String[] phrases = NLPToolsControllerOpenNLP.getInstance().getPhraseChunker().chunk(tokens, pos);
        /* for(int i=0;i<phrases.length;i++){
            System.out.println(tokens[i]+ " "+ phrases[i]);
        }
        */
        List<String> candidates = new ArrayList<String>();
        String phrase = "";
        for (int n = 0; n < tokens.length; n++) {
            if (phrases[n].equals("B-VP")) {
                phrase = tokens[n];
                for (int m = n + 1; m < tokens.length; m++) {
                    if (phrases[m].equals("I-VP")||phrases[m].endsWith("-NP")) {
                        phrase = phrase + " " + tokens[m];
                    } else {
                        n = m;
                        break;
                    }
                }
                phrase = phrase.replaceAll("\\s+", " ").trim();
                if (phrase.length() > 0)
                    candidates.add(phrase);

            }
        }
        return candidates.toArray(new String[0]);
    }

}
