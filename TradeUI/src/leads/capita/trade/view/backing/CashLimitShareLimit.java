package leads.capita.trade.view.backing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.math.BigDecimal;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import javax.servlet.ServletContext;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.bean.BaseBeanUtil;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.input.RichSelectBooleanCheckbox;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;
import oracle.adf.view.rich.component.rich.nav.RichCommandButton;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;


public class CashLimitShareLimit {
    private FacesContext fct;
    private FacesContext fctx;

    private RichSelectOneChoice fileType;

    private RichSelectOneChoice fileTypeCSEDSE;

    private RichInputText shareLimit;

    static String user_home = System.getProperty("user.home");

    static String cashLimitDir = user_home + File.separator;
    static String cashLimitDirInv = user_home + File.separator;

    private RichInputText generatedFileNameUI;
    private String downloadFileName;

    private RichCommandButton cbDownLoad;
    private RichSelectOneChoice brokerDId;
    private RichSelectBooleanCheckbox withOwnCB;
    private RichInputText generatedFileInv;
    private RichCommandButton cbDownLoadInv;
    private RichSelectBooleanCheckbox processWithAllBrokerUI;

    public CashLimitShareLimit() {
        super();
        fct = JSFUtils.getFacesContextApp();
        fctx = JSFUtils.getFacesContextApp();
    }

