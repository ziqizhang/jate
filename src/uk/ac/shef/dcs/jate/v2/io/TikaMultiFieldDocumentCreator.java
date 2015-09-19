package uk.ac.shef.dcs.jate.v2.io;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.model.JATEDocument;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A toy example to show how we can assign text from particular parts of an input
 * document to particular fields of a jate document
 */
public class TikaMultiFieldDocumentCreator extends DocumentCreator {

    private JATEProperties properties;
    protected AutoDetectParser parser;

    public TikaMultiFieldDocumentCreator(JATEProperties properties) {
        this.properties = properties;
    }

    @Override
    public JATEDocument create(String source) throws JATEException, IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(source));
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        try {
            JATEDocument doc = new JATEDocument(source);
            doc.setPath(source);
            parser.parse(in, handler, metadata);
            doc.setContent(handler.toString());

            //add two specific fields: title and links. The values are not really useful
            String dynamicFieldname = properties.getSolrFieldnameJATETermsF();
            if (dynamicFieldname == null)
                throw new JATEException("'fieldname_jate_text_f' required but is not defined in jate.properties");
            doc.getMapField2Content().put(properties.getSolrFieldnameJATETermsF().
                    replace("\\*", "title"), doc.getContent().substring(0, 100));
            doc.getMapField2Content().put(properties.getSolrFieldnameJATETermsF().
                    replace("\\*", "link"), doc.getContent().substring(101,120));
            return doc;
        } catch (SAXException e) {
            throw new JATEException(e);
        } catch (TikaException e) {
            throw new JATEException(e);
        } finally {
            in.close();
        }
    }

    @Override
    public DocumentCreator copy() {
        return new TikaMultiFieldDocumentCreator(properties);
    }
}
