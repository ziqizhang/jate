package uk.ac.shef.dcs.jate.v2.io;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.model.Document;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zqz on 15/09/2015.
 */
public class TikaSimpleDocumentCreator extends DocumentCreator {

    protected AutoDetectParser parser;
    protected BodyContentHandler handler;

    public TikaSimpleDocumentCreator(){
        this.parser=new AutoDetectParser();
        this.handler=new BodyContentHandler();
    }

    public Document create(String source) throws JATEException, IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(source));

        Metadata metadata = new Metadata();
        try {
            Document doc = new Document(source);
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

    public static void main(String[] args) throws IOException, JATEException {
        TikaSimpleDocumentCreator dc =new TikaSimpleDocumentCreator();
        dc.create("D:\\Work\\jate_github\\jate\\sample\\input/albatross.txt");

        System.out.println();
    }
}
