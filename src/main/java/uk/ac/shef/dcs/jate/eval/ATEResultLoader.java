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
 *
 */
public class ATEResultLoader {

    public static List<String> loadFromJSON(String jsonFile) throws IOException, ParseException {
        Gson gson = new Gson();
        FileInputStream jsonFileStream = new FileInputStream(jsonFile);
        try {
            JSONParser parser = new JSONParser();
            JSONArray obj = (JSONArray)parser.parse(new FileReader(jsonFile));
            Iterator<?> it = obj.iterator();
            List<JATETerm> terms = new ArrayList<>();
            while (it.hasNext()){
                JSONObject instance=(JSONObject)it.next();
                Object score =instance.get("score");
                JATETerm term=null;
                if(score==null)
                    term = new JATETerm(instance.get("string").toString(), Double.valueOf(instance.get("score").toString()));
                else
                    term=new JATETerm(instance.get("string").toString(),Double.valueOf(score.toString()));
                terms.add(term);
            }

	        Collections.sort(terms);
            List<String> result = new ArrayList<>();
	        for(JATETerm o: terms){
	            result.add(o.getString());
	        } 
	        return result;
        }
        finally {
        	if (jsonFileStream != null) {
        		try {
					jsonFileStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
    }

    /**
     * load ranked term candidates from two-column csv file
     *
     * header row is expected.
     *
     * @param csvOutputFile csv output file of ranked term candidates
     * @return List<String> ranked term candidate strings
     * @throws IOException expection if file not found
     */
    public static List<String> loadFromCSV(String csvOutputFile) throws IOException {
        String defaultSplit=",";
        BufferedReader br = null;
        List<String> rankedTermCandidates = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(csvOutputFile));

            String line = null;
            int index = 0;
            while ((line = br.readLine()) !=null) {
                //skip head
                if (index != 0) {
                    String[] termScore = line.split(defaultSplit);
                    rankedTermCandidates.add(termScore[0]);
                }
                index ++;
            }
        } finally {
            if (br!=null) {
                br.close();
            }
        }

        return rankedTermCandidates;
    }

    public static List<String> load(List<JATETerm> jateTerms) {
        List<String> result = new ArrayList<>();
        if (jateTerms != null) {
            jateTerms.forEach(jateTerm -> {result.add(jateTerm.getString());});
        }
        return result;
    }

    @Deprecated
    public static List<List<String>> loadJATE1(String jate1outputfile) throws IOException {
        List<String> lines = FileUtils.readLines(new File(jate1outputfile));
        List<List<String>> out = new ArrayList<>(lines.size());
        for(String l : lines){
            String terms = l.split("\t\t\t")[0];
            List<String> variants = new ArrayList<>();
            for(String t: terms.split("\\|")){
                variants.add(t.trim());
            }
            out.add(variants);
        }
        return out;
    }

}
