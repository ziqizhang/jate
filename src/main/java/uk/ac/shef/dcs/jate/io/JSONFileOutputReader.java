package uk.ac.shef.dcs.jate.io;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.simple.JSONObject;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zqz on 24/03/17.
 */
public class JSONFileOutputReader implements FileOutputReader{

    private Gson gson;
    public JSONFileOutputReader(Gson gson){
        this.gson=gson;
    }
    @Override
    public List<JATETerm> read(String file) {
//        JSONObject jo=gson.fromJson(file, JSONObject.class);
        Type listType = new TypeToken<ArrayList<JATETerm>>(){}.getType();
        List<JATETerm> terms = gson.fromJson(file, listType);
        return terms;
    }
}
