package leads.capita.trade.view.backing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import javax.mail.internet.AddressException;

import javax.naming.NamingException;

import leads.capita.common.am.CapitaDBServiceImpl;
import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.ADFUtils;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.bond.PriceParser;
import leads.capita.trade.bond.TradeParser;
import leads.capita.trade.exception.TradeFileDateMismatchException;
import leads.capita.trade.file.DataDump;
import leads.capita.trade.file.FTPUtils;
import leads.capita.trade.file.FileVerification;
import leads.capita.trade.file.FlexTradeFileUtil;
import leads.capita.trade.model.view.EodTickerVOImpl;
import leads.capita.trade.model.view.FileProcessErrorVOImpl;
import leads.capita.trade.model.view.FileProcessErrorVORowImpl;
import leads.capita.trade.model.view.FlexTradeDataVOImpl;
import leads.capita.trade.model.view.ImportExtFilesVOImpl;
import leads.capita.trade.model.view.ImportExtFilesVORowImpl;
import leads.capita.trade.model.view.InstrumentsVOImpl;
import leads.capita.trade.model.view.InstrumentsVORowImpl;
import leads.capita.trade.model.view.InvestorAccountsVOImpl;
import leads.capita.trade.model.view.common.TradePriceFileVO;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.logging.ADFLogger;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.input.RichInputDate;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;
import oracle.adf.view.rich.component.rich.input.RichSelectOneRadio;
import oracle.adf.view.rich.component.rich.output.RichOutputText;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import oracle.jbo.domain.DBSequence;
import oracle.jbo.domain.Timestamp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.myfaces.trinidad.model.UploadedFile;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;


public class FileUploadBean extends BaseBean {

    private static final ADFLogger logger = ADFLogger.createADFLogger(TradeParser.class);
    private UploadedFile _file;
    private UploadedFile _fileControl;
    private RichSelectOneChoice tradeFileRadio;
    private RichInputDate toDate;
    private String toDateVal;
    private int exchangeId;
    private String tradeFileType;
    private String onloadText;
    private String pageName;
    private String fileName = "File type not configure !";
    private int priceFileCount;
    private int recCount;
    private Date tradeDate;
    private String currentFilePath = null;
    private String currentCtrlFilePath = null;
    private DateFormat formatter = new SimpleDateFormat("dd-MMM-yy");

    private Date appDate;
    private long sessionId;
    private int branchId;

    public static int NUMBER = Types.NUMERIC;
    public static int DATE = Types.DATE;
    public static int VARCHAR2 = Types.VARCHAR;
    //private static final String INVESTOR_SERVICE_DATA_CONTROL = "#{data.FIModelServiceDataControl.dataProvider}";

    private CallableStatement callableStatement = null;
    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    private CapitaDBServiceImpl dbService;
    private TMPlsqlExecutor tmPlsqlExecutor = new TMPlsqlExecutor();
    private RichPopup popNoPriceFile;
    private String fileId;
    private RichOutputText fileCode;
    private RichSelectOneChoice exchange;
    DataDump dataDump = new DataDump();


    public FileUploadBean() {
        super();

        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();

    }

    public UploadedFile getFile() {
        return _file;
    }

    public void setFile(UploadedFile file) {
        _file = file;
    }

    public void setFileControl(UploadedFile _fileControl) {
        this._fileControl = _fileControl;
    }

    public UploadedFile getFileControl() {
        return _fileControl;
    }

    public void popConfirmPriceDelete(DialogEvent dialogEvent) {
        //this.deleteConfirmation(dialogEvent, "DeletePrice");
    }

