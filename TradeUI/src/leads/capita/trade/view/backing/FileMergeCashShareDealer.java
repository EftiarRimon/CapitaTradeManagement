package leads.capita.trade.view.backing;

/*Created by : Ipsheta Saha */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.text.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

import javax.faces.event.ValueChangeEvent;

import javax.xml.parsers.DocumentBuilderFactory;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.exception.handler.CustomExceptionHandling;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.file.DataDump;
import leads.capita.trade.file.FlexTradeFileUtil;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichSelectOneRadio;

import oracle.adf.view.rich.event.DialogEvent;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.ApplicationModule;
import oracle.jbo.JboException;
import oracle.jbo.Row;
import oracle.jbo.ViewObject;
import oracle.jbo.server.Entity;
import oracle.jbo.server.ViewRowImpl;

import org.apache.myfaces.trinidad.model.UploadedFile;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class FileMergeCashShareDealer {
    public static final String FTI_FILE_MERGE_CLIENT_DEALER_ITER = "FtiFileMergeCashDealerVOIterator";
    public static final String FTI_FILE_MERGE_CLIENT_DETAIL_DEALER_ITER = "FtiFileMrgCashDetDealerVOIterator";
    public static final String FTI_FILE_MERGE_POSITION_DEALER_ITER = "FtiFileMergeShareDealerVOIterator";
    public static final String FTI_FILE_MERGE_POSITION_DETAIL_DEALER_ITER = "FtiFileMrgShareDetDealerVOIterator";
    public static final String FTI_FILE_MERGE_CLIENT_PROC_DATA_ITER = "FtiDealerMergeCashDataVOIterator";
    public static final String FTI_FILE_MERGE_POSITION_PROC_DATA_ITER = "FtiDealerMergeShareDataVOIterator";

    private String currentFilePathClient = null;
    private String currentFilePathPosition = null;
    private UploadedFile _fileClient;
    private UploadedFile _fileShare;
    private RichSelectOneRadio fileFormatClientCB;
    private RichSelectOneRadio fileOptionClientUI;
    private RichSelectOneRadio fileFormatPositionCB;
    private RichSelectOneRadio fileOptionPositionUI;

    public FileMergeCashShareDealer() {
    }

    public String createClientLimit(ActionEvent actionEvent) {
        BindingContainer bindings = ADFUtils.getDCBindingContainer();
        OperationBinding operationBinding = bindings.getOperationBinding("CreateInsert");
        if (operationBinding != null)
            operationBinding.execute();
        else
            JSFUtils.addFacesErrorMessage("CreateInsert Binding Problem !!");
        return null;
    }

    public boolean isNewClient() {
        boolean isNewRow = false;
        isNewRow = this.isNewStateRow(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
        return isNewRow;
    }

    public String clientLimitSaveAction() throws Exception {
        String fileFormat = null;
        String fileOption = null;
        String fileId = FlexTradeFileUtil.getUniqueValue();
        String fileVersion = FlexTradeFileUtil.getCurSystemTime(null); //hhmmss
        if (this.getFileFormatClientCB().getValue() != null && this.getFileOptionClientUI().getValue() != null) {
            fileFormat = this.getFileFormatClientCB().getValue().toString();
            fileOption = this.getFileOptionClientUI().getValue().toString();
        }
        // System.out.println("----" + fileOption);
        DCIteratorBinding cashIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
        Row curRow = cashIter.getViewObject().getCurrentRow();
        curRow.setAttribute("FileId", fileId);
        curRow.setAttribute("FileVersion", fileVersion);
        curRow.setAttribute("FileFormat", fileFormat);
        curRow.setAttribute("FileOption", fileOption);
        this.saveRecord();
        return null;
    }

    public void fileSelectCashValChngListener(ValueChangeEvent valueChangeEvent) {
        _fileClient = (UploadedFile)valueChangeEvent.getNewValue();
        if (_fileClient.getContentType().equals("text/plain")) {
            this.setFile(_fileClient);
        } else if (_fileClient.getContentType().equals("text/xml")) {
            this.setFile(_fileClient);
        } else {
            _fileClient = null;
            System.out.println("No file has selected!");
        }
    }

    public void clientLimitUplaodFileAction(DialogEvent dialogEvent) throws ParseException, IOException, Exception {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            DCIteratorBinding cashIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
            ViewObject cashVO = cashIter.getViewObject();
            if (cashVO.getCurrentRow().getAttribute("FileFormat") != null) {
                if (cashVO.getCurrentRow().getAttribute("FileFormat").toString().equalsIgnoreCase("text")) {
                    this.uploadClientTextFile();
                } else if (cashVO.getCurrentRow().getAttribute("FileFormat").toString().equalsIgnoreCase("xml")) {
                    this.uploadClientXMLFile();
                }
            }
        }
    }
    
    public void doUpload() throws ParseException, IOException, Exception {
            
                DCIteratorBinding cashIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
                ViewObject cashVO = cashIter.getViewObject();
                if (cashVO.getCurrentRow().getAttribute("FileFormat") != null) {
                    if (cashVO.getCurrentRow().getAttribute("FileFormat").toString().equalsIgnoreCase("text")) {
                        this.uploadClientTextFile();
                    } else if (cashVO.getCurrentRow().getAttribute("FileFormat").toString().equalsIgnoreCase("xml")) {
                        this.uploadClientXMLFile();
                    }
                }
            
        }

    public String uploadClientXMLFile() throws ParseException, IOException, Exception {
        String clientCodeReg = null;
        String fileId = null;
        String fileName = null;
        UploadedFile file = this.getFile();
        if (file != null) {
            fileName = file.getFilename();
        } else if (file == null || file.getLength() <= 0) {
            JSFUtils.addFacesErrorMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            return null;
        }
        if (!file.getContentType().equals("text/xml")) {
            JSFUtils.addFacesErrorMessage("Upload XML(.xml) File !");
            return null;
        }
        DCIteratorBinding cashIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
        ViewObject cashVO = cashIter.getViewObject();
        DCIteratorBinding cashDetailIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DETAIL_DEALER_ITER);
        ViewObject cashDetailVO = cashDetailIter.getViewObject();
        if (cashVO.getCurrentRow().getAttribute("FileId") != null) {
            fileId = cashVO.getCurrentRow().getAttribute("FileId").toString();
        }

        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("DClient");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        SAXBuilder saxbuilder = new SAXBuilder();
        currentFilePathClient = destFolderPath + File.separator + file.getFilename();
        File newGenFile = new File(currentFilePathClient);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        try {
            FileInputStream fileIS = new FileInputStream(new File(currentFilePathClient));
            javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder;
            org.w3c.dom.Document doc = null;
            javax.xml.xpath.XPathExpression expr = null;
            builder = factory.newDocumentBuilder();
            doc = builder.parse(fileIS); //used to parse file using XPATH

            Map<Object, String> regInvestor = new HashMap<Object, String>();
            Document document = (Document)saxbuilder.build(newGenFile); //used to parse file using JDOM
            Element rootNode = document.getRootElement();
            List<Element> rootChilds = rootNode.getChildren();
            for (Element e : rootChilds) {
                if (e.getName().equalsIgnoreCase("Register")) {
                    // new row created for each register
                    Row cashDetailRow = cashDetailVO.createRow();
                    List<Element> list = e.getChildren();
                    for (int i = 0; i < list.size(); i++) {
                        Element node = (Element)list.get(i);
                        if (node.getName().equalsIgnoreCase("ClientCode")) {
                            cashDetailRow.setAttribute("InvestorCode", node.getText() == null ? null : node.getText());
                            clientCodeReg = node.getText();
                            regInvestor.put(clientCodeReg, clientCodeReg); //store client codes of all register tag
                            // get other element text(val.) of current investor(From Respective Limit Tag having Client code as current inv.)..
                            BigDecimal cashVal = FlexTradeFileUtil.getClientLimitValue(clientCodeReg, "Cash", doc);
                            cashDetailRow.setAttribute("BuyLimit", cashVal);
                            BigDecimal mrgRatioVal =
                                FlexTradeFileUtil.getClientLimitAttrValue(clientCodeReg, "Margin", "MarginRatio", doc);
                            cashDetailRow.setAttribute("MarginRatio", mrgRatioVal);
                            BigDecimal maxCapBuyVal =
                                FlexTradeFileUtil.getClientLimitValue(clientCodeReg, "MaxCapitalBuy", doc);
                            cashDetailRow.setAttribute("MaxCapitalBuy", maxCapBuyVal);
                            BigDecimal maxCapSellVal =
                                FlexTradeFileUtil.getClientLimitValue(clientCodeReg, "MaxCapitalSell", doc);
                            cashDetailRow.setAttribute("MaxCapitalSell", maxCapSellVal);
                            BigDecimal totalTransVal =
                                FlexTradeFileUtil.getClientLimitValue(clientCodeReg, "TotalTransaction", doc);
                            cashDetailRow.setAttribute("TotalTransaction", totalTransVal);
                            BigDecimal netTransVal =
                                FlexTradeFileUtil.getClientLimitValue(clientCodeReg, "NetTransaction", doc);
                            cashDetailRow.setAttribute("NetTransaction", netTransVal);
                        } else if (node.getName().equalsIgnoreCase("DealerID")) {
                            cashDetailRow.setAttribute("DealerId", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("BOID")) {
                            cashDetailRow.setAttribute("Boid", node.getText() == null ? null : node.getText());
                        }
                        cashDetailRow.setAttribute("FileId", fileId);
                        cashDetailRow.setAttribute("FileName", fileName);
                    }
                } else if (e.getName().equalsIgnoreCase("Deactivate")) {
                    Row cashDetRowDeactiv = cashDetailVO.createRow();
                    List<Element> deactiveList = e.getChildren();
                    for (int i = 0; i < deactiveList.size(); i++) { //children of Deactivate
                        Element deactiveNode = (Element)deactiveList.get(i);
                        if (deactiveNode.getName().equalsIgnoreCase("ClientCode")) {
                            cashDetRowDeactiv.setAttribute("InvestorCode",
                                                           deactiveNode.getText() == null ? null : deactiveNode.getText());
                            cashDetRowDeactiv.setAttribute("FtiStatus", "Deactive");
                            cashDetRowDeactiv.setAttribute("FileId", fileId);
                            cashDetRowDeactiv.setAttribute("FileName", fileName);
                        }
                    }
                } else if (e.getName().equalsIgnoreCase("Limits")) {
                    Row newCashDetailRow = null;
                    String cCode = null;
                    List<Element> limitList = e.getChildren();
                    // newCashDetailRow = cashDetailVO.createRow();
                    for (int i = 0; i < limitList.size(); i++) { //children of limit

                        Element limitNode = (Element)limitList.get(i);
                        if (limitNode.getName().equalsIgnoreCase("ClientCode")) {
                            cCode = limitNode.getText();
                        }
                        if (!regInvestor.containsKey(cCode)) { //if client Code does not found in map then
                            //create row to fti_file_merge_cash_detail to Save other client's buy_limit(cash)
                            if (limitNode.getName().equalsIgnoreCase("ClientCode")) {
                                newCashDetailRow = cashDetailVO.createRow();
                            }
                            newCashDetailRow.setAttribute("FileId", fileId);
                            newCashDetailRow.setAttribute("FileName", fileName);
                            if (limitNode.getName().equalsIgnoreCase("ClientCode")) {
                                newCashDetailRow.setAttribute("InvestorCode",
                                                              limitNode.getText() == null ? null : limitNode.getText());
                            } else if (limitNode.getName().equalsIgnoreCase("Cash")) {
                                newCashDetailRow.setAttribute("BuyLimit",
                                                              limitNode.getText() == null ? null : limitNode.getText());
                            } else if (limitNode.getName().equalsIgnoreCase("Margin")) {
                                if (limitNode.getName().equalsIgnoreCase("MarginRatio")) {
                                    BigDecimal mrgRat =
                                        new BigDecimal(limitNode.getAttribute("MarginRatio").getValue().toString());
                                    newCashDetailRow.setAttribute("MarginRatio", mrgRat);
                                }
                            } else if (limitNode.getName().equalsIgnoreCase("MaxCapitalBuy")) {
                                newCashDetailRow.setAttribute("MaxCapitalBuy",
                                                              limitNode.getText() == null ? null : limitNode.getText());
                            } else if (limitNode.getName().equalsIgnoreCase("MaxCapitalSell")) {
                                newCashDetailRow.setAttribute("MaxCapitalSell",
                                                              limitNode.getText() == null ? null : limitNode.getText());
                            } else if (limitNode.getName().equalsIgnoreCase("TotalTransaction")) {
                                newCashDetailRow.setAttribute("TotalTransaction",
                                                              limitNode.getText() == null ? null : limitNode.getText());
                            } else if (limitNode.getName().equalsIgnoreCase("NetTransaction")) {
                                newCashDetailRow.setAttribute("NetTransaction",
                                                              limitNode.getText() == null ? null : limitNode.getText());
                            }
                        }
                    }
                } else if (e.getName().equalsIgnoreCase("Suspend")) {
                    Row cashDetRowSuspend = cashDetailVO.createRow();
                    List<Element> suspendList = e.getChildren();
                    for (int i = 0; i < suspendList.size(); i++) { //children of suspend
                        Element suspendNode = (Element)suspendList.get(i);
                        if (suspendNode.getName().equalsIgnoreCase("ClientCode")) {
                            cashDetRowSuspend.setAttribute("InvestorCode",
                                                           suspendNode.getText() == null ? null : suspendNode.getText());
                            cashDetRowSuspend.setAttribute("FtiStatus", "Suspend");
                            cashDetRowSuspend.setAttribute("FileId", fileId);
                            cashDetRowSuspend.setAttribute("FileName", fileName);
                        }
                    }

                } //End of Suspend

            } //End of XML File
            this.saveRecord();
        } catch (Exception e) {
            e.printStackTrace();
            // JSFUtils.getBindings().getOperationBinding("Rollback").execute();
            JSFUtils.addFacesErrorMessage("Problem in File Processing...");
        }
        return null;
    }

    public String uploadClientTextFile() throws ParseException, IOException, Exception {
        String fileId = null;
        String fileName = null;
        UploadedFile file = this.getFile();
        if (file != null) {
            fileName = file.getFilename();
        } else if (file == null || file.getLength() <= 0) {
            JSFUtils.addFacesErrorMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            return null;
        }
        if (!file.getContentType().equals("text/plain")) {
            JSFUtils.addFacesErrorMessage("Upload Text(.txt) File !");
            return null;
        }
        DCIteratorBinding cashIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
        DCIteratorBinding cashDetailIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DETAIL_DEALER_ITER);
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("DClient");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        currentFilePathClient = destFolderPath + File.separator + file.getFilename();
        File newGenFile = new File(currentFilePathClient);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }

        FileInputStream fstream = new FileInputStream(currentFilePathClient);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        ViewObject cashVO = cashIter.getViewObject();
        ViewObject cashDetailVO = cashDetailIter.getViewObject();
        if (cashVO.getCurrentRow().getAttribute("FileId") != null) {
            fileId = cashVO.getCurrentRow().getAttribute("FileId").toString();
        } else {
            JSFUtils.addFacesErrorMessage(" File Id Not Found!!");
            return null;
        }
        String strLine = null;
        //Read File Line By Line: \\' \\
        while ((strLine = br.readLine()) != null) {
            // Print the content on the console
            String words[] = strLine.split("~");
            Row cashDetailRow = cashDetailVO.createRow();
            cashDetailRow.setAttribute("FileId", fileId);
            // for (int i=0;i<words.length;i++){
            cashDetailRow.setAttribute("FileName", fileName);
            cashDetailRow.setAttribute("InvestorCode", words[0]);
            cashDetailRow.setAttribute("Boid", words[1]);
            cashDetailRow.setAttribute("CompanyName", words[2]);
            cashDetailRow.setAttribute("IsForeign", words[3]);
            cashDetailRow.setAttribute("BuyLimit", words[4]);
            cashDetailRow.setAttribute("PanicWithdraw", words[5]);
            /* String appDate = words[2];
            Timestamp appDt = FlexTradeFileUtil.getSystemDateTimeStampWithTime(appDate);
            cashDetailRow.setAttribute("AppDate", appDt); */
        }
        //Close the input stream
        br.close();
        this.saveRecord();
        this.refreshIterator(FTI_FILE_MERGE_CLIENT_DETAIL_DEALER_ITER);
        return null;
    }

    public String processCashFile() throws Exception {
        boolean isProcessCall = false;
        String fileId = null;
        String fileFormat = null;
        String fileOption = null;
        try {
            DCIteratorBinding cashIter = ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_DEALER_ITER);
            if (cashIter.getCurrentRow().getAttribute("FileId") != null &&
                cashIter.getCurrentRow().getAttribute("FileFormat") != null &&
                cashIter.getCurrentRow().getAttribute("FileOption") != null) {
                fileId = cashIter.getCurrentRow().getAttribute("FileId").toString();
                fileFormat = cashIter.getCurrentRow().getAttribute("FileFormat").toString().toUpperCase();
                fileOption = cashIter.getCurrentRow().getAttribute("FileOption").toString();
                // System.out.println("process cash--------" + fileId);
                DataDump dataDump = new DataDump();
                isProcessCall = dataDump.dumpMergeCashFileDataDealer(fileId, fileFormat, fileOption, "DEALER");
            }

            if (isProcessCall) {
                cashIter.getCurrentRow().setAttribute("IsExec", true);
                JSFUtils.getBindings().getOperationBinding("Commit").execute();
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                this.refreshIterator(FTI_FILE_MERGE_CLIENT_DETAIL_DEALER_ITER);
                ADFUtils.findIterator(FTI_FILE_MERGE_CLIENT_PROC_DATA_ITER).executeQuery();  //to view processed data(right after processing)
                JSFUtils.addFacesInformationMessage("File Processing Done Successfully..");
            } else {
                JSFUtils.addFacesErrorMessage("Problem in File Processing..");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Problem in File Processing..");
        }
        return null;
    }

    public String createPositionLimit(ActionEvent actionEvent) {
        BindingContainer bindings = ADFUtils.getDCBindingContainer();
        OperationBinding operationBinding = bindings.getOperationBinding("CreateInsert");
        if (operationBinding != null)
            operationBinding.execute();
        else
            JSFUtils.addFacesErrorMessage("CreateInsert Binding Problem !!");
        return null;
    }

    public String positionLimitSaveAction() throws Exception {
        String fileFormat = null;
        String fileOption = null;
        String fileId = FlexTradeFileUtil.getUniqueValue();
        String fileVersion = FlexTradeFileUtil.getCurSystemTime(null);
        if (this.getFileFormatPositionCB().getValue() != null && this.getFileOptionPositionUI().getValue() != null) {
            fileFormat = this.getFileFormatPositionCB().getValue().toString();
            fileOption = this.getFileOptionPositionUI().getValue().toString();
        }
        //System.out.println("----" + fileOption);
        DCIteratorBinding shareIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DEALER_ITER);
        Row curRow = shareIter.getViewObject().getCurrentRow();
        curRow.setAttribute("FileId", fileId);
        curRow.setAttribute("FileVersion", fileVersion);
        curRow.setAttribute("FileFormat", fileFormat);
        curRow.setAttribute("FileOption", fileOption);
        this.saveRecord();
        return null;
    }

    public void fileSelectShareValChngListener(ValueChangeEvent valueChangeEvent) {
        _fileShare = (UploadedFile)valueChangeEvent.getNewValue();
        if (_fileShare.getContentType().equals("text/plain")) {
            this.setFile(_fileShare);
        } else if (_fileShare.getContentType().equals("text/xml")) {
            this.setFile(_fileShare);
        } else {
            _fileShare = null;
            System.out.println("No file has selected!");
        }
    }

    public boolean isNewPosition() {
        boolean isNewRow = false;
        isNewRow = this.isNewStateRow(FTI_FILE_MERGE_POSITION_DEALER_ITER);
        return isNewRow;
    }

    public void positionLimitUploadfileAction(DialogEvent dialogEvent) throws ParseException, IOException, Exception {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            DCIteratorBinding shareIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DEALER_ITER);
            ViewObject shareVO = shareIter.getViewObject();
            if (shareVO.getCurrentRow().getAttribute("FileFormat") != null) {
                if (shareVO.getCurrentRow().getAttribute("FileFormat").toString().equalsIgnoreCase("text")) {
                    this.uploadPositionTextFile();
                } else if (shareVO.getCurrentRow().getAttribute("FileFormat").toString().equalsIgnoreCase("xml")) {
                    this.uploadPositionXMLFile();
                }
            }
        }
    }

    public String uploadPositionTextFile() throws ParseException, IOException, Exception {
        String fileId = null;
        String fileName = null;
        UploadedFile file = this.getFile();
        if (file != null) {
            fileName = file.getFilename();
        } else if (file == null || file.getLength() <= 0) {
            JSFUtils.addFacesErrorMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            return null;
        }
        if (!file.getContentType().equals("text/plain")) {
            JSFUtils.addFacesErrorMessage("Upload Text(.txt) File !!");
            return null;
        }
        DCIteratorBinding shareIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DEALER_ITER);
        DCIteratorBinding shareDetailIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DETAIL_DEALER_ITER);
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("DPosition");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        currentFilePathPosition = destFolderPath + File.separator + file.getFilename();
        File newGenFile = new File(currentFilePathPosition);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        FileInputStream fstream = new FileInputStream(currentFilePathPosition);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        ViewObject shareVO = shareIter.getViewObject();
        ViewObject shareDetailVO = shareDetailIter.getViewObject();
        if (shareVO.getCurrentRow().getAttribute("FileId") != null) {
            fileId = shareVO.getCurrentRow().getAttribute("FileId").toString();
        }

        String strLine = null;
        //Read File Line By Line
        while ((strLine = br.readLine()) != null) {
            // Print the content on the console
            String words[] = strLine.split("~");
            Row shareDetailRow = shareDetailVO.createRow();
            shareDetailRow.setAttribute("FileId", fileId);
            shareDetailRow.setAttribute("FileName", fileName);
            shareDetailRow.setAttribute("Isin", words[0]);
            shareDetailRow.setAttribute("InstShortName", words[1]);
            shareDetailRow.setAttribute("Boid", words[2]);
            shareDetailRow.setAttribute("InvestorName", words[3]);
            shareDetailRow.setAttribute("TotalQuantity", words[4]);
            shareDetailRow.setAttribute("SalableQuantity", words[5]);
            shareDetailRow.setAttribute("InvestorCode", words[6]);
            String appDate = words[7];
            //Timestamp appDt = FlexTradeFileUtil.getSystemDateTimeStampWithTime(appDate);
            //Timestamp appDt = this.getSystemDateTimeStampWithTimeNew(appDate);
            // shareDetailRow.setAttribute("AppDate", appDt);
            // System.out.print("--- "+words[7]);
            // System.out.println();
        }
        br.close();
        this.saveRecord();
        this.refreshIterator(FTI_FILE_MERGE_POSITION_DETAIL_DEALER_ITER);
        return null;
    }

    public String uploadPositionXMLFile() throws ParseException, IOException, Exception {
        // System.out.println("In Position XML-----");
        String fileId = null;
        String fileName = null;
        UploadedFile file = this.getFile();
        if (file != null) {
            fileName = file.getFilename();
        } else if (file == null || file.getLength() <= 0) {
            JSFUtils.addFacesErrorMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            return null;
        }
        if (!file.getContentType().equals("text/xml")) {
            JSFUtils.addFacesErrorMessage("Upload XML(.xml) File !!");
            return null;
        }
        DCIteratorBinding shareIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DEALER_ITER);
        ViewObject shareVO = shareIter.getViewObject();
        DCIteratorBinding shareDetailIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DETAIL_DEALER_ITER);
        ViewObject shareDetailVO = shareDetailIter.getViewObject();
        if (shareVO.getCurrentRow().getAttribute("FileId") != null) {
            fileId = shareVO.getCurrentRow().getAttribute("FileId").toString();
        }

        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("DPosition");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        currentFilePathPosition = destFolderPath + File.separator + file.getFilename();
        SAXBuilder saxbuilder = new SAXBuilder();
        File newGenFile = new File(currentFilePathPosition);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        try {
            Document document = (Document)saxbuilder.build(newGenFile);
            Element rootNode = document.getRootElement();
            List<Element> rootChilds = rootNode.getChildren();
            for (Element e : rootChilds) {
                if (e.getName().equalsIgnoreCase("InsertOne")) {
                    Row shareDetailRow = shareDetailVO.createRow();
                    List<Element> list = e.getChildren();
                    for (int i = 0; i < list.size(); i++) {
                        Element node = (Element)list.get(i);
                        if (node.getName().equalsIgnoreCase("ClientCode")) {
                            shareDetailRow.setAttribute("InvestorCode",
                                                        node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("SecurityCode")) { //in XML security code = ins.Short name(Database column)
                            shareDetailRow.setAttribute("InstShortName",
                                                        node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("ISIN")) {
                            shareDetailRow.setAttribute("Isin", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("Quantity")) {
                            shareDetailRow.setAttribute("SalableQuantity",
                                                        node.getText() == null ? 0 : node.getText());
                        }
                        shareDetailRow.setAttribute("FileId", fileId);
                        shareDetailRow.setAttribute("FileName", fileName);
                    }
                }
            }
            this.saveRecord();
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
            JSFUtils.addFacesErrorMessage("Problem in File Processing...");
        }
        return null;
    }

    public String ProcessShareFile() {
        boolean isProcessCall = false;
        String fileId = null;
        String fileFormat = null;
        String fileOption = null;
        try {
            DCIteratorBinding shareIter = ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_DEALER_ITER);
            if (shareIter.getCurrentRow().getAttribute("FileId") != null &&
                shareIter.getCurrentRow().getAttribute("FileFormat") != null &&
                shareIter.getCurrentRow().getAttribute("FileOption") != null) {
                fileId = shareIter.getCurrentRow().getAttribute("FileId").toString();
                fileFormat = shareIter.getCurrentRow().getAttribute("FileFormat").toString().toUpperCase();
                fileOption = shareIter.getCurrentRow().getAttribute("FileOption").toString();
                // System.out.println("process share--------" + fileId);
                DataDump dataDump = new DataDump();
                isProcessCall = dataDump.dumpMergeShareFileDataDealer(fileId, fileFormat, fileOption, "DEALER");
            }

            if (isProcessCall) {
                shareIter.getCurrentRow().setAttribute("IsExec", true);
                JSFUtils.getBindings().getOperationBinding("Commit").execute();
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                this.refreshIterator(FTI_FILE_MERGE_POSITION_DETAIL_DEALER_ITER);
                //this.refreshIterator("FtiFileMergeShareVOIterator");
                ADFUtils.findIterator(FTI_FILE_MERGE_POSITION_PROC_DATA_ITER).executeQuery();
                JSFUtils.addFacesInformationMessage("File Processing Done Successfully...");
            } else {
                JSFUtils.addFacesErrorMessage("Problem in File Processing...");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Problem in File Processing...");
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
        return null;
    }

    public String saveRecord() throws Exception {
        if (changesExists()) {
            OperationBinding operationBinding = JSFUtils.getBindings().getOperationBinding("Commit");
            try {
                if (operationBinding != null) {
                    operationBinding.execute();
                    if (operationBinding.getErrors().isEmpty()) {
                        JSFUtils.addFacesInformationMessage("Saved Successfully..");
                    } else {
                        new CustomExceptionHandling().handleException((JboException)operationBinding.getErrors().get(0));
                        JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        JSFUtils.addFacesErrorMessage("Save Failed !!");
                    }
                } else {
                    JSFUtils.addFacesErrorMessage("Commit Binding Problem !!");
                    JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                }
            } catch (Exception e) {
                JSFUtils.addFacesErrorMessage(e.getMessage());
                JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                throw e;
            }
        } else {
            JSFUtils.addFacesInformationMessage("Nothing To Save....");
        }
        return null;
    }

    public boolean changesExists() {
        DCBindingContainer bindings = ADFUtils.getDCBindingContainer();
        ApplicationModule am = bindings.getDataControl().getApplicationModule();
        return am.getTransaction().isDirty() || isControllerTransactionDirty();
    }

    public static boolean isControllerTransactionDirty() {
        BindingContext bc = BindingContext.getCurrent();
        String currentDataControlFrame = bc.getCurrentDataControlFrame();
        return bc.findDataControlFrame(currentDataControlFrame).isTransactionDirty();
    }

    public void refreshIterator(String iteratorName) {
        if (iteratorName != null) {
            leads.capita.common.ui.ADFUtils.findIterator(iteratorName).executeQuery();
            leads.capita.common.ui.ADFUtils.findIterator(iteratorName).getViewObject().clearCache();
            ADFUtils.findIterator(iteratorName).refresh(DCIteratorBinding.RANGESIZE_UNLIMITED);
        }
        return;
    }

    public static boolean isNewStateRow(String iteratorName) {
        boolean isNewState = false;
        DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        if (iter != null && iter.getEstimatedRowCount() > 0) {
            ViewRowImpl rowImpl = (ViewRowImpl)iter.getCurrentRow();
            switch (rowImpl.getEntity(0).getEntityState()) {
            case Entity.STATUS_NEW:
                {
                    isNewState = true;
                    break;
                }
            }
        }
        return isNewState;
    }

    public void setFile(UploadedFile _fileCash) {
        this._fileClient = _fileCash;
    }

    public UploadedFile getFile() {
        return _fileClient;
    }

    public void setFileShare(UploadedFile _fileShare) {
        this._fileShare = _fileShare;
    }

    public UploadedFile getFileShare() {
        return _fileShare;
    }

    public void setFileFormatClientCB(RichSelectOneRadio fileFormatClientCB) {
        this.fileFormatClientCB = fileFormatClientCB;
    }

    public RichSelectOneRadio getFileFormatClientCB() {
        return fileFormatClientCB;
    }

    public void setFileOptionClientUI(RichSelectOneRadio fileOptionClientUI) {
        this.fileOptionClientUI = fileOptionClientUI;
    }

    public RichSelectOneRadio getFileOptionClientUI() {
        return fileOptionClientUI;
    }

    public void setFileFormatPositionCB(RichSelectOneRadio fileFormatPositionCB) {
        this.fileFormatPositionCB = fileFormatPositionCB;
    }

    public RichSelectOneRadio getFileFormatPositionCB() {
        return fileFormatPositionCB;
    }

    public void setFileOptionPositionUI(RichSelectOneRadio fileOptionPositionUI) {
        this.fileOptionPositionUI = fileOptionPositionUI;
    }

    public RichSelectOneRadio getFileOptionPositionUI() {
        return fileOptionPositionUI;
    }

}
