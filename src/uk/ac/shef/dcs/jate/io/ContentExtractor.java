package uk.ac.shef.dcs.jate.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

import uk.ac.shef.dcs.jate.JATEException;

/**
 * 
 * Content Extractor that wraps Tika to resolve the detection problem
 * 
 * see Tika bug via https://issues.apache.org/jira/browse/TIKA-879
 *
 */
public class ContentExtractor {

	// private Parser autoDetectParser = null;
	// quick fix for auto detect parser when some of txt file is not
	// recognisable
	private Parser txtParser = null;
	private Tika tika = null;

	/**
	 * Maximum length of the strings returned by the parseToString methods. Used
	 * to prevent out of memory problems with huge input documents. The default
	 * setting is 100k characters.
	 */
	private int maxStringLength = 100 * 1000;

	public ContentExtractor() {
		// autoDetectParser = new AutoDetectParser();
		txtParser = new TXTParser();
		tika = new Tika();
	}

	public String extractContent(URI fileURI) throws JATEException {
		File file = new File(fileURI);

		return extractContent(file);
	}

	public String extractContent(File file) throws JATEException {
		String content = "";
		if (file == null || !file.exists()) {
			throw new JATEException("File is not found!");
		}

		try {
			String contentType = Files.probeContentType(file.toPath());

			if (MediaType.TEXT_PLAIN.getBaseType().toString().equals(contentType)) {
				content = parseTXTToString(file);
			} else {
				content = tika.parseToString(file);
			}
		} catch (IOException e1) {
			throw new JATEException("I/O exception when detecting file type.");
		} catch (TikaException tikaEx) {
			throw new JATEException("Tika Content extraction exception: " + tikaEx.toString());
		}

		return content;
	}

	private String parseTXTToString(File file) throws IOException, TikaException {
		Metadata metadata = new Metadata();
		InputStream stream = TikaInputStream.get(file, metadata);
		return parseTXTToString(stream, metadata);
	}

	private String parseTXTToString(InputStream stream, Metadata metadata) throws IOException, TikaException {
		WriteOutContentHandler handler = new WriteOutContentHandler(maxStringLength);
		try {
			ParseContext context = new ParseContext();
			context.set(Parser.class, txtParser);
			txtParser.parse(stream, new BodyContentHandler(handler), metadata, context);
		} catch (SAXException e) {
			if (!handler.isWriteLimitReached(e)) {
				// This should never happen with BodyContentHandler...
				throw new TikaException("Unexpected SAX processing failure", e);
			}
		} finally {
			stream.close();
		}
		return handler.toString();
	}

	public static void main(String[] args) throws JATEException, URISyntaxException {
		ContentExtractor contentExtractor = new ContentExtractor();
		File file = new File("C:\\oak-project\\TermRecogniser\\evaluate\\lotus_notes\\ Workshop-QG9JVW.txt");
		String content = contentExtractor.extractContent(file.toURI());
		System.out.println(content);
	}
}
