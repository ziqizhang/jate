package uk.ac.shef.dcs.jate.v2.app;

import com.google.gson.Gson;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.util.IOUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 22/09/2015.
 */
public class AbstractApp {

    public static Map<String, String> getParams(String[] args) throws JATEException {
        Map<String, String> params = new HashMap<>();
        if((args.length)%2!=0){
            throw new JATEException("The number of parameters is not valid. Run app without parameters for help.");
        }
        for(int i=0; i< args.length; i++){
            String param=args[i];
            String value=args[i+1];
            i++;
            params.put(param, value);
        }
        return params;
    }

    public static void write(List<JATETerm> terms, String path) throws IOException {
        Gson gson = new Gson();
        if(path==null){
            System.out.println(gson.toJson(terms));
        }
        else{
            Writer w = IOUtil.getUTF8Writer(path);
            gson.toJson(terms,w);
            w.close();
        }
    }
}
