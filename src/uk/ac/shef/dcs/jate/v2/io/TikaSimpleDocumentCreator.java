package uk.ac.shef.dcs.jate.v2.io;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.model.JATEDocument;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zqz on 15/09/2015.
 */
public class TikaSimpleDocumentCreator extends DocumentCreator {

    protected AutoDetectParser parser;

    public TikaSimpleDocumentCreator(){
        this.parser=new AutoDetectParser();
    }

    public JATEDocument create(String source) throws JATEException, IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(source));
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        try {
            JATEDocument doc = new JATEDocument(source);
            doc.setPath(source);
            parser.parse(in, handler, metadata);
            doc.setContent(handler.toString());
            return doc;
        } catch(SAXException e){
            throw new JATEException(e);
        } catch(TikaException e){
            throw new JATEException(e);
        }finally{
            in.close();
        }
    }

    @Override
    public DocumentCreator copy() {
        return new TikaSimpleDocumentCreator();
    }
}
