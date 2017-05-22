package uk.ac.shef.dcs.jate.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class JATEUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JATEUtil.class);

    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    /**
     * statistics of files and subdirectories
     *
     * @param corpusDir  corpus directory path
     * @return Map  {1|2: number} 1 stands for directory, 2
     */
    public static Map<Integer, Long> fileStatitics(Path corpusDir) throws IOException {
        Map<Integer, Long> stats = Files.walk(corpusDir)
                .parallel()
                .collect(groupingBy(n -> Files.isDirectory(n, LinkOption.NOFOLLOW_LINKS) ? 1 : 2, counting()));
        System.out.format("Files: %d, dirs: %d. ", stats.get(2), stats.get(1));

        return stats;
    }

    public static JATEDocument loadACLRDTECDocument(InputStream fileInputStream) throws JATEException {
        return loadJATEDocFromXML(fileInputStream);
    }

    /**
     * load JATEDocument from any file.
     *
     * Raw text will be automatically extracted with Apache TIKA as document content
     * and Doc id will be set by file name.
     *
     * @param file  any file format that is supported in Tika
     * @return JATEDocument  return null if file name is null
     */
    public static JATEDocument loadJATEDocument(Path file) throws JATEException {
        if (file.getFileName() == null){
            return null;
        }

        String docId = file.getFileName().toString();

        JATEDocument jateDocument = new JATEDocument(docId);
        jateDocument.setPath(file.toAbsolutePath().toString());
        FileInputStream fileStream = null;
        try {
        	fileStream = new FileInputStream(file.toFile());
            jateDocument.setContent(parseToPlainText(fileStream));

            return jateDocument;
        } catch (FileNotFoundException e) {
            throw new JATEException(String.format("File is not found from [%s]", file.toString()));
        } finally {
        	if (fileStream != null) {
        		try {
					fileStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
    }

    /**
     * load ACL RD-TEC documents from raw text corpus
     *
     * @param rawTxtFile raw text file
     * @return JATEDocument
     */
    public static JATEDocument loadACLRDTECDocumentFromRaw(File rawTxtFile) {
        JATEDocument jateDocument = new JATEDocument(rawTxtFile.toURI());
        jateDocument.setId(rawTxtFile.getName());

        StringBuilder rawTextBuffer = new StringBuilder();
        try {
            List<String> lines = FileUtils.readLines(rawTxtFile);
            if (lines.size() > 0) {
                lines.forEach(rawTextBuffer::append);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        jateDocument.setContent(rawTextBuffer.toString());
        return jateDocument;
    }

    /**
     *
     * @param fileInputStream  ACL XML file
     * @return JATEDocument JATE Document Object
     * @throws JATEException
     */
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
                        paperParagraphs.append(" ");
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

    /**
     * load files from corpus directory recursively
     *
     * @param corpusDir  corpus directory
     * @return List<Path>
     * @throws JATEException
     */
    public static List<Path> loadFiles(Path corpusDir) throws JATEException {
        try {
            List<Path> files = new ArrayList<>();

            Files.walk(corpusDir).parallel().forEach((n) -> {
                if (!Files.isDirectory(n, LinkOption.NOFOLLOW_LINKS)) {
                    files.add(n);
                }
            });

            return files;
        } catch (IOException e) {
            //LOG.error(String.format("Failed to access corpus path [%s]", corpusDir.toUri()));
            throw new JATEException(String.format("Failed to access corpus path [%s]", corpusDir.toUri()));
        }
    }

    public static String parseToPlainText(InputStream fileStream) {
        BodyContentHandler handler = new BodyContentHandler();
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        String rawContent = "";

        try {
            parser.parse(fileStream, handler, metadata);
            rawContent = handler.toString();
        } catch (IOException | SAXException | TikaException e) {
            LOG.debug("Parsing Exception while extracting content from current file. "
                    + e.toString());
        }
        return rawContent;
    }

    public static void addNewDoc(EmbeddedSolrServer server, String docId, String docTitle,
                                 String text, JATEProperties jateProperties, boolean commit)
            throws IOException, SolrServerException, JATEException {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("id", docId);
        newDoc.addField("title_s", docTitle);
        newDoc.addField("text", text);

        newDoc.addField(jateProperties.getSolrFieldNameJATENGramInfo(), text);
        newDoc.addField(jateProperties.getSolrFieldNameJATECTerms(), text);

        server.add(newDoc);

        if (commit) {
            server.commit();
        }
    }
}
