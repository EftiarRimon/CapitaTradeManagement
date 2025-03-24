package leads.capita.trade.view.backing;
/*Main Uddin Patowary */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.math.BigDecimal;

import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Date;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import javax.servlet.ServletContext;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.file.PayInOutFileUtil;
import leads.capita.trade.plsql.PPPlsqlExecutor;

import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.input.RichSelectBooleanCheckbox;
import oracle.adf.view.rich.render.ClientEvent;

import oracle.jbo.Row;

import org.apache.myfaces.trinidad.model.UploadedFile;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;


/*
 * Main Uddin Patowary
 */
public class PayInPayOut {
    static String user_home = System.getProperty("user.home");
    static String payinoutFolder = user_home + File.separator + "payinout";
    static String payinoutLatestFolder = user_home + File.separator + "payinout" + File.separator + "latest";
    private static String PAY_IN = "01";
    private static String PAY_OUT = "02";
    String fileName = "";
    private UploadedFile _file;
    FacesContext fctx;
    private RichInputText generatedFileNameUI;
    private String lastGeneratedFile = null;
    StringBuffer errMsg = null;
    private RichSelectBooleanCheckbox withOwnCB;

    public PayInPayOut() {
        super();
        fctx = FacesContext.getCurrentInstance();
        createDefaultFolder(); //payinoutLatestFolder
        fileName = this.generateFileName();
    }
    //file download....

