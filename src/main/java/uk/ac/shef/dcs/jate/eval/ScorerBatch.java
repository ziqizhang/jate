package uk.ac.shef.dcs.jate.eval;

import org.json.simple.parser.ParseException;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class ScorerBatch {

    /*
    /home/zqz/Work/data/semrerank/ate_output/textrank_per_unsup/aclrd_ver2
"/home/zqz/Work/data/jate_data/acl-rd-corpus-2.0/acl-rd-ver2-gs-terms.txt"
/home/zqz/Work/data/semrerank/report_aclv2.csv
acl
/home/zqz/Work/jate/testdata/solr-testbed/GENIA/conf/lemmatiser
     */
    public static void main(String[] args) throws JATEException, ParseException, IOException {
        File inDir = new File(args[0]);
        for (File subFolder : inDir.listFiles()) {
            Scorer.main(new String[]{subFolder.toString(),
                    args[1],
                    args[2]+"/"+subFolder.getName()+"_scores.csv",
                    args[3],
                    args[4]});
        }
    }
}




