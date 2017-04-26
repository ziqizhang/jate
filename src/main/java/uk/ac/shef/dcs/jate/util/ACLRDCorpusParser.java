package uk.ac.shef.dcs.jate.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import javax.xml.parsers.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by zqz on 08/04/17.
 */
public class ACLRDCorpusParser {
    public static void main(String[] args) throws FileNotFoundException, JATEException {
        String inXMLFolder = args[0];
        String outTxtFolder = args[1];

        int count = 0;

        for (File f : listFileTree(new File(inXMLFolder))) {
            count++;
            System.out.println(count);
            JATEDocument doc = loadJATEDocFromXML(new FileInputStream(f));
            PrintWriter p = new PrintWriter(outTxtFolder + File.separator + f.getName() + ".txt");
            p.println(doc.getContent());
            p.close();
        }

        //process aclrdver2 abstracts to plain text
        /*String inXMLFolder="/home/zqz/Work/data/jate_data/acl-rd-corpus-2.0/raw_abstract_txt";
        String outFolder="/home/zqz/Work/data/jate_data/acl-rd-corpus-2.0/raw_abstract_plain_txt";
        int count=0;
        for (File f : listFileTree(new File(inXMLFolder))) {
            count++;
            System.out.println(count);
            JATEDocument doc = null;
            try {
                doc = loadJATEDocFromXMLV2(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
            PrintWriter p = new PrintWriter(outFolder + File.separator + f.getName() + ".txt");
            p.println(doc.getContent());
            p.close();
        }*/

        //process aclrdver2 abstracts to extrac terms
        /*String inXMLFolder = "/home/zqz/Work/data/jate_data/acl-rd-corpus-2.0/annoitation_files/merge_1-over-2";
        String outFile = "/home/zqz/Work/data/jate_data/acl-rd-corpus-2.0/acl-rd-ver2-gs-terms.txt";
        int count = 0;
        Set<String> terms = new HashSet<>();
        for (File f : listFileTree(new File(inXMLFolder))) {
            count++;
            System.out.println(count);
            try {
                terms.addAll(extractGoldstandardTermsV2(f.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<String> sorted = new ArrayList<>(terms);
        Collections.sort(sorted);
        PrintWriter p = new PrintWriter(outFile);
        for (String s : sorted)
            p.println(s);
        p.close();
*/
    }