    public void dwdFile(FacesContext facesContext, OutputStream out) throws UnsupportedEncodingException, IOException {
        //String user_home = System.getProperty("user.home");
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        //System.out.println(context.getRealPath("/"));
        File home_dirFile =
            new File(payinoutLatestFolder + File.separator + generatedFileNameUI.getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + generatedFileNameUI.getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            this.setFileName(f.getName());
            fdownload = new FileInputStream(f);
            int n;
            while ((n = fdownload.available()) > 0) {
                b = new byte[n];
                int result = fdownload.read(b);
                out.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            out.flush();
            new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        }
    }
    // write data to file...

    public boolean _writeFile(String businessType, String fileType, String exchange, String broker, String dpId,
                              String iteratorName, String productTypes) {
        //String user_home = System.getProperty("user.home");
        FacesContext fctx = FacesContext.getCurrentInstance();
        //ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        try {
            PayInOutFileUtil.moveFile(new File(payinoutLatestFolder),
                                      new File(payinoutFolder)); //delete old(fast dated) files
        } catch (IOException e) {
            e.printStackTrace();
        }
        File home_dirFile = new File(payinoutLatestFolder + File.separator + fileName);
        generatedFileNameUI.setValue(fileName);
        if (!home_dirFile.exists()) {
            try {
                home_dirFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        Writer writer = null;
        try {
            String withOwn = null;
            if (this.getWithOwnCB().getValue() != null)
                withOwn = this.getWithOwnCB().getValue().toString();
           // System.out.println("withOwn-----" + withOwn);
            DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
            iter.setRangeSize((int)iter.getEstimatedRowCount());
            if (withOwn !=null && withOwn.equalsIgnoreCase("true"))
                PayInOutFileUtil.filterIteratorBydate(iteratorName, PayInOutFileUtil.getUiTradeDate(), exchange,
                                                      fileType, productTypes);
            else
                PayInOutFileUtil.filterIteratorBydate(iteratorName, PayInOutFileUtil.getUiTradeDate(), exchange,
                                                      withOwn, fileType, productTypes);

            String newLine = String.format("%n");
            File file = new File(home_dirFile.getPath());
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(this.generateFileHeader() + newLine); //File Header
            if (iter != null) {
                // iter.setRangeSize(-1);
                Row rows[] = iter.getAllRowsInRange();
                System.out.println("------------" + rows.length);
                for (int i = 0; i < rows.length; i++) {
                    Row row = rows[i];
                    StringBuilder contents = null;
                    contents = new StringBuilder();
                    contents.append(this.generateFileLine(row, businessType, fileType, exchange, broker, dpId));
                    contents.append(_createSpace(12 - (Integer.toString(i + 1).trim().length())) +
                                    (i + 1)); //---------------Space--------------
                    contents.append(newLine);
                    writer.write(contents.toString());
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private String _createSpace(int length) {
        String space = "";
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                space += " ";
            }
        }
        return space;
    }

    //Upload file process...

    public String processUpload() {
        UploadedFile myfile = this.getFile();
        //String user_home = System.getProperty("user.home");
        File home_dirFile = new File(payinoutLatestFolder + File.separator + myfile.getFilename());
        try {
            if (!home_dirFile.exists())
                home_dirFile.createNewFile();
            else {
                String newFileName = this.generateFileName();
            }
            PrintStream newFile = new PrintStream(home_dirFile);
            InputStream inputStream = myfile.getInputStream();
            //long length = myfile.getLength();
            while (inputStream.available() > 0) {
                byte[] buffer = new byte[1024];
                inputStream.read(buffer);
                newFile.write(buffer);
            }
            newFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void set_file(UploadedFile file) {
        this._file = file;
    }

    public UploadedFile getFile() {
        return _file;
    }

    public void _fileSelection(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fctx);
        this._file = (UploadedFile)valueChangeEvent.getNewValue();
    }
    
    private String getProductTypes() {
        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        String pType = null;
        if (iter != null) {
            Row row = iter.getCurrentRow();
            if (row.getAttribute("ProductType") != null){
                if(row.getAttribute("ProductType").toString().equalsIgnoreCase("Y"))    
                    pType = "('NITA')";
                else   
                    pType = "('NITA')";
            }else  
                    pType = "('NITA')";
        }
        return pType;
    }

    public String ProcessFile() throws SQLException, Exception {
        String businessType = null;
        String fileType = null;
        String exchange = null;
        String broker = null;
        String dpId = null;
        String productTypes = null;
        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        if (iter != null) {
            Row row = iter.getCurrentRow();
            if (row.getAttribute("BusinessType") != null)
                businessType = row.getAttribute("BusinessType").toString();
            if (row.getAttribute("Type") != null)
                fileType = row.getAttribute("Type").toString();
            if (row.getAttribute("Exchange") != null)
                exchange = row.getAttribute("Exchange").toString();
            if (row.getAttribute("Broker") != null)
                broker = row.getAttribute("Broker").toString();
        }
        productTypes=getProductTypes();


        if (fileType.equals(PAY_IN)) {
            //_writeFile(businessType, fileType, exchange, broker, dpId, "PayinSettlementListVOIterator");
            TMPlsqlExecutor.generateInstrPayInOut(fileType, PayInOutFileUtil.getSystemDate("dd-MMM-yyyy", PayInOutFileUtil.getUiTradeDate()));
            _writeFile(businessType, fileType, exchange, broker, dpId, "InstrPayinOutVOIterator", productTypes);
            JSFUtils.addFacesInformationMessage("Payin file has been generated ..");
        } else if (fileType.equals(PAY_OUT)) {
            //_writeFile(businessType, fileType, exchange, broker, dpId, "PayoutSettlementListVOIterator");
            TMPlsqlExecutor.generateInstrPayInOut(fileType, PayInOutFileUtil.getSystemDate("dd-MMM-yyyy", PayInOutFileUtil.getUiTradeDate()));
            _writeFile(businessType, fileType, exchange, broker, dpId, "InstrPayOutVOIterator", productTypes);
            JSFUtils.addFacesInformationMessage("Payout file has been generated ..");
        } else {
            System.out.print("++Pay I/O++");
        }
        return null;
    }


    public BigDecimal getTotalQuantity() {
        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        String fileType = null;
        BigDecimal _total = null;
        String exCode = null;
        if (iter != null) {
            Row row = iter.getCurrentRow();
            if (row.getAttribute("Type") != null)
                fileType = row.getAttribute("Type").toString();
            if (row.getAttribute("Exchange") != null)
                exCode = row.getAttribute("Exchange").toString();
            //PayInOutFileUtil.filterIteratorBydate("PayinSettlementListVOIterator", getSystemTradeDate(), exCode);
        }
        if (fileType.equals(PAY_IN)) {
            /*  PayInOutFileUtil.filterIteratorBydate("PayinSettlementListVOIterator",
                                                  PayInOutFileUtil.getUiTradeDate("PayinPayoutVOIterator", "Sysdate"),
                                                  exCode, fileType); */
            // PayInOutFileUtil.filterIteratorByExchange("PayinSettlementListVOIterator", exCode);
            _total = rowsColumnTotal("InstrPayinOutVOIterator", "Sell", "SelectedRow", "true");
        } else if (fileType.equals(PAY_OUT)) {
            //PayInOutFileUtil.filterIteratorBydate("PayoutSettlementListVOIterator", getSystemTradeDate(), exCode);
            //PayInOutFileUtil.filterIteratorByExchange("PayoutSettlementListVOIterator", exCode);
            _total = rowsColumnTotal("InstrPayOutVOIterator", "Buy", "SelectedRow", "true");
        }
        return _total;
    }

    public Long getTotalRecords() {
        DCIteratorBinding it = ADFUtils.findIterator("PayinPayoutVOIterator");
        String fileType = null;
        Integer _total = null;
        String exCode = null;
        Long _totalRecords = null;
        DCIteratorBinding iter = null;
        if (it != null) {
            Row row = it.getCurrentRow();
            if (row.getAttribute("Type") != null)
                fileType = row.getAttribute("Type").toString();
            if (row.getAttribute("Exchange") != null)
                exCode = row.getAttribute("Exchange").toString();
        }
        if (fileType.equals(PAY_IN)) {
            /* PayInOutFileUtil.filterIteratorBydate("PayinSettlementListVOIterator",
                                                  PayInOutFileUtil.getUiTradeDate("PayinPayoutVOIterator", "Sysdate"),
                                                  exCode, fileType); */
            iter = ADFUtils.findIterator("InstrPayinOutVOIterator");
            _totalRecords = iter.getEstimatedRowCount();
        } else {
            /*  PayInOutFileUtil.filterIteratorBydate("PayoutSettlementListVOIterator",
                                                  PayInOutFileUtil.getUiTradeDate("PayinPayoutVOIterator", "Sysdate"),
                                                  exCode, fileType); */
            iter = ADFUtils.findIterator("InstrPayOutVOIterator");
            _totalRecords = iter.getEstimatedRowCount();
        }
        return _totalRecords != null ? _totalRecords : new Long(0);
    }

    private Date getSystemTradeDate() {
        //5/23/2012
        Date d = null;
        try {
            //System.out.println("-------------" + ApplicationInfo.getSystemDate());
            // Date dt=ApplicationInfo.getSystemDate();
            d =
  PayInOutFileUtil.getParsedDate(ApplicationInfo.getSystemDate() == null ? "2012-05-23" : ApplicationInfo.getSystemDate(),
                                 "yyyy-MM-dd");
            //  System.out.print("______"+PayInOutFileUtil.getParsedDate(new Date().get));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return d;
    }


    public synchronized BigDecimal rowsColumnTotal(String iteratorName, String colName, String chkBoxAttr,
                                                   String chkBoxAttVal) {
        DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        Row rows[] = iter.getAllRowsInRange();
        BigDecimal total = new BigDecimal(0);
        if (rows != null && rows.length > 0) {
            for (int i = 0; i < rows.length; i++) {
                if (rows[i].getAttribute(colName) != null)
                    total = total.add(new BigDecimal(rows[i].getAttribute(colName).toString()));
            }
        }
        return total;
    }

    public String generateFileName() {
        String fileName = null;
        String businessType = null;
        String fileType = null;
        String exchange = null;
        String broker = null;
        String dpId = null;
        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        if (iter != null) {
            Row rows[] = iter.getAllRowsInRange();
            if (rows != null && rows.length > 0) {
                Row row = rows[0];
                if (row.getAttribute("BusinessType") != null)
                    businessType = row.getAttribute("BusinessType").toString();
                if (row.getAttribute("Type") != null)
                    fileType = row.getAttribute("Type").toString();
                if (row.getAttribute("Exchange") != null)
                    exchange = row.getAttribute("Exchange").toString();
                if (row.getAttribute("Broker") != null)
                    broker = row.getAttribute("Broker").toString();
            }
        }
        fileName = PayInOutFileUtil._generateFileName(fileType, _getDpId(), new Date(), exchange);
        return fileName;
    }

    public String generateFileHeader() {
        String fileHeader = null;
        String exchange = null;
        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        if (iter != null) {
            Row rows[] = iter.getAllRowsInRange();
            if (rows != null && rows.length > 0) {
                Row row = rows[0];
                if (row.getAttribute("Exchange") != null)
                    exchange = row.getAttribute("Exchange").toString();
                //System.out.print("====="+exchange);
            }
        }
        fileHeader =
                PayInOutFileUtil._generateFileHeader(getTotalRecords().toString(), getTotalQuantity().toString(), "ADMIN",
                                                     _getDpId(), exchange);
        return fileHeader;
    }

    //    private String _getDpId() {
    //        return "57000";
    //    }

    //OWN DPID

    private String _getDpId() {
        //"015300"
        String exchange = null;
        DCIteratorBinding iter = ADFUtils.findIterator("PayinPayoutVOIterator");
        if (iter != null) {
            Row rows[] = iter.getAllRowsInRange();
            if (rows != null && rows.length > 0) {
                Row row = iter.getCurrentRow();
                if (row.getAttribute("Exchange") != null)
                    exchange = row.getAttribute("Exchange").toString();
            }
        }
        String dp = new PPPlsqlExecutor().getDepositoryCode(exchange);
        // PayInOutFileUtil._getFixedLengthString(dp, 6, true, '0', ' ')
        return PayInOutFileUtil._getFixedLengthString(dp, 6, true, '0', '0');

    }


    public Date _getCapitaSystemdate() {
        return PayInOutFileUtil.getParsedDate(ApplicationInfo.getSystemDate(), "yyyy-MM-dd");
    }

    public String generateFileLine(Row row, String businessType, String fileType, String exchange, String broker,
                                   String dpId) {
        String genLine = null;
        String dateFormat = "ddMMyyyy";
        String boId = null;
        String payIOFlag = null;
        String counterBoID = null;
        String isin = null;
        String investorCode = null;
        String tradeQty = null;
        String settlementId = null;
        String clearingBoId = null; //counter
        String brokerCode = null;
        String shortName = null;
        Timestamp payInOutDate = null;
        if (row != null) {
            //ClearingBoId,ShortName
            if (row.getAttribute("Boid") != null)
                boId = row.getAttribute("Boid").toString();
            if (row.getAttribute("Isin") != null)
                isin = row.getAttribute("Isin").toString();
            if (row.getAttribute("InvestorCode") != null)
                investorCode = row.getAttribute("InvestorCode").toString();
            if (row.getAttribute("InsSettleId") != null)
                settlementId = row.getAttribute("InsSettleId").toString();
            if (row.getAttribute("ClearingBoId") != null)
                clearingBoId = row.getAttribute("ClearingBoId").toString();
            if (row.getAttribute("ShortName") != null)
                shortName = row.getAttribute("ShortName").toString();
            if (row.getAttribute("BrokerCode") != null)
                brokerCode = row.getAttribute("BrokerCode").toString();
            if (row.getAttribute("PayInOutDate") != null) {
                payInOutDate = (Timestamp)row.getAttribute("PayInOutDate");
                //System.out.print("----------------"+settlementDate);
            }

            if (isin == null) {
                // JSFUtils.addFacesErrorMessage("ISIN not found for instrument " + shortName);
                System.out.println(" --isin -- missing for P.Out -> ");
            } else if (clearingBoId == null) {
                System.out.println(" --clearingBoId -- missing for P.Out -> ");
                //JSFUtils.addFacesErrorMessage("Clearing account Missing !");
            } else {
                if (fileType.equals(PAY_IN)) {
                    payIOFlag = "I";
                    if (row.getAttribute("Sell") != null)
                        tradeQty = row.getAttribute("Sell").toString();
                } else {
                    payIOFlag = "O";
                    if (row.getAttribute("Buy") != null)
                        tradeQty = row.getAttribute("Buy").toString();
                }
            }
            genLine =
                    PayInOutFileUtil._generateFileRow(dateFormat, new Date(payInOutDate.getTime()), boId, clearingBoId,
                                                      brokerCode, isin, tradeQty, payIOFlag, investorCode, shortName,
                                                      fileType);
        }
        return genLine;
    }

    public void setGeneratedFileNameUI(RichInputText generatedFileNameUI) {
        this.generatedFileNameUI = generatedFileNameUI;
    }

    public RichInputText getGeneratedFileNameUI() {
        return generatedFileNameUI;
    }

    public void setLastGeneratedFile(String lastGeneratedFile) {
        this.lastGeneratedFile = lastGeneratedFile;
    }

    public String getLastGeneratedFile() {
        File folder = new File(payinoutLatestFolder);
        if (!folder.exists())
            folder.mkdirs();
        return PayInOutFileUtil.lastCreatedFile(folder);
    }


    private void createDefaultFolder() {
        File default_folder = null;
        default_folder = new File(payinoutLatestFolder);
        if (!default_folder.exists()) {
            default_folder.mkdirs();
        }

    }


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
        if (this.getWithOwnCB().getValue()!=null){
            String withOwn = this.getWithOwnCB().getValue().toString();
        }
        // System.out.println("Value of WithOwn=" + withOwn);
        setAcEvent(acEvent); // save teh query event for the method that really fires the query to use.
        toggleBusyPopup(true); // Fires the popup, which when shown, fires a server listener that fires the query.

    }

    public void ProcessAction(ClientEvent clientEvent) {
        try {
            ProcessFile();
            //String withOwn = this.getWithOwnCB().getValue().toString();
            // System.out.println("Value of WithOwn=" + withOwn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        toggleBusyPopup(false);
    }

    public void setWithOwnCB(RichSelectBooleanCheckbox withOwnCB) {
        this.withOwnCB = withOwnCB;
    }

    public RichSelectBooleanCheckbox getWithOwnCB() {
        return withOwnCB;
    }

}
