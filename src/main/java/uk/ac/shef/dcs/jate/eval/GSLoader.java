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
     * @param file  GENIA Goldstandard "concept.txt" file
     * @return List<String>  a list of gs terms for the ATE evaluation
     */
    public static List<String> loadGenia(String file) throws IOException {
        List<String> terms = FileUtils.readLines(new File(file));

        List<String> filteredGSTerms = filterByStopwords(terms);

        return filteredGSTerms;
    }

    /**
     * remove concept predicates in genia 'concept' list
     * "Blood cell receptor" is not included in the content we use which is actually a "second" title of article "MEDLINE:94077003".
     * @param terms gs term list loaded directly from GENIA "concept.txt" file
     * @return List<String>  a list of gs terms for the ATE evaluation
     */
    private static List<String> filterByStopwords(List<String> terms) {
        String[] genia_gs_stop_words = {"*", "(OR", "(NOT", "(TO", "(THAN", "(VERSUS", "(AND", "(BUT", "(AS", "(AND/OR", "Blood cell receptor"};
        List<String> prunedGSTerms = new ArrayList<>();
        for (String rawGSTerm : terms) {
            boolean isFiltered = false;
            for (String gsStopWord : genia_gs_stop_words) {
                if (rawGSTerm.contains(gsStopWord)) {
                    isFiltered = true;
                    break;
                }
            }

            if (!isFiltered) {
                prunedGSTerms.add(rawGSTerm);
            }
        }
        return prunedGSTerms;
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
