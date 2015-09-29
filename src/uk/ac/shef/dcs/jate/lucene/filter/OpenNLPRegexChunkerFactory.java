package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zqz on 28/09/2015.
 */
public class OpenNLPRegexChunkerFactory extends TokenFilterFactory {

    private POSTagger tagger;
    private Map<String, Pattern[]> patterns = new HashMap<>();

    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public OpenNLPRegexChunkerFactory(Map<String, String> args) {
        super(args);
        String taggerClass = args.get("posTaggerClass");
        if (taggerClass == null)
            throw new IllegalArgumentException("Parameter 'class' for POS tagger is missing.");
        String patternStr = args.get("patterns");
        if (patternStr == null) {
            throw new IllegalArgumentException("Parameter 'patterns' for chunker is missing.");
        } else {
            try {
                initPatterns(patternStr, patterns);
            }catch (IOException ioe){
                StringBuilder sb = new StringBuilder("Initiating ");
                sb.append(this.getClass().getName()).append(" failed due to patterns. Details:\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                throw new IllegalArgumentException(sb.toString());
            }
        }
        String taggerFileModerIfExist = args.get("posTaggerModel");
        try {
            tagger = InstanceCreator.createPOSTagger(taggerClass, taggerFileModerIfExist);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Initiating ");
            sb.append(this.getClass().getName()).append(" failed due to:\n");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(sb.toString());
        }

    }

    private void initPatterns(String patternStr, Map<String, Pattern[]> patterns) throws IOException {
        //is patternStr a file?
        File f = new File(patternStr);
        if (f.exists()) {
            Map<String, List<Pattern>> m = new HashMap<>();
            LineIterator li = FileUtils.lineIterator(f);
            while(li.hasNext()){
                String lineStr=li.next();
                String[] parts = lineStr.split("\t",2);
                List<Pattern> pats = m.get(parts[0]);
                if(pats==null)
                    pats=new ArrayList<>();
                pats.add(Pattern.compile(parts[1]));
                m.put(parts[0], pats);
            }
            for(Map.Entry<String, List<Pattern>> en: m.entrySet()){
                patterns.put(en.getKey(), en.getValue().toArray(new Pattern[0]));
            }
        } else {
            patterns.put("1", new Pattern[]{Pattern.compile(patternStr)});
        }
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new OpenNLPRegexChunker(input, tagger, patterns);
    }
}
