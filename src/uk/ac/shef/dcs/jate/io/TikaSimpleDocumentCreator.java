package uk.ac.shef.dcs.jate.io;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import java.io.File;

/**
 * Created by zqz on 15/09/2015.
 */
public class TikaSimpleDocumentCreator extends DocumentCreator {

	protected ContentExtractor contentExtractor;

	public TikaSimpleDocumentCreator() {
		contentExtractor = new ContentExtractor();
	}

	public JATEDocument create(String source) throws JATEException {
		File file = new File(source);
		JATEDocument doc = new JATEDocument(source);
		doc.setPath(source);
		String content = contentExtractor.extractContent(file);
		doc.setContent(content);
		return doc;
	}

	@Override
	public DocumentCreator copy() {
		return new TikaSimpleDocumentCreator();
	}
}
