package uk.ac.shef.dcs.jate.eval;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * load gs from file
 */
public class GSLoader {

    /**
     *
     * @param file
     * @param lowercase
     * @param normalize if true, will run ascii unfolding then Lucene simple english stemmer
     * @return
     */
    public static List<String> loadGenia(String file, boolean lowercase, boolean normalize) throws IOException {
        List<String> terms = FileUtils.readLines(new File(file));
        return prune(terms, lowercase, normalize);
    }



    public static List<String> loadACLRD(String file, boolean lowercase, boolean normalize) throws IOException {
        List<String> raw = FileUtils.readLines(new File(file));
        List<String> terms = new ArrayList<>(raw.size()-1);
        int count=0;
        for(String r: raw){
            if(count==0){
                count++;
                continue;
            }
            String[] splits = r.split("\\t");
            if(splits[2].equals("0"))
                continue;
            terms.add(splits[1].trim());
        }


        return terms;
    }

    private static List<String> prune(List<String> terms, boolean lowercase, boolean normalize) {
        List<String> result = new ArrayList<>();
        EnglishMinimalStemmer stemmer = new EnglishMinimalStemmer();
        for(String t: terms){
            if(lowercase)
                t=t.toLowerCase();

            if(normalize){
                int start=0, end=t.length();
                for(int i=0; i<t.length(); i++){
                    if(Character.isLetterOrDigit(t.charAt(i))){
                        start=i;
                        break;
                    }
                }
                for(int i=t.length()-1; i>-1; i--){
                    if(Character.isLetterOrDigit(t.charAt(i))){
                        end=i+1;
                        break;
                    }
                }

                t=t.substring(start,end).trim();
                if(t.length()>0) {
                    int newEnd = stemmer.stem(t.toCharArray(), t.length());
                    t=t.substring(0, newEnd).trim();
                }

                if(t.length()>0)
                    result.add(t);

            }
        }
        return result;
    }

}