    public static Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<File>();
        if (dir == null || dir.listFiles() == null) {
            return fileTree;
        }
        for (File entry : dir.listFiles()) {
            if (entry.isFile()) fileTree.add(entry);
            else fileTree.addAll(listFileTree(entry));
        }
        return fileTree;
    }

    private static JATEDocument loadJATEDocFromXML(InputStream fileInputStream) throws JATEException {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser saxParser = null;
        JATEDocument jateDocument = null;
        try {
            saxParser = factory.newSAXParser();

            StringBuffer paperParagraphs = new StringBuffer();
            StringBuffer paperId = new StringBuffer();
            StringBuffer paperTitle = new StringBuffer();

            DefaultHandler handler = new DefaultHandler() {

                boolean paper = false;
                boolean title = false;
                boolean section = false;
                boolean sectionTitle = false;
                boolean paragraph = false;
                boolean reference = false;

                public void startElement(String uri, String localName,
                                         String qName, org.xml.sax.Attributes attributes)
                        throws SAXException {
                    if (qName.equalsIgnoreCase("Paper")) {
                        paper = true;
                        paperId.append(attributes.getValue("id"));
                    }

                    //TODO: need to skip title of reference, test data:P06-1139_cln.xml
                    if (qName.equalsIgnoreCase("title")) {
                        title = true;
                    }

                    if (qName.equalsIgnoreCase("Section")) {
                        section = true;
                    }

                    if (qName.equalsIgnoreCase("SectionTitle")) {
                        sectionTitle = true;
                    }

                    if (qName.equalsIgnoreCase("Paragraph")) {
                        paragraph = true;
                    }

                    if (qName.equalsIgnoreCase("Reference")) {
                        reference = true;
                    }
                }

                public void endElement(String uri, String localName,
                                       String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("Paragraph")) {
                        paragraph = false;
                    }
                }

                public void characters(char ch[], int start, int length) throws SAXException {
                    if (paper) {
                        paper = false;
                    }

                    if (title) {
                        title = false;

                        if (!reference) {
                            paperTitle.append(new String(ch, start, length)).append("\n");
                        }
                        reference = false;
                    }

                    if (section) {
                        section = false;
                    }

                    if (sectionTitle) {
                        sectionTitle = false;
                    }

                    if (paragraph) {
                        String paragraph = new String(ch, start, length);

                        paperParagraphs.append(paragraph);
                    }
                }
            };

            saxParser.parse(fileInputStream, handler);

            StringBuffer fullText = new StringBuffer();
            fullText.append(paperTitle).append("\n").append(paperParagraphs);

            String normalizedText = Normalizer.normalize(fullText.toString(), Normalizer.Form.NFD);
            normalizedText = StringEscapeUtils.unescapeXml(normalizedText);
            String cleanedText = cleanText(normalizedText);
            jateDocument = new JATEDocument(paperId.toString());
            jateDocument.setContent(cleanedText.trim());

        } catch (ParserConfigurationException e) {
            throw new JATEException("Failed to initialise SAXParser!" + e.toString());
        } catch (SAXException e) {
            throw new JATEException("Failed to initialise SAXParser!" + e.toString());
        } catch (IOException ioe) {
            throw new JATEException("I/O Exception when parsing input file!" + ioe.toString());
        }
        return jateDocument;
    }

    private static JATEDocument loadJATEDocFromXMLV2(File f) throws JATEException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream xmlFileStream = new FileInputStream(f);
        try {
            JATEDocument jdoc = new JATEDocument(f.toString());
            Document document = docBuilder.parse(xmlFileStream);

            Element doc = document.getDocumentElement();
            NodeList list = doc.getChildNodes();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeName().equalsIgnoreCase("title"))
                    sb.append(n.getTextContent()).append(". \n");
                else if (n.getNodeName().equalsIgnoreCase("section")) {
                    NodeList s = n.getChildNodes();
                    for (int j = 0; j < s.getLength(); j++) {
                        Node sn = s.item(j);
                        if (sn.getNodeName().equalsIgnoreCase("S")) {
                            sb.append(sn.getTextContent()).append(" ");
                        }
                    }
                }
            }

            jdoc.setContent(sb.toString().trim());
            jdoc.setPath(f.getPath());
            return jdoc;

        } finally {
            xmlFileStream.close();
        }
    }

    public static Set<String> extractGoldstandardTermsV2(String xmlFile) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "//term";
        InputSource inputSource = new InputSource(xmlFile);
        NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);

        Set<String> all = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element con = (Element) nodes.item(i);
            String concept = con.getTextContent().trim();

            if (concept.length() > 2)
                all.add(concept);

        }

        return all;

    }

    public static String cleanText(String normalizedText) {
        List<String> extractBrokenWords = extractBrokenWords(normalizedText);
//        extractBrokenWords.parallelStream().forEach((extractBrokenWord) ->
//                fixBrokenWords(normalizedParagraph, extractBrokenWord) );
        String cleanedText = normalizedText;
        for (String extractBrokenWord : extractBrokenWords) {
            cleanedText = fixBrokenWords(cleanedText, extractBrokenWord);
        }

        return cleanedText;
    }

    /**
     * two regex pattern matching rules to extract broken words in ACL RD-TEC corpus caused by pdf converter
     * e.g., "P r e v i o u s" for "previous"
     *
     * @param paragraphs text
     * @return List<String>  a list of matched "broken word" text
     */
    public static List<String> extractBrokenWords(String paragraphs) {
        List<String> brokenWords = new ArrayList<>();
        Pattern p1 = Pattern.compile("([A-Z]\\s([a-z]\\s){3,10})");
        Matcher m = p1.matcher(paragraphs);
        while (m.find()) {
            brokenWords.add(m.group());
        }

        Pattern p2 = Pattern.compile("([A-Z]\\s([A-Z]\\s){3,10})");
        m = p2.matcher(paragraphs);
        while (m.find()) {
            brokenWords.add(m.group());
        }
        return brokenWords;
    }

    /**
     * clean text for fixing broken words
     *
     * @param paragraph  text
     * @param brokenWord matched broken word
     * @return String  cleaned text
     */
    public static String fixBrokenWords(String paragraph, String brokenWord) {
        return paragraph.replaceAll(brokenWord, brokenWord.replaceAll(" ", "").concat(" "));
    }
}
