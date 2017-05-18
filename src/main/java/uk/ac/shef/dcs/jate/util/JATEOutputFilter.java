package uk.ac.shef.dcs.jate.util;

import com.google.gson.Gson;
import uk.ac.shef.dcs.jate.io.JSONFileOutputReader;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zqz on 18/05/17.
 */
public class JATEOutputFilter {

    public static void main(String[] args) throws IOException {
        filterNumbers(args[0], args[1]);
    }

    public static final void filterNumbers(String inFolder,
                                           String outFolder) throws IOException {
        Gson gson = new Gson();
        JSONFileOutputReader reader = new JSONFileOutputReader(gson);

        for(File f: new File(inFolder).listFiles()){
            int count=0;
            List<JATETerm> terms = reader.read(f.toString());
            Iterator<JATETerm> it = terms.iterator();
            while(it.hasNext()){
                JATETerm t = it.next();
                String filtered = t.getString().replaceAll("[^a-zA-Z]","").trim();
                if(filtered.length()==0) {
                    it.remove();
                    count++;
                }
                t.setString(t.getString().trim());
            }

            Writer w = IOUtil.getUTF8Writer(outFolder+File.separator+f.getName());
            gson.toJson(terms, w);
            w.close();
            System.out.println("filtered: "+count);
        }
    }
}
