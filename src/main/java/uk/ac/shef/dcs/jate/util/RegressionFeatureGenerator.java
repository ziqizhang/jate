package uk.ac.shef.dcs.jate.util;

import org.apache.sis.util.StringBuilders;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.indexing.IndexingHandler;
import uk.ac.shef.dcs.jate.io.TikaSimpleDocumentCreator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Created by zqz on 11/01/17.
 */
public class RegressionFeatureGenerator {
    public static void main(String[] args) throws JATEException, IOException, SolrServerException {
        EmbeddedSolrServer server;
        CoreContainer testBedContainer = new CoreContainer("/home/zqz/Work/jate/testdata/solr-testbed");
        testBedContainer.load();
        server = new EmbeddedSolrServer(testBedContainer, "GENIA");

        File[] files= new File("/home/zqz/Work/data/jate_data/genia_gs/text/files_standard").listFiles();
        List<String> tasks = new ArrayList<>();
        JATEProperties prop = new JATEProperties();
        for(File f: files) {
            tasks.add(f.toString());
        }

        //############### this block is for building a testing index #########
        /*IndexingHandler indexer = new IndexingHandler();
        indexer.index(tasks, 100,
                new TikaSimpleDocumentCreator(), server, prop);
        server.close();
        System.exit(0);*/

        //############## the following is the code for fetching features from the built index ######
        WordShapeFBMaster wordShapeFBMaster =
                new WordShapeFBMaster(server.getCoreContainer().getCore("GENIA").getSearcher().get(),
                        prop,0,null);
        PositionFeatureMaster positionFeatureMaster =
                new PositionFeatureMaster(server.getCoreContainer().getCore("GENIA").getSearcher().get(),
                        prop,0);
        WordShapeFeature wordshapeFeature= (WordShapeFeature)wordShapeFBMaster.build();
        PositionFeature positionFeature= (PositionFeature)positionFeatureMaster.build();

        int c=0;
        System.out.println("\nTOTAL="+wordshapeFeature.getAllTerms().size());
        for(String term : wordshapeFeature.getAllTerms()){
            c++;
            System.out.println("#"+c+" term (term has been normalised!!!)="+term);
            System.out.println("\thasAcronymToken:"+wordshapeFeature.getHasAcronymFeature(term));
            System.out.println("\thasNumericToken:"+wordshapeFeature.getHasNumberFeature(term));
            System.out.println("\thasSymbolChar:"+wordshapeFeature.getHasSymbolFeature(term));
            System.out.println("\thasDigitChar:"+wordshapeFeature.getHasDigitFeature(term));
            System.out.println("\thasUppercaseChar:"+wordshapeFeature.getHasUppercaseFeature(term));

            //frequency found in doc titles
            Integer inTitle=positionFeature.getFoundInDocTitles(term);
            if(inTitle==null){
                inTitle=0;
            }
            System.out.println("\tfrequencyInDocTitle (this can be used to compute a ratio to total frequency):"+inTitle);
            //PDFT: distances of the source paragraph from the doc title, measured as #of paragraphs from title divided by
            //total paragraphs in doc
            List<Double> paragraphDistancesFromTitle=positionFeature.getParDistFromTitle(term);
            double[] minMaxAvgPDFT=calculateMinMaxAvg(paragraphDistancesFromTitle);
            System.out.println("\tmin PDFT="+minMaxAvgPDFT[0]);
            System.out.println("\tmax PDFT="+minMaxAvgPDFT[1]);
            System.out.println("\tavg PDFT="+minMaxAvgPDFT[2]);

            //SDFT: distances of the source sentence from the doc title, measured as #of sentences from title divided by
            //total sentences in doc
            List<Double> sentenceDistancesFromTitle=positionFeature.getSentDistFromTitle(term);
            double[] minMaxAvgSDFT=calculateMinMaxAvg(sentenceDistancesFromTitle);
            System.out.println("\tmin SDFT="+minMaxAvgSDFT[0]);
            System.out.println("\tmax SDFT="+minMaxAvgSDFT[1]);
            System.out.println("\tavg SDFT="+minMaxAvgSDFT[2]);

            //SDFP: distances of the source sentence from the first sentence of its containing paragraph, measured as
            // #of sentences from the first sentence divided by
            //total sentences in the paragraph
            List<Double> sentenceDistancesFromPar=positionFeature.getSentDistFromPar(term);
            double[] minMaxAvgSDFP=calculateMinMaxAvg(sentenceDistancesFromPar);
            System.out.println("\tmin SDFP="+minMaxAvgSDFP[0]);
            System.out.println("\tmax SDFP="+minMaxAvgSDFP[1]);
            System.out.println("\tavg SDFP="+minMaxAvgSDFP[2]);

        }
        System.exit(0);
    }

    protected static double[] calculateMinMaxAvg(List<Double> numbers){
        Collections.sort(numbers);
        Double max=numbers.get(numbers.size()-1);
        Double min=numbers.get(0);
        double total = 0;
        for(Double d: numbers)
            total+=d;
        Double avg=total/(double)numbers.size();
        return new double[]{min, max, avg};
    }
}
