package uk.ac.shef.dcs.jate.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by zqz on 24/03/17.
 */
public class CSVFileOutputReader  implements FileOutputReader {
    private CSVFormat format;

    public CSVFileOutputReader(CSVFormat format){
        this.format =format;
    }
    @Override
    public List<JATETerm> read(String file) throws IOException {
        CSVParser parser = new CSVParser(new FileReader(file),
                format);

        List<JATETerm> out = new ArrayList<>();
        for(CSVRecord rec: parser.getRecords()){
            out.add(new JATETerm(rec.get(0).trim(),
                    Double.valueOf(rec.get(1).trim())));
        }
        Collections.sort(out);
        return out;
    }
}
