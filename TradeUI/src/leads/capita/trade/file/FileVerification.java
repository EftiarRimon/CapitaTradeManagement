package leads.capita.trade.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.sql.SQLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import leads.capita.trade.exception.TradeFileDateMismatchException;
import leads.capita.trade.plsql.TMPlsqlExecutor;


public class FileVerification {
    BufferedReader br;

    public FileVerification() {
        super();
    }

    /*
     * Varify the file data sequence upto 10 lines based on data type.
     */

    public boolean verifyMSAPlus(InputStream inputStream, Date tradeDate) throws IOException,
                                                                                 TradeFileDateMismatchException {
        String line = "";
        String delimiter = "\\~";
        int i = 0;
        boolean isValidData = false;
        Long born;
        Long boid;
        String instrumentCode;
        String treader;
        Date tradeFileDate;
        Integer quantity;
        Float price;

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

        br = new BufferedReader(new InputStreamReader(inputStream));

        while ((line = br.readLine()) != null) {

            if (line.trim().length() > 0) {
                try {
                    String[] row = line.split("\\~");
                    int j = 0;

                    /*  for (String data : row)
                        {
                            System.out.print(j+"::"+data+" ");
                            j++;
                        }
                        */

                    if (row == null) {
                        isValidData = false;
                        break;
                    }

                    born = DataParser.parseLong(row[0]);
                    instrumentCode = row[1];
                    treader = row[3];
                    quantity = DataParser.parseInt(row[5]);
                    price = DataParser.parseFloat(row[6]);

                    tradeFileDate = DataParser.parseDate(row[7]);

                    if ((tradeFileDate != null) && (tradeDate.compareTo(tradeFileDate) != 0)) {
                        throw new TradeFileDateMismatchException("File date(" + formatter.format(tradeFileDate) +
                                                                 ") and uploaded date(" + formatter.format(tradeDate) +
                                                                 ")  has been mismatched!");
                    }

                    boid = DataParser.parseLong(row[14]);

                    if ((born != null) && (instrumentCode != null) && (treader != null) && (tradeFileDate != null) &&
                        (quantity != null) && (price != null) && (boid != null)) {
                        isValidData = true;
                    } else {
                        isValidData = false;
                    }

                } catch (ArrayIndexOutOfBoundsException e) {
                    isValidData = false;
                }
            }


            i++;
            if ((i > 10) || isValidData) {
                break;
            }
        }
        return isValidData;
    }


