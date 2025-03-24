package leads.capita.trade.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.net.URL;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpServletRequest;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;


import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.jbo.Row;

import org.apache.commons.io.IOUtils;
import org.apache.myfaces.trinidad.model.UploadedFile;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;


/*
 * Utility class for Flex Trade Integration
 * Created By Main Uddin Patowary
 */
public class FlexTradeFileUtil {
    private static final Logger logger = Logger.getLogger(FlexTradeFileUtil.class.getName());
    private static String USER_HOME = System.getProperty("user.home");
    private static String FILE_NAME_DATE_FORMAT = "yyyyMMdd-hhmmss";
    private static String FLEX_FILE_FOLDER_NAME = "flex_trade";
    private static String FLEX_XSD_FOLDER_NAME = "flex_xsd";
    private static String DATE_PART_FORMAT = "yyyyMMdd";
    private FacesContext fct;
    private static ResourceBundle messagebundle;

    public FlexTradeFileUtil() {
        super();
        fct = JSFUtils.getFacesContext();
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");
    }

    public static String getMD5HashContentForFile(String fileNamePath) throws FileNotFoundException {
        String checksum = null;
        FileInputStream fis = null;
        try {
            File file = new File(fileNamePath);
            if (!file.exists()) {
                return null;
            }

            fis = new FileInputStream(fileNamePath);
            MessageDigest md = MessageDigest.getInstance("MD5");
            //Using MessageDigest update() method to provide input
            byte[] buffer = new byte[2048];
            int numOfBytesRead;
            while ((numOfBytesRead = fis.read(buffer)) > 0) {
                md.update(buffer, 0, numOfBytesRead);
            }
            byte[] hash = md.digest();
            checksum = new BigInteger(1, hash).toString(16); //don't use this, truncates leading zero
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    logger.log(Level.SEVERE, null, ex);
                }
        }
        return checksum;
    }

    /*
 * File Type:
 * Clients
 * Trades
 * Positions
 * eod-tickers
 * etc
 */

    public static String getGeneratedFileNameCommonPart(String fileType, String dateTimeFormat) {
        String dateString = null;
        String fileNameCommonPart = null;
        SimpleDateFormat format = null;
        if (dateTimeFormat == null)
            format = new SimpleDateFormat(FILE_NAME_DATE_FORMAT);
        else
            format = new SimpleDateFormat(dateTimeFormat);
        try {
            String appDate = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();
            String sysTime = getSystemTimeHHMMSS();

            dateString = appDate + "-" + sysTime;
            fileNameCommonPart = dateString + "-" + fileType;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        }
        return fileNameCommonPart;
    }

    public static String getSystemTimeHHMMSS() {
        String timeString = null;
        SimpleDateFormat format = new SimpleDateFormat("hhmmss");
        timeString = format.format(new Date());
        return timeString;

    }


    public static String appendStringWithFileName(String fileName, String appnedString) {
        String newFileName = null;

        try {
            if (fileName.contains(".") && fileName != null && !fileName.equals(""))
                newFileName = fileName.substring(0, (fileName.lastIndexOf("."))) + appnedString;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        }
        return newFileName;
    }

    public static String getHomeDirPath() {
        return USER_HOME;
    }

    public static String getFileExt(String fileName) {
        String fileType = null;
        if (fileName != null)
            fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        return fileType;
    }

    public static boolean isValidFileFormat(String fileName, String reqExtension) {
        String fileExt = null;
        if (fileName != null)
            fileExt = getFileExt(fileName);
        if (fileExt.equalsIgnoreCase(reqExtension))
            return true;
        else
            return false;
    }

    public static String getFTIXsdFolderName() {
        return messagebundle.getString("leads_capita_flex_trade_cur_xsd_folder");
    }

    public static String getFTIXsdVersionFolderName() {
        return messagebundle.getString("leads_capita_flex_trade_cur_xsd_version_folder");
    }

    public static String getUniqueValue() {
        SimpleDateFormat format = null;
        String valueString = null;
        try {
            format = new SimpleDateFormat("yyMMddhhmmss");
            valueString = format.format(new Date());
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        }
        return valueString;
    }

    //xml root element

    public static String generateRootWithSchema(String rootElementName, String encoding,
                                                Map<String, String> nsNameValue) {


        /*  if (nsNameValue == null) {
            nsNameValue = new HashMap<String, String>();
            nsNameValue.put("ProcessingMode", "InsertOrUpdate");
            nsNameValue.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            nsNameValue.put("xsi:noNamespaceSchemaLocation", "Flextrade-BOS-Clients.xsd");
            nsNameValue.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        } */


        StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" ");
        try {
            xml.append("encoding=");
            xml.append("\"");
            xml.append(encoding == null ? "UTF-8" : encoding);
            xml.append("\" ");
            xml.append("?><");
            xml.append(rootElementName + " ");
            if (nsNameValue != null && nsNameValue.size() > 0) {
                Iterator<String> keySetIterator = nsNameValue.keySet().iterator();
                while (keySetIterator.hasNext()) {
                    String key = keySetIterator.next();
                    xml.append(key + "=");
                    xml.append("\"");
                    xml.append(nsNameValue.get(key));
                    xml.append("\" ");
                }
            }
            xml.append("/>");

            //System.out.println("--- " + xml.toString());
        } catch (Exception e) {
            System.out.println("Problem in Generate Root With Schema " + e.getMessage());
        }
        return xml != null ? xml.toString() : null;
    }


    public static boolean validateXSDUsingSAX(String xmlFilePath,
                                              String xsdFilePath) throws ParserConfigurationException, IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            SAXParser parser = null;
            try {
                factory.setSchema(schemaFactory.newSchema(new Source[] { new StreamSource(xsdFilePath) }));
                parser = factory.newSAXParser();
            } catch (SAXException se) {
                System.out.println("SCHEMA : " + se.getMessage()); // problem in the XSD itself
                return false;
            }

            XMLReader reader = parser.getXMLReader();
            reader.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException {
                    System.out.println("WARNING: " + e.getMessage()); // do nothing
                }

                public void error(SAXParseException e) throws SAXException {
                    System.out.println("ERROR : " + e.getMessage());
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    System.out.println("FATAL : " + e.getMessage());
                    throw e;
                }
            });

            reader.parse(new InputSource(xmlFilePath));
            return true;
        } catch (ParserConfigurationException pce) {
            throw pce;
        } catch (IOException io) {
            throw io;
        } catch (SAXException se) {
            return false;
        }
    }


    public static boolean validateWithExtXSDUsingSAX(String xmlFilePath,
                                                     String xsdFilePath) throws ParserConfigurationException,
                                                                                IOException, SAXException {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                Schema sch = schemaFactory.newSchema(new StreamSource(xsdFilePath));
                Validator validator = sch.newValidator();
                validator.validate(new StreamSource(xmlFilePath));
                return true;
            } catch (SAXException se) {
                se.printStackTrace();
                JSFUtils.addFacesErrorMessage(se.getMessage());
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                JSFUtils.addFacesErrorMessage(e.getMessage());
                return false;
            }

        } catch (Exception se) {
            se.printStackTrace();

            return false;
        }
    }

    public static String getFlexTradeFileFolder(String ftradeVerFolderName) throws ParseException {
        String folderName = USER_HOME + File.separator + FLEX_FILE_FOLDER_NAME + File.separator + ftradeVerFolderName;
        SimpleDateFormat format;
        format = new SimpleDateFormat("dd-MMM-yyyy");
        String folderPath = null;
        Date curDate = null;
        Date lastModified = null;
        try {
            curDate = format.parse(format.format(new Date(new Date().getTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        File folder = new File(folderName);
        try {
            if (!folder.exists()) {
                folder.mkdirs();
                folderPath = folder.getPath();
            } else {
                folderPath = folder.getPath();
                /* if (folder.isDirectory()) {
                    File[] files = folder.listFiles();
                    for (File f : files) {
                        if (f.isFile()) {
                            lastModified = format.parse(format.format(new Date(f.lastModified())));
                            if (lastModified.compareTo(curDate) < 0) {
                                f.delete();
                            }
                        }

                    }
                } */

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folderPath;
        //System.out.println("---- " + folderPath);
    }


    public static String getFlexTradeXSDFolder(String ftradeVerFolderName) throws ParseException {

        String folderName =
            USER_HOME + File.separator + FLEX_FILE_FOLDER_NAME + File.separator + FLEX_XSD_FOLDER_NAME +
            File.separator + ftradeVerFolderName;
        SimpleDateFormat format;
        format = new SimpleDateFormat("dd-MMM-yyyy");
        String folderPath = null;
        Date curDate = null;
        Date lastModified = null;
        try {
            curDate = format.parse(format.format(new Date(new Date().getTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        File folder = new File(folderName);
        try {
            if (!folder.exists()) {
                folder.mkdirs();
                folderPath = folder.getPath();
            } else {
                folderPath = folder.getPath();
                /*  if (folder.isDirectory()) {
                    File[] files = folder.listFiles();
                    for (File f : files) {
                        if (f.isFile()) {
                            lastModified = format.parse(format.format(new Date(f.lastModified())));
                            if (lastModified.compareTo(curDate) < 0) {
                                f.delete();
                            }
                        }

                    }
                } */
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return folderPath;
    }

    public static void downloadUrlFileUsingStream(String urlFilePath, String destFilePath) throws IOException {
        URL url = new URL(urlFilePath);
        try {
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            FileOutputStream fis = new FileOutputStream(destFilePath);
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fis.write(buffer, 0, count);
            }
            fis.close();
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadUrlFileUsingNIO(String urlFilePath, String destFilePath) throws IOException {
        URL url = new URL(urlFilePath);
        try {
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(destFilePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isValidXmlWithControllFile(String xmlFilePath,
                                                     String controllFilePath) throws FileNotFoundException,
                                                                                     JDOMException, IOException {
        boolean isValid = false;
        try {
            if (getFileExt(xmlFilePath).equalsIgnoreCase("xml") &&
                getFileExt(controllFilePath).equalsIgnoreCase("xml")) {
                String xmlHash = getMD5HashContentForFile(xmlFilePath);
                String controllHash = getControllAttrValue(controllFilePath, "Hash");
                if (xmlHash.equalsIgnoreCase(controllHash))
                    isValid = true;
                else
                    isValid = false;

            } else
                isValid = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isValid;
    }


    public static String getControllAttrValue(String controllFilePath, String attrName) throws JDOMException,
                                                                                               IOException {
        String attrValue = null;
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new java.io.File(controllFilePath));
        try {
            Element controll = document.getRootElement();
            if (controll.getName().equals("Control")) {
                if (controll.getAttribute(attrName) != null)
                    attrValue = controll.getAttribute(attrName).getValue();
            } else {
                System.out.println(" May be an invalid controll file ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attrValue;
    }

    public static boolean fileUploadAndSave(String destinationFolder, String newFile,
                                            UploadedFile file) throws IOException, Exception {
        boolean issuccess = false;
        try {
            InputStream inputStream = file.getInputStream();
            File outputFile = null;
            if (newFile != null || !newFile.equals(""))
                outputFile = new File(destinationFolder + File.separator + newFile);
            else
                outputFile = new File(destinationFolder + File.separator + file.getFilename());
            IOUtils.copy(inputStream, new FileOutputStream(outputFile));
            issuccess = true;
        } catch (IOException e) {
            throw new IOException(" File can not be saved !" + e.getMessage());
        } catch (Exception e) {
            throw new Exception(" File can not be saved !" + e.getMessage());
        }
        return issuccess;
    }


    public static boolean fileUploadInDisk(String destinationFolder, String newFile,
                                           UploadedFile file) throws IOException, Exception {
        boolean issuccess = false;
        try {
            InputStream inputStream = file.getInputStream();
            File outputFile = null;
            if (newFile != null || !newFile.equals(""))
                outputFile = new File(destinationFolder + File.separator + newFile);
            else
                outputFile = new File(destinationFolder + File.separator + file.getFilename());
            FileOutputStream os = new FileOutputStream(outputFile);
            byte[] bytes = new byte[1024];
            int read = 0;
            while ((read = inputStream.read(bytes, 0, bytes.length)) != -1) {
                os.write(bytes, 0, read);
            }
            os.flush();
            inputStream.close();
            os.close();
            issuccess = true;
        } catch (IOException e) {
            throw new IOException(" File can not be saved !!" + e.getMessage());
        } catch (Exception e) {
            throw new Exception(" File can not be saved !!" + e.getMessage());
        }
        return issuccess;
    }


    public static String getTradeFileDetailAttrValue(String tradeFilePath, String attrName) throws JDOMException,
                                                                                                   IOException {
        String attrValue = null;
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new java.io.File(tradeFilePath));
        try {
            Element trades = document.getRootElement();
            if (trades.getName().equals("Trades")) {
                Element detailEl = null;
                if (trades.getChildren().size() > 0) {
                    detailEl = trades.getChildren().get(0);
                    if (detailEl.getAttribute(attrName) != null)
                        attrValue = detailEl.getAttribute(attrName).getValue();
                }
            } else {
                System.out.println(" May be an invalid Trades file ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attrValue;
    }

    //For flex trade file
    //20141117-071041-Clients-DSL.xml

    public static String getShortNameFromFile(String fileName) {
        String shortName = null;
        if (fileName != null || fileName.equals("")) {
            String fName = fileName.substring(0, fileName.lastIndexOf("."));
            String arr[] = fName.split("-");
            if (arr.length >= 4) {
                shortName = arr[3];
                if (shortName.length() != 3) {
                    JSFUtils.addFacesErrorMessage("May be invalid file name!!");
                    JSFUtils.addFacesErrorMessage("Or Invalid short name !!");
                    return null;
                }
            } else {
                JSFUtils.addFacesErrorMessage("May be invalid file name!!");
                JSFUtils.addFacesErrorMessage("Or Invalid short name !!");
                return null;
            }
        }

        return shortName;
    }

    public static String getFileNameDatePart(String fileName) {
        if (fileName != null || fileName.equals("")) {
            return fileName.substring(0, 8);
        } else
            return null;
    }
    //For flex trade file

    public static String getFileNameTimePart(String fileName) {
        if (fileName != null || fileName.equals("")) {
            return fileName.substring(8, 15);
        } else
            return null;
    }

    public static String getStringWithoutCDATA(String cdataValue) {
        String rValue = null;
        if (cdataValue != null && !cdataValue.equals("")) {
            rValue = cdataValue.trim();
            if (cdataValue.startsWith("<![CDATA[")) {
                rValue = cdataValue.substring(9);
                int i = rValue.indexOf("]]>");
                if (i == -1) {
                    throw new IllegalStateException("argument starts with <![CDATA[ but cannot find pairing ]]>");
                }
                rValue = rValue.substring(0, i);
            }
        }

        return rValue;
    }

    public static String getCurSystemTime(String timeFormat) {
        String timeString = null;
        SimpleDateFormat format = null;
        if (timeFormat != null && !timeFormat.equals(""))
            format = new SimpleDateFormat(timeFormat);
        else
            format = new SimpleDateFormat("hh:mm:ss");
        timeString = format.format(new Date());
        return timeString;
    }

    public static String getApplicationDateStringyyyyMMdd() {
        SimpleDateFormat format = new SimpleDateFormat(DATE_PART_FORMAT);
        String dateString = null;
        try {
            dateString = format.format(new Date(ApplicationInfo.getSystemDateInOracleTimeStamp().getTime()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateString;
    }

    public static String getURLWithContextPath(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() +
            request.getContextPath();
    }

    public static String documentToString(Document doc) {
        if (doc == null)
            return null;
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        return out.outputString(doc);
    }

    /****************************************************************************/

    public static String elementToString(Element e) {
        if (e == null)
            return null;
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        return out.outputString(e);
    }

    /****************************************************************************/

    public static String elementToStringDump(Element e) {
        if (e == null)
            return null;
        XMLOutputter out = new XMLOutputter(Format.getCompactFormat());
        return out.outputString(e);
    }

    /****************************************************************************/

    public static Document stringToDocument(String s) {
        try {
            if (s == null)
                return null;
            return new SAXBuilder().build(new StringReader(s));
        } catch (JDOMException jde) {
            logger.log(Level.SEVERE, null, jde);
            return null;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, null, ioe);
            return null;
        }
    }

    /****************************************************************************/

    public static Element stringToElement(String s) {
        if (s == null)
            return null;
        Document doc = stringToDocument(s);
        return doc.getRootElement();
    }

    /****************************************************************************/

    public static Document fileToDocument(String path) {
        try {
            if (path == null)
                return null;
            return new SAXBuilder().build(new File(path));
        } catch (JDOMException jde) {
            logger.log(Level.SEVERE, null, jde);
            return null;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, null, ioe);
            return null;
        }
    }

    /****************************************************************************/

    /** saves a JDOM Document to a file */
    public static void documentToFile(Document doc, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            XMLOutputter xop = new XMLOutputter(Format.getPrettyFormat());
            xop.output(doc, fos);
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, null, ioe);
        }
    }

    /****************************************************************************/

    public static String getDefaultValueForType(String dataType) {
        if (dataType == null)
            return "null";
        else if (dataType.equalsIgnoreCase("boolean"))
            return "false";
        else if (dataType.equalsIgnoreCase("string"))
            return "";
        else
            return "0";
    }

    /****************************************************************************/

    public static String encodeEscapes(String s) {
        if (s == null)
            return s;
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"",
                                                                                                     "&quot;").replaceAll("'",
                                                                                                                          "&apos;");
    }

    public static String decodeEscapes(String s) {
        if (s == null)
            return s;
        return s.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&quot;",
                                                                                                     "\"").replaceAll("&apos;",
                                                                                                                      "'");
    }

    /****************************************************************************/

    public static String formatXMLString(String s) {
        if (s == null)
            return null;
        if (s.startsWith("<?xml"))
            return documentToString(stringToDocument(s));
        else
            return elementToString(stringToElement(s));
    }

    public static String formatXMLStringAsDocument(String s) {
        return documentToString(stringToDocument(s));
    }

    public static String formatXMLStringAsElement(String s) {
        return elementToString(stringToElement(s));
    }

    public static String _getAttrValueFromIter(String iteratorName, String attrName) {
        Row curRow = null;
        String attrVale = null;
        try {
            DCIteratorBinding iterBinding = ADFUtils.findIterator(iteratorName);
            if (iterBinding != null && iterBinding.getEstimatedRowCount() > 0) {
                curRow = iterBinding.getCurrentRow();
                if (curRow == null)
                    curRow = iterBinding.getViewObject().first();
                if (curRow.getAttribute(attrName) != null)
                    attrVale = curRow.getAttribute(attrName).toString();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attrVale;
    }

    public static String _getFirstRowAttrValueFromIter(String iteratorName, String attrName) {
        Row curRow = null;
        String attrVale = null;
        try {
            DCIteratorBinding iterBinding = ADFUtils.findIterator(iteratorName);
            if (iterBinding != null && iterBinding.getEstimatedRowCount() > 0) {
                curRow = iterBinding.getViewObject().first();
                if (curRow.getAttribute(attrName) != null)
                    attrVale = curRow.getAttribute(attrName).toString();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attrVale;
    }


    public static Map<String, String> getVOAttrValueInMap(String iteratorName, String attrName) {
        Map<String, String> rValue = new HashMap<String, String>();
        if (iteratorName != null || !iteratorName.equals("")) {
            try {
                DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
                Row rows[] = iter.getAllRowsInRange();
                if (rows.length > 0) {
                    for (Row r : rows) {
                        if (r.getAttribute(attrName) != null) {
                            String attrVal = r.getAttribute(attrName).toString();
                            rValue.put(attrVal, attrVal);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rValue;
    }

    public static String getFixedTengthTraderId(String traderId) {
        String _traderId = null;
        if (traderId == null) {
            JSFUtils.addFacesErrorMessage("Trader Id is required !!");
            return null;
        }
        if (traderId.length() == 10)
            _traderId = traderId;
        else {
            String commonPart = null;
            if (traderId.length() > 7) {
                commonPart = traderId.substring(0, 7);
                String sValue = traderId.substring(7);
                if (sValue.length() == 1)
                    _traderId = commonPart + "00" + sValue;
                else if (sValue.length() == 2)
                    _traderId = commonPart + "0" + sValue;
                else
                    _traderId = commonPart + sValue;
            }

        }

        return _traderId;
    }

    public static Date getAppDateWithTime(String appDate) throws ParseException {
        Date dateTime = null;
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String time = new SimpleDateFormat("hh:mm:ss").format(new Date());
        String st = appDate + " " + time;
        dateTime = new Date(sf.parse(st).getTime());
        return dateTime;
    }

    public static Timestamp getSystemDateTimeStampWithTime(String sysDate) {
        Timestamp dts = null;
        try {
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String time = new SimpleDateFormat("hh:mm:ss").format(new Date());
            String st = sysDate + " " + time;
            dts = new Timestamp(sf.parse(st).getTime());
        } catch (ParseException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return dts;
    }

    public static void main(String[] args) throws JDOMException, IOException, ParseException,
                                                  ParserConfigurationException, SAXException,
                                                  XPathExpressionException {
        //SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        //System.out.println(getFileNameDatePart("20141105-102813-XYX") + "---" +
        // getFileNameTimePart("20141105-102813-XYX"));

        //System.out.println("*** " + getStringWithoutCDATA("<![CDATA[QSMDRYCELL]]>"));
        String traderId = "BSLTDRR10";
        //String md5v=getFixedTengthTraderId("BSLTDRR122");
        //System.out.println("--- "+md5v);

        /*  SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date = "2014-11-10";
        String time = new SimpleDateFormat("hh:mm:ss").format(new Date());
        String st = date + " " + time;

        System.out.println("---- " + new Date(sf.parse(st).getTime()));

        FileInputStream fstream = new FileInputStream("C:\\ShareLimit.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine = null;

        //Read File Line By Line: \\' \\
        while ((strLine = br.readLine()) != null) {
            // Print the content on the console
            String words[] = strLine.split("~");
            for (String wrd : words) {
                System.out.print("--- " + wrd);
            }
            System.out.println();
        }

        //Close the input stream
        br.close(); */

        // System.out.print("----- " + getShortNameFromFile("20141117-071041-Clients-DSL.xml"));
        //query();

        String hsh = getMD5HashContentForFile("C:\\20150225-144802-trades-ONE.xml");
        System.out.println(hsh);
        //String chsh = getControllAttrValue("E:\\remotetest\\controll\\20131030-060002-Clients-BSL-ctrl.xml", "Hash");
        //if (hsh.equals(chsh))
        // System.out.println("true");
        // else
        //System.out.println("Not match !");
        /*  String x =
            "71043111565771307052242366043505620131131732626327614213772755376004544500766625401706273113314473324617623547643406760142276700337403344025530627160131736515421677445403";
        BigInteger bi = new BigInteger(x.trim(), 8);
        System.out.println(new BigInteger(x.trim()).toString(8) + "-------------" + bi); */
    }


    //calling: FlexTradeFileUtil.FlexTradeFileType.CLIENTS.getValue()

    public static BigDecimal getClientLimitValue(String filePath, String clientCode,
                                                 String tagName) throws FileNotFoundException,
                                                                        ParserConfigurationException,
                                                                        XPathExpressionException, SAXException,
                                                                        IOException {
        FileInputStream file = new FileInputStream(new File(filePath));
        BigDecimal rVal = BigDecimal.ZERO;
        javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder;
        org.w3c.dom.Document doc = null;
        javax.xml.xpath.XPathExpression expr = null;
        builder = factory.newDocumentBuilder();
        doc = builder.parse(file);
        // Create a XPathFactory
        javax.xml.xpath.XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        expr = xpath.compile("/Clients/Limits[ClientCode/text()='" + clientCode + "'" + "]/" + tagName);
        Double number = (Double)expr.evaluate(doc, XPathConstants.NUMBER);
        //System.out.println("--**-- "+number);
        if (number == null || number == Double.NaN || Double.isNaN(number) || Double.isInfinite(number))
            rVal = BigDecimal.ZERO;
        else
            rVal = new BigDecimal(number);
        return rVal;
    }

    public static BigDecimal getClientLimitValue(String clientCode, String tagName,
                                                 org.w3c.dom.Document doc) throws FileNotFoundException,
                                                                                  ParserConfigurationException,
                                                                                  XPathExpressionException,
                                                                                  SAXException, IOException {
        //FileInputStream file = new FileInputStream(new File(filePath));
        BigDecimal rVal = BigDecimal.ZERO;
        /*  javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder;
        org.w3c.dom.Document doc = null;
        javax.xml.xpath.XPathExpression expr = null;
        builder = factory.newDocumentBuilder();
        doc = builder.parse(file); */
        // Create a XPathFactory
        javax.xml.xpath.XPathExpression expr = null;
        javax.xml.xpath.XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        expr = xpath.compile("/Clients/Limits[ClientCode/text()='" + clientCode + "'" + "]/" + tagName);
        Double number = (Double)expr.evaluate(doc, XPathConstants.NUMBER);
        //System.out.println("--**-- "+number);
        if (number == null || number == Double.NaN || Double.isNaN(number) || Double.isInfinite(number))
            rVal = BigDecimal.ZERO;
        else
            rVal = new BigDecimal(number);
        return rVal;
    }

    public static BigDecimal getClientLimitAttrValue(String clientCode, String tagName, String attrName,
                                                     org.w3c.dom.Document doc) throws FileNotFoundException,
                                                                                      ParserConfigurationException,
                                                                                      XPathExpressionException,
                                                                                      SAXException, IOException {

        //FileInputStream file = new FileInputStream(new File(filePath));
        BigDecimal rVal = BigDecimal.ZERO;
        /* javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder;
        org.w3c.dom.Document doc = null;
        javax.xml.xpath.XPathExpression expr = null;
        builder = factory.newDocumentBuilder();
        doc = builder.parse(file); */
        // Create a XPathFactory
        javax.xml.xpath.XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        XPathExpression expression =
            xpath.compile("/Clients/Limits[ClientCode/text()='" + clientCode + "'" + "]/" + tagName + "+/@" +
                          attrName);

        Double number = (Double)expression.evaluate(doc, XPathConstants.NUMBER);
        //  System.out.println("---- "+number);
        if (number == null || number == Double.NaN || Double.isNaN(number) || Double.isInfinite(number))
            rVal = BigDecimal.ZERO;
        else
            rVal = new BigDecimal(number);
        return rVal;
    }


    public static BigDecimal getClientLimitAttrValue(String filePath, String clientCode, String tagName,
                                                     String attrName) throws FileNotFoundException,
                                                                             ParserConfigurationException,
                                                                             XPathExpressionException, SAXException,
                                                                             IOException {

        BigDecimal rVal = BigDecimal.ZERO;
        FileInputStream file = new FileInputStream(new File(filePath));
        javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder;
        org.w3c.dom.Document doc = null;
        javax.xml.xpath.XPathExpression expr = null;
        builder = factory.newDocumentBuilder();
        doc = builder.parse(file);
        // Create a XPathFactory
        javax.xml.xpath.XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        XPathExpression expression =
            xpath.compile("/Clients/Limits[ClientCode/text()='" + clientCode + "'" + "]/" + tagName + "+/@" +
                          attrName);

        Double number = (Double)expression.evaluate(doc, XPathConstants.NUMBER);
        //  System.out.println("---- "+number);
        if (number == null || number == Double.NaN || Double.isNaN(number) || Double.isInfinite(number))
            rVal = BigDecimal.ZERO;
        else
            rVal = new BigDecimal(number);
        return rVal;
    }


    public static void query() throws ParserConfigurationException, SAXException, IOException,
                                      XPathExpressionException {
        // Standard of reading a XML file

        /* new FileInputStream(new File("E:\\DOCS\\DOCS\\FlexTrade\\Flex_trade\\141016-123315-Clients-HAI.XML"));
        BigDecimal number =
            getClientLimitValue("E:\\DOCS\\DOCS\\FlexTrade\\Flex_trade\\141016-123315-Clients-HAI.XML", "39013",
                                "NetTransaction");
        System.out.println("CAsh of objects " + number); */
        //E:\remotetest\controll

        // Cast the result to a DOM NodeList
        //NodeList nodes = (NodeList) result;
        //for (int i = 0; i < nodes.getLength(); i++) {
        //System.out.println("Cast the result to a DOM NodeList :  "+nodes.item(i).getNodeValue());
        //}
    }

    public static String getFileHexString(String fileNamePath) {
        String checksum = null;

        File file = new File(fileNamePath);
        if (!file.exists()) {
            return null;
        }

        //String hash = MD5.asHex(MD5.getHash(new File(filename)));


        return null;
    }

    public enum FlexTradeFileType {
        CLIENTS("Clients"),
        TRADES("Trades"),
        POSITIONS("Positions"),
        EODTICKERS("eod-tickers");

        private final String value;

        FlexTradeFileType(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }


    // Account Type ENUM For Flex Trade

    public enum FlexAccountType {

        N("N"),
        D("D"),
        I("I"),
        MF("I"),
        RB("N"),
        F("F"),
        NRB("R"),
        OM("I"),
        O("O"),
        ASI("N"),
        MB("MB"),
        AMC("AMC"),
        BANK("BANK"),
        NBFI("NBFI"),
        INS("INS"),
        AIFM("AIFM"),
        AIF("AIF"),
        RPPF("RPPF");

        private final String value;

        FlexAccountType(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
