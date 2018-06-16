package uk.ac.shef.dcs.jate.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * A utility class for calculating corpus or GS statistics
 */
public class Stats {

    /**
     * calculate % of multi-word expressions and single word experssions in the GS
     *
     * @param inFile absolute file path to the GS list file
     */
    public static void calcMWEvsSWE(String inFile) throws IOException {
        List<String> terms=FileUtils.readLines(new File(inFile), Charset.forName("utf-8"));
        int mwe=0, swe=0;
        for (String t : terms){
            if (t.split("\\s+").length==1)
                swe++;
            else
                mwe++;
        }
        System.out.println(String.format("For %s, Total terms=%d, swe=%d (%.2f), mwe=%d (%.2f)",
                inFile,terms.size(), swe, (double)swe/terms.size(),
                mwe, (double) mwe/terms.size()));
    }

    public static void main(String[] args) throws IOException {
        calcMWEvsSWE("/home/zz/Work/data/jate_data/ttc/gs-en-windenergy.txt");
        calcMWEvsSWE("/home/zz/Work/data/jate_data/ttc/gs-en-mobile-technology.txt");
        calcMWEvsSWE("/home/zz/Work/data/jate_data/acl-rd-corpus-2.0/acl-rd-ver2-gs-terms.txt");
        calcMWEvsSWE("/home/zz/Work/data/jate_data/genia_gs/concept/genia_gs_terms_v2.txt");

    }
}
