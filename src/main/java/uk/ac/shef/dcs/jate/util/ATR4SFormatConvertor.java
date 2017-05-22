package uk.ac.shef.dcs.jate.util;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.solr.common.util.Pair;
import uk.ac.shef.dcs.jate.io.JSONFileOutputReader;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.*;
import java.util.*;

/**
 * Created by zqz on 18/05/17.
 */
public class ATR4SFormatConvertor {

    public static void main(String[] args) throws IOException {
        String attf = args[0];
        String inFolder=args[1];
        String outFolder=args[2];
        String outPerFile=args[2]+File.separator+"min1_per_file";

        Pair<Map<String, Set<String>>, List<JATETerm>> ttfOutput = calculateTTFOutput(attf);

        Gson gson = new Gson();
        File outFolderFile = new File(outFolder);
        outFolderFile.mkdirs();
        File outPerFileFolder = new File(outPerFile);
        outPerFileFolder.mkdirs();

        //write ttf output
        Writer w = IOUtil.getUTF8Writer(outFolderFile+File.separator+"ttf.json");
        gson.toJson(ttfOutput.getValue(), w);
        w.close();

        //write output per file
        writeOutputPerFile(outPerFile, ttfOutput.getKey());

        //write all other algorithm;s output

        JSONFileOutputReader reader = new JSONFileOutputReader(gson);
        List<JATETerm> refTerms = new ArrayList<>();
        Set<String> refTermStrings = new HashSet<>();
        for(JATETerm jt: refTerms)
            refTermStrings.add(jt.getString());

        for(File f: new File(inFolder).listFiles()){
            if(f.toString().equals(attf))
                continue;
            System.out.println(f);
            List<String> lines = FileUtils.readLines(f);
            List<JATETerm> terms = new ArrayList<>();
            for(String l: lines){
//                WeightedTerm(accuracy_rate_of_syntactic_disambiguation,0.0)
                int start=l.indexOf("(");
                String tString=l.substring(start+1, l.length()-1);
                String[] parts = tString.split(",");
                String termString = parts[0];
                termString=termString.substring(0, termString.indexOf("[")).trim();
                String ts = termString.replaceAll("_", " ").trim();

                if(refTerms.size()>0 && !refTermStrings.contains(ts))
                    continue;
                JATETerm term = new JATETerm(ts,
                        Double.valueOf(parts[parts.length-1]));
                terms.add(term);
            }
            File path = new File(args[2]+File.separator+"min1");
            path.mkdirs();
            w = IOUtil.getUTF8Writer(path.toString()+File.separator+f.getName());
            gson.toJson(terms, w);
            w.close();
        }
    }

    private static void writeOutputPerFile(String outPerFile, Map<String, Set<String>> key) throws FileNotFoundException {
        Map<String, Set<String>> fileToTerms = new HashMap<>();
        for(Map.Entry<String, Set<String>> e: key.entrySet()){
            String term = e.getKey();
            Set<String> files =e.getValue();

            for(String f: files){
                Set<String> terms = fileToTerms.get(f);
                if(terms==null)
                    terms= new HashSet<>();
                terms.add(term);
                fileToTerms.put(f, terms);
            }
        }

        for(Map.Entry<String, Set<String>> e: fileToTerms.entrySet()){
            String filename=e.getKey()+".txt";
            PrintWriter p = new PrintWriter(outPerFile+File.separator+filename);
            List<String> all = new ArrayList<>(e.getValue());
            Collections.sort(all);
            for(String a: all)
                p.println(a);
            p.close();
        }
    }

    private static Pair<Map<String, Set<String>>, List<JATETerm>> calculateTTFOutput(String attf) throws IOException {
        List<String> lines = FileUtils.readLines(new File(attf));
        List<JATETerm> terms = new ArrayList<>();
        Map<String, Set<String>> termFilesMap = new HashMap<>();
        for(String l: lines){
//                WeightedTerm(accuracy_rate_of_syntactic_disambiguation,0.0)
            int start=l.indexOf("(");
            String tString=l.substring(start+1, l.length()-1);
            start=tString.indexOf("[");
            int end=tString.indexOf("]");
            String termString = tString.substring(0, start).replaceAll("_", " ").trim();
            String fileString = tString.substring(start+1,end).trim();
            String scoreString = tString.substring(end+2).trim();

            String[] sourceFiles = fileString.split(",");
            JATETerm term = new JATETerm(termString,
                    Double.valueOf(scoreString)*sourceFiles.length);
            terms.add(term);

            Set<String> sourceFilesSet = new HashSet<>(Arrays.asList(sourceFiles));
            termFilesMap.put(termString, sourceFilesSet);
        }
        Collections.sort(terms);
        return new Pair<>(termFilesMap, terms);
    }

    public static Set<String> readAllTermStrings(File f) throws IOException {
        List<String> lines = FileUtils.readLines(f);
        Set<String> out = new HashSet<>();
        for(String l: lines){
//                WeightedTerm(accuracy_rate_of_syntactic_disambiguation,0.0)
            int start=l.indexOf("(");
            String tString=l.substring(start+1, l.length()-1);
            String[] parts = tString.split(",");
            String ts = parts[0].replaceAll("_", " ").trim();

            out.add(ts);
        }
        return out;
    }
}
