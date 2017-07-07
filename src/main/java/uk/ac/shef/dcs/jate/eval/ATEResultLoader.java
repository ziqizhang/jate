package uk.ac.shef.dcs.jate.eval;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * load the output of an App class into memory.
 */
public class ATEResultLoader {

    public static void main(String[] args) throws IOException, ParseException {
        /*List<JATETerm> original=loadToTerms("/home/zqz/Work/data/termrank/output_genia_atr4s/Relevance.txt");
        List<JATETerm> modified=loadToTerms("/home/zqz/Work/data/termrank/output_genia_atr4s/" +
                "Relevance-em_g-uni-sg-100-w3-m1,g_dn-pn-top1-pnl0-t0.5.json");*/
        /*List<JATETerm> original=loadToTerms("/home/zqz/Work/data/termrank/output_genia_g/weirdness.json");
        List<JATETerm> modified=loadToTerms("/home/zqz/Work/data/termrank/output_genia_g/" +
                "weirdness-em_gdbp-uni-sg-100-w3-m1,g_dn-pn-top1-pnl0-t0.5.json");*/

        /*List<JATETerm> original=loadToTerms("/home/zqz/Work/data/termrank/output_aclv2/Relevance.txt");
        List<JATETerm> modified=loadToTerms("/home/zqz/Work/data/termrank/output_aclv2/" +
                "Relevance-em_aclv2dbp-uni-sg-100-w3-m1,g_dn-pn-top1-pnl0-t0.5.json");*/

        List<JATETerm> original = loadToTerms("/home/zqz/Work/data/termrank/jate_lrec2016/aclrd_ver2/min1/weirdness.json");
        List<JATETerm> original2 = loadToTerms("/home/zqz/Work/data/termrank/output_aclv2/Relevance.txt");
        /*List<JATETerm> modified=loadToTerms("/home/zqz/Work/data/termrank/output_aclv2/" +
                "Relevance-em_aclv2dbp-uni-sg-100-w3-m1,g_dn-pn-top1-pnl0-t0.5.json");
*/
        System.out.println();
    }

    public static List<String> load(String jsonFile) throws IOException, ParseException {
        return toString(loadToTerms(jsonFile));
    }

    public static List<String> loadToTermsRawText(String rawTextFile) throws IOException, ParseException {
        List<String> lines = FileUtils.readLines(new File(rawTextFile));
        List<JATETerm> terms = new ArrayList<>();
        for (String l : lines) {
            String[] splits = l.split(",");
            terms.add(new JATETerm(splits[0].trim(), Double.valueOf(splits[1])));
        }

        Collections.sort(terms);

        return toString(terms);
    }

    public static List<JATETerm> loadToTerms(String jsonFile) throws IOException, ParseException {
        Gson gson = new Gson();
        FileInputStream jsonFileStream = new FileInputStream(jsonFile);
        try {
            JSONParser parser = new JSONParser();
            JSONArray obj = (JSONArray) parser.parse(new FileReader(jsonFile));
            Iterator<?> it = obj.iterator();
            List<JATETerm> terms = new ArrayList<>();
            while (it.hasNext()) {
                JSONObject instance = (JSONObject) it.next();
                Object s = instance.get("score-mult");
                if (s == null)
                    s = instance.get("score");
                JATETerm term = new JATETerm(instance.get("string").toString(), Double.valueOf(s.toString()));
                terms.add(term);
            }

            Collections.sort(terms);

            return terms;
        } finally {
            if (jsonFileStream != null) {
                try {
                    jsonFileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> toString(List<JATETerm> terms) throws IOException, ParseException {

        List<String> result = new ArrayList<>();
        for (JATETerm o : terms) {
            result.add(o.getString());
        }
        return result;

    }

    public static List<String> load(List<JATETerm> jateTerms) {
        List<String> result = new ArrayList<>();
        if (jateTerms != null) {
            jateTerms.forEach(jateTerm -> {
                result.add(jateTerm.getString());
            });
        }
        return result;
    }

    @Deprecated
    public static List<List<String>> loadJATE1(String jate1outputfile) throws IOException {
        List<String> lines = FileUtils.readLines(new File(jate1outputfile));
        List<List<String>> out = new ArrayList<>(lines.size());
        for (String l : lines) {
            String terms = l.split("\t\t\t")[0];
            List<String> variants = new ArrayList<>();
            for (String t : terms.split("\\|")) {
                variants.add(t.trim());
            }
            out.add(variants);
        }
        return out;
    }

}
