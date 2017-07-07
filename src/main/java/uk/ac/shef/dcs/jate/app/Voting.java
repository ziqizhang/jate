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
        String inFolder = "/home/zqz/Work/data/termrank/jate_lrec2016/genia_atr4s/min1";
        String outFile="/home/zqz/Work/data/termrank/jate_lrec2016/genia_atr4s/vote/vote.json";

        /*String inFolder = "/home/zqz/Work/data/termrank/jate_lrec2016/aclrd_ver2_atr4s/min1";
        String outFile="/home/zqz/Work/data/termrank/jate_lrec2016/aclrd_ver2_atr4s/vote/vote.json";
*/
        /*String inFolder = "/home/zqz/Work/data/termrank/jate_lrec2016/ttc_mobile_atr4s/min1";
        String outFile="/home/zqz/Work/data/termrank/jate_lrec2016/ttc_mobile_atr4s/vote/vote.json";
*/
        /*String inFolder = "/home/zqz/Work/data/termrank/jate_lrec2016/ttc_wind_atr4s/min1";
        String outFile="/home/zqz/Work/data/termrank/jate_lrec2016/ttc_wind_atr4s/vote/vote.json";
*/
        /*Map<String, Double> weights = new HashMap<>();
        weights.put("chisquare.json",1.0);
        weights.put("cvalue.json",1.0);
        weights.put("glossex.json",1.0);
        weights.put("rake.json",1.0);
        weights.put("relevance.json",1.0);
        weights.put("tfidf.json",1.0);
        weights.put("weirdness.json",1.0);

        Map<String, FileOutputReader> readers = new HashMap<>();
        FileOutputReader jsonFileOutputReader = new JSONFileOutputReader(new Gson());
        readers.put("chisquare.json",jsonFileOutputReader);
        readers.put("cvalue.json",jsonFileOutputReader);
        readers.put("glossex.json",jsonFileOutputReader);
        readers.put("rake.json",jsonFileOutputReader);
        readers.put("relevance.json",jsonFileOutputReader);
        readers.put("tfidf.json",jsonFileOutputReader);
        readers.put("weirdness.json",jsonFileOutputReader);*/

        Map<String, Double> weights = new HashMap<>();
        weights.put("Basic.txt",1.0);
        weights.put("ComboBasic.txt",1.0);
        weights.put("LinkProbability.txt",1.0);
        weights.put("NovelTopicModel.txt",1.0);
        weights.put("PU.txt",1.0);

        Map<String, FileOutputReader> readers = new HashMap<>();
        FileOutputReader jsonFileOutputReader = new JSONFileOutputReader(new Gson());
        readers.put("Basic.txt",jsonFileOutputReader);
        readers.put("ComboBasic.txt",jsonFileOutputReader);
        readers.put("LinkProbability.txt",jsonFileOutputReader);
        readers.put("NovelTopicModel.txt",jsonFileOutputReader);
        readers.put("PU.txt",jsonFileOutputReader);

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
            for (int i = 0; i < pair.getKey().size(); i++) {
                JATETerm jt = pair.getKey().get(i);
                String termStr = jt.getString();
                double rankScore = 1.0 / (i + 1) * pair.getValue();
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
