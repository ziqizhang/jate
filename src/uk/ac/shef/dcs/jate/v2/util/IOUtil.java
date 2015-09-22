package uk.ac.shef.dcs.jate.v2.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by zqz on 22/09/2015.
 */
public class IOUtil {
    public static Writer getUTF8Writer(String file) throws FileNotFoundException {
        return new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8));
    }
}
