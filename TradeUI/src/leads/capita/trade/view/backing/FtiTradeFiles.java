package leads.capita.trade.view.backing;

/*Created by : Ipsheta Saha */

import com.sun.mail.smtp.SMTPTransport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;

import java.sql.Timestamp;

import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;

import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import javax.faces.event.ValueChangeEvent;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import javax.servlet.ServletContext;

import javax.servlet.http.HttpServletRequest;

import leads.capita.common.ui.util.ADFUtils;

import leads.capita.common.ui.util.JSFUtils;
import leads.capita.email.Messaging.EmailConf;

import leads.capita.email.Messaging.EmailContent;
import leads.capita.email.Messaging.EmailUtil;
import leads.capita.trade.file.FlexTradeFileUtil;

import leads.capita.trade.file.PayInOutFileUtil;

import leads.capita.upload.java.FileUpload;
import leads.capita.upload.java.UploadParam;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.input.RichSelectOneRadio;

import oracle.jbo.Row;

import oracle.jbo.ViewObject;

import org.apache.myfaces.trinidad.model.UploadedFile;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class FtiTradeFiles {
    private String newFileName = null;
    static String xmlFileName = null;
    static String controlFileName = null;
    static String textFileName = null;
    private RichSelectOneRadio fileTypeRadio;
    private RichInputText generatedXMLFileUI; //to get value for download action listener
    private RichInputText generatedControlFileUI; //to get value for download action listener
    private RichInputText generatedTextFileUI; //to get value for download action listener
    private static final String xsd_url_path = "flex-tradexsd/v7-10/";
    private static final String client_xsd_url_path = "Flextrade-BOS-Trades.xsd";
    String smtpUser = null;
    String smtpPassword = null;
    private static String USER_HOME = System.getProperty("user.home");
    private UploadedFile priceFile;
    private UploadedFile prcCtrlFile;
    private RichPopup sendMailPopupUI;
    private RichInputText mbankEmailIdUI;

    public FtiTradeFiles() {
    }

    public String generateFile() throws ParseException, IOException {
        if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("xml")) {
            this.generateXMLFile();
        } else if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("text")) {
            this.generateTextFile();
        }
        return null;
    }

    private String getMbankShortName() {
        String shortName = null;
        Object mBankId = null;
        Object groupWord = null;
        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesVOIterator");
        if (tradesIter != null && tradesIter.getEstimatedRowCount() > 0) {
            Row r = tradesIter.getCurrentRow();
            if (r == null)
                r = tradesIter.getViewObject().first();
            mBankId = r.getAttribute("MbankId");
            if (mBankId != null){
                shortName = r.getAttribute("MbankShortName").toString();
            }else if (r.getAttribute("GroupWord") != null)
                groupWord = r.getAttribute("GroupWord").toString();
            if (shortName == null) {
                if (groupWord != null)
                    shortName = groupWord.toString();
                else
                    return null;
            } else if (shortName.length() >= 4) {
                JSFUtils.addFacesErrorMessage("Short name should be less than or equal 3 characters !");
                return null;
            }
        }

        return shortName;
    }

    private String getMbankEmail() {
        String mailId = null;
        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesVOIterator");
        if (tradesIter != null && tradesIter.getEstimatedRowCount() > 0) {
            Row r = tradesIter.getCurrentRow();
            if (r == null)
                r = tradesIter.getViewObject().first();

            if (this.mbankEmailIdUI.getValue() != null)
                mailId = this.mbankEmailIdUI.getValue().toString();

            if (mailId == null || mailId.equals("")) {
                if (r.getAttribute("Email") != null)
                    mailId = r.getAttribute("Email").toString();
            }
        }
        return mailId;
    }

    public String getFtiFillType(String fillType) {
        String fillVal = null;
        if (fillType.equalsIgnoreCase("P")) {
            fillVal = "PF";
        } else if (fillType.equalsIgnoreCase("F")) {
            fillVal = "FILL";
        } else {
            fillVal = fillType;
        }
        return fillVal;
    }

    public String getFtiBoard(String boardType) { //not used
        String boardVal = null;
        if (boardType.equalsIgnoreCase("S")) {
            boardVal = "SPOT";
        } else if (boardType.equalsIgnoreCase("P")) {
            boardVal = "PUBLIC";
        } else if (boardType.equalsIgnoreCase("I")) {
            boardVal = "BUYIN";
        } else if (boardType.equalsIgnoreCase("B")) {
            boardVal = "BLOCK";
        } else {
            boardVal = boardType;
        }
        return boardVal;
    }

    public String generateXMLFile() throws ParseException {
        String shortName = null;
        String trdDate = null;
        boolean ctrlOK = false;
        DCIteratorBinding boardTypeIter = ADFUtils.findIterator("FtiMarketTypeIterator");
        ViewObject vo = boardTypeIter.getViewObject();
        Row[] rowset = vo.getAllRowsInRange();
        //Load all DB Market Type value
        Map<String, String> boardMap = new HashMap<String, String>();
        if (rowset.length > 0) {
            for (int i = 0; i < rowset.length; i++) {
                if (rowset[i].getAttribute("MsaType") != null && rowset[i].getAttribute("FtiType") != null) {
                    boardMap.put(rowset[i].getAttribute("MsaType").toString(),
                                 rowset[i].getAttribute("FtiType").toString());
                }
            }
        } else {
            JSFUtils.addFacesErrorMessage("No Configuration Found For Market Type(Board)!");
        }

        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades XML");
        shortName = getMbankShortName();
        if (shortName == null) {
            shortName = "";
        }
        String postFix = shortName + "(DSE)";
        newFileName =
                destFolderPath + File.separator + FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.TRADES.getValue(),
                                                                                                   null);

        newFileName = newFileName + "-" + postFix + ".xml";
        File generatedfile = new File(newFileName);

        try {
            Element element = new Element("Trades");
            Document document = new Document(element);
            tradesIter.setRangeSize((int)tradesIter.getEstimatedRowCount());
            Row[] rows = tradesIter.getViewObject().getAllRowsInRange();

            for (int i = 0; i < rows.length; i++) {
                Row r = rows[i];
                Element detail = new Element("Detail");
                detail.setAttribute(new Attribute("Action",
                                                  r.getAttribute("Action") == null ? "-" : r.getAttribute("Action").toString()));
                detail.setAttribute(new Attribute("Status",
                                                  r.getAttribute("Status") == null ? "-" : r.getAttribute("Status").toString()));
                detail.setAttribute(new Attribute("ISIN",
                                                  r.getAttribute("Isin") == null ? "-" : r.getAttribute("Isin").toString()));
                detail.setAttribute(new Attribute("AssetClass",
                                                  r.getAttribute("Assetclass") == null ? "-" : r.getAttribute("Assetclass").toString()));
                detail.setAttribute(new Attribute("OrderID",
                                                  r.getAttribute("OrderId") == null ? "-" : r.getAttribute("OrderId").toString()));
                detail.setAttribute(new Attribute("RefOrderID",
                                                  r.getAttribute("Born") == null ? "-" : r.getAttribute("Born").toString()));
                detail.setAttribute(new Attribute("Side",
                                                  r.getAttribute("BuySell") == null ? "-" : r.getAttribute("BuySell").toString()));
                detail.setAttribute(new Attribute("BOID",
                                                  r.getAttribute("Boid") == null ? "-" : r.getAttribute("Boid").toString()));
                detail.setAttribute(new Attribute("SecurityCode",
                                                  r.getAttribute("InstrumentCodeMarket") == null ? "-" : r.getAttribute("InstrumentCodeMarket").toString()));
                if (r.getAttribute("CompulsorySpot") != null &&
                    r.getAttribute("CompulsorySpot").toString().equalsIgnoreCase("Y")) {
                    detail.setAttribute(new Attribute("Board", "PUBLIC"));
                } else {
                    String board = r.getAttribute("MarketType").toString();
                    String attrVal = boardMap.get(board);
                    detail.setAttribute(new Attribute("Board", attrVal));
                }

                if (r.getAttribute("TradeDate") != null && r.getAttribute("TradeDate").toString() != null) {
                    Object trDate = r.getAttribute("TradeDate");
                    Timestamp date = (Timestamp)trDate;
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                    trdDate = format.format(new Date(date.getTime()));
                    detail.setAttribute(new Attribute("Date", trdDate));
                } else {
                    detail.setAttribute(new Attribute("Date", "-"));
                }
                detail.setAttribute(new Attribute("Time",
                                                  r.getAttribute("TradeTime") == null ? "-" : r.getAttribute("TradeTime").toString()));
                detail.setAttribute(new Attribute("Quantity",
                                                  r.getAttribute("TradeQuantity") == null ? "-" : r.getAttribute("TradeQuantity").toString()));
                detail.setAttribute(new Attribute("Price",
                                                  r.getAttribute("Price") == null ? "0" : r.getAttribute("Price").toString()));
                detail.setAttribute(new Attribute("Value",
                                                  r.getAttribute("TradeValue") == null ? "0" : r.getAttribute("TradeValue").toString()));
                detail.setAttribute(new Attribute("ExecID",
                                                  r.getAttribute("Execid") == null ? "-" : r.getAttribute("Execid").toString()));
                detail.setAttribute(new Attribute("Session",
                                                  r.getAttribute("SessionId") == null ? "-" : r.getAttribute("SessionId").toString()));
                if (r.getAttribute("FillType") != null) {
                    String fill = r.getAttribute("FillType").toString();
                    String attrVal = getFtiFillType(fill);
                    detail.setAttribute(new Attribute("FillType", attrVal));
                }
                /* detail.setAttribute(new Attribute("BranchID",
                                                  r.getAttribute("BranchId") == null ? "-" : r.getAttribute("BranchId").toString())); */
                detail.setAttribute(new Attribute("Category",
                                                  r.getAttribute("InstrumentGroup") == null ? "-" : r.getAttribute("InstrumentGroup").toString()));
                detail.setAttribute(new Attribute("CompulsorySpot",
                                                  r.getAttribute("CompulsorySpot") == null ? "-" : r.getAttribute("CompulsorySpot").toString()));
                detail.setAttribute(new Attribute("ClientCode",
                                                  r.getAttribute("Customer") == null ? "-" : r.getAttribute("Customer").toString()));
                detail.setAttribute(new Attribute("TraderDealerID",
                                                  r.getAttribute("TraderId") == null ? "-" : r.getAttribute("TraderId").toString()));
                detail.setAttribute(new Attribute("OwnerDealerID",
                                                  r.getAttribute("OwnerDealerId") == null ? "-" : r.getAttribute("OwnerDealerId").toString()));
                detail.setAttribute(new Attribute("TradeReportType",
                                                  r.getAttribute("Tradereporttype") == null ? "-" : r.getAttribute("Tradereporttype").toString()));

                document.getRootElement().addContent(detail);
            }
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(document, new FileWriter(generatedfile.getPath()));
            //set file name of newly generated file
            xmlFileName =
                    FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.TRADES.getValue(),
                                                                     null) + "-" + postFix + ".xml";
            controlFileName = FlexTradeFileUtil.appendStringWithFileName(generatedfile.getName(), "-ctrl.xml");
            ctrlOK = generateControlFile(generatedfile);
            if (ctrlOK) {
                JSFUtils.addFacesInformationMessage("XML File Generated Successfully..");
            } else {
                JSFUtils.addFacesErrorMessage("Problem in File Generation!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Problem While Generating XML File !!");
        }
        return null;
    }

    private boolean generateControlFile(File generateCtrlFile) throws JDOMException, IOException {
        boolean ctrlOk = false;
        Map<String, String> nsNameValue = null;
        try {
            String ctrlFileHashCode = FlexTradeFileUtil.getMD5HashContentForFile(generateCtrlFile.getPath());
            nsNameValue = new HashMap<String, String>();
            nsNameValue.put("Hash", ctrlFileHashCode);
            nsNameValue.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            nsNameValue.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            nsNameValue.put("xsi:noNamespaceSchemaLocation", "Flextrade-BOS-Control.xsd");
            nsNameValue.put("Method", "MD5");

            File generatedControlfileName =
                new File(FlexTradeFileUtil.appendStringWithFileName(generateCtrlFile.getName(), "-ctrl.xml"));

            String rootElemenet = FlexTradeFileUtil.generateRootWithSchema("Control", null, nsNameValue);

            Document doc = new SAXBuilder().build(new StringReader(rootElemenet));
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc,
                             new FileWriter(generateCtrlFile.getParent() + File.separator + generatedControlfileName.getName()));
            ctrlOk = true;
        } catch (Exception e) {
            ctrlOk = false;
            e.printStackTrace();
        }
        return ctrlOk;
    }

    public String generateTextFile() throws ParseException, IOException {
        String born = null;
        String instCode = null;
        String isin = null;
        String traderId = null;
        String buySell = null;
        String trdQty = null;
        String trdPrice = null;
        String trdDate = null;
        String trdTime = null;
        String mkType = null;
        String filType = null;
        String howla = "N";
        String foreign = "N";
        String customer = null;
        String boid = null;
        String trdContractNo = null;
        String compSpot = null;
        String instCat = null;
        String mbankShortName = getMbankShortName();
        if (mbankShortName == null) {
            mbankShortName = "";
        }
        File home_dir = null;
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades TEXT");
        String postFix = mbankShortName + "(DSE)";
        newFileName =
                destFolderPath + File.separator + FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.TRADES.getValue(),
                                                                                                   null);

        newFileName = newFileName + "-" + postFix + ".txt";
        try {
            home_dir = new File(newFileName);
            FileWriter stream = new FileWriter(home_dir);
            Writer output = new BufferedWriter(stream);

            String linegap = String.format("%n");
            String printLine = null;

            /*  DCIteratorBinding boardTypeIter = ADFUtils.findIterator("FtiMarketTypeIterator");
            ViewObject vo = boardTypeIter.getViewObject();
            Row[] rowset = vo.getAllRowsInRange();
            //Load all DB Market Type value
            Map<String, String> boardMap = new HashMap<String, String>();
            if (rowset.length > 0) {
                for (int i = 0; i < rowset.length; i++) {
                    if (rowset[i].getAttribute("FtiType") != null && rowset[i].getAttribute("MsaType") != null) {
                        boardMap.put(rowset[i].getAttribute("FtiType").toString(),
                                     rowset[i].getAttribute("MsaType").toString());
                    }
                }
            } else {
                JSFUtils.addFacesErrorMessage("No Configuration Found For Market Type(Board)!");
            } */

            DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesVOIterator");
            tradesIter.setRangeSize((int)tradesIter.getEstimatedRowCount());
            Row[] rows = tradesIter.getViewObject().getAllRowsInRange();
            int counter = 10000;
            for (int i = 0; i < rows.length; i++) {
                counter = counter + 1;
                Row r = rows[i];
                if (r.getAttribute("Born") != null && !(r.getAttribute("Born").toString().equalsIgnoreCase(""))) {
                    born = r.getAttribute("Born").toString();
                }
                if (r.getAttribute("InstrumentCode") != null &&
                    !(r.getAttribute("InstrumentCode").toString().equalsIgnoreCase(""))) {
                    instCode = r.getAttribute("InstrumentCode").toString();
                }
                if (r.getAttribute("Isin") != null && !(r.getAttribute("Isin").toString().equalsIgnoreCase(""))) {
                    isin = r.getAttribute("Isin").toString();
                }
                if (r.getAttribute("TraderId") != null &&
                    !(r.getAttribute("TraderId").toString().equalsIgnoreCase(""))) {
                    traderId = r.getAttribute("TraderId").toString();
                }
                if (r.getAttribute("BuySell") != null &&
                    !(r.getAttribute("BuySell").toString().equalsIgnoreCase(""))) {
                    buySell = r.getAttribute("BuySell").toString();
                }
                if (r.getAttribute("TradeQuantity") != null &&
                    !(r.getAttribute("TradeQuantity").toString().equalsIgnoreCase(""))) {
                    trdQty = r.getAttribute("TradeQuantity").toString();
                }
                if (r.getAttribute("Price") != null && !(r.getAttribute("Price").toString().equalsIgnoreCase(""))) {
                    trdPrice = r.getAttribute("Price").toString();
                }
                if (r.getAttribute("TradeDate") != null &&
                    !(r.getAttribute("TradeDate").toString().equalsIgnoreCase(""))) {
                    Object trDate = r.getAttribute("TradeDate");
                    Timestamp date = (Timestamp)trDate;
                    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                    trdDate = format.format(new Date(date.getTime()));
                }
                if (r.getAttribute("TradeTime") != null &&
                    !(r.getAttribute("TradeTime").toString().equalsIgnoreCase(""))) {
                    trdTime = r.getAttribute("TradeTime").toString();
                }
                if (r.getAttribute("MarketType") != null &&
                    !(r.getAttribute("MarketType").toString().equalsIgnoreCase(""))) {
                    mkType = r.getAttribute("MarketType").toString();
                    if (mkType.equalsIgnoreCase("PUBLIC"))
                        mkType = "P";
                    else if (mkType.equalsIgnoreCase("SPOT"))
                        mkType = "S";
                    else if (mkType.equalsIgnoreCase("BUYIN"))
                        mkType = "I";
                    else if (mkType.equalsIgnoreCase("BLOCK"))
                        mkType = "B";
                }
                if (r.getAttribute("FillType") != null &&
                    !(r.getAttribute("FillType").toString().equalsIgnoreCase(""))) {
                    filType = "P";
                }
                if (r.getAttribute("HowlaType") != null &&
                    !(r.getAttribute("HowlaType").toString().equalsIgnoreCase(""))) {
                    howla = r.getAttribute("HowlaType").toString();
                }
                if (r.getAttribute("ForeignFlag") != null &&
                    !(r.getAttribute("ForeignFlag").toString().equalsIgnoreCase(""))) {
                    foreign = r.getAttribute("ForeignFlag").toString();
                }
                if (r.getAttribute("Customer") != null &&
                    !(r.getAttribute("Customer").toString().equalsIgnoreCase(""))) {
                    customer = r.getAttribute("Customer").toString();
                }
                if (r.getAttribute("Boid") != null && !(r.getAttribute("Boid").toString().equalsIgnoreCase(""))) {
                    boid = r.getAttribute("Boid").toString();
                }
                if (r.getAttribute("TradeContractNo") != null &&
                    !(r.getAttribute("TradeContractNo").toString().equalsIgnoreCase(""))) {
                    trdContractNo = r.getAttribute("TradeContractNo").toString();
                } else {
                    trdContractNo = String.valueOf(i);
                }
                if (r.getAttribute("CompulsorySpot") != null &&
                    !(r.getAttribute("CompulsorySpot").toString().equalsIgnoreCase(""))) {
                    compSpot = r.getAttribute("CompulsorySpot").toString();
                }
                if (r.getAttribute("InstrumentGroup") != null &&
                    !(r.getAttribute("InstrumentGroup").toString().equalsIgnoreCase(""))) {
                    instCat = r.getAttribute("InstrumentGroup").toString();
                }

                printLine =
                        String.valueOf(counter) + "~" + instCode + "~" + isin + "~" + traderId + "~" + buySell + "~" +
                        trdQty + "~" + trdPrice + "~" + trdDate + "~" + trdTime + "~" + mkType + "~" + filType + "~" +
                        howla + "~" + foreign + "~" + customer + "~" + boid + "~" + trdContractNo + "~" + compSpot +
                        "~" + instCat;
                output.write(printLine + linegap);
            }
            output.close();
            textFileName =
                    FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.TRADES.getValue(),
                                                                     null) + "-" + postFix + ".txt";
            JSFUtils.addFacesInformationMessage("Text File Generated Successfully..");
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Problem While Generating Text File !!");
        }
        return null;
    }

    public void xmlFileDownloadListener(FacesContext facesContext, OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades XML");
        File home_dirFile = new File(destFolderPath + File.separator + getGeneratedXMLFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + getGeneratedXMLFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            fdownload = new FileInputStream(f);
            int n;
            while ((n = fdownload.available()) > 0) {
                b = new byte[n];
                int result = fdownload.read(b);
                outputStream.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            outputStream.flush();
            new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        }
    }

    public void controlFileDownloadListener(FacesContext facesContext,
                                            OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades XML");
        File home_dirFile =
            new File(destFolderPath + File.separator + getGeneratedControlFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + getGeneratedControlFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            fdownload = new FileInputStream(f);
            int n;
            while ((n = fdownload.available()) > 0) {
                b = new byte[n];
                int result = fdownload.read(b);
                outputStream.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            outputStream.flush();
            new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        }
    }

    public void textFileDownloadListener(FacesContext facesContext, OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades TEXT");
        File home_dirFile = new File(destFolderPath + File.separator + getGeneratedTextFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + getGeneratedTextFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            fdownload = new FileInputStream(f);
            int n;
            while ((n = fdownload.available()) > 0) {
                b = new byte[n];
                int result = fdownload.read(b);
                outputStream.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            outputStream.flush();
            outputStream.close();
            new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        }
    }

    public String validateFileWithXsd() {
        try {
            boolean isValidate = false;
            String generatedClientFilePath = null;
            String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades XML");
            String destXsdFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Xsd");
            if (getGeneratedXMLFileUI().getValue() != null) {
                HttpServletRequest request =
                    (HttpServletRequest)JSFUtils.getFacesContext().getExternalContext().getRequest();
                generatedClientFilePath =
                        destFolderPath + File.separator + getGeneratedXMLFileUI().getValue().toString();
                //String clientFileXsdPath = destFolderPath + File.separator + "Flextrade-BOS-Clients.xsd";
                String urlPath =
                    FlexTradeFileUtil.getURLWithContextPath(request) + "/" + xsd_url_path + client_xsd_url_path;

                String clientFileXsdPath = destXsdFolderPath + File.separator + client_xsd_url_path;
                FlexTradeFileUtil.downloadUrlFileUsingStream(urlPath, clientFileXsdPath);
                isValidate = FlexTradeFileUtil.validateWithExtXSDUsingSAX(generatedClientFilePath, clientFileXsdPath);
                if (isValidate)
                    JSFUtils.addFacesInformationMessage("File Validated Successfully");
                else
                    JSFUtils.addFacesErrorMessage("File Validation Error !");
            } else
                JSFUtils.addFacesInformationMessage("No File Found To Validate");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFilePath(String srcFolderName, RichInputText fileNameUI) throws ParseException {
        String filePath = null;
        String folderPath = FlexTradeFileUtil.getFlexTradeFileFolder(srcFolderName);
        if (fileNameUI.getValue() != null) {
            filePath = folderPath + File.separator + fileNameUI.getValue().toString();
        } else {
            JSFUtils.addFacesErrorMessage("File not generated properly..");
        }
        return filePath;
    }

    public ArrayList<String> getPathForSelectedFile() throws ParseException, IOException, Exception {
        String xmlFolder = "Trades XML";
        String textFolder = "Trades TEXT";
        String path = null;
        ArrayList<String> filePathList = new ArrayList<String>();

        if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("xml")) {
            path = this.getFilePath(xmlFolder, generatedXMLFileUI);
            filePathList.add(path);
            path = this.getFilePath(xmlFolder, generatedControlFileUI);
            filePathList.add(path);
        } else if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("text")) {
            path = this.getFilePath(textFolder, generatedTextFileUI);
            filePathList.add(path);
        }

        if (this.getPriceFile() != null) {
            path = this.getFileUploadPath(this.getPriceFile()); //get price file path
            filePathList.add(path);
        }

        if (this.getPrcCtrlFile() != null) {
            path = this.getFileUploadPath(this.getPrcCtrlFile()); //get price ctrl file path
            filePathList.add(path);
        }
        return filePathList;
    }

    public String getFileUploadPath(UploadedFile file) throws IOException, Exception {
        UploadParam uParam = null;
        try {
            uParam = new UploadParam();
            uParam.setUploadFile(file);
            uParam.setDestPath(USER_HOME + File.separator + "UploadedFiles" + File.separator + file.getFilename());
            uParam.setCreateFolderifNotExist(true);
            FileUpload uploadObj = new FileUpload(uParam);
            uploadObj.uploadFile();
            String messageText = "File Uploaded Successfully..";
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return uParam.getDestPath();
    }

    public String sendFileToMbank() throws ParseException, IOException, Exception {
        try {
            String to = null;
            Map<String, EmailContent> flyweigtTemplateContent = new HashMap<String, EmailContent>();
            String templateCode = EmailUtil.getEmailTemplate("EmailTemplateDummyVOIterator", "EmailTemplate");
            if (templateCode == null) {
                JSFUtils.addFacesErrorMessage("Please Select Email Template !");
                sendMailPopupUI.cancel();
                return null;
            }

            EmailConf emailConf = new EmailConf("EmailConfVOIterator");
            EmailContent emailCont = new EmailContent();

            if (this.getMbankEmail() != null)
                to = this.getMbankEmail().trim();

            if (to == null || to.equals("")) {
                JSFUtils.addFacesErrorMessage("No Email Id. Found For Selected Mbank !");
                sendMailPopupUI.cancel();
                return null;
            }

            ArrayList<String> filePathList = getPathForSelectedFile();
            Map<String, String> smtpConfMap = emailConf.getSmtpConfInfoList();
            String host = smtpConfMap.get("host").toString().trim();
            SMTPTransport transport = emailConf.getTransport(emailConf, emailCont);

            try {
                if (transport != null && !transport.isConnected())
                    transport.connect(host, emailCont.getSmtpUser().trim(), emailCont.getSmtpUserPassword().trim());
                else
                    System.out.println("Connection not established!!");

                emailCont.setIteratorName("EmailTemplateVOIterator");
                //loop if send to many
                emailCont.setTemplateCode(templateCode);
                //For template specific info, check if already same template used previously then get from map
                // else put in map to retrieve it when required
                if (flyweigtTemplateContent.get(templateCode) == null) {
                    emailCont = EmailUtil.getCommonMailBody(emailCont);
                    flyweigtTemplateContent.put(templateCode, emailCont);
                } else
                    emailCont = flyweigtTemplateContent.get(templateCode);

                //set all content of email
                emailCont.setTo(to);
                emailCont.setFilePathList(filePathList);
                emailConf.sendMail(emailCont);
                JSFUtils.addFacesInformationMessage("File(s) Sent Successfully");
                sendMailPopupUI.cancel();
            } catch (MessagingException me) {
                me.printStackTrace();
                throw me;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            } finally {
                transport.close();
            }

        } catch (NoSuchProviderException nspe) {
            JSFUtils.addFacesErrorMessage(nspe.getMessage());
            nspe.printStackTrace();
        } catch (MessagingException me) {
            JSFUtils.addFacesErrorMessage("File Generation Failed ! Regenerate File And Send Again");
            JSFUtils.addFacesErrorMessage(me.getMessage());
            me.printStackTrace();
        } catch (ParseException pe) {
            JSFUtils.addFacesErrorMessage(pe.getMessage());
            pe.printStackTrace();
        } catch (IOException ioe) {
            JSFUtils.addFacesErrorMessage("File Generation Failed ! Regenerate File And Send Again");
            ioe.printStackTrace();
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void getPriceFileValChngListener(ValueChangeEvent valueChangeEvent) {
        priceFile = (UploadedFile)valueChangeEvent.getNewValue();
        this.setPriceFile(priceFile);
    }

    public void getPriceCtrlFileValChngListener(ValueChangeEvent valueChangeEvent) {
        prcCtrlFile = (UploadedFile)valueChangeEvent.getNewValue();
        this.setPrcCtrlFile(prcCtrlFile);
    }

    public String getNewGenXMLFile() {
        return xmlFileName;
    }

    public String getNewGenControlFile() {
        return controlFileName;
    }

    public String getNewGenTextFile() {
        return textFileName;
    }

    public void setFileTypeRadio(RichSelectOneRadio fileTypeRadio) {
        this.fileTypeRadio = fileTypeRadio;
    }

    public RichSelectOneRadio getFileTypeRadio() {
        return fileTypeRadio;
    }

    public void setGeneratedXMLFileUI(RichInputText generatedXMLFileUI) {
        this.generatedXMLFileUI = generatedXMLFileUI;
    }

    public RichInputText getGeneratedXMLFileUI() {
        return generatedXMLFileUI;
    }

    public void setGeneratedControlFileUI(RichInputText generatedControlFileUI) {
        this.generatedControlFileUI = generatedControlFileUI;
    }

    public RichInputText getGeneratedControlFileUI() {
        return generatedControlFileUI;
    }

    public void setGeneratedTextFileUI(RichInputText generatedTextFileUI) {
        this.generatedTextFileUI = generatedTextFileUI;
    }

    public RichInputText getGeneratedTextFileUI() {
        return generatedTextFileUI;
    }

    public void setPriceFile(UploadedFile priceFile) {
        this.priceFile = priceFile;
    }

    public UploadedFile getPriceFile() {
        return priceFile;
    }

    public void setPrcCtrlFile(UploadedFile prcCtrlFile) {
        this.prcCtrlFile = prcCtrlFile;
    }

    public UploadedFile getPrcCtrlFile() {
        return prcCtrlFile;
    }

    public void setSendMailPopupUI(RichPopup sendMailPopupUI) {
        this.sendMailPopupUI = sendMailPopupUI;
    }

    public RichPopup getSendMailPopupUI() {
        return sendMailPopupUI;
    }

    public void setMbankEmailIdUI(RichInputText mbankEmailIdUI) {
        this.mbankEmailIdUI = mbankEmailIdUI;
    }

    public RichInputText getMbankEmailIdUI() {
        return mbankEmailIdUI;
    }
}