    public static void addGlobalMessage(String msg) {
        FacesMessage message = new FacesMessage(msg);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    void syncAppState() {

        try {
            //appDate=ApplicationInfo.getSystemDate()!=null? formatter.parse( new SimpleDateFormat("dd-MMM-yy").format(ApplicationInfo.getSystemDate())):appDate;
            branchId =
                    ApplicationInfo.getCurrentUserBranch() != null ? Integer.parseInt(ApplicationInfo.getCurrentUserBranch()) :
                    9;
            appDate = tmPlsqlExecutor.getAppDate(branchId);
            sessionId = ApplicationInfo.getSessionId() != null ? ApplicationInfo.getSessionId() : 1;

        } catch (Exception e) {
            JSFUtils.addFacesInformationMessage("Application Date or Branch or Session Synchronization Problem.");
            e.printStackTrace();
        }
    }

    private ImportExtFilesVORowImpl getImportExtFilesRow(ImportExtFilesVOImpl importEstFilesVOImpl, String fileId,
                                                         Date tradeDate) {
        SimpleDateFormat tradeDateFormatter = new SimpleDateFormat("dd-MMM-yy");
        System.out.println("file ===" + fileId);
        importEstFilesVOImpl.setWhereClause("FILE_ID = " + "\'" + fileId + "\' AND FILE_DATE = " + "\'" +
                                            tradeDateFormatter.format(tradeDate) + "\'");
        importEstFilesVOImpl.executeQuery();
        ImportExtFilesVORowImpl importExt = (ImportExtFilesVORowImpl)importEstFilesVOImpl.first();

        return importExt != null ? importExt : null;
    }

    public String doUpload() throws IOException, NamingException, TradeFileDateMismatchException, SQLException {

        UploadedFile file = this.getFile();
        UploadedFile ctrlFile = this.getFileControl(); //get control file
        FileVerification fileVerification = new FileVerification();
        // DataDump dataDump = new DataDump();

        if ((toDate == null) || (this.toDate.getValue() == null)) {
            if (toDate == null) {
                this.toDate = new RichInputDate();
            }
            this.toDate.setValue(new Date());
            toDateVal = formatter.format(new Date());
        } else {
            toDateVal = formatter.format((Date)toDate.getValue());
        }

        DCIteratorBinding tradeFileLovIteratorBindings = ADFUtils.findIterator("TradeFileConfVOIterator");
        tradeFileType = tradeFileLovIteratorBindings.getCurrentRow().getAttribute("FileId").toString();
        exchangeId =
                Integer.valueOf(tradeFileLovIteratorBindings.getCurrentRow().getAttribute("ExchangeId").toString());

        try {
            tradeDate = formatter.parse(formatter.format((Date)toDate.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Need to uncomment before final checkin..
        /* if (fileVerification.nonTradeHolidayChecker(this.exchangeId, tradeDate)) {
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Cannot be parse at holiday!! "));
            return null;
        } */

        if (file == null || file.getLength() <= 0) {
            //ADFUtils.addGlobalMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            this.addGlobalMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            return null;
        }
        if (this.getTradeFileType() == null) {
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Selection Failed!! "));
            return null;
        }

        if (this.exchangeId == 0) {
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Exchange mismatched !! "));
            return null;
        }

        InputStreamReader inputReader = new InputStreamReader(file.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputReader);
        try {
            if (getTradeFileType().equals("MSAPLUS")) {
                if (dataDump.validTradeFile() == 1) {

                    // System.out.println("exchange:"+exchangeId);

                    if (fileVerification.verifyMSAPlus(file.getInputStream(), tradeDate)) {
                        dataDump.dumpMSAPlusData(exchangeId, file.getFilename(), toDateVal, file.getInputStream());
                        fct.addMessage("SuccessMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_INFO, "", "MSAPLUS Trade File has been loaded!"));
                    } else {
                        fct.addMessage("ErrorMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "MSAPLUS Trade File varification Failed!! Enter Correct File"));
                    }

                } else {
                    JSFUtils.addFacesInformationMessage("Price File not processed!");
                    return null;

                }
            } else if (getTradeFileType().equals("BT")) {
                if (dataDump.validTradeFile() == 1) {

                    if (fileVerification.verifyBT(file.getInputStream(), tradeDate)) {
                        dataDump.dumpBTData(exchangeId, file.getFilename(), toDateVal, file.getInputStream());
                        fct.addMessage("SuccessMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_INFO, "", "BT Trade File has been loaded!"));
                    } else {
                        fct.addMessage("ErrorMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "BT Trade File verification Failed!! Enter Correct File"));
                    }

                } else {
                    JSFUtils.addFacesInformationMessage("Price File not processed!");
                    return null;

                }
            } else if (getTradeFileType().equals("PRICE_DSE_REPORT")) {
                if (fileVerification.verifyPrice("PRICE_DSE_REPORT", file.getInputStream(), tradeDate)) {

                    dataDump.dumpPriceData(exchangeId, file.getFilename(), toDateVal, file.getInputStream());

                    DCIteratorBinding priceFilesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
                    ViewObject importPriceFilesVO = priceFilesIteratorBindings.getViewObject();
                    //importPriceFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' and FILE_ID=upper('" +tradeFileType + "') ");
                    importPriceFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' ");

                    importPriceFilesVO.executeQuery();
                    fct.addMessage("SuccessMsg",
                                   new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The File has been loaded!"));
                } else {
                    fct.addMessage("ErrorMsg",
                                   new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "DSE Price File varification Failed!! Enter Correct File"));
                }

            } else if (getTradeFileType().equals("FLEX_PRICE_FILE")) { //for flex trade(price file EOD_Ticker)
                loadFlexPriceFile(file, ctrlFile);
            } else if (getTradeFileType().equals("FLEX_TRADE_FILE")) { //for flex trade(trade file flex_trade_data)
                loadFlexTradeFile(file, ctrlFile);
            } else if (getTradeFileType().equals("PRICE_CSE")) {
                CSEPriceParser csePriceParser = new CSEPriceParser();

                BindingContext bindingCtx = BindingContext.getCurrent();
                BindingContainer container = (DCBindingContainer)bindingCtx.getCurrentBindingsEntry();

                DCIteratorBinding instrumentIterator = (DCIteratorBinding)container.get("InstrumentsVOIterator");
                DCIteratorBinding importExtFileIterator = (DCIteratorBinding)container.get("ImportExtFilesVOIterator");

                TradePriceFileVO tradePriceFileVO =
                    (TradePriceFileVO)((DCIteratorBinding)container.get("TradePriceFileVOIterator")).getViewObject();
                InstrumentsVOImpl instruments = (InstrumentsVOImpl)instrumentIterator.getViewObject();
                ImportExtFilesVOImpl importExtFilesVOImpl =
                    (ImportExtFilesVOImpl)importExtFileIterator.getViewObject();

                List<String> invalidInstruments =
                    csePriceParser.parse(bufferedReader, importExtFilesVOImpl, instruments, tradePriceFileVO,
                                         file.getFilename(), "PRICE_CSE", tradeDate);
                if (invalidInstruments.size() == 0) {
                    importExtFilesVOImpl.getApplicationModule().getTransaction().commit();
                    fct.addMessage("InfoMsg",
                                   new FacesMessage(FacesMessage.SEVERITY_ERROR, "", file.getFilename() + " has been uploaded successfully!"));

                } else {
                    StringBuilder message = new StringBuilder();
                    for (String shortName : invalidInstruments) {
                        message.append(shortName + ", ");
                    }
                    message.append("are not exist in system. \n Please create thus instruments first");
                    fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", message.toString()));
                    importExtFilesVOImpl.getApplicationModule().getTransaction().rollback();

                }
            } else if (getTradeFileType().equals("TB_TRADE_DSE")) {
                new TMPlsqlExecutor().deleteTBFile("TB_TRADE_DSE", 0);
                BindingContext bindingCtx = BindingContext.getCurrent();
                BindingContainer container = bindingCtx.getCurrentBindingsEntry();
                DCIteratorBinding importExtFileIterator = (DCIteratorBinding)container.get("ImportExtFilesVOIterator");
                DCIteratorBinding fileErrorIterator = (DCIteratorBinding)container.get("FileProcessErrorVOIterator");
                DCIteratorBinding investorIterator = (DCIteratorBinding)container.get("InvestorAccountsVOIterator");
                DCIteratorBinding instrumentIterator = (DCIteratorBinding)container.get("InstrumentsVOIterator");
                DCIteratorBinding bindings = (DCIteratorBinding)container.get("FlexTradeDataVOIterator");
                FlexTradeDataVOImpl flexTradeDataVO = (FlexTradeDataVOImpl)bindings.getViewObject();
                InstrumentsVOImpl instruments = (InstrumentsVOImpl)instrumentIterator.getViewObject();
                InvestorAccountsVOImpl investors = (InvestorAccountsVOImpl)investorIterator.getViewObject();
                ImportExtFilesVOImpl importExtFilesVOImpl =
                    (ImportExtFilesVOImpl)importExtFileIterator.getViewObject();
                FileProcessErrorVOImpl fileProcessErrorVOImpl =
                    (FileProcessErrorVOImpl)fileErrorIterator.getViewObject();

                if (ApplicationInfo.getBusinessType().equals("BROKER")) {

                    TradeParser tradeParser = new TradeParser();
                    Integer result =
                        tradeParser.parse(exchangeId, file.getFilename(), tradeDate, file.getInputStream(),
                                          flexTradeDataVO, instruments, investors, importExtFilesVOImpl,
                                          "TB_TRADE_DSE");
                    
                    if (tradeParser.getErrorCode() != 0) {
                        flexTradeDataVO.reset();
                        logger.log(logger.ERROR, tradeParser.getErrors().toString());
                        StringBuilder message = new StringBuilder();

                        if (result.intValue() > 0) {

                            importExtFilesVOImpl.getApplicationModule().getTransaction().commit();
                            for (int i = 0; i < tradeParser.getErrors().size(); i++) {
                                String error = tradeParser.getErrors().get(i).toString();
                                /*final FileProcessErrorVORowImpl row = (FileProcessErrorVORowImpl) fileProcessErrorVOImpl.createRow();
                            row.setFileName(file.getFilename());
                            row.setProcessDate(new Timestamp(tradeDate.getTime()));
                            row.setErrorDetail(error);
                            System.out.println("error +"+error);
                            ImportExtFilesVORowImpl importExt =  getImportExtFilesRow(importExtFilesVOImpl, "TB_TRADE_DSE", tradeDate);
                            DBSequence dbSeqNumber = new DBSequence(importExt.getImportId());
                            Integer importId = new Long(dbSeqNumber.getValue()).intValue();
                            System.out.println(importId + " row "+  importExt);
                            row.setImportId(importId);
                            row.setErrType("OTHERS");*/

                                message.append(error + "\n");
                                //fileProcessErrorVOImpl.getApplicationModule().getTransaction().commit();
                            }


                        } else {
                            for (String error : tradeParser.getErrors()) {
                                message.append(error + "\n");
                            }
                            ;
                        }

                        message.append(" \n Please correct the mismatch first");
                        fct.addMessage("ErrorMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_ERROR, "", message.toString()));
                        flexTradeDataVO.getApplicationModule().getTransaction().rollback();
                        //fileProcessErrorVOImpl.getApplicationModule().getTransaction().rollback();
                    } else {
                        flexTradeDataVO.getApplicationModule().getTransaction().commit();
                        importExtFilesVOImpl.getApplicationModule().getTransaction().commit();
                        //fileProcessErrorVOImpl.getApplicationModule().getTransaction().commit();

                        fct.addMessage("InfoMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_INFO, "", file.getFilename() + " has been uploaded successfully!"));

                    }

                }
            } else if (getTradeFileType().equals("TB_TRADE_CSE")) {

                if (dataDump.validTradeFile() == 1) {

                    if (fileVerification.verifyBT(file.getInputStream(), tradeDate)) {
                        dataDump.dumpBTData(exchangeId, file.getFilename(), toDateVal, file.getInputStream());
                        fct.addMessage("SuccessMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_INFO, "", "BT Trade File has been loaded!"));
                    } else {
                        fct.addMessage("ErrorMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "BT Trade File verification Failed!! Enter Correct File"));
                    }

                } else {
                    JSFUtils.addFacesInformationMessage("Price File not processed!");
                    return null;

                }

            } else if (getTradeFileType().equals("TB_PRICE")) {

                new TMPlsqlExecutor().deleteTBFile("TB_PRICE", 0);
                BindingContext bindingCtx = BindingContext.getCurrent();
                BindingContainer container = bindingCtx.getCurrentBindingsEntry();
                DCIteratorBinding importExtFileIterator = (DCIteratorBinding)container.get("ImportExtFilesVOIterator");
                DCIteratorBinding fileErrorIterator = (DCIteratorBinding)container.get("FileProcessErrorVOIterator");
                DCIteratorBinding instrumentIterator = (DCIteratorBinding)container.get("InstrumentsVOIterator");
                DCIteratorBinding bindings = (DCIteratorBinding)container.get("EodTickerVOIterator");
                EodTickerVOImpl eodTickerVO = (EodTickerVOImpl)bindings.getViewObject();
                InstrumentsVOImpl instruments = (InstrumentsVOImpl)instrumentIterator.getViewObject();
                ImportExtFilesVOImpl importExtFilesVOImpl =
                    (ImportExtFilesVOImpl)importExtFileIterator.getViewObject();
                FileProcessErrorVOImpl fileProcessErrorVOImpl =
                    (FileProcessErrorVOImpl)fileErrorIterator.getViewObject();

                if (ApplicationInfo.getBusinessType().equals("BROKER")) {

                    PriceParser priceParser = new PriceParser();
                    Integer result =
                        priceParser.parse(exchangeId, file.getFilename(), tradeDate, file.getInputStream(),
                                          eodTickerVO, instruments, importExtFilesVOImpl, "TB_PRICE");
                    if (priceParser.getErrorCode() != 0) {

                        logger.log(logger.ERROR, priceParser.getErrors().toString());
                        StringBuilder message = new StringBuilder();
                        if (result.intValue() > 0) {
                            importExtFilesVOImpl.getApplicationModule().getTransaction().commit();
                            System.out.println("priceParser.getErrors().size() " + priceParser.getErrors().size());
                            for (int i = 0; i < priceParser.getErrors().size(); i++) {
                                String error = priceParser.getErrors().get(i).toString();
                                System.out.println("error" + error);

                                message.append(error + "\n");
                            }
                        } else {
                            for (String error : priceParser.getErrors()) {
                                message.append(error + "\n");
                            }
                            ;
                        }

                        message.append(" \n Please correct the mismatch first");
                        fct.addMessage("ErrorMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_ERROR, "", message.toString()));
                        eodTickerVO.getApplicationModule().getTransaction().rollback();
                    } else {
                        eodTickerVO.getApplicationModule().getTransaction().commit();
                        importExtFilesVOImpl.getApplicationModule().getTransaction().commit();

                        fct.addMessage("InfoMsg",
                                       new FacesMessage(FacesMessage.SEVERITY_INFO, "", file.getFilename() + " has been uploaded successfully!"));

                    }

                }
            }

            // System.out.println(tradeFileType);
            DCIteratorBinding priceFilesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
            ViewObject importPriceFilesVO = priceFilesIteratorBindings.getViewObject();
            importPriceFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' and FILE_ID=upper('" + tradeFileType +
                                              "') ");
            //importPriceFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' ");
            importPriceFilesVO.executeQuery();

        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
        } finally {
            bufferedReader.close();
            inputReader.close();
        }
        return null;
    }

    private String loadFlexPriceFile(UploadedFile file, UploadedFile controlFile) throws ParseException, IOException,
                                                                                         Exception {
        boolean isValidFile = false;
        boolean isProcessCall = false;
        String fileId = FlexTradeFileUtil.getUniqueValue();
        String fileName = file.getFilename();

        DCIteratorBinding eodTickerIter = ADFUtils.findIterator("EodTickerVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Price");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);

        currentFilePath = destFolderPath + File.separator + file.getFilename();
        if (controlFile != null) {
            String ctrlFileName = controlFile.getFilename();
            FlexTradeFileUtil.fileUploadAndSave(destFolderPath, controlFile.getFilename(), controlFile);
            currentCtrlFilePath = destFolderPath + File.separator + controlFile.getFilename();
        }

        File newGenFile = new File(currentFilePath);
        String fileNameDatePart = FlexTradeFileUtil.getFileNameDatePart(file.getFilename());
        String appDate = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();

        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }

        if (!fileNameDatePart.equalsIgnoreCase(appDate)) {
            JSFUtils.addFacesErrorMessage(" Not Current Dated File!!");
            return null;
        }
        //Xml & ctrl Validation method call
        isValidFile = this.validateXmlPriceWithControl(currentFilePath, currentCtrlFilePath);
        if (!isValidFile) {
            JSFUtils.addFacesErrorMessage("Control Validation Failed !");
            return null;
        }
        this.dumpDataFromFlexPriceFile(newGenFile, fileId, fileName);
        OperationBinding operationCommit = null;
        operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
        if (operationCommit != null) {
            try {
                operationCommit.execute();
                if (operationCommit.getErrors().isEmpty()) {
                    isProcessCall = dataDump.dumpFlexPriceData(fileId);
                    if (isProcessCall) {
                        JSFUtils.addFacesInformationMessage("EOD Price File Loaded Successfully");
                    } else {
                        JSFUtils.addFacesErrorMessage("Problem in Loading EOD Price File..");
                    }
                } else {
                    System.out.println("-----" + operationCommit.getErrors());
                    JSFUtils.addFacesErrorMessage("Problem in Loading EOD Price File");
                    JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                    eodTickerIter.executeQuery();
                }
            } catch (Exception e) {
                JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                eodTickerIter.executeQuery();
                e.printStackTrace();
                e.getMessage();
            }
        }
        return null;
    }

    private String loadFlexTradeFile(UploadedFile file, UploadedFile controlFile) throws ParseException, IOException,
                                                                                         Exception {
        boolean isValidFile = false;
        boolean isProcessCall = false;
        String fileId = FlexTradeFileUtil.getUniqueValue();
        String fileName = file.getFilename();
        DCIteratorBinding flexTradeIter = ADFUtils.findIterator("FlexTradeDataVOIterator");
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trade");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        currentFilePath = destFolderPath + File.separator + file.getFilename();

        if (controlFile != null) {
            String ctriFileName = controlFile.getFilename();
            FlexTradeFileUtil.fileUploadAndSave(destFolderPath, controlFile.getFilename(), controlFile);
            currentCtrlFilePath = destFolderPath + File.separator + controlFile.getFilename();
        }

        File newGenFile = new File(currentFilePath);
        String fileNameDatePart = FlexTradeFileUtil.getFileNameDatePart(file.getFilename());
        String appDate = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();
        //System.out.println("---" + currentFilePath);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        if (!fileNameDatePart.equalsIgnoreCase(appDate)) {
            JSFUtils.addFacesInformationMessage(" Not Current Dated File!!");
            return null;
        }
        //XML & Control Validation method call
        isValidFile = this.validateXmlTradeWithControl(currentFilePath, currentCtrlFilePath);

        if (!isValidFile) {
            JSFUtils.addFacesErrorMessage("Control Validation Failed !");
            return null;
        }
        this.dumpDataFromFlexTradeFile(newGenFile, fileId, fileName);
        OperationBinding operationCommit = null;
        operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
        if (operationCommit != null) {
            try {

                if (dataDump.validTradeFile() == 1) {

                    operationCommit.execute();
                    if (operationCommit.getErrors().isEmpty()) {
                        System.out.println("FlexTradeFileId====" + fileId);
                        isProcessCall = dataDump.dumpFlexTradeData(fileId);
                        if (isProcessCall) {
                            JSFUtils.addFacesInformationMessage("Flex Trade File Loaded Successfully");
                        } else {

                            JSFUtils.addFacesErrorMessage("Problem in Loading Flex Trade File..");
                        }
                    } else {
                        System.out.println("Problem in Loading-----" + operationCommit.getErrors());
                        JSFUtils.addFacesErrorMessage("Problem in Loading Flex Trade File");
                        JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        //iterator need to be executed
                        flexTradeIter.executeQuery();
                    }

                } else {
                    JSFUtils.addFacesInformationMessage("Price File not processed!");
                    return null;

                }

            } catch (Exception e) {
                e.printStackTrace();
                JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                flexTradeIter.executeQuery();
                throw e;
            }
        }


        return null;
    }

    private boolean validateXmlPriceWithControl(String xmlFilePath,
                                                String controlFilePath) throws FileNotFoundException, JDOMException,
                                                                               IOException {
        boolean isValid = false;
        String xmlHash = null;
        String controlHash = null;
        //Check whether control validation required
        String ctrlValidation =
            FlexTradeFileUtil._getAttrValueFromIter("FtiConfigParamPriceVOIterator", "ControlValidation");
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
            // System.out.println("control hash----"+controlHash);
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

    private boolean validateXmlTradeWithControl(String xmlFilePath,
                                                String controlFilePath) throws FileNotFoundException, JDOMException,
                                                                               IOException {
        boolean isValid = false;
        String xmlHash = null;
        String controlHash = null;
        //Check whether control validation required
        String ctrlValidation =
            FlexTradeFileUtil._getAttrValueFromIter("FtiConfigParamTradeVOIterator", "ControlValidation");
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
            //  System.out.println("file hash11----"+xmlHash);
            controlHash = FlexTradeFileUtil.getControllAttrValue(controlFilePath, "Hash");
            // System.out.println("control hash11----"+controlHash);
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


    public void fileSelect(ValueChangeEvent valueChangeEvent) throws ParseException, IOException {
        DCIteratorBinding tradeFileLovIteratorBindings = ADFUtils.findIterator("TradeFileConfVOIterator");
        tradeFileType = tradeFileLovIteratorBindings.getCurrentRow().getAttribute("FileId").toString();
        _file = (UploadedFile)valueChangeEvent.getNewValue();
        this.setFile(_file);
        if (getTradeFileType().equalsIgnoreCase("MSAPLUS") || getTradeFileType().equalsIgnoreCase("BT") ||
            getTradeFileType().equalsIgnoreCase("PRICE_DSE_REPORT"))

            if (!_file.getContentType().equals("text/plain")) {
                _file = null;
                System.out.println("No file has selected");
            } else if (getTradeFileType().equalsIgnoreCase("FLEX_PRICE_FILE") ||
                       getTradeFileType().equalsIgnoreCase("FLEX_TRADE_FILE"))
                if (!_file.getContentType().equals("text/xml")) {
                    _file = null;
                    System.out.println("No file has selected!");
                }
    }

    public String fileSelectControl(ValueChangeEvent valueChangeEvent) {
        _fileControl = (UploadedFile)valueChangeEvent.getNewValue();
        this.setFileControl(_fileControl);
        if (getTradeFileType().equalsIgnoreCase("FLEX_PRICE_FILE") ||
            getTradeFileType().equalsIgnoreCase("FLEX_TRADE_FILE")) {
            if (!_fileControl.getContentType().equals("text/xml")) {
                _fileControl = null;
                JSFUtils.addFacesErrorMessage("Control File not Selected!");
            }
        } else if (getTradeFileType().equalsIgnoreCase("MSAPLUS") || getTradeFileType().equalsIgnoreCase("BT") ||
                   getTradeFileType().equalsIgnoreCase("PRICE_DSE_REPORT")) {
            _fileControl = null;
            JSFUtils.addFacesInformationMessage("No need to select control file!");
            return null;
        }
        return null;
    }


    public String getTradeFileType() {
        return tradeFileType;
    }

    public void setToDate(RichInputDate toDate) {
        this.toDate = toDate;
    }

    public RichInputDate getToDate() {
        //&(toDate.getValue()==null)
        /* if ((toDate==null))
        {
            this.toDate = new RichInputDate();
            this.toDate.setValue(new Date());
        }
*/
        return toDate;
    }

    public void dateExchageChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(JSFUtils.getFacesContext());

        toDateVal = formatter.format((Date)toDate.getValue());
        JSFUtils.storeOnSession("tradeDate", toDateVal);
        makePreuploadEnv();

    }

    private void makePreuploadEnv() {

        this.syncAppState();
        if (JSFUtils.getFromSession("tradeDate") != null) {
            toDateVal = JSFUtils.getFromSession("tradeDate").toString();
            try {
                this.toDate.setValue(formatter.parse(toDateVal));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ((toDate == null) || (this.toDate.getValue() == null)) {
            if (toDate == null) {
                this.toDate = new RichInputDate();
            }
            this.toDate.setValue(appDate);
            toDateVal = formatter.format(appDate);
        } else {
            toDateVal = formatter.format((Date)toDate.getValue());
        }

        DCIteratorBinding tradeFileConfIteratorBindings = ADFUtils.findIterator("TradeFileConfVOIterator");
        DCIteratorBinding importfilesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");

        /// tradeFileType = tradeFileConfIteratorBindings.getCurrentRow().getAttribute("ExchangeId").toString()
        //Row row = tradeFileConfIteratorBindings.getCurrentRow();
        //exchangeId = Integer.parseInt(row.getAttribute("ExchangeId").toString());
        //fileId=row.getAttribute("FileId").toString();

        if ((toDateVal != null)) {
            try {
                ViewObject tradeFileConfFilesVO = tradeFileConfIteratorBindings.getViewObject();
                tradeFileConfFilesVO.setWhereClause("EXCHANGE_ID IN (10,11) AND FILE_ID='PRICE_DSE_REPORT' " +
                                                    "AND upper(STATUS)='ACTIVE'  OR FILE_ID='FLEX_PRICE_FILE' OR FILE_ID='PRICE_CSE' ");
                tradeFileConfFilesVO.executeQuery();
                Row tradeFileConfRow = null;
                while (tradeFileConfFilesVO.hasNext()) {
                    tradeFileConfRow = tradeFileConfFilesVO.next();
                }
                tradeFileConfRow = tradeFileConfFilesVO.getCurrentRow();
                if (tradeFileConfRow != null) {
                    tradeFileType = "PRICE_DSE_REPORT";
                    exchangeId = 10;
                    tradeFileConfFilesVO.executeQuery();
                    ViewObject importExtFilesVO = importfilesIteratorBindings.getViewObject();
                    importExtFilesVO.setWhereClause("FILE_DATE ='" + toDateVal +
                                                    "' AND FILE_ID='PRICE_DSE_REPORT' OR FILE_ID='FLEX_PRICE_FILE'  OR FILE_ID='PRICE_CSE' ");
                    importExtFilesVO.executeQuery();

                    importExtFilesVO.getQuery();

                    priceFileCount = importExtFilesVO.getRowCount();

                    if (priceFileCount == 0) {

                    } else {
                        tradeFileConfFilesVO.setWhereClause("upper(STATUS)='ACTIVE' ");
                        tradeFileConfFilesVO.executeQuery();

                        /*
                        if (tradeFileConfFilesVO.getRowCount()>1)
                        {
                            if (JSFUtils.getFromSession("tradeFileSession") != null) {
                                tradeFileType = JSFUtils.getFromSession("tradeFileSession").toString();

                                Row row;
                                do {

                                    row=tradeFileConfFilesVO.getCurrentRow();

                                    if (row!=null) {
                                    if ((row.getAttribute("FileId")!=null) &&
                                        (row.getAttribute("FileId").toString().equals(tradeFileType))){
                                        break;
                                    } else {
                                        row=tradeFileConfFilesVO.next();
                                    }
                                    tradeFileConfFilesVO.next();
                                    } else {
                                        break;
                                    }
                                }while(row!=null);

                                tradeFileRadio.setValue(tradeFileType);

                                importExtFilesVO.setWhereClause("FILE_DATE ='" + toDateVal +
                                                                  "' and FILE_ID=upper('" + tradeFileType +"') ");
                                importExtFilesVO.executeQuery();

                            } else {
                                tradeFileType="PRICE_DSE_REPORT";
                            }

                        }
                        */
                    }

                    /*System.out.println(tradeFileType);
                    System.out.println(importExtFilesVO.getQuery());
                    System.out.println("Upload :" + importExtFilesVO.getRowCount());
                    */

                    // DCIteratorBinding importfilesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
                    //ViewObject importExtFilesVO = importfilesIteratorBindings.getViewObject();
                    importExtFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' AND FILE_ID=UPPER('" +
                                                    tradeFileType +
                                                    "') OR FILE_ID='FLEX_PRICE_FILE' OR FILE_ID='PRICE_CSE' ");
                    importExtFilesVO.executeQuery();
                    importExtFilesVO.setWhereClause(null);
                }

                else {
                    ViewObject importExtFilesVO = importfilesIteratorBindings.getViewObject();
                    importExtFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' and FILE_ID=upper('" + fileId +
                                                    "') ");
                    importExtFilesVO.executeQuery();
                    priceFileCount = importExtFilesVO.getRowCount();

                    fct.addMessage("ErrorMsg",
                                   new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Price File has not configured!"));
                }
            } catch (Exception ne) {
                ne.printStackTrace();
                fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error"));
            }
        }
    }


    public void showPopupFileDeletion(DialogEvent dialogEvent) {
        //this.deleteConfirmation(dialogEvent, "DeleteFile");

        if (dialogEvent.getOutcome() == DialogEvent.Outcome.ok) {
            this.deleteThisFile();
        }

        //#{FileUploadBean.deleteThisFile}

    }

    public String deleteThisFile() {
        DCIteratorBinding dcImportFilesItrBindings = bindings.findIteratorBinding("ImportExtFilesVOIterator");
        Row importFilesRow = dcImportFilesItrBindings.getCurrentRow();
        String fileType = importFilesRow.getAttribute("FileId").toString();
        try {
            if (fileType.equals("TB_TRADE_DSE") || fileType.equals("TB_TRADE_CSE") || fileType.equals("TB_PRICE")) {
                if (fileType.equals("TB_TRADE_DSE"))
                    new TMPlsqlExecutor().deleteTBFile(fileType, 0);
                else if (fileType.equals("TB_TRADE_CSE"))
                    new TMPlsqlExecutor().deleteTBFile(fileType,
                                                       Integer.parseInt(importFilesRow.getAttribute("ImportId").toString()));
                else if (fileType.equals("TB_PRICE"))
                    new TMPlsqlExecutor().deleteTBFile(fileType, 0);
                
            } else {

                new TMPlsqlExecutor().deleteTradeFile(Integer.parseInt(importFilesRow.getAttribute("ImportId").toString()));
                new TMPlsqlExecutor().deleteThisFile(Integer.parseInt(importFilesRow.getAttribute("ImportId").toString()));
                this.makePreuploadEnv();
            }
            
            fct.addMessage("ErrorMsg", new FacesMessage("File has successfully Deleted"));

        } catch (Exception e) {
            fct.addMessage("ErrorMsg", new FacesMessage("The file has already in the processed!"));

            e.printStackTrace();
            return null;
        }
        return null;
    }

    public String deleteFlexTradeData() {
        try {
            //getTradeFileType().equals("TB_TRADE_DSE")
            DCIteratorBinding dcImportFilesItrBindings = bindings.findIteratorBinding("ImportExtFilesVOIterator");
            Row importFilesRow = dcImportFilesItrBindings.getCurrentRow();
            new TMPlsqlExecutor().deleteTradeFile(Integer.parseInt(importFilesRow.getAttribute("ImportId").toString()));
            new TMPlsqlExecutor().deleteThisFile(Integer.parseInt(importFilesRow.getAttribute("ImportId").toString()));
            this.makePreuploadEnv();
            fct.addMessage("ErrorMsg", new FacesMessage("File has successfully Deleted"));

        } catch (Exception e) {
            fct.addMessage("ErrorMsg", new FacesMessage("The file has already in the processed!"));

            e.printStackTrace();
            return null;
        }
        return null;
    }

    public void setPopNoPriceFile(RichPopup popNoPriceFile) {
        this.popNoPriceFile = popNoPriceFile;
    }

    public RichPopup getPopNoPriceFile() {
        return popNoPriceFile;
    }

    public int getRecCount() {
        return recCount;
    }


    public String getOnloadText() {
        //System.out.println("tradeDate:............" + JSFUtils.getFromSession("tradeDate"));
        //System.out.println("fileType:............" + JSFUtils.getFromSession("fileType"));
        this.makePreuploadEnv();
        if (onloadText == null) {
            onloadText = "::";

            if ((toDate == null) || (this.toDate.getValue() == null)) {
                if (toDate == null) {
                    this.toDate = new RichInputDate();
                }
                this.toDate.setValue(appDate);
                toDateVal = formatter.format(appDate);
            } else {
                toDateVal = formatter.format((Date)toDate.getValue());
            }

            ViewObject importExtFilesVO = ADFUtils.findIterator("ImportExtFilesVOIterator").getViewObject();
            importExtFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' AND FILE_ID='PRICE_DSE_REPORT'");
            importExtFilesVO.executeQuery();
            importExtFilesVO.setWhereClause(null);

            // Row loadedFileRow = ADFUtils.findIterator("ImportExtFilesVOIterator").getViewObject().getCurrentRow();
            Row loadedFileRow = ADFUtils.findIterator("ImportExtFilesVOIterator").getViewObject().first();
            long totalRow = ADFUtils.findIterator("ImportExtFilesVOIterator").getEstimatedRowCount();
            if (totalRow != 0) {
                if (loadedFileRow.getAttribute("ProcessedRec") != null) {
                    Integer processed = Integer.parseInt(loadedFileRow.getAttribute("ProcessedRec").toString());
                    if (processed == 0) {
                        DCIteratorBinding tradeFileConfIteratorBindings =
                            ADFUtils.findIterator("TradeFileConfVOIterator");
                        ViewObject tradeFileConfFilesVO = tradeFileConfIteratorBindings.getViewObject();
                        tradeFileConfFilesVO.setWhereClause(" QRSLT.EXCHANGE_ID IN (10,11) AND QRSLT.FILE_ID='PRICE_DSE_REPORT' " +
                                                            "AND upper(QRSLT.STATUS)='ACTIVE' OR QRSLT.FILE_ID='FLEX_PRICE_FILE' OR QRSLT.FILE_ID='PRICE_CSE' ");
                        tradeFileConfFilesVO.executeQuery();

                    } /* else if(processed > 0){
                            System.out.println("2======");
                            tradeFileConfFilesVO.setWhereClause(null);
                            tradeFileConfFilesVO.executeQuery();
                        } */
                }
            }

        }
        return onloadText;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPageName() {

        //DCIteratorBinding tradeFileLovIteratorBindings = ADFUtils.findIterator("TradeFileConfVOIterator");
        //tradeFileType = tradeFileLovIteratorBindings.getCurrentRow().getAttribute("FileId").toString();

        DCIteratorBinding dcImportFilesItrBindings = bindings.findIteratorBinding("ImportExtFilesVOIterator");

        tradeFileType = dcImportFilesItrBindings.getCurrentRow().getAttribute("FileId").toString();

        if (tradeFileType.equals("MSAPLUS")) {
            return "goMsaPlus";
        } else if (tradeFileType.equals("BT")) {
            return "goBtFile";
        } else if (tradeFileType.equals("PRICE_DSE_REPORT")) {
            return "goPriceFile";
        } else if (tradeFileType.equals("FLEX_PRICE_FILE")) {
            return "goPriceFile";
        } else if (tradeFileType.equals("FLEX_TRADE_FILE")) {
            return "goMsaPlus";
        } else if (tradeFileType.equals("PRICE_CSE")) {
            return "goPriceFile";
        }

        return "goMsaPlus";
    }

    public void goToTaskFlow(ActionEvent actionEvent) {
        // Add event code here...
        // check if hte action has a component attatched


        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
        // System.out.println("...." + tradeFileType);
        //System.out.println("fileType:............" + JSFUtils.getFromSession("fileType"));

        toDateVal = dateFormat.format((Date)toDate.getValue());
        JSFUtils.storeOnSession("tradeDate", toDateVal);
        // System.out.println(toDateVal);


        //  if ((toDate != null) || (this.toDate.getValue() != null)) {
        //           toDateVal = dateFormat.format((Date)toDate.getValue());
        //          JSFUtils.storeOnSession("tradeDate", toDateVal);
        // JSFUtils.storeOnSession("exchangeId", exchangeId);

        // JSFUtils.storeOnSession("tradeFileType", tradeFileType);
        //  }


    }

    public void setFileCode(RichOutputText fileCode) {
        this.fileCode = fileCode;
    }

    public RichOutputText getFileCode() {
        return fileCode;
    }


    public void tradeFileChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);

        if ((toDate == null) || (this.toDate.getValue() == null)) {
            if (toDate == null) {
                this.toDate = new RichInputDate();
            }
            this.toDate.setValue(new Date());
            toDateVal = formatter.format(new Date());
        } else {
            toDateVal = formatter.format((Date)toDate.getValue());
        }

        DCIteratorBinding tradeFileLovIteratorBindings = ADFUtils.findIterator("TradeFileConfVOIterator");

        tradeFileType = tradeFileLovIteratorBindings.getCurrentRow().getAttribute("FileId").toString();
        exchangeId =
                Integer.valueOf(tradeFileLovIteratorBindings.getCurrentRow().getAttribute("ExchangeId").toString());
        //System.out.println( tradeFileType);
        JSFUtils.storeOnSession("tradeFileSession", tradeFileType);

        DCIteratorBinding priceFilesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        ViewObject importPriceFilesVO = priceFilesIteratorBindings.getViewObject();
        // importPriceFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' and FILE_ID=upper('" + tradeFileType +"') ");
        importPriceFilesVO.setWhereClause("FILE_DATE ='" + toDateVal + "' AND FILE_ID=('" + tradeFileType + "')");

        importPriceFilesVO.executeQuery();
        importPriceFilesVO.setWhereClause(null);
    }

    public String loadFilesFromFTP() throws IOException, AddressException {
        int ctr = 0;
        //Map<String, String> fileTypeMap =new HashMap<String, String>();
        //fileTypeMap.put(fileTypeRadio,fileTypeRadio);
        FTPClient ftpClient = new FTPClient();
        if (!ftpClient.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();

            FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
        }
        List<FTPFile> tradeFileList = FTPUtils.getTradesFileFromFtp(ftpClient);

        DCIteratorBinding tradeFilesIter = ADFUtils.findIterator("FtiFtpFilesVOIterator");
        ViewObject tradeFilesVO = tradeFilesIter.getViewObject();
        if (tradeFilesVO != null) {
            Map<String, String> existingFiles =
                FlexTradeFileUtil.getVOAttrValueInMap("FtiFtpFilesVOIterator", "FileName");
            int counter = 0;
            for (FTPFile fList : tradeFileList) {
                if (!existingFiles.containsKey(fList.getName())) { //should not load existing files
                    counter++;
                    Row tradeFilesRow = tradeFilesVO.createRow();
                    String fileId = FlexTradeFileUtil.getUniqueValue();
                    System.out.println(counter + "-----" + fileId);
                    tradeFilesRow.setAttribute("FtiFileId", fileId + counter);
                    tradeFilesRow.setAttribute("FileName", fList.getName());
                    tradeFilesRow.setAttribute("FileType", "Trades");
                    tradeFilesRow.setAttribute("FileSize", fList.getSize() / 1000);
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
                e.printStackTrace();
                e.getMessage();
            }
        } else if (ctr == 0) {
            JSFUtils.addFacesInformationMessage("No File(s) to Load from FTP..");
        }
        return null;
    }

    public String dumpFlexTradeFileFrmFTP() throws IOException, Exception {
        boolean isDumpTrade = false;
        boolean isDumpPrice = false;
        // String destFolderPath = null;
        String fileTypeRadio = this.getTradeFileRadio().getValue().toString();
        // System.out.println("radio----" + fileTypeRadio);
        DCIteratorBinding tradeFilesIter = ADFUtils.findIterator("FtiFtpFilesVOIterator");
        Row tradeFileRow = tradeFilesIter.getCurrentRow();
        String filename = tradeFileRow.getAttribute("FileName").toString();
        String fileNameDatePart = FlexTradeFileUtil.getFileNameDatePart(filename);
        String appDate = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();
        if (!fileNameDatePart.equalsIgnoreCase(appDate)) {
            JSFUtils.addFacesErrorMessage(" Not Current Dated File!!");
            return null;
        }

        FTPClient ftpclient = new FTPClient();
        if (!ftpclient.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            FTPUtils.ftpConnect(ftpclient, ftpHost, ftpUser, ftpPass, isActiveMode);
        }

        if (filename.contains("Trade") || filename.contains("trade") || filename.contains("TRADE")) {
            if (fileTypeRadio.equalsIgnoreCase("Flex Trade File")) {
                String destTradeFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Trade");
                FTPUtils.ftpTradeFileDownload(ftpclient, null, destTradeFolderPath, filename);
                isDumpTrade =
                        dumpFlexTradeFileData(destTradeFolderPath + File.separator + filename, tradeFileRow, filename);
            } else {
                JSFUtils.addFacesErrorMessage("Wrong File Name Selected..");
                ftpclient.disconnect();
                return null;
            }
        } else if (filename.contains("Ticker") || filename.contains("ticker") || filename.contains("TICKER")) {
            if (fileTypeRadio.equalsIgnoreCase("Flex Price File")) {
                String destPriceFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Price");
                FTPUtils.ftpTradeFileDownload(ftpclient, null, destPriceFolderPath, filename);
                isDumpPrice =
                        dumpFlexPriceFileData(destPriceFolderPath + File.separator + filename, tradeFileRow, filename);
            } else {
                JSFUtils.addFacesErrorMessage("Wrong File Name Selected..");
                ftpclient.disconnect();
                return null;
            }
        }
        //System.out.println("file location----"+destFolderPath + File.separator + filename);
        if (isDumpTrade) {
            JSFUtils.addFacesInformationMessage("Flex Trade File Loaded Successfully..");
            ftpclient.disconnect();
        }
        if (isDumpPrice) {
            JSFUtils.addFacesInformationMessage("Flex Price File Loaded Successfully..");
            ftpclient.disconnect();
        }
        return null;
    }

    public boolean dumpFlexTradeFileData(String srcTradeFilePath, Row curFtpTradeFileRow, String fileName) {
        boolean dumpTrade = false;
        boolean isProcessCall = false;
        String fileId = FlexTradeFileUtil.getUniqueValue();
        DCIteratorBinding flexTradeIter = ADFUtils.findIterator("FlexTradeDataVOIterator");
        File newGenFile = new File(srcTradeFilePath);
        String fileNameDatePart = FlexTradeFileUtil.getFileNameDatePart(fileName);
        this.dumpDataFromFlexTradeFile(newGenFile, fileId, fileName);
        curFtpTradeFileRow.setAttribute("Status", "P");
        OperationBinding operationCommit = null;
        operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
        if (operationCommit != null) {
            try {
                operationCommit.execute();
                if (operationCommit.getErrors().isEmpty()) {
                    isProcessCall = dataDump.dumpFlexTradeData(fileId);
                    if (isProcessCall) {
                        dumpTrade = true;
                    } else {
                        curFtpTradeFileRow.setAttribute("Status", "N");
                        operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
                        operationCommit.execute();
                        dumpTrade = false;
                    }
                } else {
                    System.out.println("-----" + operationCommit.getErrors());
                    JSFUtils.addFacesErrorMessage("Problem in Loading Flex Trade File");
                    JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                    flexTradeIter.executeQuery();
                }
            } catch (Exception e) {
                e.printStackTrace();
                e.getMessage();
                JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                flexTradeIter.executeQuery();
            }
        }
        return dumpTrade;
    }

    public boolean dumpFlexPriceFileData(String srcPriceFilePath, Row curFtpPriceFileRow, String fileName) {
        boolean dumpPrice = false;
        boolean isProcessCall = false;
        String fileId = FlexTradeFileUtil.getUniqueValue();
        DCIteratorBinding eodTickerIter = ADFUtils.findIterator("EodTickerVOIterator");
        File newGenFile = new File(srcPriceFilePath);
        this.dumpDataFromFlexPriceFile(newGenFile, fileId, fileName);
        curFtpPriceFileRow.setAttribute("Status", "P");
        OperationBinding operationCommit = null;
        operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
        if (operationCommit != null) {
            try {
                operationCommit.execute();
                if (operationCommit.getErrors().isEmpty()) {
                    isProcessCall = dataDump.dumpFlexPriceData(fileId);
                    if (isProcessCall) {
                        dumpPrice = true;
                    } else {
                        curFtpPriceFileRow.setAttribute("Status", "N");
                        operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
                        operationCommit.execute();
                        dumpPrice = false;
                    }
                } else {
                    System.out.println("-----" + operationCommit.getErrors());
                    JSFUtils.addFacesErrorMessage("Problem in Loading EOD Price File");
                    JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                    eodTickerIter.executeQuery();
                }
            } catch (Exception e) {
                e.printStackTrace();
                e.getMessage();
                JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                eodTickerIter.executeQuery();
            }
        }

        return dumpPrice;
    }

    /*
    private ActionEvent acEvent = null;

    public void setAcEvent(ActionEvent acEvent) {
        this.acEvent = acEvent;
    }

    public ActionEvent getAcEvent() {
        return acEvent;
    }

    public void toggleBusyPopup(boolean isShown) {
        FacesContext context = FacesContext.getCurrentInstance();
        RichPopup popup = (RichPopup)JSFUtils.findComponentInRoot("spinnerPopUp");
        ExtendedRenderKitService service = Service.getRenderKitService(context, ExtendedRenderKitService.class);
        if (isShown) {
            service.addScript(context,
                              "var popup = AdfPage.PAGE.findComponent(\"" + popup.getClientId(context) + "\"); popup.show();");
        } else {
            service.addScript(context,
                              "var popup = AdfPage.PAGE.findComponent(\"" + popup.getClientId(context) + "\"); popup.hide();");
        }
        return;
    }


    public void processAction(ActionEvent acEvent) {
        setAcEvent(acEvent); // save teh query event for the method that really fires the query to use.
        toggleBusyPopup(true); // Fires the popup, which when shown, fires a server listener that fires the query.

    }

    public void ProcessAction(ClientEvent clientEvent) throws IOException, NamingException {
        // try {
        doUpload();
        // } catch (Exception e) {
            e.printStackTrace();
        }//
        toggleBusyPopup(false);
    }

    public void setToDateVal(String toDateVal) {
        this.toDateVal = toDateVal;
    }

    public String getToDateVal() {
        return toDateVal;
    }


    //Method to set the value of page flow scope created on runtime

    public void setPageFlowScopeValue(String name, Number value) {
        ADFContext adfCtx = ADFContext.getCurrent();
        Map pageFlowScope = adfCtx.getPageFlowScope();
        pageFlowScope.put(name, value);
    }

    //method to get the value of page flow scope created on runtime

    public Object getPageFlowScopeValue(String name) {
        ADFContext adfCtx = ADFContext.getCurrent();
        Map pageFlowScope = adfCtx.getPageFlowScope();
        Object val = pageFlowScope.get(name);

        if (val == null)
            return 0;
        else
            return val;
    }
/*
    public void setExchange(RichSelectOneChoice exchange) {

        this.exchange = exchange;
    }

    public RichSelectOneChoice getExchange() {
        return exchange;
    }
*/


    public void setTradeFileType(String tradeFileType) {
        this.tradeFileType = tradeFileType;
    }

    public int getPriceFileCount() {
        return priceFileCount;
    }


    public void setTradeFileRadio(RichSelectOneChoice tradeFileRadio) {
        this.tradeFileRadio = tradeFileRadio;
    }

    public RichSelectOneChoice getTradeFileRadio() {
        return tradeFileRadio;
    }


    public void setCurrentFilePath(String currentFilePath) {
        this.currentFilePath = currentFilePath;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public String dumpDataFromFlexPriceFile(File newPriceFile, String fileID, String fileName) {
        DCIteratorBinding eodTickerIter = ADFUtils.findIterator("EodTickerVOIterator");
        ViewObject vo = eodTickerIter.getViewObject();
        SAXBuilder saxbuilder = null;
        saxbuilder = new SAXBuilder();
        Integer counter = 0;
        try {
            Document document = null;
            document = saxbuilder.build(newPriceFile);
            Element rootNode = document.getRootElement();
            List list = rootNode.getChildren("Ticker");
            // ViewObject vo = eodTickerIter.getViewObject();
            // String fileId = FlexTradeFileUtil.getUniqueValue();
            for (int i = 0; i < list.size(); i++) {
                Element node = null;
                node = (Element)list.get(i);
                if (vo != null) {
                    Row eodTickerRow = vo.createRow();

                    eodTickerRow.setAttribute("FileId", fileID);
                    eodTickerRow.setAttribute("FileName", fileName);
                    eodTickerRow.setAttribute("ExchangeId", 10);
                    eodTickerRow.setAttribute("TradeDate",
                                              node.getAttributeValue("TradeDate") == null ? null : node.getAttributeValue("TradeDate").toString());
                    eodTickerRow.setAttribute("TradeTime",
                                              node.getAttributeValue("TradeTime") == null ? null : node.getAttributeValue("TradeTime").toString());
                    eodTickerRow.setAttribute("SecurityCode",
                                              node.getAttributeValue("SecurityCode") == null ? null : node.getAttributeValue("SecurityCode").toString());
                    eodTickerRow.setAttribute("Isin",
                                              node.getAttributeValue("ISIN") == null ? null : node.getAttributeValue("ISIN").toString());
                    eodTickerRow.setAttribute("InstrCategory",
                                              node.getAttributeValue("Category") == null ? null : node.getAttributeValue("Category").toString());
                    eodTickerRow.setAttribute("OpenPrice",
                                              node.getAttributeValue("Open") == null ? 0 : new BigDecimal(node.getAttributeValue("Open")).setScale(2,
                                                                                                                                                   BigDecimal.ROUND_HALF_UP));
                    eodTickerRow.setAttribute("HighPrice",
                                              node.getAttributeValue("High") == null ? 0 : new BigDecimal(node.getAttributeValue("High")).setScale(2,
                                                                                                                                                   BigDecimal.ROUND_HALF_UP));
                    eodTickerRow.setAttribute("LowPrice",
                                              node.getAttributeValue("Low") == null ? 0 : new BigDecimal(node.getAttributeValue("Low")).setScale(2,
                                                                                                                                                 BigDecimal.ROUND_HALF_UP));
                    // System.out.println(" == " + new BigDecimal(node.getAttributeValue("ClosingPrice")).setScale(2));
                    eodTickerRow.setAttribute("ClosePrice",
                                              node.getAttributeValue("Close") == null ? 0 : new BigDecimal(node.getAttributeValue("Close")).setScale(2,
                                                                                                                                                     BigDecimal.ROUND_HALF_UP));
                    eodTickerRow.setAttribute("CompulsorySpot",
                                              node.getAttributeValue("CompulsorySpot") == null ? null :
                                              node.getAttributeValue("CompulsorySpot").toString());
                    eodTickerRow.setAttribute("Sector",
                                              node.getAttributeValue("Sector") == null ? null : node.getAttributeValue("Sector").toString());
                    eodTickerRow.setAttribute("TickVar",
                                              node.getAttributeValue("Var") == null ? 0 : new BigDecimal(node.getAttributeValue("Var")).setScale(2,
                                                                                                                                                 BigDecimal.ROUND_HALF_UP));
                    eodTickerRow.setAttribute("VarPercent",
                                              node.getAttributeValue("VarPercent") == null ? 0 : new BigDecimal(node.getAttributeValue("VarPercent")).setScale(2,
                                                                                                                                                               BigDecimal.ROUND_HALF_UP));
                    eodTickerRow.setAttribute("AssetClass",
                                              node.getAttributeValue("AssetClass") == null ? null : node.getAttributeValue("AssetClass").toString());
                    counter++;
                    try {
                        eodTickerRow.validate();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (JSFUtils.getBindings().getOperationBinding("Rollback") != null)
                            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        JSFUtils.addFacesErrorMessage("Problem found in record " + counter +
                                                      "  Instrument Category :" + node.getAttributeValue("Category") +
                                                      "  Security Code :" + node.getAttributeValue("SecurityCode"));
                        JSFUtils.addFacesErrorMessage(e.getMessage());
                        return null;
                    }

                    //System.out.println("All attribute has set");
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            jdomex.printStackTrace();
            System.out.println(jdomex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage() + "Record No:" + counter);
        }
        return null;
    }

    public String dumpDataFromFlexTradeFile(File newTradeFile, String fileID, String fileName) {
        DCIteratorBinding flexTradeIter = ADFUtils.findIterator("FlexTradeDataVOIterator");
        ViewObject vo = flexTradeIter.getViewObject();
        String fileNameDatePart = FlexTradeFileUtil.getFileNameDatePart(fileName);
        SAXBuilder saxbuilder = null;
        saxbuilder = new SAXBuilder();
        Integer counter = 0;
        boolean isToValidate = isValidateTradeRow();
        try {
            Document document = null;
            document = saxbuilder.build(newTradeFile);
            Element rootNode = document.getRootElement();
            List list = rootNode.getChildren("Detail");
            for (int i = 0; i < list.size(); i++) {
                Element node = null;
                node = (Element)list.get(i);
                if (vo != null) {
                    Row flexTradeRow = vo.createRow();
                    flexTradeRow.setAttribute("FileId", fileID);
                    flexTradeRow.setAttribute("FileName", fileName);
                    flexTradeRow.setAttribute("Action",
                                              node.getAttributeValue("Action") == null ? null : node.getAttributeValue("Action").toString());
                    flexTradeRow.setAttribute("Assetclass",
                                              node.getAttributeValue("AssetClass") == null ? null : node.getAttributeValue("AssetClass").toString());
                    flexTradeRow.setAttribute("Board",
                                              node.getAttributeValue("Board") == null ? null : node.getAttributeValue("Board").toString());
                    flexTradeRow.setAttribute("Boid",
                                              node.getAttributeValue("BOID") == null ? null : node.getAttributeValue("BOID").toString());
                    flexTradeRow.setAttribute("BranchId", 11);
                    flexTradeRow.setAttribute("ClientCode",
                                              node.getAttributeValue("ClientCode") == null ? null : node.getAttributeValue("ClientCode").toString());
                    flexTradeRow.setAttribute("CompCategory",
                                              node.getAttributeValue("Category") == null ? null : node.getAttributeValue("Category").toString());
                    flexTradeRow.setAttribute("CompulsorySpot",
                                              node.getAttributeValue("CompulsorySpot") == null ? null :
                                              node.getAttributeValue("CompulsorySpot").toString());
                    flexTradeRow.setAttribute("Execid",
                                              node.getAttributeValue("ExecID") == null ? null : node.getAttributeValue("ExecID").toString());

                    //file date validation (date in file name & file row should be same)
                    if (node.getAttributeValue("Date") != null) {
                        String fileRowDate = node.getAttributeValue("Date").toString();
                        if (fileNameDatePart.equals(fileRowDate)) {
                            flexTradeRow.setAttribute("FileDate", fileRowDate);
                        } else {
                            JSFUtils.addFacesErrorMessage("Date mismatch found with file name and row !");
                            return null;
                        }
                    } else {
                        flexTradeRow.setAttribute("FileDate", null);
                    }
                    flexTradeRow.setAttribute("FileTime",
                                              node.getAttributeValue("Time") == null ? null : node.getAttributeValue("Time").toString());
                    flexTradeRow.setAttribute("FillType",
                                              node.getAttributeValue("FillType") == null ? null : node.getAttributeValue("FillType").toString());
                    flexTradeRow.setAttribute("ForeignFlag", null);
                    flexTradeRow.setAttribute("HowlaType", null);
                    flexTradeRow.setAttribute("Isin",
                                              node.getAttributeValue("ISIN") == null ? null : node.getAttributeValue("ISIN").toString());
                    flexTradeRow.setAttribute("OrderId",
                                              node.getAttributeValue("OrderID") == null ? null : node.getAttributeValue("OrderID").toString());
                    flexTradeRow.setAttribute("OwnerDealerId",
                                              node.getAttributeValue("OwnerDealerID") == null ? null : node.getAttributeValue("OwnerDealerID").toString());

                    flexTradeRow.setAttribute("Price",
                                              node.getAttributeValue("Price") == null ? 0 : new BigDecimal(node.getAttributeValue("Price")).setScale(2));
                    flexTradeRow.setAttribute("Quantity",
                                              node.getAttributeValue("Quantity") == null ? 0 : node.getAttributeValue("Quantity").toString());
                    flexTradeRow.setAttribute("RefOrderId",
                                              node.getAttributeValue("RefOrderID") == null ? null : node.getAttributeValue("RefOrderID").toString());
                    flexTradeRow.setAttribute("SecurityCode",
                                              node.getAttributeValue("SecurityCode") == null ? null : node.getAttributeValue("SecurityCode").toString());
                    flexTradeRow.setAttribute("SessionId",
                                              node.getAttributeValue("Session") == null ? null : node.getAttributeValue("Session").toString());
                    flexTradeRow.setAttribute("Side",
                                              node.getAttributeValue("Side") == null ? null : node.getAttributeValue("Side").toString());
                    flexTradeRow.setAttribute("Status",
                                              node.getAttributeValue("Status") == null ? null : node.getAttributeValue("Status").toString());
                    flexTradeRow.setAttribute("TradeContractNo", null);
                    flexTradeRow.setAttribute("TradeValue",
                                              node.getAttributeValue("Value") == null ? 0 : new BigDecimal(node.getAttributeValue("Value")).setScale(2));
                    flexTradeRow.setAttribute("TraderDealerId",
                                              node.getAttributeValue("TraderDealerID") == null ? null :
                                              node.getAttributeValue("TraderDealerID").toString());
                    flexTradeRow.setAttribute("Tradereporttype",
                                              node.getAttributeValue("TradeReportType") == null ? null :
                                              node.getAttributeValue("TradeReportType").toString());
                    counter++;

                    try {
                        if (isToValidate)
                            flexTradeRow.validate();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (JSFUtils.getBindings().getOperationBinding("Rollback") != null)
                            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        JSFUtils.addFacesErrorMessage("Problem found in record " + counter + "  Client Code :" +
                                                      node.getAttributeValue("ClientCode") + "  Security Code :" +
                                                      node.getAttributeValue("SecurityCode"));
                        JSFUtils.addFacesErrorMessage(e.getMessage());
                    }
                    //System.out.println("All attribute has set");
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            System.out.println("Err--Record no ***--" + counter);
            jdomex.printStackTrace();
            System.out.println(jdomex.getMessage());
        } catch (Exception ex) {
            System.out.println("Errr-Record no ***--" + counter);
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }
        return null;
    }

    //conditionaly enable/disable control file upload button

    public boolean isControlRequired() {
        boolean isDisable = false;
        String fileType = null;
        if (this.tradeFileRadio.getValue() != null) {
            fileType = this.tradeFileRadio.getValue().toString();
            //  System.out.println("file type----" + fileType);
            if (fileType.equalsIgnoreCase("Flex Price File")) {
                String priceValidation =
                    FlexTradeFileUtil._getAttrValueFromIter("FtiConfigParamPriceVOIterator", "ControlValidation");
                // System.out.println("price validation----" + priceValidation);
                if (priceValidation.equalsIgnoreCase("Y"))
                    isDisable = true;
            } else if (fileType.equalsIgnoreCase("Flex Trade File")) {
                String tradeValidation =
                    FlexTradeFileUtil._getAttrValueFromIter("FtiConfigParamTradeVOIterator", "ControlValidation");
                // System.out.println("trade validation----" + tradeValidation);
                if (tradeValidation.equalsIgnoreCase("Y"))
                    isDisable = true;
            }
        }
        return isDisable;
    }

    private boolean isValidateTradeRow() {
        boolean isValidate = false;
        String field_value = "TRADE_ROW_VALIDATE";
        try {
            String packg_function = "pkg_capita_utl.get_table_col_string_value(?,?,?,?,?,?)";
            Object[] params =
            { "FTI_CONFIG_PARAM", "CONTROL_VALIDATION", "FILE_TYPE", "'" + field_value + "'", null, null }; //Sp Params
            String rVal = TMPlsqlExecutor.getdbCallStringReturn(packg_function, params);
            if (rVal.equalsIgnoreCase("Y"))
                isValidate = true;
            else
                isValidate = false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isValidate;
    }


}
