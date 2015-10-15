package uk.ac.shef.dcs.jate.io;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import java.io.IOException;

/**
 * Created by zqz on 15/09/2015.
 */
public abstract class DocumentCreator {

    public abstract JATEDocument create(String source) throws JATEException, IOException;

    public abstract DocumentCreator copy();
}
