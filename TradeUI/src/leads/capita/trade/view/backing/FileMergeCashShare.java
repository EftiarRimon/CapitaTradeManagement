package leads.capita.trade.view.backing;

/*Created by : Ipsheta Saha */

import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.sql.SQLException;
import java.sql.Timestamp;

import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import javax.xml.parsers.DocumentBuilderFactory;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.exception.handler.CustomExceptionHandling;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.file.DataDump;
import leads.capita.trade.file.FlexTradeFileUtil;

import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;
import oracle.adf.view.rich.component.rich.input.RichSelectOneRadio;
import oracle.adf.view.rich.context.AdfFacesContext;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.binding.AttributeBinding;
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
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class FileMergeCashShare {
    FacesContext fct;
    private UploadedFile _fileClient;
    private UploadedFile _fileClientControl;
    private UploadedFile _fileShare;
    private UploadedFile _fileShareControl;
    private String currentFilePathClient = null;
    private String currentFilePathClientControl = null;
    private String currentFilePathPosition = null;
    private String currentFilePathPositionControl = null;
    private RichSelectOneRadio fileFormatPositionCB;
    private RichSelectOneRadio fileFormatClientCB;
    private RichSelectOneRadio fileOptionClientUI;
    private RichSelectOneRadio fileOptionPositionUI;

    public FileMergeCashShare() {
        super();
        fct = JSFUtils.getFacesContextApp();
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

    public void fileSelectCtrlCashValChngListener(ValueChangeEvent valueChangeEvent) {
        _fileClientControl = (UploadedFile)valueChangeEvent.getNewValue();
        this.setFileClientControl(_fileClientControl);
        if (!_fileClientControl.getContentType().equals("text/xml")) {
            _fileClientControl = null;
            JSFUtils.addFacesErrorMessage("Upload Valid Control File");
        }
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
        isNewRow = this.isNewStateRow("FtiFileMergeCashVOIterator");
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
        DCIteratorBinding cashIter = ADFUtils.findIterator("FtiFileMergeCashVOIterator");
        Row curRow = cashIter.getViewObject().getCurrentRow();
        curRow.setAttribute("FileId", fileId);
        curRow.setAttribute("FileVersion", fileVersion);
        curRow.setAttribute("FileFormat", fileFormat);
        curRow.setAttribute("FileOption", fileOption);
        this.saveRecord();
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

    public boolean isNewPosition() {
        boolean isNewRow = false;
        isNewRow = this.isNewStateRow("FtiFileMergeShareVOIterator");
        return isNewRow;
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
            fct.addMessage("Info!", new FacesMessage("Nothing To Save...."));
        }
        return null;
    }

    public void clientLimitUplaodFileAction(DialogEvent dialogEvent) throws ParseException, IOException, Exception {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            DCIteratorBinding cashIter = ADFUtils.findIterator("FtiFileMergeCashVOIterator");
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

    public String uploadClientTextFile() throws ParseException, IOException, Exception {
        String fileId = null;
        String fileName = null;
        String mbankShortName = null;
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
        DCIteratorBinding cashIter = ADFUtils.findIterator("FtiFileMergeCashVOIterator");
        DCIteratorBinding cashDetailIter = ADFUtils.findIterator("FtiFileMergeCashDetailVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("MClient");
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
        mbankShortName = FlexTradeFileUtil._getAttrValueFromIter("MbankActiveAllLOVIterator", "MbankShortName");
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
            cashDetailRow.setAttribute("MbankShortName", mbankShortName);
            cashDetailRow.setAttribute("InvestorCode", words[0]);
            cashDetailRow.setAttribute("Boid", words[1]);
            cashDetailRow.setAttribute("CompanyName", words[2]);
            cashDetailRow.setAttribute("IsForeign", words[3]);
            cashDetailRow.setAttribute("BuyLimit", words[4]);
            cashDetailRow.setAttribute("PanicWithdraw", words[5]);
            //cashDetailRow.setAttribute("FtiStatus", "Active");
            /* String appDate = words[2];
            Timestamp appDt = FlexTradeFileUtil.getSystemDateTimeStampWithTime(appDate);
            cashDetailRow.setAttribute("AppDate", appDt); */
        }
        //Close the input stream
        br.close();
        this.saveRecord();
        this.refreshIterator("FtiFileMergeCashDetailVOIterator");
        this.refreshIterator("OmnibusCashMbankVOIterator");
        return null;
    }

    public String uploadClientXMLFile() throws ParseException, IOException, Exception {
        String clientCodeReg = null;
        String fileId = null;
        String mbankShortName = null;
        String fileName = null;
        boolean isValidFile = false;
        UploadedFile file = this.getFile();
        UploadedFile fileCtrl = this.getFileClientControl();
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
        DCIteratorBinding cashIter = ADFUtils.findIterator("FtiFileMergeCashVOIterator");
        ViewObject cashVO = cashIter.getViewObject();
        DCIteratorBinding cashDetailIter = ADFUtils.findIterator("FtiFileMergeCashDetailVOIterator");
        ViewObject cashDetailVO = cashDetailIter.getViewObject();
        DCIteratorBinding limitsWithMarketIter = ADFUtils.findIterator("OmnibusClientLimitWithMarketIterator");
        ViewObject limitsWithMarketVo = limitsWithMarketIter.getViewObject();
        mbankShortName = FlexTradeFileUtil._getAttrValueFromIter("MbankActiveAllLOVIterator", "MbankShortName");

        if (cashVO.getCurrentRow().getAttribute("FileId") != null) {
            fileId = cashVO.getCurrentRow().getAttribute("FileId").toString();
        }

        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("MClient");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        SAXBuilder saxbuilder = new SAXBuilder();
        currentFilePathClient = destFolderPath + File.separator + file.getFilename();

        if (fileCtrl != null) {
            FlexTradeFileUtil.fileUploadAndSave(destFolderPath, fileCtrl.getFilename(), fileCtrl);
            currentFilePathClientControl = destFolderPath + File.separator + fileCtrl.getFilename();
        }

        File newGenFile = new File(currentFilePathClient);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        //Xml & ctrl Validation method call
        isValidFile = this.validateXmlClientWithControl(currentFilePathClient, currentFilePathClientControl);
        if (!isValidFile) {
            JSFUtils.addFacesErrorMessage("Control Validation Failed..");
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
                        cashDetailRow.setAttribute("MbankShortName", mbankShortName);
                        cashDetailRow.setAttribute("FileName", fileName);
                        cashDetailRow.setAttribute("FtiStatus", "New");
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
                            cashDetRowDeactiv.setAttribute("MbankShortName", mbankShortName);
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
                            newCashDetailRow.setAttribute("MbankShortName", mbankShortName);
                            newCashDetailRow.setAttribute("FileName", fileName);
                            newCashDetailRow.setAttribute("FtiStatus", "Active");
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
                        } else if (suspendNode.getName().equalsIgnoreCase("Sell_Suspend")) {
                            if (suspendNode.getText().equalsIgnoreCase("Resume"))
                                cashDetRowSuspend.setAttribute("FtiStatus", "Resume");
                            else
                                cashDetRowSuspend.setAttribute("FtiStatus", "Suspend");
                        } else if (suspendNode.getName().equalsIgnoreCase("Buy_Suspend")) {
                            if (suspendNode.getText().equalsIgnoreCase("Resume"))
                                cashDetRowSuspend.setAttribute("FtiStatus", "Resume");
                            else
                                cashDetRowSuspend.setAttribute("FtiStatus", "Suspend");
                        }
                        cashDetRowSuspend.setAttribute("FileId", fileId);
                        cashDetRowSuspend.setAttribute("MbankShortName", mbankShortName);
                        cashDetRowSuspend.setAttribute("FileName", fileName);
                    }

                }  else if (e.getName().equalsIgnoreCase("LimitsWithMarket")) {
                                    Row limitsWithMarketRow = null;
                                    String cCode = null;
                                    DCIteratorBinding investorAcconutIter = ADFUtils.findIterator("InvestorAccountListVOIterator");
                                    ViewObject investorAcconutVo = investorAcconutIter.getViewObject();
                                    List<Element> limitList = e.getChildren();
                                    for (int i = 0; i < limitList.size(); i++) { //children of limit

                                        Element limitNode = (Element)limitList.get(i);
                                        String market = null;
                                        if (limitNode.getName().equalsIgnoreCase("Market")) {
                                            market =
                                                limitNode.getAttribute("Market").getValue().toString();
                                        }
                                        
                                        if (limitNode.getName().equalsIgnoreCase("ClientCode")) {
                                            cCode = limitNode.getText();
                                        }
                                        if (!regInvestor.containsKey(cCode)) { 
                                            if (limitNode.getName().equalsIgnoreCase("ClientCode")) {
                                                limitsWithMarketRow = limitsWithMarketVo.createRow();
                                            }
                                            
                                            limitsWithMarketRow.setAttribute("BoardRefCode", market);
                                            
                                            if (limitNode.getName().equalsIgnoreCase("ClientCode")) {
                                                limitsWithMarketRow.setAttribute("InvestorCode",
                                                                              limitNode.getText() == null ? null : limitNode.getText());
                                                Row investorAccountRow = investorAcconutVo.getCurrentRow();
                                                limitsWithMarketRow.setAttribute("AccountId",investorAccountRow.getAttribute("AccountId"));
                                            }  else if (limitNode.getName().equalsIgnoreCase("MaxCapitalBuy")) {
                                                limitsWithMarketRow.setAttribute("MaxCapitalBuy",
                                                                              limitNode.getText() == null ? null : limitNode.getText());
                                            }
                                        }
                                    }
                                } 
            } //End of XML File
            this.saveRecord();
            this.refreshIterator("OmnibusCashMbankVOIterator");

        } catch (Exception e) {
            e.printStackTrace();
            // JSFUtils.getBindings().getOperationBinding("Rollback").execute();
            JSFUtils.addFacesErrorMessage("Problem in File Processing...");
        }
        return null;
    }

    private boolean validateXmlClientWithControl(String xmlFilePath,
                                                 String controlFilePath) throws FileNotFoundException, JDOMException,
                                                                                IOException {
        boolean isValid = false;
        String xmlHash = null;
        String controlHash = null;
        //Check whether control validation required
        String ctrlValidation =
            FlexTradeFileUtil._getAttrValueFromIter("FtiConfigParamOmnibusClientVOIterator", "ControlValidation");
        if (ctrlValidation == null || ctrlValidation.equalsIgnoreCase("N")) { //control validation not req.
            isValid = true;
            return isValid;
        }
        if (ctrlValidation.equalsIgnoreCase("Y") &&
            controlFilePath == null) { //control validation req. but ctrl file not uploaded
            JSFUtils.addFacesErrorMessage("Control File Validation Required!!");
            isValid = false;
            return isValid;
        }
        File newGenCtrlFile = new File(controlFilePath);
        if (!newGenCtrlFile.exists() || newGenCtrlFile.length() < 1) {
            JSFUtils.addFacesErrorMessage("Control File upload fail!!");
            isValid = false;
            return isValid;
        }

        if (xmlFilePath != null && controlFilePath != null) {
            xmlHash = FlexTradeFileUtil.getMD5HashContentForFile(xmlFilePath);
            // System.out.println("file hash----"+xmlHash);
            controlHash = FlexTradeFileUtil.getControllAttrValue(controlFilePath, "Hash");
            //  System.out.println("control hash----"+controlHash);
            if (xmlHash.trim().equals(controlHash.trim())) //if both hash matches then valid file
                isValid = true;
            else
                isValid = false;
        } else {
            JSFUtils.addFacesErrorMessage("File Path Not Found..");
            isValid = false;
        }
        return isValid;
    }

    //get number of unregistered investor for particular mbank of selected file

    public void mbankChangeForCashValChngListener(ValueChangeEvent valueChangeEvent) {
        String mbankShortName = valueChangeEvent.getNewValue().toString();
        ViewObject unRegInvVO = ADFUtils.findIterator("OmnubusCashUnRegInvestorVOIterator").getViewObject();
        unRegInvVO.setNamedWhereClauseParam("mbankShortName", mbankShortName);
        unRegInvVO.executeQuery();
        unRegInvVO.setWhereClause(null);

    }

    public void prodChangeForCashValChngListener(ValueChangeEvent valueChangeEvent) {
        try {
            String prodId = valueChangeEvent.getNewValue().toString();
            ViewObject subProdVO = ADFUtils.findIterator("SubProductByProductLOVIterator").getViewObject();
            subProdVO.setNamedWhereClauseParam("productId", prodId);
            subProdVO.executeQuery();
            subProdVO.setWhereClause(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createInvFromCashLimitDialogListener(DialogEvent dialogEvent) throws Exception {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            cbCreateInvestorFromCashLimitFile();
        }
    }

    public String cbCreateInvestorFromCashLimitFile() throws Exception {
        try {
            Row fileHeaderRow = ADFUtils.findIterator("FtiFileMergeCashVOIterator").getViewObject().getCurrentRow();
            String fileId = fileHeaderRow.getAttribute("FileId").toString();
            String fileType = fileHeaderRow.getAttribute("FileType").toString();
            Row r = ADFUtils.findIterator("OmnibusInvRegDummyVOIterator").getViewObject().getCurrentRow();
            String mbankShortName =
                FlexTradeFileUtil._getAttrValueFromIter("MbankActiveAllLOVIterator", "MbankShortName");
            String orgBrId = FlexTradeFileUtil._getAttrValueFromIter("BranchLOVIterator", "BranchId");
            String productId = r.getAttribute("ProductId").toString();
            String subProductId =
                FlexTradeFileUtil._getAttrValueFromIter("SubProductByProductLOVIterator", "SubProductId");
            String traderId = FlexTradeFileUtil._getAttrValueFromIter("TraderIdLOVIterator", "FtiTwsNo");

            /* System.out.println("mbank--" + mbankShortName);
            System.out.println("branch--" + orgBrId);
            System.out.println("prod--" + productId);
            System.out.println("subProd--" + subProductId);
            System.out.println("Trader--" + traderId); */
            new TMPlsqlExecutor().createInvFromOmnibusLimitFileData(fileId, fileType, mbankShortName, orgBrId,
                                                                    productId, subProductId, traderId);
            ApplicationInfo.getCurrentUserDBTransaction().commit();
            this.refreshIterator("OmnubusCashUnRegInvestorVOIterator");
            JSFUtils.addFacesInformationMessage("Investor Created Successfully for " + mbankShortName);
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage("Process Failed !");
            throw e;
        }
        return null;
    }

    public String processCashFile() throws Exception {
        boolean isProcessCall = false;
        String fileId = null;
        String fileFormat = null;
        String fileOption = null;
        try {
            DCIteratorBinding cashIter = ADFUtils.findIterator("FtiFileMergeCashVOIterator");
            if (cashIter.getCurrentRow().getAttribute("FileId") != null &&
                cashIter.getCurrentRow().getAttribute("FileFormat") != null &&
                cashIter.getCurrentRow().getAttribute("FileOption") != null) {
                fileId = cashIter.getCurrentRow().getAttribute("FileId").toString();
                fileFormat = cashIter.getCurrentRow().getAttribute("FileFormat").toString().toUpperCase();
                fileOption = cashIter.getCurrentRow().getAttribute("FileOption").toString();
                // System.out.println("process cash--------" + fileId);
                DataDump dataDump = new DataDump();
                isProcessCall = dataDump.dumpMergeCashFileData(fileId, fileFormat, fileOption, "MBANK");
            }

            if (isProcessCall) {
                cashIter.getCurrentRow().setAttribute("IsExec", true);
                JSFUtils.getBindings().getOperationBinding("Commit").execute();
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                this.refreshIterator("FtiFileMergeCashDetailVOIterator");
                ADFUtils.findIterator("FtiOmnibusMergeCashDataVOIterator").executeQuery(); //to get processed data (from view data button)
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

    public void popUpDelClientMbankDialogListener(DialogEvent dialogEvent) {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            deleteClientMbankRecords();
        }
    }

    private boolean isClientDeleteValid(String fileId, String mbankShortName) {
        ViewObject omCashProcVo = ADFUtils.findIterator("OmnibusCashLimitProcessedDataIterator").getViewObject();
        omCashProcVo.setNamedWhereClauseParam("file_id", fileId);
        omCashProcVo.setNamedWhereClauseParam("mbank_short_name", mbankShortName);
        omCashProcVo.executeQuery();
        omCashProcVo.setWhereClause(null);
        if (omCashProcVo.getAllRowsInRange().length == 0) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public String deleteClientMbankRecords() {
        try {
            String message = null;
            String fileId = FlexTradeFileUtil._getAttrValueFromIter("FtiFileMergeCashVOIterator", "FileId");
            String mbankShortName =
                FlexTradeFileUtil._getAttrValueFromIter("OmnibusCashMbankVOIterator", "MbankShortName");
            if (isClientDeleteValid(fileId, mbankShortName)) {
                ApplicationInfo.getCurrentUserDBTransaction().executeCommand("delete from fti_file_merge_cash_detail t where t.file_id = '" +
                                                                             fileId + "' and t.mbank_short_name = '" +
                                                                             mbankShortName + "'");
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                message = "Record(s) Deleted Successfully..";
            } else {
                message = "Cannot delete file. Processed record(s) found !";
            }
            JSFUtils.addFacesInformationMessage(message);
            this.refreshIterator("OmnibusCashMbankVOIterator");
            this.refreshIterator("FtiFileMergeCashDetailVOIterator");
            //to disable radio button after deletion(mbank)
            /* if (iter != null) {
                            Row r = iter.getCurrentRow();
                            if (r != null) {
                                r.refresh(Row.REFRESH_UNDO_CHANGES);
                            }
                        }
                        AdfFacesContext.getCurrentInstance().addPartialTarget(fileOptionClientUI);
                        AdfFacesContext.getCurrentInstance().addPartialTarget(fileFormatClientCB); */

        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage("Error Occured !!");
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
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
        DCIteratorBinding shareIter = ADFUtils.findIterator("FtiFileMergeShareVOIterator");
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

    public void fileSelectCtrlPositionValChngListener(ValueChangeEvent valueChangeEvent) {
        _fileShareControl = (UploadedFile)valueChangeEvent.getNewValue();
        this.setFileShareControl(_fileShareControl);
        if (!_fileShareControl.getContentType().equals("text/xml")) {
            _fileShareControl = null;
            JSFUtils.addFacesErrorMessage("Upload Valid Control File");
        }
    }

    public void positionLimitUploadfileAction(DialogEvent dialogEvent) throws ParseException, IOException, Exception {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            DCIteratorBinding shareIter = ADFUtils.findIterator("FtiFileMergeShareVOIterator");
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
        String mbankShortName = null;
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
        DCIteratorBinding shareIter = ADFUtils.findIterator("FtiFileMergeShareVOIterator");
        DCIteratorBinding shareDetailIter = ADFUtils.findIterator("FtiFileMergeShareDetailVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("MPosition");
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
        mbankShortName = FlexTradeFileUtil._getAttrValueFromIter("MbankActiveAllLOVIterator", "MbankShortName");

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
            shareDetailRow.setAttribute("MbankShortName", mbankShortName);
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
        this.refreshIterator("FtiFileMergeShareDetailVOIterator");
        this.refreshIterator("OmnibusShareMbankVOIterator");
        return null;
    }

    public String uploadPositionXMLFile() throws ParseException, IOException, Exception {
        // System.out.println("In Position XML-----");
        String fileId = null;
        String fileName = null;
        String mbankShortName = null;
        boolean isValidFile = false;
        UploadedFile file = this.getFile();
        UploadedFile fileCtrl = this.getFileShareControl();
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
        DCIteratorBinding shareIter = ADFUtils.findIterator("FtiFileMergeShareVOIterator");
        ViewObject shareVO = shareIter.getViewObject();
        DCIteratorBinding shareDetailIter = ADFUtils.findIterator("FtiFileMergeShareDetailVOIterator");
        ViewObject shareDetailVO = shareDetailIter.getViewObject();
        mbankShortName = FlexTradeFileUtil._getAttrValueFromIter("MbankActiveAllLOVIterator", "MbankShortName");

        if (shareVO.getCurrentRow().getAttribute("FileId") != null) {
            fileId = shareVO.getCurrentRow().getAttribute("FileId").toString();
        }

        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("MPosition");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        currentFilePathPosition = destFolderPath + File.separator + file.getFilename();
        SAXBuilder saxbuilder = new SAXBuilder();

        if (fileCtrl != null) {
            FlexTradeFileUtil.fileUploadAndSave(destFolderPath, fileCtrl.getFilename(), fileCtrl);
            currentFilePathPositionControl = destFolderPath + File.separator + fileCtrl.getFilename();
        }

        File newGenFile = new File(currentFilePathPosition);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        //Xml & ctrl Validation method call
        isValidFile = this.validateXmlPositionWithControl(currentFilePathPosition, currentFilePathPositionControl);
        if (!isValidFile) {
            JSFUtils.addFacesErrorMessage("Control Validation Failed..");
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
                            String part1 = null;
                            String part2 = null;
                            if(node.getText() != null){
                                String[] parts = node.getText().split("\\.");
                                part1 = parts[0];
                                if(parts.length > 1)
                                     part2 = parts[1];
                            }
                            
                            shareDetailRow.setAttribute("InstShortName",
                                                        node.getText() == null ? null : part1);
                        } else if (node.getName().equalsIgnoreCase("ISIN")) {
                            shareDetailRow.setAttribute("Isin", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("Quantity")) {
                            shareDetailRow.setAttribute("SalableQuantity",
                                                        node.getText() == null ? 0 : node.getText());
                        }
                        shareDetailRow.setAttribute("FileId", fileId);
                        shareDetailRow.setAttribute("FileName", fileName);
                        shareDetailRow.setAttribute("MbankShortName", mbankShortName);
                    }
                }
            }
            this.saveRecord();
            this.refreshIterator("OmnibusShareMbankVOIterator");
        } catch (Exception e) {
            e.printStackTrace();
            // JSFUtils.getBindings().getOperationBinding("Rollback").execute();
            JSFUtils.addFacesErrorMessage("Problem in File Processing...");
        }
        return null;
    }

    private boolean validateXmlPositionWithControl(String xmlFilePath,
                                                   String controlFilePath) throws FileNotFoundException, JDOMException,
                                                                                  IOException {
        boolean isValid = false;
        String xmlHash = null;
        String controlHash = null;
        //Check whether control validation required
        String ctrlValidation =
            FlexTradeFileUtil._getAttrValueFromIter("FtiConfigParamOmnibusPositionVOIterator", "ControlValidation");
        if (ctrlValidation == null || ctrlValidation.equalsIgnoreCase("N")) { //control validation not req.
            isValid = true;
            return isValid;
        }
        if (ctrlValidation.equalsIgnoreCase("Y") &&
            controlFilePath == null) { //control validation req. but ctrl file not uploaded
            JSFUtils.addFacesErrorMessage("Control File Validation Required!!");
            isValid = false;
            return isValid;
        }
        File newGenCtrlFile = new File(controlFilePath);
        if (!newGenCtrlFile.exists() || newGenCtrlFile.length() < 1) {
            JSFUtils.addFacesErrorMessage("Control File upload fail!!");
            isValid = false;
            return isValid;
        }

        if (xmlFilePath != null && controlFilePath != null) {
            xmlHash = FlexTradeFileUtil.getMD5HashContentForFile(xmlFilePath);
            // System.out.println("file hash1----"+xmlHash);
            controlHash = FlexTradeFileUtil.getControllAttrValue(controlFilePath, "Hash");
            //  System.out.println("control hash1----"+controlHash);
            if (xmlHash.trim().equals(controlHash.trim())) //if both hash matches then valid file
                isValid = true;
            else
                isValid = false;
        } else {
            JSFUtils.addFacesErrorMessage("File Path Not Found..");
            isValid = false;
        }
        return isValid;
    }

    public void mbankChangeForShareValChngListener(ValueChangeEvent valueChangeEvent) {
        String mbankShortName = valueChangeEvent.getNewValue().toString();
        ViewObject unRegInvVO = ADFUtils.findIterator("OmnibusShareUnRegInvestorVOIterator").getViewObject();
        unRegInvVO.setNamedWhereClauseParam("mbankShortName", mbankShortName);
        unRegInvVO.executeQuery();
        unRegInvVO.setWhereClause(null);
    }

    public void prodChangeForShareValChngListener(ValueChangeEvent valueChangeEvent) {
        try {
            String prodId = valueChangeEvent.getNewValue().toString();
            ViewObject subProdVO = ADFUtils.findIterator("SubProductByProductLOVIterator").getViewObject();
            subProdVO.setNamedWhereClauseParam("productId", prodId);
            subProdVO.executeQuery();
            subProdVO.setWhereClause(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createInvFromShareLimitDialogListener(DialogEvent dialogEvent) throws Exception {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            cbCreateInvestorFromShareLimitFile();
        }
    }

    public String cbCreateInvestorFromShareLimitFile() throws Exception {
        try {
            Row fileHeaderRow = ADFUtils.findIterator("FtiFileMergeShareVOIterator").getViewObject().getCurrentRow();
            String fileId = fileHeaderRow.getAttribute("FileId").toString();
            //FlexTradeFileUtil._getAttrValueFromIter("FtiFileMergeShareVOIterator", "FileId");
            String fileType = fileHeaderRow.getAttribute("FileType").toString();
            Row r = ADFUtils.findIterator("OmnibusInvRegDummyVOIterator").getViewObject().getCurrentRow();
            String mbankShortName =
                FlexTradeFileUtil._getAttrValueFromIter("MbankActiveAllLOVIterator", "MbankShortName");
            String orgBrId = FlexTradeFileUtil._getAttrValueFromIter("BranchLOVIterator", "BranchId");
            String productId = r.getAttribute("ProductId").toString();
            String subProductId =
                FlexTradeFileUtil._getAttrValueFromIter("SubProductByProductLOVIterator", "SubProductId");
            String traderId = FlexTradeFileUtil._getAttrValueFromIter("TraderIdLOVIterator", "FtiTwsNo");

            /* System.out.println("mbank--" + mbankShortName);
            System.out.println("branch--" + orgBrId);
            System.out.println("prod--" + productId);
            System.out.println("subProd--" + subProductId);
            System.out.println("Trader--" + traderId); */
            new TMPlsqlExecutor().createInvFromOmnibusLimitFileData(fileId, fileType, mbankShortName, orgBrId,
                                                                    productId, subProductId, traderId);
            ApplicationInfo.getCurrentUserDBTransaction().commit();
            this.refreshIterator("OmnibusShareUnRegInvestorVOIterator");
            JSFUtils.addFacesInformationMessage("Investor Created Successfully for " + mbankShortName);
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage("Process Failed !");
            throw e;
        }
        return null;
    }

    public String ProcessShareFile() throws Exception {
        boolean isProcessCall = false;
        String fileId = null;
        String fileFormat = null;
        String fileOption = null;
        try {
            DCIteratorBinding shareIter = ADFUtils.findIterator("FtiFileMergeShareVOIterator");
            if (shareIter.getCurrentRow().getAttribute("FileId") != null &&
                shareIter.getCurrentRow().getAttribute("FileFormat") != null &&
                shareIter.getCurrentRow().getAttribute("FileOption") != null) {
                fileId = shareIter.getCurrentRow().getAttribute("FileId").toString();
                fileFormat = shareIter.getCurrentRow().getAttribute("FileFormat").toString().toUpperCase();
                fileOption = shareIter.getCurrentRow().getAttribute("FileOption").toString();
                // System.out.println("process share--------" + fileId);
                DataDump dataDump = new DataDump();
                isProcessCall = dataDump.dumpMergeShareFileData(fileId, fileFormat, fileOption, "MBANK");
            }

            if (isProcessCall) {
                shareIter.getCurrentRow().setAttribute("IsExec", true);
                JSFUtils.getBindings().getOperationBinding("Commit").execute();
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                this.refreshIterator("FtiFileMergeShareDetailVOIterator");
                //this.refreshIterator("FtiFileMergeShareVOIterator");
                ADFUtils.findIterator("FtiOmnibusMergeShareDataVOIterator").executeQuery(); //to get processed data(from view data button)
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

    public void popUpDelPositionMbankDialogListener(DialogEvent dialogEvent) {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            deletePositionMbankRecords();
        }
    }

    private boolean isPositionDeleteValid(String fileId, String mbankShortName) {
        ViewObject omPositionProcVo = ADFUtils.findIterator("OmnibusShareLimitProcessedDataIterator").getViewObject();
        omPositionProcVo.setNamedWhereClauseParam("file_id", fileId);
        omPositionProcVo.setNamedWhereClauseParam("mbank_short_name", mbankShortName);
        omPositionProcVo.executeQuery();
        omPositionProcVo.setWhereClause(null);
        if (omPositionProcVo.getAllRowsInRange().length == 0) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private String deletePositionMbankRecords() {
        try {
            String message = null;
            String fileId = FlexTradeFileUtil._getAttrValueFromIter("FtiFileMergeShareVOIterator", "FileId");
            String mbankShortName =
                FlexTradeFileUtil._getAttrValueFromIter("OmnibusShareMbankVOIterator", "MbankShortName");
            if (isPositionDeleteValid(fileId, mbankShortName)) {
                ApplicationInfo.getCurrentUserDBTransaction().executeCommand("delete from fti_file_merge_share_detail t where t.file_id = '" +
                                                                             fileId + "' and t.mbank_short_name = '" +
                                                                             mbankShortName + "'");
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                message = "Record(s) Deleted Successfully...";
            } else {
                message = "Cannot delete file. Processed record(s) found !";
            }
            JSFUtils.addFacesInformationMessage(message);
            this.refreshIterator("OmnibusShareMbankVOIterator");
            this.refreshIterator("FtiFileMergeShareDetailVOIterator");
            //to disable radio button after deletion(mbank)
            /*  if (iter != null) {
                            Row r = iter.getCurrentRow();
                            if (r != null) {
                                r.refresh(Row.REFRESH_UNDO_CHANGES);
                            }
                        }
                        AdfFacesContext.getCurrentInstance().addPartialTarget(fileFormatPositionCB);
                        AdfFacesContext.getCurrentInstance().addPartialTarget(fileOptionPositionUI); */
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage("Error Occoured !");
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
        }
        return null;
    }

    private Timestamp getSystemDateTimeStampWithTimeNew(String sysDate) {
        Timestamp dts = null;
        try {
            SimpleDateFormat sf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
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

    public void setFile(UploadedFile _fileCash) {
        this._fileClient = _fileCash;
    }

    public UploadedFile getFile() {
        return _fileClient;
    }

    public void setFileClientControl(UploadedFile _fileClientControl) {
        this._fileClientControl = _fileClientControl;
    }

    public UploadedFile getFileClientControl() {
        return _fileClientControl;
    }

    public void setFileShare(UploadedFile _fileShare) {
        this._fileShare = _fileShare;
    }

    public UploadedFile getFileShare() {
        return _fileShare;
    }

    public void setFileShareControl(UploadedFile _fileShareControl) {
        this._fileShareControl = _fileShareControl;
    }

    public UploadedFile getFileShareControl() {
        return _fileShareControl;
    }

    public void setFileFormatPositionCB(RichSelectOneRadio fileFormatPositionCB) {
        this.fileFormatPositionCB = fileFormatPositionCB;
    }

    public RichSelectOneRadio getFileFormatPositionCB() {
        return fileFormatPositionCB;
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

    public void setFileOptionPositionUI(RichSelectOneRadio fileOptionPositionUI) {
        this.fileOptionPositionUI = fileOptionPositionUI;
    }

    public RichSelectOneRadio getFileOptionPositionUI() {
        return fileOptionPositionUI;
    }

}
