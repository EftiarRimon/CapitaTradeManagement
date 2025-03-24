package leads.capita.trade.view.backing;

/*Created by : Ipsheta Saha */

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

import java.util.Date;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.file.FlexTradeFileUtil;

import leads.capita.trade.file.PayInOutFileUtil;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.input.RichSelectOneRadio;

import oracle.jbo.Row;

import oracle.jbo.ViewObject;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class FtiDealerTradeFiles {
    private String newFileName = null;
    static String xmlFileName = null;
    static String controlFileName = null;
    static String textFileName = null;
    private RichSelectOneRadio fileTypeRadio;
    private static final String xsd_url_path = "flex-tradexsd/v7-10/";
    private static final String client_xsd_url_path = "Flextrade-BOS-Trades.xsd";
    private RichInputText generatedXMLFileUI;
    private RichInputText generatedControlFileUI;
    private RichInputText generatedTextFileUI;

    public FtiDealerTradeFiles() {
    }

    private String getOrgShortName() {
        String shortName = null;
        String bussinessType = ApplicationInfo.getBusinessType();
        if (bussinessType.equalsIgnoreCase("BROKER"))
            shortName = FlexTradeFileUtil._getAttrValueFromIter("BrokerLOVIterator", "ShortName");
        else
            shortName = FlexTradeFileUtil._getFirstRowAttrValueFromIter("MerchantBankVOIterator", "MbankShortName");

        return shortName;
    }

    public String generateFileAction() throws ParseException, IOException {
        if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("xml")) {
            this.generateXMLFile();
        } else if (this.getFileTypeRadio().getValue().toString().equalsIgnoreCase("text")) {
            this.generateTextFile();
        }
        return null;
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

    public String getFtiBoard(String boardType) {               //not used
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
        
        DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiDealerTradeFilesVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades Dealer XML");
        shortName = this.getOrgShortName();
        newFileName =
                destFolderPath + File.separator + FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.TRADES.getValue(),
                                                                                                   null);

        newFileName = newFileName + "-" + shortName + ".xml";
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
                                                  r.getAttribute("InstrumentCode") == null ? "-" : r.getAttribute("InstrumentCode").toString()));
                if (r.getAttribute("CompulsorySpot") != null &&
                    r.getAttribute("CompulsorySpot").toString().equalsIgnoreCase("Y")) {
                    detail.setAttribute(new Attribute("Board", "PUBLIC"));
                } else {
                    String board = r.getAttribute("MarketType").toString();
                    String attrVal = boardMap.get(board);
                    detail.setAttribute(new Attribute("Board", attrVal));
                    System.out.println(board+"---"+attrVal);
                }
                if (r.getAttribute("TradeDate") != null && r.getAttribute("TradeDate").toString() != null) {
                    Object trDate = r.getAttribute("TradeDate");
                    Timestamp date = (Timestamp)trDate;
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                    trdDate = format.format(new Date(date.getTime()));
                    //System.out.println("date-----"+trdDate);
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
                                                                     null) + "-" + shortName + ".xml";
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

    public String validateFileWithXsd() {
        try {
            boolean isValidate = false;
            String generatedClientFilePath = null;
            String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades Dealer XML");
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

        File home_dir = null;
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades Dealer TEXT");
        String postFix = "TEXT file";
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

            DCIteratorBinding tradesIter = ADFUtils.findIterator("FtiDealerTradeFilesVOIterator");
            tradesIter.setRangeSize((int)tradesIter.getEstimatedRowCount());
            Row[] rows = tradesIter.getViewObject().getAllRowsInRange();

            for (int i = 0; i < rows.length; i++) {
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
                    ;
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
                    // filType = r.getAttribute("FillType").toString();
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
                }else{
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
                        born + "~" + instCode + "~" + isin + "~" + traderId + "~" + buySell + "~" + trdQty + "~" + trdPrice +
                        "~" + trdDate + "~" + trdTime + "~" + mkType + "~" + filType + "~" + howla + "~" + foreign +
                        "~" + customer + "~" + boid + "~" + trdContractNo + "~" + compSpot + "~" + instCat;
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
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades Dealer XML");
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
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades Dealer XML");
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
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trades Dealer TEXT");
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

    public void setFileTypeRadio(RichSelectOneRadio fileTypeRadio) {
        this.fileTypeRadio = fileTypeRadio;
    }

    public RichSelectOneRadio getFileTypeRadio() {
        return fileTypeRadio;
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

}
