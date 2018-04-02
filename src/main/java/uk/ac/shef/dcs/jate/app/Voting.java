package uk.ac.shef.dcs.jate.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.csv.CSVFormat;
import org.apache.solr.common.util.Pair;
import uk.ac.shef.dcs.jate.io.CSVFileOutputReader;
import uk.ac.shef.dcs.jate.io.FileOutputReader;
import uk.ac.shef.dcs.jate.io.JSONFileOutputReader;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Given the RANKED result of several different algorithms (must be applied to the same candidate set of terms,
 * applying a weighted voting algorithm.
 * <p>
 * The new score will be the sum of (1.0 divided by the rank a term by each algorithm, scaled by the weight of that algorithm)
 */
public class Voting {

    public static void main(String[] args) throws IOException {
        String inFolder = "/data/seed-terms/genia";
        String outFile="/data/seed-terms/genia/voted.json";
        Map<String, Double> weights = new HashMap<>();
        weights.put("genia_attf_seed_terms.json",1.0);
        weights.put("genia_chisquare_seed_terms.json",1.0);
        weights.put("genia_cvalue_seed_terms.json",1.0);
        weights.put("genia_cvalue_seed_terms_mttf1.json",1.0);
        weights.put("genia_glossex_seed_terms.json",1.0);
        weights.put("genia_rake_seed_terms.json",1.0);
        weights.put("genia_termex_seed_terms.json",1.0);
        weights.put("genia_tfidf_seed_terms.json",1.0);
        weights.put("genia_ttf_seed_terms.json",1.0);
        weights.put("genia_weirdness_seed_terms.json",1.0);
        weights.put("genia_text_rank_result.csv",1.0);

        Map<String, FileOutputReader> readers = new HashMap<>();
        FileOutputReader jsonFileOutputReader = new JSONFileOutputReader(new Gson());
        FileOutputReader csvFileOutputReader = new CSVFileOutputReader(CSVFormat.DEFAULT);
        readers.put("genia_attf_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_chisquare_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_cvalue_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_cvalue_seed_terms_mttf1.json",jsonFileOutputReader);
        readers.put("genia_glossex_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_rake_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_termex_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_tfidf_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_ttf_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_weirdness_seed_terms.json",jsonFileOutputReader);
        readers.put("genia_text_rank_result.csv",csvFileOutputReader);

        Voting voting = new Voting();
        Pair[] results = voting.readAlgorithmResults(inFolder, weights, readers);
        List<JATETerm> newResult = voting.vote(results);
        Writer w = IOUtil.getUTF8Writer(outFile);
        new Gson().toJson(newResult, w);
        w.close();
    }

    /**
     * the program will look for files in the pattern '[algorithm_name].[ext]'
     *
     * @param inFolder     that contains output of algorithms
     * @param algAndWeight a map that contains algorithm name and its weight
     * @return
     */
    public Pair[] readAlgorithmResults(String inFolder,
                                       Map<String, Double> algAndWeight,
                                       Map<String, FileOutputReader> algAndReader) {
        Pair[] pairs = new Pair[algAndWeight.size()];
        int i=0;
        for(Map.Entry<String, Double> en: algAndWeight.entrySet()){
            File f = new File(inFolder+File.separator+en.getKey());
            FileOutputReader reader= algAndReader.get(en.getKey());
            List<JATETerm> terms = null;
            try {
                terms = reader.read(f.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            pairs[i] = new Pair<>(terms, en.getValue());
            i++;
        }

        return pairs;
    }

    public List<JATETerm> vote(Pair... algResultWithWeight) {
        if (algResultWithWeight.length == 0)
            return new ArrayList<>();

        Map<String, Double> voteScores = new HashMap<>();
        List<JATETerm> out = new ArrayList<>();

        for (Pair result : algResultWithWeight) {
            Pair<List<JATETerm>, Double> pair = (Pair<List<JATETerm>, Double>) result;
            for (int i = 0; i < pair.first().size(); i++) {
                JATETerm jt = pair.first().get(i);
                String termStr = jt.getString();
                double rankScore = 1.0 / (i + 1) * pair.second();
                Double finalScore = voteScores.get(termStr);
                if (finalScore == null)
                    finalScore = 0.0;
                finalScore += rankScore;
                voteScores.put(termStr, finalScore);
            }
        }

        for (Map.Entry<String, Double> entry : voteScores.entrySet()) {
            out.add(new JATETerm(entry.getKey(), entry.getValue()));
        }
        Collections.sort(out);
        return out;
    }
}
