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

    public static List<String> load(String jsonFile) throws IOException, ParseException {
        Gson gson = new Gson();
        FileInputStream jsonFileStream = new FileInputStream(jsonFile);
        try {
            JSONParser parser = new JSONParser();
            JSONArray obj = (JSONArray)parser.parse(new FileReader(jsonFile));
            Iterator<?> it = obj.iterator();
            List<JATETerm> terms = new ArrayList<>();
            while (it.hasNext()){
                JSONObject instance=(JSONObject)it.next();
                Object s = instance.get("score-mult");
                if(s==null)
                    s=instance.get("score");
                JATETerm term = new JATETerm(instance.get("string").toString(), Double.valueOf(s.toString()));
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
