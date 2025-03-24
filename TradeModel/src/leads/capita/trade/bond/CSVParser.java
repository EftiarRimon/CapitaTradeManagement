package leads.capita.trade.bond;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

public class CSVParser {
    public static final char SEPARATOR = ',';

    public static Iterator<Map<String, String>> getData(InputStream inputStream) throws IOException {
        //Reader reader = new FileReader(fileName);
        return new CsvMapper().readerFor(Map.class).with(CsvSchema.emptySchema().withHeader().withColumnSeparator(SEPARATOR).withoutQuoteChar()).readValues(inputStream);
    }
}