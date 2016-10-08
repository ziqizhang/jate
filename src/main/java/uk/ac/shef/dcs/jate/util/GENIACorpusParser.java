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


/**
 * Read GENIA corpus in the xml format and creates a corpus of raw text files
 */
public class GENIACorpusParser {
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

					p.println(sent.getTextContent());
				}
				p.close();
			}
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
        System.out.println("words in GS terms:"+countWordsInTerms(args[0]));
        System.out.println("words in total:"+countWords(args[0]));
        parse(args[0],args[1]);
    }
}
