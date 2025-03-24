package leads.capita.trade.view.backing;

import com.sun.mail.smtp.SMTPTransport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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

import javax.faces.context.FacesContext;

import javax.faces.event.ValueChangeEvent;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;

import javax.mail.Session;

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

import org.apache.myfaces.trinidad.model.UploadedFile;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class FtiTradeFilesCse {
    private String newFileName = null;
    static String xmlFileName = null;
    static String controlFileName = null;
    static String textFileName = null;
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
    private RichSelectOneRadio fileTypeRadio;

    public FtiTradeFilesCse() {
        super();
    }

    private String getMbankShortName() {
        String shortName = null;
        Object mBankId = null;
        Object groupWord = null;
        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesCseVOIterator");
        if (tradesIter != null && tradesIter.getEstimatedRowCount() > 0) {
            Row r = tradesIter.getCurrentRow();
            if (r == null)
                r = tradesIter.getViewObject().first();
            mBankId = r.getAttribute("MbankId");
            if (mBankId != null)
                shortName = r.getAttribute("MbankShortName").toString();
            else if (r.getAttribute("GroupWord") != null)
                groupWord = r.getAttribute("GroupWord").toString();
            if (shortName == null) {
                if (groupWord != null)
                    shortName = groupWord.toString();
                else
                    return null;
            } else if (shortName.length() >= 4) {
                JSFUtils.addFacesErrorMessage("Short name should be greater than or equal 4 characters !");
                return null;
            }
        }

        return shortName;
    }
    
    private String getMbankEmail() {
        String mailId = null;
        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesCseVOIterator");
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

    public String generateFile() throws ParseException, IOException {
        if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("xml")) {
            this.generateXMLFile();
        } else if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("text")) {
            this.generateTextFile();
        }
        return null;
    }

    public String generateXMLFile() throws ParseException {
        String shortName = null;
        String trdDate = null;
        String ordrDate = null;
        boolean ctrlOK = false;
        String ctrlAppend = null;
        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesCseVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades XML");
        shortName = getMbankShortName();
        String postFix = shortName + "(CSE)";
        if (shortName == null) {
            shortName="";
        }
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
                detail.setAttribute(new Attribute("TraderId",
                                                  r.getAttribute("TraderId") == null ? "-" : r.getAttribute("TraderId").toString()));
                detail.setAttribute(new Attribute("Scrip",
                                                  r.getAttribute("Scrip") == null ? "-" : r.getAttribute("Scrip").toString()));
                detail.setAttribute(new Attribute("InstrumentCode",
                                                  r.getAttribute("InstrumentCode") == null ? "-" : r.getAttribute("InstrumentCode").toString()));
                detail.setAttribute(new Attribute("BuySell",
                                                  r.getAttribute("BuySell") == null ? "-" : r.getAttribute("BuySell").toString()));
                detail.setAttribute(new Attribute("Quantity",
                                                  r.getAttribute("TradeQuantity") == null ? "-" : r.getAttribute("TradeQuantity").toString()));
                detail.setAttribute(new Attribute("Price",
                                                  r.getAttribute("Price") == null ? "-" : r.getAttribute("Price").toString()));
                detail.setAttribute(new Attribute("ClientCode",
                                                  r.getAttribute("Customer") == null ? "-" : r.getAttribute("Customer").toString()));
                detail.setAttribute(new Attribute("OpBrker",
                                                  r.getAttribute("Opbrker") == null ? "-" : r.getAttribute("Opbrker").toString()));
                detail.setAttribute(new Attribute("OpTrder",
                                                  r.getAttribute("Optrder") == null ? "-" : r.getAttribute("Optrder").toString()));
                detail.setAttribute(new Attribute("TID",
                                                  r.getAttribute("Tid") == null ? "-" : r.getAttribute("Tid").toString()));

                if (r.getAttribute("TradeDate") != null && r.getAttribute("TradeDate").toString() != null) {
                    Object trDate = r.getAttribute("TradeDate");
                    Timestamp date = (Timestamp)trDate;
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");//yyyyMMdd
                    trdDate = format.format(new Date(date.getTime()));
                    detail.setAttribute(new Attribute("TradeDate", trdDate));//need to change
                } else {
                    detail.setAttribute(new Attribute("TradeDate", "-"));
                }

                detail.setAttribute(new Attribute("TradeTime",
                                                  r.getAttribute("TradeTime") == null ? "-" : r.getAttribute("TradeTime").toString()));
                detail.setAttribute(new Attribute("OrderDate",
                                                  r.getAttribute("OrderDate") == null ? "-" : r.getAttribute("OrderDate").toString()));
                detail.setAttribute(new Attribute("OrderTime",
                                                  r.getAttribute("OrderTime") == null ? "-" : r.getAttribute("OrderTime").toString()));

                document.getRootElement().addContent(detail);
            }
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(document, new FileWriter(generatedfile.getPath()));
            //set file name of newly generated file
            xmlFileName =
                    FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.TRADES.getValue(),
                                                                     null) + "-" + postFix + ".xml";
            //ctrlAppend = postFix + "-ctrl.xml";
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

    private boolean generateControlFile(File generateCtrlFile) throws JDOMException,
                                                                                             IOException {
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
        String mbankShortName = "";
        String traderId = "";
        String scrip = "";
        String instrumentCode = "";
        String buySell = "";
        String trdQty = "";
        String trdPrice = "";
        String customer = "";
        String opBrkr = "";
        String opTrder = "";
        String tid = "";
        String trdDate = "";
        String trdTime = "";
        String ordrDate = "";
        // String status = "";
        String ordrTime = "";

        File home_dir = null;
        mbankShortName = getMbankShortName();
        if (mbankShortName == null) {
            mbankShortName="";
        }
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades TEXT");
        String postFix = mbankShortName + "(CSE)";
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
            DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiTradeFilesCseVOIterator");
            tradesIter.setRangeSize((int)tradesIter.getEstimatedRowCount());
            Row[] rows = tradesIter.getViewObject().getAllRowsInRange();
            for (int i = 0; i < rows.length; i++) {
                Row r = rows[i];
                if (r.getAttribute("TraderId") != null &&
                    !(r.getAttribute("TraderId").toString().equalsIgnoreCase(""))) {
                    traderId = r.getAttribute("TraderId").toString();
                }
                if (r.getAttribute("Scrip") != null && !(r.getAttribute("Scrip").toString().equalsIgnoreCase(""))) {
                    scrip = r.getAttribute("Scrip").toString();
                }
                if (r.getAttribute("InstrumentCode") != null &&
                    !(r.getAttribute("InstrumentCode").toString().equalsIgnoreCase(""))) {
                    instrumentCode = r.getAttribute("InstrumentCode").toString();
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
                if (r.getAttribute("Customer") != null &&
                    !(r.getAttribute("Customer").toString().equalsIgnoreCase(""))) {
                    customer = r.getAttribute("Customer").toString();
                }
                if (r.getAttribute("Opbrker") != null &&
                    !(r.getAttribute("Opbrker").toString().equalsIgnoreCase(""))) {
                    opBrkr = r.getAttribute("Opbrker").toString();
                }
                if (r.getAttribute("Optrder") != null &&
                    !(r.getAttribute("Optrder").toString().equalsIgnoreCase(""))) {
                    opTrder = r.getAttribute("Optrder").toString();
                }
                if (r.getAttribute("Tid") != null && !(r.getAttribute("Tid").toString().equalsIgnoreCase(""))) {
                    tid = r.getAttribute("Tid").toString();
                }
                if (r.getAttribute("TradeDate") != null &&
                    !(r.getAttribute("TradeDate").toString().equalsIgnoreCase(""))) {
                    Object trDate = r.getAttribute("TradeDate");
                    Timestamp date = (Timestamp)trDate;
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                    trdDate = format.format(new Date(date.getTime()));
                }
                if (r.getAttribute("TradeTime") != null &&
                    !(r.getAttribute("TradeTime").toString().equalsIgnoreCase(""))) {
                    trdTime = r.getAttribute("TradeTime").toString();
                }
                if (r.getAttribute("OrderDate") != null &&
                    !(r.getAttribute("OrderDate").toString().equalsIgnoreCase(""))) {
                    ordrDate = r.getAttribute("OrderDate").toString();
                }
                /*  if (r.getAttribute("ValidStatus") != null && !(r.getAttribute("ValidStatus").toString().equalsIgnoreCase(""))) {
                    status = r.getAttribute("Born").toString();
                } */
                if (r.getAttribute("OrderTime") != null &&
                    !(r.getAttribute("OrderTime").toString().equalsIgnoreCase(""))) {
                    ordrTime = r.getAttribute("OrderTime").toString();
                }
                printLine =
                        traderId + "|" + scrip + "|" + instrumentCode + "|" + buySell + "|" + trdQty + "|" + trdPrice +
                        "|" + customer + "|" + opBrkr + "|" + opTrder + "|" + tid + "|" + trdDate + "|" + trdTime +
                        "|" + ordrDate + "|" + ordrTime;
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
        priceFile=(UploadedFile)valueChangeEvent.getNewValue();
        this.setPriceFile(priceFile);
    }

    public void getPriceCtrlFileValChngListener(ValueChangeEvent valueChangeEvent) {
        prcCtrlFile=(UploadedFile)valueChangeEvent.getNewValue();
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

    public void setSendMailPopupUI(RichPopup sendMailPopupUI) {
        this.sendMailPopupUI = sendMailPopupUI;
    }

    public RichPopup getSendMailPopupUI() {
        return sendMailPopupUI;
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

    public void setMbankEmailIdUI(RichInputText mbankEmailIdUI) {
        this.mbankEmailIdUI = mbankEmailIdUI;
    }

    public RichInputText getMbankEmailIdUI() {
        return mbankEmailIdUI;
    }

    public void setFileTypeRadio(RichSelectOneRadio fileTypeRadio) {
        this.fileTypeRadio = fileTypeRadio;
    }

    public RichSelectOneRadio getFileTypeRadio() {
        return fileTypeRadio;
    }
}
