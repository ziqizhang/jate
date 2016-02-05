package uk.ac.shef.dcs.jate.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Read GENIA corpus in the xml format and creates a corpus of raw text files
 */
public class GENIACorpusParser {
    public static void parse(String xmlFile, String outFolder) throws ParserConfigurationException, IOException, SAXException {
        //Get the DOM Builder Factory
        DocumentBuilder docBuilder =DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document document =
                docBuilder.parse(
                new FileInputStream(new File(xmlFile)));

        NodeList nodeList = document.getDocumentElement().getElementsByTagName("article");
        for(int i=0; i<nodeList.getLength(); i++){
            Element doc = (Element)nodeList.item(i);
            NodeList docId=doc.getElementsByTagName("bibliomisc");
            String id = docId.item(0).getFirstChild().getNodeValue();
            id = i+"_"+id.replaceAll("[^0-9a-zA-Z]","_");
            PrintWriter p = new PrintWriter(outFolder+File.separator+id+".txt");
            NodeList sentences=doc.getElementsByTagName("sentence");
            for(int j=0; j<sentences.getLength(); j++){
                Node sent = sentences.item(j);

                p.println(sent.getTextContent());
            }
            p.close();
        }

    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        parse(args[0],args[1]);
    }
}
