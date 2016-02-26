package uk.ac.shef.dcs.jate.eval;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * load gs from file
 */
public class GSLoader {

    /**
     * @param file
     * @return
     */
    public static List<String> loadGenia(String file) throws IOException {
        List<String> terms = FileUtils.readLines(new File(file));
        return terms;
        //return loadGenia(new File(file), lowercase, normalize);
    }
    
    private static Function<String, String> mapToTerm = (line) -> {
        String[] p = line.split("\t");
        return p[1];
    };

    public static List<String> loadACLRD(String file) throws IOException {
        List<String> raw = FileUtils.readLines(new File(file));
        List<String> terms = new ArrayList<>(raw.size() - 1);
        int count = 0;
        for (String r : raw) {
            if (count == 0) {
                count++;
                continue;
            }
            String[] splits = r.split("\\t");
            if (splits[2].equals("0"))
                continue;
            terms.add(splits[1].trim());
        }


        return terms;
    }

}
