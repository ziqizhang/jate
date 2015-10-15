package uk.ac.shef.dcs.jate.app;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zqz on 30/09/2015.
 */
public class PatternTester {
    public static void main(String[] args) throws IOException {
        Map<String, Pattern[]> patterns = new HashMap<>();
        initPatterns("D:\\Work\\jate_github\\jate\\example\\patterns/aclrdtec.patterns",
                patterns);

        RegexNameFinder rnf = new RegexNameFinder(patterns);
        String[] testGeniaPatterns = new String[]{
               // "JJ","NN","NNS","NNP",".",
                //"IN","JJ","NN","NNS","NNP",".",
              //  "NN","JJ","IN","JJ","NN","NNS","NNP"

                "DT","NN","NN","CC","NNP","NNP","NN","IN","DT","VBZ","JJ","NN","NN","IN","JJ","."
        };
        Span[] rs = rnf.find(testGeniaPatterns);
        System.out.println(rs);

    }

    private static void initPatterns(String patternStr, Map<String, Pattern[]> patterns) throws IOException {
        //is patternStr a file?
        File f = new File(patternStr);
        if (f.exists()) {
            Map<String, List<Pattern>> m = new HashMap<>();
            LineIterator li = FileUtils.lineIterator(f);
            while (li.hasNext()) {
                String lineStr = li.next();
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
        } else {
            patterns.put("1", new Pattern[]{Pattern.compile(patternStr)});
        }
    }
}
