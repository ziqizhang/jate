package uk.ac.shef.dcs.jate.eval;

import com.google.gson.Gson;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * load the output of an App class into memory.
 *
 */
public class ATEResultLoader {

    public static List<String> load(String jsonFile) throws FileNotFoundException, UnsupportedEncodingException {
        Gson gson = new Gson();
        List terms=gson.fromJson(new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(jsonFile), StandardCharsets.UTF_8)), List.class);

        List<String> result = new ArrayList<>();
        //todo

        return result;
    }

}
