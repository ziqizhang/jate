package uk.ac.shef.dcs.jate.io;

import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface FileOutputReader {

    List<JATETerm> read(String file) throws IOException;
}