    public boolean verifyBT(InputStream inputStream, Date tradeDate) throws IOException,
                                                                            TradeFileDateMismatchException {
        String line = "";
        String delimiter = "\\|";
        int i = 0;
        boolean isValidData = false;
        String tid;
        String instrumentCode;
        String treader;
        Date tradeFileDate;
        Integer quantity;
        Float price;

        // old file means that the code has been written considering that the file will contain 13 elements
        // but we found that there are some files which contains 12 elements
        // so we will alter our logic here based on the number of elements we find in the file
        boolean oldFile = false;

        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        br = new BufferedReader(new InputStreamReader(inputStream));

        //line = br.readLine();
        while ((line = br.readLine()) != null) {

            tid = "";
            instrumentCode = "";
            treader = "";
            tradeFileDate = null;
            quantity = 0;
            price = 0.0f;

            if (line.trim().length() > 0) {

                try {
                    String[] row = line.split("\\|");
                    int j = 0;
                    for (String data : row) {
                        System.out.print(j + "::" + data + " ");
                        j++;
                    }
                    System.out.print("Length is" + "::" + row.length + " ");

                    if (row == null) {
                        System.out.print("Found Null here" + "::" + row.length + " ");
                        isValidData = false;
                        break;
                    }

                    //0::CTG01 1::11002 2::PEOPI 3::B 4::40 5::1035.00 6::125 7:: 8:: 9::39397 10::30/11/2010 11::1:50:30 12::30/11/2010 13::1:48:04
                    if (row.length == 14) {
                        oldFile = true;
                    }
                    treader = row[0];
                    instrumentCode = row[2];
                    quantity = DataParser.parseInt(row[4]);
                    price = DataParser.parseFloat(row[5]);
                    if (oldFile) {
                        tid = row[9];
                    } else {
                        tid = row[9];
                    }


                    try {
                        if (oldFile) {
                            tradeFileDate = formatter.parse(row[10]);
                        } else {
                            tradeFileDate = formatter.parse(row[10]);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if ((tradeFileDate != null) && (tradeDate.compareTo(tradeFileDate) != 0)) {
                        throw new TradeFileDateMismatchException("File date(" + formatter.format(tradeFileDate) +
                                                                 ") and uploaded date(" + formatter.format(tradeDate) +
                                                                 ")  has been mismatched!");
                    }

                    if ((tid != null) && (instrumentCode != null) && (treader != null) && (tradeFileDate != null) &&
                        (quantity != null) && (price != null)) {
                        isValidData = true;
                    } else {
                        isValidData = false;
                    }
                }

                catch (ArrayIndexOutOfBoundsException e) {
                    isValidData = false;
                }
            }
            i++;
            if ((i > 10) || isValidData) {
                break;
            }
        }
        return isValidData;
    }


    public boolean verifyPrice(String exchange, InputStream inputStream, Date tradeDate) throws IOException,
                                                                                                TradeFileDateMismatchException {
        boolean isValidData = false;
        if (exchange.toUpperCase().equals("PRICE_DSE_REPORT")) {
            isValidData = verifyDSEComplexPrice(inputStream, tradeDate);
        }
        return isValidData;
    }

    public boolean nonTradeHolidayChecker(int exchangeId, Date tradeDate) throws IOException,
                                                                                 TradeFileDateMismatchException,
                                                                                 SQLException {
        boolean isValidData = false, isHoliday = false, isNonTradeDay = false;
        TMPlsqlExecutor tm = new TMPlsqlExecutor();
        System.out.println("INSIDE Caller");
        isNonTradeDay = tm.callnonTradeHolidayChecker(exchangeId, tradeDate, false);


        return isNonTradeDay;
    }

    private boolean verifyDSEComplexPrice(InputStream inputStream, Date tradeDate) throws IOException,
                                                                                          TradeFileDateMismatchException {
        String line = "";
        String delimiter = "\\|";
        int lineNo = 0;
        boolean isValidData = false;
        Date fileDate = null;
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        br = new BufferedReader(new InputStreamReader(inputStream));

        // Reade the file date
        while ((line = br.readLine()) != null) {
            String datePointer = "TODAY'S SHARE MARKET : ";
            if (line.contains(datePointer)) {
                try {
                    fileDate =
                            formatter.parse(line.substring(line.indexOf(datePointer) + datePointer.length()).trim());
                    isValidData = true;
                    break;

                } catch (Exception e) {
                    isValidData = false;
                    e.printStackTrace();
                }
            } else {
                isValidData = false;
            }

            lineNo++;
            if (lineNo > 5) {
                break;
            }
        }


        if ((fileDate != null) && (tradeDate.compareTo(fileDate) != 0)) {
            isValidData = false;
            throw new TradeFileDateMismatchException("File date(" + formatter.format(fileDate) +
                                                     ") and uploaded date(" + formatter.format(tradeDate) +
                                                     ") has been mismatched!");
        }


        while (isValidData && (lineNo > 120)) {
            line = br.readLine();
            System.out.println(line);
            System.out.println(lineNo);
            String headerPointer = "Instr Code     Open     High      Low    Close    %Chg  Trade   Volume Value(Mn)";

            if ((line != null) && (line.contains(headerPointer))) {
                isValidData = true;
                break;
            } else {
                isValidData = false;
            }

            lineNo++;
            if (lineNo > 120) {
                break;
            }
        }

        //  System.out.println(isValidData);

        return isValidData;
    }


}
