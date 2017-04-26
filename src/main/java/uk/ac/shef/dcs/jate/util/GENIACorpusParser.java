package uk.ac.shef.dcs.jate.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;


/**
 * Read GENIA corpus in the xml format and creates a corpus of raw text files
 */
public class GENIACorpusParser {
    public static Set<String> GENIA_GS_IGNORE=new HashSet<>();
    static{
        GENIA_GS_IGNORE.add("*");
        GENIA_GS_IGNORE.add("(OR");
        GENIA_GS_IGNORE.add("(NOT");
        GENIA_GS_IGNORE.add("(TO");
        GENIA_GS_IGNORE.add("(THAN");
        GENIA_GS_IGNORE.add("(VERSUS");
        GENIA_GS_IGNORE.add("(AND");
        GENIA_GS_IGNORE.add("(BUT");
        GENIA_GS_IGNORE.add("(AS");
        GENIA_GS_IGNORE.add("(AND/OR");
    }


    public static void parse(String xmlFile, String outFolder) throws ParserConfigurationException, IOException, SAXException {
        //Get the DOM Builder Factory
        DocumentBuilder docBuilder =DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream xmlFileStream = new FileInputStream(new File(xmlFile));
        try {
			Document document = docBuilder.parse(xmlFileStream);

			NodeList nodeList = document.getDocumentElement().getElementsByTagName("article");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element doc = (Element) nodeList.item(i);
				NodeList docId = doc.getElementsByTagName("bibliomisc");
				String id = docId.item(0).getFirstChild().getNodeValue();
				id = i + "_" + id.replaceAll("[^0-9a-zA-Z]", "_");
				PrintWriter p = new PrintWriter(outFolder + File.separator + id + ".txt");
				NodeList sentences = doc.getElementsByTagName("sentence");

				for (int j = 0; j < sentences.getLength(); j++) {
					Node sent = sentences.item(j);

					if(j==0)
						p.println(sent.getTextContent());
					else
						p.print(sent.getTextContent()+" ");

				}
				p.println();
				p.close();
			}
        } finally {
        	xmlFileStream.close();
        }

    }

    public static boolean ignore(String candidate){
        for(String ign: GENIA_GS_IGNORE)
            if(candidate.contains(ign))
                return true;
            /*if(candidate.startsWith("*"))
                System.out.println();*/
        return false;
    }

    /**
     * extract texts within '<cons></cons>' tags as gold standard terms
     * @param xmlFile
     * @param outFile
     */
    public static void extractGoldstandardTerms(String xmlFile, String outFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder docBuilder =DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream xmlFileStream = new FileInputStream(new File(xmlFile));
        try {
            Document document = docBuilder.parse(xmlFileStream);
            PrintWriter p = new PrintWriter(outFile);
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("cons");
            Set<String> all = new HashSet<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element con = (Element) nodeList.item(i);
                String attr=con.getAttribute("lex");
                if(ignore(attr))
                    continue;
                String concept=con.getTextContent().trim();

                if(concept.length()>2)
                    all.add(concept);

            }

            List<String> sorted = new ArrayList<>(all);
            Collections.sort(sorted);
            for(String c: sorted)
                p.println(c);
            p.close();
        } finally {
            xmlFileStream.close();
        }
    }

    public static int countWordsInTerms(String xmlFile) throws ParserConfigurationException, IOException, SAXException {
        //Get the DOM Builder Factory
        DocumentBuilder docBuilder =DocumentBuilderFactory.newInstance().newDocumentBuilder();

        FileInputStream xmlFileStream = new FileInputStream(new File(xmlFile));
        try {
	        Document document = docBuilder.parse(xmlFileStream);
	
	        int total=0;
	        NodeList nodeList = document.getDocumentElement().getElementsByTagName("cons");
	        for(int i=0; i<nodeList.getLength(); i++){
	            Node con = nodeList.item(i);
	            String context = con.getTextContent().replaceAll("[^a-zA-Z0-9]"," ").trim();
	            total+=context.split("\\s+").length;
	        }
	        return total;
        } finally {
        	if (xmlFileStream != null) {
        		xmlFileStream.close();
        	}
        }
    }

    public static int countWords(String xmlFile) throws ParserConfigurationException, IOException, SAXException {
        //Get the DOM Builder Factory
        DocumentBuilder docBuilder =DocumentBuilderFactory.newInstance().newDocumentBuilder();

        FileInputStream fileStream = new FileInputStream(new File(xmlFile));
        try {
	        Document document = docBuilder.parse(fileStream);
	
	        int total=0;
	        NodeList nodeList = document.getDocumentElement().getElementsByTagName("sentence");
	        for(int i=0; i<nodeList.getLength(); i++){
	                Node sent = nodeList.item(i);
	            String sentext = sent.getTextContent().replaceAll("[^a-zA-Z0-9]"," ").trim();
	            total+=sentext.split("\\s+").length;
	        }
	        return total;
        } finally {
        	fileStream.close();
        }
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//        System.out.println("words in GS terms:"+countWordsInTerms(args[0]));
//        System.out.println("words in total:"+countWords(args[0]));
        //parse(args[0],args[1]);
        extractGoldstandardTerms(args[0],args[1]);
    }
}
