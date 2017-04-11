package uk.ac.shef.dcs.jate.eval;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * load gs from file
 */
public class GSLoader {

    /**
     * @param file  GENIA Goldstandard terms file
     * @return List<String>  a list of gs terms
     */
    public static List<String> loadGenia(String file) throws IOException {
        List<String> terms = FileUtils.readLines(new File(file));
        return terms;
    }

    /**
     *
     * @param file  ACL RD-TEC goldstandard term file (with invalid and valid terms)
     * @return List<String>  list of goldstandard terms
     * @throws IOException
     */
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

        System.out.println("total="+terms.size());
        return terms;
    }

}
