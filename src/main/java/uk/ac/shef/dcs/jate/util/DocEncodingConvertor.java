package uk.ac.shef.dcs.jate.util;

import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * Created by zqz on 18/05/17.
 */
public class DocEncodingConvertor {
    public static void main(String[] args) throws IOException {
        for (File f: new File(args[0]).listFiles()){
            String content=FileUtils.readFileToString(f);
            File outFile = new File(args[1]+File.separator+f.getName());

            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(outFile), "UTF8"));

            out.write(content);
            out.flush();
            out.close();
        }
    }
}
