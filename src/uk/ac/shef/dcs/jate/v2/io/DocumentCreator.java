package uk.ac.shef.dcs.jate.v2.io;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.model.JATEDocument;

import java.io.IOException;

/**
 * Created by zqz on 15/09/2015.
 */
public abstract class DocumentCreator {

    public abstract JATEDocument create(String source) throws JATEException, IOException;

    public abstract DocumentCreator copy();
}
