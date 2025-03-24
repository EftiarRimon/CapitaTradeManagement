package leads.capita.trade.file;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

public class DataParser {
    
    public DataParser() {
        super();
    }
    
    public static Integer parseInt(String inputValue)
    {
        try
        {
            return Integer.parseInt(inputValue);
        }
        catch ( Exception e )
        {
            return null;
        }
    }
    
    public static Long parseLong(String inputValue)
    {
        try
        {
            return Long.parseLong(inputValue);
        }
        catch ( Exception e )
        {
            return null;
        }
    }
    
    public static Float parseFloat(String inputValue)
    {
        try
        {
            return Float.parseFloat(inputValue);
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    public static Double parseDouble(String inputValue)
    {
        try
        {
            return Double.parseDouble(inputValue);
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    public static Date parseDate(String inputValue)
    {
        DateFormat formatter  = new SimpleDateFormat("dd-MM-yyyy");
        
        try
        {
            return formatter.parse(inputValue);
        }
        catch ( Exception e )
        {
            return null;
        }
    }
    
    public static Date parseDate(String inputValue, String format)
    {
        DateFormat formatter  = new SimpleDateFormat(format);
        try
        {
            return formatter.parse(inputValue);
        }
        catch ( Exception e )
        {
            return null;
        }
    }
}
