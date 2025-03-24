package leads.capita.trade.view.backing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import java.math.BigDecimal;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import javax.mail.internet.AddressException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import javax.xml.parsers.ParserConfigurationException;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.file.FTPUtils;
import leads.capita.trade.file.FlexTradeFileUtil;
import leads.capita.trade.file.PayInOutFileUtil;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.RichQuery;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.context.AdfFacesContext;
import oracle.adf.view.rich.event.DialogEvent;
import oracle.adf.view.rich.event.PopupFetchEvent;
import oracle.adf.view.rich.model.QueryDescriptor;
import oracle.adf.view.rich.model.QueryModel;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewCriteria;
import oracle.jbo.ViewCriteriaItem;
import oracle.jbo.ViewCriteriaItemHints;
import oracle.jbo.ViewCriteriaManager;
import oracle.jbo.ViewCriteriaRow;
import oracle.jbo.ViewObject;
import oracle.jbo.uicli.binding.JUCtrlListBinding;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.xml.sax.SAXException;


//import sun.net.ftp.FtpClient;


public class PositionLimitFile {

    private FacesContext fct;
    private String newFileName = null;
    private RichInputText generatedPositionFileUI;
    private RichInputText generatedPositionControlFileUI;
    static String positionFileName = null;
    static String positionControlFileName = null;
    private static final String Xsd_Url_Path = "flex-tradexsd/v7-10/";
    private static final String position_Xsd_Url_Path = "Flextrade-BOS-Positions.xsd";
    /* FlexTradeFileUtil.getHomeDirPath() + File.separator + "141016-123323-Positions-HAI.XML" */
    TMPlsqlExecutor tmplSql = new TMPlsqlExecutor();
    private RichQuery richPositionUIQuery;
    String bussinessType = ApplicationInfo.getBusinessType();

    public PositionLimitFile() {
        super();
        fct = JSFUtils.getFacesContextApp();

    }

    private DCIteratorBinding getIterator(String iterName) {
        BindingContext ctx = BindingContext.getCurrent();
        DCBindingContainer bc = (DCBindingContainer)ctx.getCurrentBindingsEntry();
        DCIteratorBinding iterator = bc.findIteratorBinding(iterName);
        return iterator;
    }

