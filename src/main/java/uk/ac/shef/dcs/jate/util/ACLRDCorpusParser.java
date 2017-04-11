package uk.ac.shef.dcs.jate.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
        String inXMLFolder=args[0];
        String outTxtFolder=args[1];

        int count=0;

        for(File f: listFileTree(new File(inXMLFolder))) {
            count++;
            System.out.println(count);
            JATEDocument doc = loadJATEDocFromXML(new FileInputStream(f));
            PrintWriter p = new PrintWriter(outTxtFolder+File.separator+f.getName()+".txt");
            p.println(doc.getContent());
            p.close();
        }
    }

    public static Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<File>();
        if(dir==null||dir.listFiles()==null){
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
     * @param paragraphs  text
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
     * @param paragraph   text
     * @param brokenWord  matched broken word
     * @return String  cleaned text
     */
    public static String fixBrokenWords(String paragraph, String brokenWord) {
        return paragraph.replaceAll(brokenWord, brokenWord.replaceAll(" ", "").concat(" "));
    }
}
