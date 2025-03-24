package leads.capita.trade.file;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.math.BigDecimal;

import java.sql.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.regex.Pattern;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;


/*
 * Main Uddin patowary
 */
public class PayInOutFileUtil {
    static String regexMMDDYYYY = "^(0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])[- /.](19|20)\\d\\d$";
    static String regexDDMMYYYY = "^(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d$";
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    static String user_home = System.getProperty("user.home");
    static String payinoutFolder = user_home + File.separator + "payinout";
    static String payinoutLatestFolder = user_home + File.separator + "payinout" + File.separator + "latest";

    public PayInOutFileUtil() {
        super();
    }

    public static boolean isAValidMMDDYYYYDate(String date) {
        return Pattern.matches(regexMMDDYYYY, date);
    }

    public static boolean isAValidDDMMYYYYDate(String date) {
        return Pattern.matches(regexDDMMYYYY, date);
    }

    public static String _getDateString(String format, Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date == null ? new Date(getUiTradeDate().getTime()) : date);
    }

    public static String _getFixedLengthString(String value, int length, boolean isPadding, char leadingChar,
                                               char emptyFill) {
        String _returnVal = null;
        int diff = 0;
        if (value != null && !value.equals("") && length != 0) {
            if (value.trim().length() < length) {
                diff = (length - value.trim().length());
                StringBuffer paddingChars = new StringBuffer();
                for (int i = 0; i < diff; i++) {
                    paddingChars.append(leadingChar);
                }
                if (isPadding) {
                    _returnVal = paddingChars.append(value).toString();
                } else {
                    _returnVal = value + paddingChars.toString();
                }
            } else {
                if (value.trim().length() == length)
                    _returnVal = value;
                else {
                    _returnVal = value.substring(0, length);
                    //_returnVal = value;
                    // System.out.print(value + " --Invalid chars--___--length-  " + length);
                }
            }
        } else {
            StringBuffer paddingChars = new StringBuffer();
            if (length != 0) {
                for (int i = 0; i < length; i++) {
                    paddingChars.append(emptyFill);
                }
            }
            _returnVal = paddingChars.toString();
        }

        return _returnVal;
    }

    public String _getFileSL(String curSl) {
        String _returnval = null;
        Integer intval = null;
        if (curSl != null && !curSl.equals("")) {
            intval = DataParser.parseInt(curSl);
            if (intval != null) {
                intval++;
                if (intval >= 10) {
                    _returnval = intval.toString();
                } else {
                    _returnval = "0" + intval;
                }
            }
        } else {
            _returnval = "01";
        }
        return _returnval;
    }


    public static String _getFileSlFromFileName(String fileName) {
        String _fileSl = null;
        int dotIndex = 0;
        if (fileName != null && !fileName.equals("")) {
            dotIndex = fileName.lastIndexOf(".");
            _fileSl = fileName.trim().substring(++dotIndex);
        }
        return _fileSl == null ? "00" : _fileSl;
    }

    public static String _getNewFileSL(String curSl) {
        String _returnval = null;
        Integer intval = null;
        if (curSl != null && !curSl.equals("")) {
            intval = DataParser.parseInt(curSl);
            if (intval != null) {
                intval++;
                if (intval >= 10) {
                    _returnval = intval.toString();
                } else {
                    _returnval = "0" + intval;
                }
            }
        } else {
            _returnval = "01";
        }
        return _returnval;
    }

    //Sample File Name
    //String uploadId,String dpId,Date date,String sl

    public static String _generateFileName(String uploadId, String dpId, Date date, String fileNameExt) {
        String fName = null;
        //String user_home = System.getProperty("user.home");
        File home_dirFile = new File(PayInOutFileUtil.payinoutLatestFolder);
        StringBuffer _filename = new StringBuffer();
        _filename.append(uploadId); //payIn(01)/payOUT(02) _-Upload ID
        _filename.append(_getFixedLengthString(dpId, 6, true, '0', ' ')); //DP ID
        _filename.append(_getDateString("ddMMyyyy", new Date(date.getTime()))); //date
        fName = _filename.toString();
        String existFileName = _getExistingFileName(home_dirFile, fName);
        //System.out.print("--------" + existFileName);
        _filename.append(".");
        if (fileNameExt != null && fileNameExt.equals("10"))
            _filename.append("01");
        else if (fileNameExt != null && fileNameExt.equals("11"))
            _filename.append("02");
        else
            _filename.append(_getNewFileSL(_getFileSlFromFileName(existFileName))); //SL NO
        return _filename.toString();
    }

    //Sample Header
    //String totalRecords,String totalQty,String operatorId,String dpId,String exchangeId

    public static String _generateFileHeader(String totalRecords, String totalQty, String operatorId, String dpId,
                                             String exchangeCode) {
        StringBuffer _fileHeader = new StringBuffer();
        _fileHeader.append(_getFixedLengthString(totalRecords, 7, true, '0', ' ')); //Total Records
        _fileHeader.append(_getFixedLengthString(totalQty, 13, true, '0', ' ')); //Total Quantity
        _fileHeader.append(_getFixedLengthString(operatorId, 6, true, ' ', ' ')); //Operator ID
        _fileHeader.append(_getFixedLengthString(dpId, 6, true, '0', ' ')); //DP ID
        _fileHeader.append(_getFixedLengthString(exchangeCode, 2, true, '0', '-')); //Exchamge
        return _fileHeader.toString();
    }


    //Sample Row
    /*String dateFormat,Date excutionDate,String boId,String counterBoID,String brokerCode,String isin,String tradeQty,String payIOFlag
   ,String refNumber,String settlementId,char emptyFill,char leadingChar
*/
    // For Pay in

    public static String _generateFileRow(String dateFormat, Date excutionDate, String boId, String counterBoID,
                                          String brokerCode, String isin, String tradeQty, String payIOFlag,
                                          String refNumber, String rowNum) {
        StringBuffer _row = new StringBuffer();
        _row.append(_getDateString(dateFormat, excutionDate)); //Execution Date
        _row.append(_getFixedLengthString(boId, 16, true, '0', ' ')); //BO ID
        _row.append(_getFixedLengthString(counterBoID, 16, true, '0', ' ')); //Counter ID
        _row.append(_getFixedLengthString(brokerCode, 6, true, '0', ' ')); //Broker Code
        _row.append(_getFixedLengthString(isin, 12, true, '0', ' ')); //ISIN
        _row.append(_getFixedLengthString(tradeQty, 12, true, '0', ' ')); //Trade Quantity
        _row.append(payIOFlag); //Pay IN -I/ Pay OUT-O Flag
        _row.append(_getFixedLengthString(refNumber, 16, true, '0', ' ')); //Internal ref  number. (Investor Code)
        //_row.append(_getFixedLengthString(rowNum, 12, true, ' ', ' ')); //SL ID
        //System.out.print(row.toString().length()+"-- --___--- " + row.toString());
        return _row.toString();
    }

    // For Pay out

    public static String _generateFileRow(String dateFormat, Date excutionDate, String boId, String counterBoID,
                                          String brokerCode, String isin, String tradeQty, String payIOFlag,
                                          String refNumber, String rowNum, String fileType) {
        StringBuffer _row = new StringBuffer();
        _row.append(_getDateString(dateFormat, excutionDate)); //Execution Date
        if (fileType.equalsIgnoreCase("01")) {
            _row.append(_getFixedLengthString(boId, 16, true, '0', ' ')); //BO ID
            _row.append(_getFixedLengthString(counterBoID, 16, true, '0', ' ')); //Counter ID
        } else {
            _row.append(_getFixedLengthString(counterBoID, 16, true, '0', ' ')); //Counter ID
            _row.append(_getFixedLengthString(boId, 16, true, '0', ' ')); //BO ID
        }

        _row.append(_getFixedLengthString(brokerCode, 6, true, '0', ' ')); //Broker Code
        _row.append(_getFixedLengthString(isin, 12, true, '0', ' ')); //ISIN
        _row.append(_getFixedLengthString(tradeQty, 12, true, '0', ' ')); //Trade Quantity
        _row.append(payIOFlag); //Pay IN -I/ Pay OUT-O Flag
        _row.append(_getFixedLengthString(refNumber, 16, true, '0', ' ')); //Internal ref  number. (Investor Code)
        //_row.append(_getFixedLengthString(rowNum, 12, true, ' ', ' ')); //SL ID
        //System.out.print(row.toString().length()+"-- --___--- " + row.toString());
        return _row.toString();
    }


    public void copy(File src, File dst) throws IOException {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void _renameFile(File src, File dst) throws IOException {
        try {
            boolean success = src.renameTo(dst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void moveFile(File src, File dst) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
        Date curDate = null;
        Date lastModified = null;
        try {
            curDate = format.parse(format.format(new Date(getUiTradeDate().getTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            if (src.exists()) {
                if (src.isDirectory()) {
                    File[] files = src.listFiles();
                    for (File f : files) {
                        if (f.isFile()) {
                            //f.setLastModified(getUiTradeDate().getTime());
                            lastModified = format.parse(format.format(new Date(f.lastModified())));
                            if (lastModified.compareTo(curDate) > 0) {
                                boolean success = f.renameTo(new File(dst, f.getName()));
                                if (success)
                                    f.delete();
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String _extractDate(String lineString, int startPos, int endPos) {
        String data = null;
        if (lineString != null && !lineString.equals(""))
            data = lineString.substring(startPos, endPos);
        else {
            System.out.print("&&  String not found ! to _extractDate");
        }
        return data;
    }


    public boolean _checkForExistingFile(File file) {
        Date lastModified = null;
        boolean fileFound = false;
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            Date curDate = format.parse(format.format(new Date(getUiTradeDate().getTime())));
            if (file != null) {
                if (file.exists()) {
                    if (file.isFile()) {
                        lastModified = format.parse(format.format(new Date(file.lastModified())));
                        if (lastModified.compareTo(curDate) == 0) {
                            fileFound = true;
                        }
                    }
                }

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return fileFound;
    }


    public static String _getExistingFileName(File folder, String genFileName) {
        Date lastModified = null;
        boolean fileFound = false;
        String existingFileName = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            // Date curDate = format.parse(format.format(new Date(getUiTradeDate().getTime())));
            Date curDate = format.parse(format.format(new Date()));
            if (folder.exists()) {
                File[] allFiles = folder.listFiles();
                if (allFiles.length > 0) {
                    for (File f : allFiles) {
                        if (f.isFile()) {
                            lastModified = format.parse(format.format(new Date(f.lastModified())));
                            if (lastModified.compareTo(curDate) != 0) {
                                if (f.getName().contains(genFileName.toLowerCase())) {
                                    existingFileName = f.getName();
                                    fileFound = true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return existingFileName;
    }


    public static String lastCreatedFile(File dir) {
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choise = null;
        if (files.length > 0) {
            for (File file : files) {
                if (file.lastModified() > lastMod) {
                    choise = file;
                    lastMod = file.lastModified();
                }
            }
        }
        return choise != null ? choise.getName() : null;
    }

    public String getLineFromFileText(File fileName) {
        FileInputStream fstream = null;
        DataInputStream in = null;
        try {
            BufferedReader br = null;
            fstream = new FileInputStream(fileName);
            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
                System.out.println(strLine);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }


    public String getDpIdFromBOID(String boId) {
        String dpId = "";
        if (boId != null) {
            dpId.substring(2, 8);
        }
        return dpId;
    }

    public static void filterIteratorBydate(String iter, Date date, String exCode, String fileType, String productTypes) {
        DCIteratorBinding it = ADFUtils.findIterator(iter);
        it.setRangeSize((int)it.getEstimatedRowCount());
        ViewObject VO = it.getViewObject();
        String d = new SimpleDateFormat("dd-MMM-yyyy").format(date.getTime());
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        // bankVO.setWhereClause(" QRSLT.BANK_CODE='" + bankCode + "'");
        if (fileType.equalsIgnoreCase("01")){
            String pType = getCurrentRowValue("PayinPayoutVOIterator", "ProductType");
            whereClauseSql.append("to_char(QRSLT.PAY_IN_OUT_DATE,'dd-MON-yyyy') = upper('" + d + "')");
            whereClauseSql.append("AND QRSLT.PAY_IN_OUT_TYPE='PAY_IN'");
            if(pType != null && pType.equalsIgnoreCase("Y"))
                whereClauseSql.append("AND QRSLT.PRODUCT_TYPE in " + productTypes);
            else
                whereClauseSql.append("AND QRSLT.PRODUCT_TYPE not in " + productTypes);
        }
        else{
            whereClauseSql.append("to_char(QRSLT.PAY_IN_OUT_DATE,'dd-MON-yyyy') = upper('" + d + "')");
            whereClauseSql.append("AND QRSLT.PAY_IN_OUT_TYPE='PAY_OUT'");
        }
        whereClauseSql.append("AND QRSLT.EXCHANGE_CODE='" + exCode + "'");
        VO.setWhereClause(whereClauseSql.toString());
        VO.setRangeSize(-1);
        VO.addQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        // System.out.print("<--With  own--> \n " + VO.getQuery() + ADFUtils.findIterator(iter).getEstimatedRowCount());
        VO.executeQuery();
        VO.setWhereClause(null);
    }

    public static void filterIteratorBydate(String iter, Date date, String exCode, String isWithOwn, String fileType, String productTypes) {
        DCIteratorBinding it = ADFUtils.findIterator(iter);
        it.setRangeSize((int)it.getEstimatedRowCount());
        ViewObject VO = it.getViewObject();
        String d = new SimpleDateFormat("dd-MMM-yyyy").format(date.getTime());
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        // bankVO.setWhereClause(" QRSLT.BANK_CODE='" + bankCode + "'");
        if (fileType.equalsIgnoreCase("01")){
            String pType = getCurrentRowValue("PayinPayoutVOIterator", "ProductType");
            whereClauseSql.append("to_char(QRSLT.PAY_IN_OUT_DATE,'dd-MON-yyyy') = upper('" + d + "')");
            whereClauseSql.append("AND QRSLT.PAY_IN_OUT_TYPE='PAY_IN'");
                if(pType != null && pType.equalsIgnoreCase("Y"))
                    whereClauseSql.append("AND QRSLT.PRODUCT_TYPE in " + productTypes);
                else
                    whereClauseSql.append("AND QRSLT.PRODUCT_TYPE not in " + productTypes);
        }
        else{
            whereClauseSql.append("to_char(QRSLT.PAY_IN_OUT_DATE,'dd-MON-yyyy') = upper('" + d + "')");
            whereClauseSql.append("AND QRSLT.PAY_IN_OUT_TYPE='PAY_OUT'");
        }
        whereClauseSql.append("AND QRSLT.EXCHANGE_CODE='" + exCode + "'");
        whereClauseSql.append("AND QRSLT.PRODUCT_TYPE <>'OWN'");
        VO.setWhereClause(whereClauseSql.toString());
        VO.setRangeSize(-1);
        VO.addQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        //System.out.print("<--With out Own--> \n " + VO.getQuery() + " "+ADFUtils.findIterator(iter).getEstimatedRowCount());
        VO.executeQuery();
        VO.setWhereClause(null);
    }

    public static void filterIteratorByExchange(String iter, String exCode) {
        DCIteratorBinding it = ADFUtils.findIterator(iter);
        ViewObject VO = it.getViewObject();
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        whereClauseSql.append("QRSLT.EXCHANGE_CODE='" + exCode + "'");
        VO.setWhereClause(whereClauseSql.toString());
        VO.addQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        //System.out.print("<----> \n " + VO.getQuery());
        VO.executeQuery();
        VO.setWhereClause(null);
    }


    //Date d, String pattern
    //E MMM dd HH:mm:ss z yyyy
    //.substring(0, date.indexOf(" "))

    public static Date getParsedDate(String date, String pattern) {
        String rVal = null;
        Date parsedDate = null;
        try {
            SimpleDateFormat parsFormat = null;
            parsFormat = new SimpleDateFormat(pattern);
            parsedDate = parsFormat.parse(date);
        } catch (ParseException e) {
            // System.out.println("--Cheque print----date Parsing Prob. for "+date);
            try {
                SimpleDateFormat parsFormat = null;
                parsFormat = new SimpleDateFormat("dd-MM-yyyy");
                parsedDate = parsFormat.parse(date);
            } catch (ParseException ex) {
                //ex.printStackTrace();
                try {
                    SimpleDateFormat parsFormat = null;
                    parsFormat = new SimpleDateFormat("dd-MMM-yyyy");
                    parsedDate = parsFormat.parse(date);
                } catch (ParseException pex) {
                    //pex.printStackTrace();
                    try {
                        SimpleDateFormat parsFormat = null;
                        parsFormat = new SimpleDateFormat("dd/MM/yyyy");
                        parsedDate = parsFormat.parse(date);
                    } catch (ParseException pnex) {
                        //pnex.printStackTrace();
                        try {
                            SimpleDateFormat parsFormat = null;
                            parsFormat = new SimpleDateFormat("MM/dd/yyyy");
                            parsedDate = parsFormat.parse(date);
                        } catch (ParseException pnexp) {
                            //pnexp.printStackTrace();
                            try {
                                SimpleDateFormat parsFormat = null;
                                parsFormat = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
                                parsedDate = parsFormat.parse(date);
                            } catch (ParseException pndexp) {
                                //pndexp.printStackTrace();
                                try {
                                    SimpleDateFormat parsFormat = null;
                                    parsFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                                    parsedDate = parsFormat.parse(date);
                                } catch (ParseException pex1) {
                                    // pex1.printStackTrace();
                                    try {
                                        SimpleDateFormat parsFormat = null;
                                        parsFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                                        parsedDate = parsFormat.parse(date);
                                    } catch (ParseException pex2) {
                                        // pex2.printStackTrace();
                                        try {
                                            SimpleDateFormat parsFormat = null;
                                            parsFormat = new SimpleDateFormat("E MMM dd yyyy");
                                            parsedDate = parsFormat.parse(date);
                                        } catch (ParseException pex3) {
                                            System.out.println("--PayIn/Out date Parsing Prob. for " + date);
                                        }
                                    }

                                }

                            }

                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parsedDate;
    }

    //Filter Iterator.............

    public void _filteeIterator(String iter, String exCode, Date date) {
        DCIteratorBinding dcIter = ADFUtils.findIterator(iter);
        ViewObject VO = dcIter.getViewObject();
        String d = new SimpleDateFormat("dd-MMM-yyyy").format(new Date(date.getTime()));
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        whereClauseSql.append("Exchanges.EXCHANGE_CODE='" + exCode + "'");
        /*  whereClauseSql.append(" AND to_char(TradeInstrumentSettlements.TRADING_DATE,'dd-MON-yyyy') = upper('" + date +
                              "')"); */
        VO.setWhereClause(whereClauseSql.toString());
        VO.executeQuery();

    }

    public static Date getUiTradeDate() {

        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        Date d = null;
        Timestamp tms = null;
        if (iter != null) {
            Row r = iter.getCurrentRow();
            if (r != null) {
                if (r.getAttribute("Sysdate") != null) {
                    tms = (Timestamp)r.getAttribute("Sysdate");
                    d = new Date(tms.getTime());

                }
            }
        } 
        return d;
    }


    public static Date getUiTradeDate(String iterName, String attr) {

        DCIteratorBinding iter = ADFUtils.findIterator(iterName);
        Date d = null;
        Timestamp tms = null;
        if (iter != null) {
            Row r = iter.getCurrentRow();
            if (r != null) {
                if (r.getAttribute(attr) != null) {
                    tms = (Timestamp)r.getAttribute(attr);
                    d = new Date(tms.getTime());

                }
            }
        }
        return d;
    }
    
    public static String getCurrentRowValue(String iterName, String attr) {

        DCIteratorBinding iter = ADFUtils.findIterator(iterName);
        String attrValue = null;
        if (iter != null) {
            Row r = iter.getCurrentRow();
            if (r != null) {
                if (r.getAttribute(attr) != null) {
                    attrValue =  r.getAttribute(attr).toString();
                }
            }
        }
        return attrValue;
    }

    public static String getSystemDate(String format, Date date) {
        String sysDate = null;
        try {
            //Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());
            sysDate = new SimpleDateFormat(format).format(new Date(date.getTime()));
        } catch (Exception e) {
            System.out.print("Date  Formetting Problem ---Public Offer Declare  !!");
        }
        return sysDate;
    }

    private Date getSystemTradeDate() {
        Date d = null;
        try {
            d = PayInOutFileUtil.getParsedDate(ApplicationInfo.getSystemDate(), "yyyy-MM-dd");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return d;
    }

}