    public static Date getSysDate() {
        Date sDate = null;
        try {
            sDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return sDate;
    }

    public String generatePositionData() {
        String fileOption = null;
        String deactivateAll = "N";
        String appDate = new SimpleDateFormat("dd-MMM-yyyy").format(getSysDate());
        DCIteratorBinding positionParameterIter = getIterator("PositionParameterVOIterator");
        ViewObject positionParameterVo = positionParameterIter.getViewObject();
        Row positionParameterRow = positionParameterVo.getCurrentRow();
        if (positionParameterRow != null) {
            if (positionParameterRow.getAttribute("FileOption") != null &&
                !(positionParameterRow.getAttribute("FileOption").toString().equalsIgnoreCase(""))) {
                fileOption = positionParameterRow.getAttribute("FileOption").toString();
                try {
                    tmplSql.ftiPositionLimitProcCall(fileOption, appDate, deactivateAll);
                    ApplicationInfo.getCurrentUserDBTransaction().commit();
                    fct.addMessage("Complete Msg", new FacesMessage("Generate Position Data Successfully"));
                    refreshVersion();
                    executeLovIterator("VersionNo");


                } catch (Exception e) {
                    ApplicationInfo.getCurrentUserDBTransaction().rollback();
                    JSFUtils.addFacesErrorMessage(e.getMessage());
                    return null;
                }

            } else {
                JSFUtils.addFacesErrorMessage("File Option is Mandatory");
            }

        }

        return null;
    }

    public void executeLovIterator(String versionNo) {
        try {
            BindingContext bctx = BindingContext.getCurrent();
            BindingContainer bindings = bctx.getCurrentBindingsEntry();
            JUCtrlListBinding list = (JUCtrlListBinding)bindings.get(versionNo);
            if (list != null) {
                DCIteratorBinding iter = list.getListIterBinding();
                if (iter != null && iter.getEstimatedRowCount() > 0) {
                    iter.executeQuery();
                }
            }
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshVersion() {
        RichQuery queryComp = getRichPositionUIQuery();
        QueryModel queryModel = queryComp.getModel();
        QueryDescriptor queryDescriptor = queryComp.getValue();
        queryModel.reset(queryDescriptor);
        queryComp.refresh(FacesContext.getCurrentInstance());
        AdfFacesContext.getCurrentInstance().addPartialTarget(queryComp);

    }


    public String processPositionFile() throws JDOMException, ParseException, IOException {
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
        String shortName = null;
        //String bussinessType = ApplicationInfo.getBusinessType(); //"BROKER" ;
        if (bussinessType.equalsIgnoreCase("BROKER"))
            shortName = FlexTradeFileUtil._getAttrValueFromIter("BrokerLOVIterator", "ShortName");
        else
            shortName = FlexTradeFileUtil._getFirstRowAttrValueFromIter("PositionLimitVOIterator", "BrokerShortName");

        newFileName =
                destFolderPath + File.separator + FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.POSITIONS.getValue(),
                                                                                                   null);
        newFileName = newFileName + "-" + shortName + ".xml";

        File generatedfile = new File(newFileName);

        /*  positionFileName =
                FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.POSITIONS.getValue(),
                                                                 null) + "-" + shortName + ".xml";
        positionControlFileName = FlexTradeFileUtil.appendStringWithFileName(generatedfile.getName(), "-ctrl.xml"); */

        if (processedPosition(generatedfile)) {
            positionFileName = generatedfile.getName()+ "-" + shortName;
            positionFileName = FlexTradeFileUtil.appendStringWithFileName(positionFileName, ".xml");
            positionControlFileName = FlexTradeFileUtil.appendStringWithFileName(generatedfile.getName(), "-ctrl.xml");
            processPositionControl(generatedfile);
            fct.addMessage("Complete Msg", new FacesMessage("File Processing done Successfully"));
            //System.out.println("File Saved!");
        }
        
        return null;
    }


    private boolean isValid(Row row) {

        if (row.getAttribute("InvestorCode") == null ||
            (row.getAttribute("InvestorCode").toString().equalsIgnoreCase(""))) {
            JSFUtils.addFacesErrorMessage("Investor Code can not be empty!!");
            return false;
        }
        if (bussinessType.equalsIgnoreCase("MBANK")) {
            if (row.getAttribute("TradingCode") == null ||
                (row.getAttribute("TradingCode").toString().equalsIgnoreCase(""))) {
                JSFUtils.addFacesErrorMessage("Trading Code is Mandatory for (" + row.getAttribute("InvestorCode") +
                                              ")!");
                return false;
            }
        }
        
        row = null;
        return true;
    }

    private boolean processedPosition(File generatedfile) throws IOException, JDOMException {

        Map<String, String> nsNameValue = null;

        DCIteratorBinding positionXmlIter = getIterator("PositionLimitVOIterator");
        //positionXmlIter.setRangeSize((int)positionXmlIter.getEstimatedRowCount());
        Row[] rows = positionXmlIter.getAllRowsInRange();

        Long noOfLines1 = (positionXmlIter.getViewObject().getEstimatedRowCount());
        String nolines = noOfLines1.toString();
        Integer noOfLines = Integer.valueOf(nolines);

        String insShortNameWithBoard = "";
        String boardMarketType = "";
        
        if (noOfLines > 0) {
            try {
                nsNameValue = new HashMap<String, String>();
                String operation = FlexTradeFileUtil._getAttrValueFromIter("PositionLimitVOIterator", "Operation");
                if (operation.equalsIgnoreCase("I"))
                    //nsNameValue.put("ProcessingMode", "BatchInsert");
                    nsNameValue.put("ProcessingMode", "BatchInsertOrUpdate");
                else
                    nsNameValue.put("ProcessingMode", "IncrementQuantity");
                nsNameValue.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
                nsNameValue.put("xsi:noNamespaceSchemaLocation", "Flextrade-BOS-Positions.xsd");
                nsNameValue.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

                String rootElemenet = FlexTradeFileUtil.generateRootWithSchema("Positions", null, nsNameValue);

                Document doc = new SAXBuilder().build(new StringReader(rootElemenet));


                for (int i = 0; i < rows.length; i++) {
                    Row r = rows[i];


                    if (isValid(r)) {

                        Element insertOne = new Element("InsertOne");

                        if (bussinessType.equalsIgnoreCase("MBANK"))
                            insertOne.addContent(new Element("ClientCode").setText(r.getAttribute("TradingCode") ==
                                                                                   null ? null :
                                                                                   r.getAttribute("TradingCode").toString()));
                        else

                            insertOne.addContent(new Element("ClientCode").setText(r.getAttribute("InvestorCode") ==
                                                                                   null ? null :
                                                                                   r.getAttribute("InvestorCode").toString()));
                        
                        boardMarketType = r.getAttribute("BoardRefCode") == null ? null : r.getAttribute("BoardRefCode").toString();
                        
                        if(!boardMarketType.equalsIgnoreCase("MM")){
                            insShortNameWithBoard = r.getAttribute("ShortName") == null ? null : r.getAttribute("ShortName").toString() 
                                                                                                 + "."+ boardMarketType;
                        } else insShortNameWithBoard = r.getAttribute("ShortName") == null ? null : r.getAttribute("ShortName").toString();
                        
                        
                        insertOne.addContent(new Element("SecurityCode").addContent(new CDATA(insShortNameWithBoard)));
                        insertOne.addContent(new Element("ISIN").addContent(new CDATA(r.getAttribute("Isin") == null ?
                                                                                      null :
                                                                                      r.getAttribute("Isin").toString())));
                        Double qty = new Double(r.getAttribute("SalableQty").toString());
                        Integer qt=qty.intValue();
                        insertOne.addContent(new Element("Quantity").setText(r.getAttribute("SalableQty") == null ?
                                                                             "0" :
                                                                             qt.toString()));
                        Double salabl =
                            new Double(r.getAttribute("AvgCost") == null ? "0" : r.getAttribute("AvgCost").toString());
                        Double avgCost =
                            new Double(r.getAttribute("SalableQty") == null ? "0" : r.getAttribute("SalableQty").toString());
                        Double totalCost = salabl * avgCost;
                        DecimalFormat df = new DecimalFormat("#.#####");
                        Double totalCostFormated = totalCost.doubleValue();
                        insertOne.addContent(new Element("TotalCost").setText(df.format(totalCostFormated).toString()));
                        insertOne.addContent(new Element("PositionType").setText("Long"));


                        doc.getRootElement().addContent(insertOne);
                    } else {
                        return false;
                    }

                }
                XMLOutputter xmlOutput = new XMLOutputter();
                xmlOutput.setFormat(Format.getPrettyFormat());
                xmlOutput.output(doc, new FileWriter(generatedfile.getPath()));
                
                nsNameValue.clear();
                System.gc();
             
                xmlOutput = null;
                nsNameValue = null;
                doc = null;
                rows = null;
            } catch (IOException io) {
                nsNameValue = null;
                rows = null;
                System.out.println(io.getMessage());
            } catch (Exception e) {
                nsNameValue = null;
                rows = null;
                System.out.println(e.getMessage());
            }
        } else {
            fct.addMessage("Complete Msg", new FacesMessage("No Data to generate"));
            positionFileName = null;
            positionControlFileName = null;
            return false;
        }
        return true;
    }


    public String getNewGenFile() {
        return positionFileName;
    }

    public String getNewGenControlFile() {
        return positionControlFileName;
    }

    private void processPositionControl(File positionFile) throws FileNotFoundException, JDOMException, IOException {

        Map<String, String> nsNameValue = null;

        String positionFileHashCode = FlexTradeFileUtil.getMD5HashContentForFile(positionFile.getPath());
        nsNameValue = new HashMap<String, String>();
        nsNameValue.put("Hash", positionFileHashCode);
        nsNameValue.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        nsNameValue.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        nsNameValue.put("xsi:noNamespaceSchemaLocation", "Flextrade-BOS-Control.xsd");
        nsNameValue.put("Method", "MD5");

        File generatedControlfileName =
            new File(FlexTradeFileUtil.appendStringWithFileName(positionFile.getName(), "-ctrl.xml"));

        String rootElemenet = FlexTradeFileUtil.generateRootWithSchema("Control", null, nsNameValue);

        Document doc = new SAXBuilder().build(new StringReader(rootElemenet));
        FileWriter fw = new FileWriter(positionFile.getParent() + File.separator + generatedControlfileName.getName());
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc,
                         fw);
        
        fw.close();
        xmlOutput = null;
        doc = null;

    }

    public void generatedPositionListener(FacesContext facesContext, OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
        File home_dirFile =
            new File(destFolderPath + File.separator + getGeneratedPositionFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + getGeneratedPositionFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            //this.setFileName(f.getName());
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
            fdownload.close();
            outputStream.close();
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        } catch (NullPointerException e) {
            JSFUtils.addFacesErrorMessage("No File Found !");
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
    }


    public void generatedPositionControlListener(FacesContext facesContext,
                                                 OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
        File home_dirFile =
            new File(destFolderPath + File.separator + getGeneratedPositionControlFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + getGeneratedPositionControlFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            //this.setFileName(f.getName());
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
            fdownload.close();
            outputStream.close();
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        } catch (NullPointerException e) {
            JSFUtils.addFacesErrorMessage("No File Found !");
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
    }

    public void setGeneratedPositionFileUI(RichInputText generatedPositionFileUI) {
        this.generatedPositionFileUI = generatedPositionFileUI;
    }

    public RichInputText getGeneratedPositionFileUI() {
        return generatedPositionFileUI;
    }

    public void setGeneratedPositionControlFileUI(RichInputText generatedPositionControlFileUI) {
        this.generatedPositionControlFileUI = generatedPositionControlFileUI;
    }

    public RichInputText getGeneratedPositionControlFileUI() {
        return generatedPositionControlFileUI;
    }

    public String sendFileThroughFTP() throws IOException, AddressException {

        FTPClient ftpClient = new FTPClient();
        FTPClient ftpClient2 = new FTPClient();
        boolean isSuccess = false;
        if (!ftpClient.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
        }

        if (!ftpClient2.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            FTPUtils.ftpConnect(ftpClient2, ftpHost, ftpUser, ftpPass, isActiveMode);
        }

        try {
            //URLConnection remoteFtp=FTPUtils.getFtpByURLConnection(userName, hostName, password, null)
            String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
            //FTPUtils.ftpConnect(ftpClient, "192.168.20.107", "akila", "akila");
            File home_dirFile =
                new File(destFolderPath + File.separator + getGeneratedPositionFileUI().getValue().toString());
            File home_dirFileControl =
                new File(destFolderPath + File.separator + getGeneratedPositionControlFileUI().getValue().toString());

            if (ftpClient != null)
                ftpClient.setDefaultTimeout(20000);

            //ftpClient.changeWorkingDirectory("/positions");
            boolean isTrasfered = FTPUtils.uploadFile(ftpClient, home_dirFile.getPath(), null);

            System.out.println("isTrasfered---" + isTrasfered);
            //if (isTrasfered)
            //  ftpClient.disconnect();
            // FTPUtils.ftpConnect(ftpClient, "192.168.20.107", "mohin", "mainuddin");


            /*  boolean success1 = ftpClient.completePendingCommand();
            boolean success2 = ftpClient2.completePendingCommand(); */
            if (isTrasfered) {
                ftpClient.logout();
                if (ftpClient != null && ftpClient.isConnected()) {
                    ftpClient.disconnect();
                    isSuccess = true;
                }
            }

            Thread.currentThread();
            Thread.currentThread().sleep(10000);

            if (ftpClient2 != null)
                ftpClient2.setDefaultTimeout(30000);

            boolean isTrasfered2 = FTPUtils.uploadFile(ftpClient2, home_dirFileControl.getPath(), null);
            System.out.println("isTrasfered2---" + isTrasfered);
            if (isTrasfered2) {
                ftpClient2.logout();
                if (ftpClient2 != null && ftpClient2.isConnected()) {
                    ftpClient2.disconnect();
                    isSuccess = true;
                }
            }

            /* ftpClient.getReplyCode();
            ftpClient2.getReplyCode(); */
            /* if (isTrasfered && isTrasfered2) {
                if (ftpClient != null && ftpClient.isConnected())
                    ftpClient.disconnect();
                if (ftpClient2 != null && ftpClient2.isConnected())
                    ftpClient2.disconnect();
            } */
            if (isSuccess)
                fct.addMessage("Complete Msg", new FacesMessage("File Send Successfully"));
        } catch (Exception e) {
            if (ftpClient != null && ftpClient.isConnected())
                ftpClient.disconnect();
            if (ftpClient2 != null && ftpClient2.isConnected())
                ftpClient2.disconnect();
            fct.addMessage("Complete Msg", new FacesMessage("File Send Error"));
            e.printStackTrace();
        }

        return null;
    }

    public String validatePositionFileWithXsd() throws ParseException, ParserConfigurationException, IOException,
                                                       SAXException {

        try {
            boolean isValidate = false;
            String generatedPositionFilePath = null;
            String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
            String destXsdFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Xsd");
            if (getGeneratedPositionFileUI().getValue() != null) {
                HttpServletRequest request =
                    (HttpServletRequest)JSFUtils.getFacesContext().getExternalContext().getRequest();
                generatedPositionFilePath =
                        destFolderPath + File.separator + getGeneratedPositionFileUI().getValue().toString();
                //String clientFileXsdPath = destFolderPath + File.separator + "Flextrade-BOS-Positions.xsd";
                String urlPath =
                    FlexTradeFileUtil.getURLWithContextPath(request) + "/" + Xsd_Url_Path + position_Xsd_Url_Path;

                String positionFileXsdPath = destXsdFolderPath + File.separator + position_Xsd_Url_Path;
                FlexTradeFileUtil.downloadUrlFileUsingStream(urlPath, positionFileXsdPath);
                isValidate =
                        FlexTradeFileUtil.validateWithExtXSDUsingSAX(generatedPositionFilePath, positionFileXsdPath);
                if (isValidate)
                    fct.addMessage("Complete Msg", new FacesMessage("File Validate Successfully"));
                else
                    fct.addMessage("Complete Msg", new FacesMessage("File Validate Error"));
            } else
                fct.addMessage("Complete Msg", new FacesMessage("No File Found To Validate"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void setRichPositionUIQuery(RichQuery richPositionUIQuery) {
        this.richPositionUIQuery = richPositionUIQuery;
    }

    public RichQuery getRichPositionUIQuery() {
        return richPositionUIQuery;
    }

    public void showPositionFileDialogListener(DialogEvent dialogEvent) throws IOException {

        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        }

    }

    public void showPositionFileFetchListener(PopupFetchEvent popupFetchEvent) throws IOException {
        int ctr = 0;
        //Map<String, String> fileTypeMap =new HashMap<String, String>();
        //fileTypeMap.put(fileTypeRadio,fileTypeRadio);
        try {
            FTPClient ftpClient = new FTPClient();
            if (!ftpClient.isConnected()) {
                String ftpUser = FTPUtils.getFTPUser();
                String ftpHost = FTPUtils.getFTPHost();
                String ftpPass = FTPUtils.getFTPUserPassword();
                String isActiveMode = FTPUtils.getIsActiveMode();
                String ftpProtocol = FTPUtils.getFTPProtocol();
                FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
            }
            List<FTPFile> positionFileList = FTPUtils.getPositionFileFromFtp(ftpClient);

            DCIteratorBinding positionFilesIter = ADFUtils.findIterator("FtiFtpPositionFilesVOIterator");
            ViewObject positionFilesVO = positionFilesIter.getViewObject();
            if (positionFilesVO != null) {
                Map<String, String> existingFiles =
                    FlexTradeFileUtil.getVOAttrValueInMap("FtiFtpPositionFilesVOIterator", "FileName");
                int counter = 0;
                for (FTPFile fList : positionFileList) {
                    if (!existingFiles.containsKey(fList.getName())) {
                        counter++;
                        Row positionFilesRow = positionFilesVO.createRow();
                        String fileId = FlexTradeFileUtil.getUniqueValue();
                        System.out.println(counter + "-----" + fileId);
                        positionFilesRow.setAttribute("FtiFileId", fileId + counter);
                        positionFilesRow.setAttribute("FileName", fList.getName());
                        positionFilesRow.setAttribute("FileType", "Position");
                        positionFilesRow.setAttribute("FileSize", fList.getSize() / 1000);
                        ctr++;
                        //System.out.println("size----" + fList.getSize() / 1000);
                    }
                }
            }
            OperationBinding operationCommit = null;
            operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
            if (ctr > 0 && operationCommit != null) {
                try {
                    operationCommit.execute();
                    if (operationCommit.getErrors().isEmpty()) {
                        JSFUtils.addFacesInformationMessage("File(s) Loaded From FTP Successfully");
                        FTPUtils.ftpDisConnect(ftpClient);
                    } else {
                        System.out.println("-----" + operationCommit.getErrors());
                        JSFUtils.addFacesErrorMessage("Problem in Loading Trade Files");
                        JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                    }
                } catch (Exception e) {
                    JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                    e.printStackTrace();
                    e.getMessage();
                }
            } else if (ctr == 0) {
                JSFUtils.addFacesInformationMessage("No New File(s) to Load from FTP..");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideCriteriaItem(String viewCriteriaName, String criteriaItemName, boolean condition,
                                  String showHint) {
        if (viewCriteriaName != null) {
            ViewCriteria v = this.getViewCriteria(viewCriteriaName);
            if (v != null) {
                boolean found = false;
                while (v.hasNext() && !found) {
                    ViewCriteriaRow vcr = (ViewCriteriaRow)v.next();
                    if (vcr != null) {
                        ViewCriteriaItem[] vcis = vcr.getCriteriaItemArray();
                        if (vcis != null && vcis.length > 0) {
                            for (int j = 0; j < vcis.length && !found; j++) {
                                ViewCriteriaItem vci = vcis[j];
                                if (vci != null && criteriaItemName != null &&
                                    criteriaItemName.equals(vci.getName())) {
                                    found = true;
                                    if (bussinessType.equalsIgnoreCase("MBANK")) {
                                        vci.setRequiredString(ViewCriteriaItem.VCITEM_REQUIRED_STR);
                                    }
                                    vci.setProperty(ViewCriteriaItemHints.CRITERIA_RENDERED_MODE,
                                                    condition ? ViewCriteriaItemHints.CRITERIA_RENDERED_MODE_NEVER :
                                                    showHint);
                                    v.saveState();
                                }
                            }
                        }
                    }
                    if (found)
                        break;
                }
            }
        }
    }

    public String getRenderBrokerIdFromViewCriteria() {
        hideCriteriaItem("PositionLimitVOCriteria", "BrokerId", bussinessType.equalsIgnoreCase("MBANK") ? false : true,
                         ViewCriteriaItemHints.CRITERIA_RENDERED_MODE_DEFAULT);
        return null;

    }

    private ViewCriteria getViewCriteria(String string) {
        ViewCriteria vc = null;
        try {
            DCIteratorBinding iter = ADFUtils.findIterator("PositionLimitVOIterator");
            ViewObject vo = iter.getViewObject();
            ViewCriteriaManager vcr = vo.getViewCriteriaManager();
            vc = vcr.getViewCriteria(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vc;
    }
    
    private boolean getVersinoNo(String viewCriteriaName, String criteriaItemName) {
        boolean found = true;
        if (viewCriteriaName != null) {
            ViewCriteria v = this.getViewCriteria(viewCriteriaName);
            if (v != null) {
                System.out.println(v.getName() + "--^^^$-- " + v.getRowCount());
                found = false;
                    ViewCriteriaRow vcr = (ViewCriteriaRow)v.first();
                    if (vcr != null) {
                        ViewCriteriaItem[] vcis = vcr.getCriteriaItemArray();
                        if (vcis != null && vcis.length > 0) {
                            for (int j = 0; j < vcis.length && !found; j++) {
                                ViewCriteriaItem vci = vcis[j];
                                if (vci != null && vci.getName().equals(criteriaItemName)) {
                                    if(vci.getValue() != null)
                                        found = false;
                                    else
                                        found = true;
                                }
                            }
                        }
                    }
            }
        }
        return found;
    }

    
    public boolean getExecuteLovIteratorVersion() {
        
        String viewCriteriaName = "PositionLimitVOCriteria";
        String criteriaItemName = "VersionNo";
        boolean found = true;
        found = this.getVersinoNo(viewCriteriaName, criteriaItemName);
        return found;
    }

}
