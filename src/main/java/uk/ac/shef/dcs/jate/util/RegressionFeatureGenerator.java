package uk.ac.shef.dcs.jate.util;

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
        IndexingHandler indexer = new IndexingHandler();
        indexer.index(tasks, 100,
                new TikaSimpleDocumentCreator(), server, prop);
        System.exit(0);
        //server.commit();

        WordShapeFBMaster wordShapeFBMaster =
                new WordShapeFBMaster(server.getCoreContainer().getCore("GENIA").getSearcher().get(),
                        prop,0,null);
        PositionFeatureMaster positionFeatureMaster =
                new PositionFeatureMaster(server.getCoreContainer().getCore("GENIA").getSearcher().get(),
                        prop,0);
        WordShapeFeature wordshapeFeature= (WordShapeFeature)wordShapeFBMaster.build();

        PositionFeature positionFeature= (PositionFeature)positionFeatureMaster.build();


    }
}
