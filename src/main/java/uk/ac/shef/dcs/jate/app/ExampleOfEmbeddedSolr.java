package uk.ac.shef.dcs.jate.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;

import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.JATETerm;

public class ExampleOfEmbeddedSolr {
	public static void main(String[] args) {
		
    	String workingDir = System.getProperty("user.dir");
    	
    	System.out.println("working dir:"+workingDir);
    	
    	
    	/*
    	String solrHome = "C:\\oak-project\\SPEEAK-PC-TermRecognition\\solr-5.3.0\\data";
    	String solrCore = "tatasteel";
    	
    	String jatePropertyFile="";
    	Map<String, String> params = new HashMap<>();
    	
    	
    	try {
    		AppATTF appATTF = new AppATTF(params);
			appATTF.extract(solrHome, solrCore, jatePropertyFile);			
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
	/**
     * Extract terms from embedded solr server
     * @throws Exception 
     */
    public List<JATETerm> extract(String solrHome, String coreName, String jatePropertyFile, Map<String, String> params) throws Exception {
        //IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

    	EmbeddedSolrServer server = null;
    	try {
    		server = new EmbeddedSolrServer(new File(solrHome).toPath(), coreName);
    		
    		SolrCore solrCore = server.getCoreContainer().getCore(coreName);
    		
    		SolrIndexSearcher indexSearcher = solrCore.getSearcher().get();
    	   		
    		Integer totalNumDocs = (Integer)indexSearcher.getStatistics().get("numDocs");
    		System.out.println("num of docs: "+totalNumDocs);
    		    		
    		JATEProperties properties = new JATEProperties(jatePropertyFile);
    		final LeafReader indexReader = indexSearcher.getLeafReader();
    		
    	    Fields lfields = indexReader.fields();
    	    System.out.println(""+lfields.size());
    	    
    	    Terms ngramInfo = lfields.terms(properties.getSolrFieldNameJATENGramInfo());
    	    
    	    TermsEnum termsEnum = ngramInfo.iterator();
            List<BytesRef> allLuceneTerms = new ArrayList<>();

            while (termsEnum.next() != null) {
                BytesRef t = termsEnum.term();
                if (t.length == 0)
                    continue;
                allLuceneTerms.add(BytesRef.deepCopyOf(t));
            }
            
            System.out.println("allLuceneTerms: "+allLuceneTerms.size());
    	    
    		indexSearcher.close();
    		solrCore.close();
		} finally {
			if (server != null){
				server.close();	
				server.getCoreContainer().shutdown();
			}
			
		}
    	
        /**
        JATEProperties properties = new JATEProperties(jatePropertyFile);
        FrequencyTermBasedFBMaster featureBuilder = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased feature = (FrequencyTermBased)featureBuilder.build();
        Algorithm attf = new ATTF();
        attf.registerFeature(FrequencyTermBased.class.getName(), feature);

        List<JATETerm> terms=attf.execute(feature.getMapTerm2TTF().keySet());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true")) {
            collectTermInfo(indexReader, terms);
        }
        indexReader.close();
        return terms;
        **/
        return null;
    }
}
