package uk.ac.shef.dcs.jate.io;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import java.io.File;

/**
 * A toy example to show how we can assign text from particular parts of an
 * input document to particular fields of a jate document
 */
public class TikaMultiFieldDocumentCreator extends DocumentCreator {

	private JATEProperties properties;
	protected ContentExtractor contentExtractor;

	public TikaMultiFieldDocumentCreator(JATEProperties properties) {
		this.properties = properties;
		contentExtractor = new ContentExtractor();
	}

	@Override
	public JATEDocument create(String filePath) throws JATEException {
		File file = new File(filePath);

		JATEDocument doc = new JATEDocument(filePath);
		doc.setPath(filePath);
		String content = contentExtractor.extractContent(file);
		doc.setContent(content);

		// add two specific fields: title and links. The values are not really
		// useful
		String dynamicFieldName = properties.getSolrFieldNameJATECTermsF();
		//TODO: check the comments below
		// JATEProperties.PROPERTY_SOLR_FIELD_MAP_DOC_PARTS fields extracted by Tika where terms will be extracted from
		if (dynamicFieldName == null)
			throw new JATEException(String.format("'%s' required but is not defined in jate.properties",
					JATEProperties.PROPERTY_SOLR_FIELD_MAP_DOC_PARTS));
		doc.getMapField2Content().put(properties.getSolrFieldNameJATECTermsF().replace("\\*", "title"),
				doc.getContent().substring(0, 100));
		doc.getMapField2Content().put(properties.getSolrFieldNameJATECTermsF().replace("\\*", "link"),
				doc.getContent().substring(101, 120));
		return doc;
	}

	@Override
	public DocumentCreator copy() {
		return new TikaMultiFieldDocumentCreator(properties);
	}
}