    private String getSystemDateCash() {
        String sysDate = null;
        try {

            Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());

            //          Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse("2013-07-30");

            sysDate = new SimpleDateFormat("dd-MM-yyyy").format(rawDate);

        } catch (Exception e) {
            //System.out.print("Date  Formetting Problem--Cash !!");
            JSFUtils.addFacesErrorMessage("Date  Formetting Problem--Cash !!");
        }
        return sysDate;
    }

    private String getSystemDateCashCSE() {
        String sysDate = null;
        try {

            Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());
            // Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse("2013-07-30");
            sysDate = new SimpleDateFormat("yyyy-MM-dd").format(rawDate);

        } catch (Exception e) {
            // System.out.print("Date  Formetting Problem--Cash !!");
            JSFUtils.addFacesErrorMessage("Date  Formetting Problem--Cash !!");
        }
        return sysDate;
    }

    public void fileTypeValueChange(ValueChangeEvent valueChangeEvent) {

        valueChangeEvent.getComponent().processUpdates(fct);
        cbDownLoadInv.setDisabled(true);
        cbDownLoad.setDisabled(true);

    }

    // first create the file

    //then copy the file from the location

    //then print the file in another location

    public void generateFileListener(FacesContext facesContext,
                                     OutputStream outputStream) throws UnsupportedEncodingException, IOException {
        // Add event code here...
        //  generatedFileNameUI.setValue("CashLimit.txt");
        // change made here
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();

        try {
            //  File processedfile = new File(cashLimitDir);

            File processedfile = null;
            if (ApplicationInfo.getBusinessType().equals("BROKER")) {
                processedfile = new File(cashLimitDir + "CashLimit.txt");
            } else {
                if (brokerDId.getValue() != null){
                    processedfile = new File(cashLimitDir + brokerDId.getValue().toString() + " - CashLimit - "+ fileTypeCSEDSE.getValue().toString() +".txt");
                }else{
                    processedfile = new File(cashLimitDir + "CashLimit-" + ApplicationInfo.getSystemDate()+ " - " + fileTypeCSEDSE.getValue().toString() +".txt");
                }
            }
            //CashLimit
            //ShareLimit
            File userPC =
                new File(context.getRealPath("/") + File.separator + generatedFileNameUI.getValue().toString());
            FileInputStream fdownLoad;

            byte[] b;

            DCIteratorBinding dcIter = ADFUtils.findIterator("CashLimitLimitVOIterator");

            Long rows = (dcIter.getViewObject().getEstimatedRowCount());


            if (rows > 0) {
                try {
                    File f = new File(processedfile.getPath());
                    fdownLoad = new FileInputStream(f);
                    int n;
                    while ((n = fdownLoad.available()) > 0) {
                        b = new byte[n];
                        int result = fdownLoad.read(b);
                        outputStream.write(b, 0, b.length);
                        if (result == -1)
                            break;
                    }

                    outputStream.flush();
                    //              new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
                    copyFile(processedfile, userPC);
                } catch (IOException e) {
                    JSFUtils.addFacesErrorMessage("Error Reading file");
                }
            } else {
                fct.addMessage("!Info", new FacesMessage("No data to read"));
            }
            cbDownLoad.setDisabled(true);

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void generatedFileInvestor(FacesContext facesContext,
                                      OutputStream outputStream) throws UnsupportedEncodingException, IOException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        try {
            File procesedfileInv = null;
            if (ApplicationInfo.getBusinessType().equals("BROKER")) {
                procesedfileInv = new File(cashLimitDirInv + "CashLimitInv.txt");
            } else {
                if (brokerDId.getValue() != null)
                    procesedfileInv =
                            new File(cashLimitDirInv + brokerDId.getValue().toString() + " - CashLimitInv - "+ fileTypeCSEDSE.getValue().toString() +".txt");
                else
                    procesedfileInv =
                            new File(cashLimitDirInv + "CashLimitInv-" + ApplicationInfo.getSystemDate()+ " - " + fileTypeCSEDSE.getValue().toString() + ".txt");
            }
            File userPCInv =
                new File(context.getRealPath("/") + File.separator + generatedFileInv.getValue().toString());


            FileInputStream fdownLoadInv;

            byte[] b;

            DCIteratorBinding dcIter = ADFUtils.findIterator("CashLimitLimitVOIterator");

            Long rows = (dcIter.getViewObject().getEstimatedRowCount());


            if (rows > 0) {
                try {
                    File fInv = new File(procesedfileInv.getPath());
                    fdownLoadInv = new FileInputStream(fInv);
                    int n;
                    while ((n = fdownLoadInv.available()) > 0) {
                        b = new byte[n];
                        int result = fdownLoadInv.read(b);
                        outputStream.write(b, 0, b.length);
                        if (result == -1)
                            break;
                    }

                    outputStream.flush();
                    //              new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
                    copyFile(procesedfileInv, userPCInv);
                } catch (IOException e) {
                    JSFUtils.addFacesErrorMessage("Error Reading file");
                }
            } else {
                fct.addMessage("!Info", new FacesMessage("No data to read"));
            }
            cbDownLoadInv.setDisabled(true);

        } catch (Exception e) {
            e.getMessage();
        }
    }


    public void copyFile(File src, File dst) throws IOException {

        DCIteratorBinding dcIter = ADFUtils.findIterator("CashLimitLimitVOIterator");

        Long rows = (dcIter.getViewObject().getEstimatedRowCount());


        if (rows > 0) {
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
        } else {
            fct.addMessage("!Info", new FacesMessage("No data to read"));
        }
    }

    public String processFile_new() throws Exception {
     
        String businessType = ApplicationInfo.getBusinessType();

        //broker flow
        if (businessType.equals("BROKER")) {
            if ((((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue() != null)) {
                String fileType =
                    (((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString());

                Double sellLimit = null;
                if (this.getShareLimit().getValue() != null &&
                    !this.getShareLimit().getValue().toString().equals("")) {

                    try {
                        sellLimit = Double.parseDouble(this.getShareLimit().getValue().toString());


                    } catch (Exception e) {
                        fct.addMessage("Error Msg", new FacesMessage("Please enter numeric value " + e.getMessage()));
                    }

                    if (((RichInputText)JSFUtils.findComponentInRoot("itShareLimit")).getValue() == null) {

                        sellLimit = 0.0;
                    }

                    // createShareLimit("BROKER", sellLimit);
                }

                createCashLimit_new(businessType, sellLimit);
                cbDownLoad.setDisabled(false);
                cbDownLoadInv.setDisabled(false);
            }

        } else {
            // MBank flow
            mbankValidation(((RichSelectOneChoice)JSFUtils.findComponentInRoot("socBroker")).getValue(),
                            this.getProcessWithAllBrokerUI().getValue());
            // if (((RichSelectOneChoice)JSFUtils.findComponentInRoot("socBroker")).getValue() != null) {
            if ((((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue() != null)) {
                String fileType =
                    (((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString());
                Double sellLimit = null;
                if (this.getShareLimit().getValue() != null &&
                    !this.getShareLimit().getValue().toString().equals("")) {
                    try {
                        sellLimit = Double.parseDouble(this.getShareLimit().getValue().toString());


                    } catch (Exception e) {
                        fct.addMessage("Error Msg", new FacesMessage("Please enter numeric value  " + e.getMessage()));
                    }
                    if (((RichInputText)JSFUtils.findComponentInRoot("itShareLimit")).getValue() == null) {

                        sellLimit = 0.0;
                    }

                    // createShareLimit("BROKER", sellLimit);
                }

                createCashLimit_new(businessType, sellLimit);
                cbDownLoad.setDisabled(false);
                cbDownLoadInv.setDisabled(false);
            }
            /* }else {
                fct.addMessage("Info", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please Select Broker"));
            } */
        }

        return null;


    }

    private void mbankValidation(Object brokerVal, Object procAllVal) throws Exception {
        if ((procAllVal == null || procAllVal.toString().equals("false")) && brokerVal == null) {
            fct.addMessage("Info", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please Select Broker"));
            throw new Exception("Select A Broker !!");
        }
    }

    private void createCashLimit_new(String businessType, Double sellLimit) {

        File home_dir = null;
        File home_dirInv = null;
        DCIteratorBinding dcIter = ADFUtils.findIterator("CashLimitLimitVOIterator");
        ViewObject vo = ADFUtils.findIterator("CashLimitLimitVOIterator").getViewObject();
        String holdingDate = getSystemDateCash();
        
        if (businessType.equalsIgnoreCase("MBANK")) {
            getCurrentSettingsMBCash(businessType);
            //dcIter.setRangeSize((int)dcIter.getEstimatedRowCount());

            Row[] r = dcIter.getViewObject().getAllRowsInRange();
            /*
            Long noOfLines1 = (dcIter.getViewObject().getEstimatedRowCount());
            String nolines = noOfLines1.toString();
            Integer noOfLines = Integer.valueOf(nolines);
            */
            int len = r.length;
            
            if (len > 0) {
                try {
                    String fileType =
                        (((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString());
                    if (brokerDId.getValue() != null) {
                        home_dir = new File(cashLimitDir + brokerDId.getValue().toString() + " - CashLimit - "+ fileType +".txt");
                        home_dirInv =
                                new File(cashLimitDirInv + brokerDId.getValue().toString() + " - CashLimitInv - " + fileType + ".txt");
                    } else {
                        home_dir = new File(cashLimitDir + "CashLimit-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                        home_dirInv =
                                new File(cashLimitDirInv + "CashLimitInv-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                    }
                    FileWriter stream = new FileWriter(home_dir);
                    Writer output = new BufferedWriter(stream);
                    FileWriter streamInv = new FileWriter(home_dirInv);
                    Writer outputInv = new BufferedWriter(streamInv);

                    String linegap = String.format("%n");

                    String investorCode = null;
                    String investorName = null;
                    String BOID = null;
                    String isForeign = null;
                    String panicWithdraw = null;
                    String prdType = null;
                    List notMapTrdInv = new ArrayList();
                    String holdingDateUP = getSystemDateCashCSE();
                    
                    for (int i = 0; i < len; i++) {
                        Row preadRow = r[i];

                        if (preadRow.getAttribute("ProductType") != null &&
                            !(preadRow.getAttribute("ProductType").toString().equalsIgnoreCase(""))) {
                            prdType = preadRow.getAttribute("ProductType").toString();
                        }
                        //if (prdType.equalsIgnoreCase("IDA") || prdType.equalsIgnoreCase("OWN")) {
                        if (preadRow.getAttribute("TradingCode") != null &&
                            !(preadRow.getAttribute("TradingCode").toString().equalsIgnoreCase(""))) {
                            investorCode = preadRow.getAttribute("TradingCode").toString();
                        } else {
                            notMapTrdInv.add(preadRow.getAttribute("InvestorCode").toString());
                            continue;
                        }

                        if (preadRow.getAttribute("Name") != null &&
                            !(preadRow.getAttribute("Name").toString().equalsIgnoreCase(""))) {
                            investorName = preadRow.getAttribute("Name").toString();
                        } else {
                            investorName = "na";
                        }

                        if (preadRow.getAttribute("Boid") != null &&
                            !(preadRow.getAttribute("Boid").toString().equalsIgnoreCase(""))) {
                            BOID = preadRow.getAttribute("Boid").toString();
                        } else {
                            BOID = "na";
                        }

                        if (preadRow.getAttribute("IsForeign") != null &&
                            !(preadRow.getAttribute("IsForeign").toString().equalsIgnoreCase(""))) {
                            isForeign = preadRow.getAttribute("IsForeign").toString();
                        } else {
                            isForeign = "n";
                        }

                        if (preadRow.getAttribute("PanicWithdraw") != null &&
                            !(preadRow.getAttribute("PanicWithdraw").toString().equalsIgnoreCase(""))) {
                            panicWithdraw = preadRow.getAttribute("PanicWithdraw").toString();
                        } else {
                            panicWithdraw = "n";
                        }
                        //get the buy limit
                        String cashLimit = preadRow.getAttribute("CashLimit").toString();
                        
                        Long bLimit = this.getBuyLimit(sellLimit, cashLimit, fileType);
                        //preadRow.setAttribute("BuyLimit", String.valueOf(bLimit));
                        Double bLimitNew = this.getBuyLimitNew(sellLimit, cashLimit);
                        
                        String printLine = null;
                        String printLineInv = null;

                        if (fileType.equalsIgnoreCase("DSE")) {

                            java.sql.Timestamp ts1 = new java.sql.Timestamp(new Date().getTime());
                            oracle.jbo.domain.Date jboDate = new oracle.jbo.domain.Date(ts1);
                            holdingDate = getSystemDateCash() + ":" + jboDate.timeValue().toString();

                            //                             printLine =investorCode + "~" + BOID + "~" + investorName + "~" + isForeign + "~" + bLimit.toString() +"~"+
                            //                                panicWithdraw + "~" + holdingDate;
                            //
                            //updated emdad july 30, 2013

                            printLine =
                                    investorCode + "~" + BOID + "~" + investorName + "~" + isForeign + "~" + bLimit.toString() +
                                    "~" + panicWithdraw + "~" + holdingDate;


                            // 1st Isforeign flag
                            //2nd panic widthdraw flag
                        } else {
                           
                            /*
                            printLine =
                                    investorCode + "|" + (new DecimalFormat("0.00").format(bLimit1)).toString() + "|" +
                                    holdingDate;
                            */
                            printLine =
                                    investorCode + "|" + bLimitNew.intValue() + "|" +
                                    holdingDateUP;
                            printLineInv =
                                    BOID + "~~~~" + investorName + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + investorCode;
                        }

                        output.write(printLine + linegap);
                        outputInv.write(printLineInv + linegap);

                    }
                    output.close();
                    outputInv.close();
                    if (brokerDId.getValue() != null) {
                        generatedFileNameUI.setValue(brokerDId.getValue().toString() + " - CashLimit - "+ fileType +".txt");
                        generatedFileInv.setValue(brokerDId.getValue().toString() + " - CashLimitInv - "+ fileType +".txt");
                    } else {
                        generatedFileNameUI.setValue("CashLimit-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                        generatedFileInv.setValue("CashLimitInv-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                    }
                    if (notMapTrdInv == null || notMapTrdInv.isEmpty()) {
                        fct.addMessage("Complete Msg", new FacesMessage("File Processing done Successfully"));
                    } else {
                        StringBuffer sb = new StringBuffer();
                        sb.append("Following investor does not exists Trading code: ");
                        boolean firstTime = true;
                        for (int i = 0; i < notMapTrdInv.size(); i++) {
                            if (!firstTime) {
                                sb.append(", ");
                            }
                            sb.append(notMapTrdInv.get(i));
                            firstTime = false;
                        }

                        fct.addMessage("Complete Msg", new FacesMessage("File Processing done Successfully"));
                        if (sb != null)
                            fct.addMessage("Fail Msg", new FacesMessage(sb.toString()));
                    }
                    notMapTrdInv.clear();
                    stream.close();
                    streamInv.close();
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                    e.getMessage();
                    System.gc();
                }
            } else {
                fct.addMessage("Complete Msg", new FacesMessage("No Data to generate"));
                generatedFileNameUI.setValue("CashLimit.txt");
            }

        } else {
            // for broker
            getCurrentSettingsMBCash(businessType);
            //dcIter.setRangeSize((int)dcIter.getEstimatedRowCount());
            Row[] r = dcIter.getViewObject().getAllRowsInRange();
            /*
            Long noOfLines1 = (dcIter.getViewObject().getEstimatedRowCount());
            String nolines = noOfLines1.toString();
            Integer noOfLines = Integer.valueOf(nolines);
            */
            int len = r.length;
            
            if (len > 0) {
                try {
                    String fileType =
                        (((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString());
                    
                    home_dir = new File(cashLimitDir + "CashLimit.txt");
                    FileWriter stream = new FileWriter(home_dir);
                    Writer output = new BufferedWriter(stream);

                    home_dirInv = new File(cashLimitDirInv + "CashLimitInv.txt");
                    FileWriter streamInv = new FileWriter(home_dirInv);
                    Writer outputInv = new BufferedWriter(streamInv);

                    String linegap = String.format("%n");

                    String investorCode = null;
                    String investorName = null;
                    String BOID = null;

                    Row preadRow = null;
                    holdingDate = getSystemDateCashCSE();
                    
                    for (int i = 0; i < len; i++) {
                        preadRow = r[i];
                        
                        investorCode = preadRow.getAttribute("TradingCode").toString();
                        if (preadRow.getAttribute("Name") != null &&
                            !(preadRow.getAttribute("Name").toString().equalsIgnoreCase(""))) {
                            investorName = preadRow.getAttribute("Name").toString();
                        } else {
                            investorName = "na";
                        }
                        if (preadRow.getAttribute("Boid") != null &&
                            !(preadRow.getAttribute("Boid").toString().equalsIgnoreCase(""))) {
                            BOID = preadRow.getAttribute("Boid").toString();
                        } else {
                            BOID = "na";
                        }

                        // get the buy limit
                        String cashLimit = preadRow.getAttribute("CashLimit").toString();
                        
                        Long bLimit = this.getBuyLimit(sellLimit, cashLimit, fileType);
                        //preadRow.setAttribute("BuyLimit", String.valueOf(bLimit));
                        Double bLimitNew = this.getBuyLimitNew(sellLimit, cashLimit);
                        //preadRow.setAttribute("Calculated", (new DecimalFormat("#.00").format(bLimit1)));
                        
                        String printLine = null;
                        String printLineInv = null;
                        
                        if (fileType.equalsIgnoreCase("DSE")) {
                            printLine =
                                    investorCode + "~" + BOID + "~" + investorName + "~" + "N" + "~" + bLimit.intValue() +
                                    "~" + "N" + "~" + holdingDate;
                            
                            
                        } else {
                            // for CSE
                            printLine =
                                    investorCode + "|" + bLimitNew.intValue() + "|" +
                                    holdingDate;
                            printLineInv =
                                    BOID + "~~~~" + investorName + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + investorCode;
                           
                        }
                        output.write(printLine + linegap);
                        outputInv.write(printLineInv + linegap);
                        
                    }
                    output.close();
                    outputInv.close();
                    generatedFileNameUI.setValue("CashLimit.txt");
                    generatedFileInv.setValue("CashLimitInv.txt");
                    
                    fct.addMessage("Complete Msg", new FacesMessage("File Processing done Successfully"));
                    
                    stream.close();
                    streamInv.close();
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                    e.getMessage();
                    System.gc();
                }
            }
        }
    }


    public void SetCurrentBrokerInvList(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        DCIteratorBinding dcibBro = ADFUtils.findIterator("CSBrokerLOVIterator");
        DCIteratorBinding dcibCashLimit = ADFUtils.findIterator("CashLimitLimitVOIterator");

        try {
            if (valueChangeEvent.getNewValue() != null) {
                /*String processAllBroker = "false";
                if(this.getProcessWithAllBrokerUI().getValue() != null)
                    processAllBroker = this.getProcessWithAllBrokerUI().getValue().toString();

                if(processAllBroker.equalsIgnoreCase("false")){
                    ViewObject cLimitVO = dcibCashLimit.getViewObject();
                    ViewObject brokerVO = dcibBro.getViewObject();


                    brokerVO.setWhereClause("BROKER_NAME='" + valueChangeEvent.getNewValue().toString() + "'");
                    dcibBro.executeQuery();

                    Integer bid = Integer.valueOf(brokerVO.first().getAttribute("BrokerId").toString());
                    System.out.println("bid = " + bid);
                    if (cLimitVO.getEstimatedRowCount() > 0) {

                        cLimitVO.setWhereClause("broker_id=" + bid);
                        cLimitVO.executeQuery();
                        cLimitVO.setWhereClause(null);
                    } else {
                        Map<String, String> paramValue = new HashMap<String, String>();
                        paramValue.put("BROKER_ID", bid.toString());
                        BaseBeanUtil.executeViewWithWhereClauseParam("CashLimitLimitVOIterator", paramValue);
                    }
                }*/

            }
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }

    }


    public void getCurrentSettingsMBCash(String businessType) {
        try {
            DCIteratorBinding dcibBro = ADFUtils.findIterator("CSBrokerLOVIterator");
            DCIteratorBinding dcibCashLimit = ADFUtils.findIterator("CashLimitLimitVOIterator");
            ViewObject csLimitVo = dcibCashLimit.getViewObject();
            if (brokerDId.getValue() != null) {

                dcibBro.getViewObject().setWhereClause("BROKER_NAME='" + brokerDId.getValue().toString() + "'");

                dcibBro.getViewObject().executeQuery();


                Integer bid = Integer.valueOf(dcibBro.getViewObject().first().getAttribute("BrokerId").toString());

                if (csLimitVo.getEstimatedRowCount() > 0) {
                    filterCashLimit(businessType, csLimitVo, bid);
                } else {
                    DCIteratorBinding dcibCash = ADFUtils.findIterator("CashLimitLimitVOIterator");
                    ViewObject csCashVo = dcibCash.getViewObject();
                    filterCashLimit(businessType, csCashVo, bid);
                }
            } else {
                if (dcibCashLimit.getViewObject().getEstimatedRowCount() > 0) {
                    String withOwn = this.getWithOwnCB().getValue().toString();

                    if (withOwn.equalsIgnoreCase("false")) {
                        csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND PRODUCT_TYPE <> 'OWN'");
                    } else
                        csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "'");

                    csLimitVo.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
                    csLimitVo.executeQuery();
                    csLimitVo.setWhereClause(null);
                }
            }
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
    }

    private void filterCashLimit(String businessType, ViewObject csLimitVo, Integer bid) {
        String processAllBroker = "false";
        if (this.getProcessWithAllBrokerUI().getValue() != null) {
            processAllBroker = this.getProcessWithAllBrokerUI().getValue().toString();
        }
        if (processAllBroker.equalsIgnoreCase("true")) {
            String withOwn = this.getWithOwnCB().getValue().toString();

            if (withOwn.equalsIgnoreCase("false")) {
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND PRODUCT_TYPE <> 'OWN'");
            } else
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "'");
            csLimitVo.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
            csLimitVo.executeQuery();
            csLimitVo.setWhereClause(null);
        } else {
            String withOwn = this.getWithOwnCB().getValue().toString();

            if (withOwn.equalsIgnoreCase("false")) {
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType +
                                         "' AND PRODUCT_TYPE <> 'OWN' and broker_id=" + bid);
            } else
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' and broker_id=" + bid);
            csLimitVo.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
            csLimitVo.executeQuery();
            csLimitVo.setWhereClause(null);
        }
    }

    //====================================================================================================

    public void setShareLimit(RichInputText shareLimit) {
        this.shareLimit = shareLimit;
    }

    public RichInputText getShareLimit() {
        return shareLimit;
    }

    public void setFileType(RichSelectOneChoice fileType) {
        this.fileType = fileType;
    }

    public RichSelectOneChoice getFileType() {
        return fileType;
    }

    public void setGeneratedFileNameUI(RichInputText generatedFileNameUI) {
        this.generatedFileNameUI = generatedFileNameUI;
    }

    public RichInputText getGeneratedFileNameUI() {
        return generatedFileNameUI;
    }

    public void setDownloadFileName(String downloadFileName) {
        this.downloadFileName = downloadFileName;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }


    public boolean isValEmpty() {
        if (this.generatedFileNameUI.getValue() == null || this.generatedFileInv.getValue() == null) {
            this.getCbDownLoad().setDisabled(true);
            this.getCbDownLoadInv().setDisabled(true);
            return true;
        }
        return false;
    }

    public boolean isAllowEnable() {
        String user = ApplicationInfo.getUser();
        return BaseBeanUtil.hasSpecialPermission("CASH_SHARE_LIMIT_ALL_ACTION", user,
                                                 "SpecialFunctionPermissionVOIterator");
    }

    private Long getBuyLimit(Double sellLimit, String CashLimit, String fileType) {
        Double bLimit = 0.0;
        Double cLimit = 0.0;
        long csLimit = 0;
        try {
            cLimit = Double.parseDouble(CashLimit);
            if (sellLimit != null && sellLimit.doubleValue() > 0.0) {
                bLimit = (sellLimit / 100.0) * (cLimit);
            } else {
                if (cLimit.doubleValue() < 1.0) {
                    bLimit = 0.0;
                } else {
                    bLimit = cLimit;
                }
            }

            double mLimit = Math.floor(Double.parseDouble(bLimit.toString()));
            csLimit = (long)mLimit;
            if (fileType.equalsIgnoreCase("DSE")) {
                BigDecimal csLimitPolicy = BaseBeanUtil.getGlPolicyValue("6");
                if (csLimitPolicy.intValue() == 1) //1 for divided in thousand
                    csLimit = csLimit / 1000;
            }
            //      csLimit=csLimit*1000;
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return csLimit;
    }

    private double getBuyLimitNew(Double sellLimit, String CashLimit) {
        Double bLimit = 0.0;
        Double cLimit = 0.0;
        cLimit = Double.parseDouble(CashLimit);
        if (sellLimit != null && sellLimit.doubleValue() > 0.0) {
            bLimit = (sellLimit / 100.0) * (cLimit);
        } else {
            if (cLimit.doubleValue() < 1.0) {
                bLimit = 0.0;
            } else {
                bLimit = cLimit;
            }
        }
        return bLimit;
    }

    public boolean isBuyLimitInThousand() {
        BigDecimal csLimitPolicy = BaseBeanUtil.getGlPolicyValue("6");
        if (csLimitPolicy.intValue() == 0)
            return false;
        return true;
    }

    public static void main(String[] s) {

        Double ssssss = 55945005.4693;

        Double seeswe = (ssssss * 60) / 100;
        System.out.println(new DecimalFormat("#.00").format(seeswe));
        System.out.println(seeswe);


        /* double dLimit = Math.floor(Double.parseDouble("1515.12"));
        long cLimit = (long)dLimit;
        cLimit = cLimit / 1000;
        System.out.println(cLimit); */
    }

    public void setCbDownLoad(RichCommandButton cbDownLoad) {
        this.cbDownLoad = cbDownLoad;
    }

    public RichCommandButton getCbDownLoad() {
        return cbDownLoad;
    }

    public void setFileTypeCSEDSE(RichSelectOneChoice fileTypeCSEDSE) {
        this.fileTypeCSEDSE = fileTypeCSEDSE;
    }

    public RichSelectOneChoice getFileTypeCSEDSE() {
        return fileTypeCSEDSE;
    }

    public void setBrokerDId(RichSelectOneChoice brokerDId) {
        this.brokerDId = brokerDId;
    }

    public RichSelectOneChoice getBrokerDId() {
        return brokerDId;
    }

    public void setWithOwnCB(RichSelectBooleanCheckbox withOwnCB) {
        this.withOwnCB = withOwnCB;
    }

    public RichSelectBooleanCheckbox getWithOwnCB() {
        return withOwnCB;
    }

    public void setGeneratedFileInv(RichInputText generatedFileInv) {
        this.generatedFileInv = generatedFileInv;
    }

    public RichInputText getGeneratedFileInv() {
        return generatedFileInv;
    }

    public void setCbDownLoadInv(RichCommandButton cbDownLoadInv) {
        this.cbDownLoadInv = cbDownLoadInv;
    }

    public RichCommandButton getCbDownLoadInv() {
        return cbDownLoadInv;
    }

    public void setProcessWithAllBrokerUI(RichSelectBooleanCheckbox processWithAllBrokerUI) {
        this.processWithAllBrokerUI = processWithAllBrokerUI;
    }

    public RichSelectBooleanCheckbox getProcessWithAllBrokerUI() {
        return processWithAllBrokerUI;
    }

}
