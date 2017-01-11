package uk.ac.shef.dcs.jate.util;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.indexing.IndexingHandler;
import uk.ac.shef.dcs.jate.io.TikaSimpleDocumentCreator;

import java.io.IOException;


/**
 * Created by zqz on 11/01/17.
 */
public class RegressionFeatureGenerator {
    public static void main(String[] args) throws JATEException, IOException, SolrServerException {
        EmbeddedSolrServer server;
        CoreContainer testBedContainer = new CoreContainer("");
        testBedContainer.load();
        server = new EmbeddedSolrServer(testBedContainer, "genia");

        JATEProperties prop = new JATEProperties();
        IndexingHandler indexer = new IndexingHandler();
        indexer.index(null, 100,
                new TikaSimpleDocumentCreator(), server, prop);
        server.commit();

        WordShapeFBMaster wordShapeFBMaster =
                new WordShapeFBMaster(server.getCoreContainer().getCore("genia").getSearcher().get(),
                        prop,0,null);
        PositionFeatureMaster positionFeatureMaster =
                new PositionFeatureMaster(server.getCoreContainer().getCore("genia").getSearcher().get(),
                        prop,0);
        WordShapeFeature wordshapeFeature= (WordShapeFeature)wordShapeFBMaster.build();

        PositionFeature positionFeature= (PositionFeature)positionFeatureMaster.build();


    }
}
