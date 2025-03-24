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
import leads.capita.common.ui.ADFUtils;
import leads.capita.common.ui.bean.BaseBeanUtil;
import leads.capita.common.ui.util.JSFUtils;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputText;

import oracle.adf.view.rich.component.rich.input.RichSelectBooleanCheckbox;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;

import oracle.adf.view.rich.component.rich.nav.RichCommandButton;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class ShareLimit {
    private FacesContext fct;
    private FacesContext fctx;

    static String user_home = System.getProperty("user.home");
    static String shareLimitDir = user_home + File.separator;
    static String shareLimitDirInv = user_home + File.separator;

    private RichInputText generatedFileNameUI;
    private RichInputText generatedFileInv;
    private RichSelectOneChoice fileTypeCSEDSE;
    private RichCommandButton cbDownLoad;
    private RichCommandButton cbDownLoadInv;
    private RichSelectOneChoice brokerName;
    private RichSelectBooleanCheckbox withOwnCB;
    private RichSelectBooleanCheckbox processWithAllBrokerUI;


    public ShareLimit() {
        super();
        fct = JSFUtils.getFacesContextApp();
        fctx = JSFUtils.getFacesContextApp();

    }


    public void SetCurrentBrokerValueChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        DCIteratorBinding dcibBro = ADFUtils.findIterator("CSBrokerLOVIterator");
        DCIteratorBinding dcShareLimit = ADFUtils.findIterator("ShareLimitVOIterator");

        try {
            /*if (valueChangeEvent.getNewValue() != null) {
                ViewObject brokerVO = dcibBro.getViewObject();
                brokerVO.setWhereClause("BROKER_NAME='" + valueChangeEvent.getNewValue().toString() + "'");
                dcibBro.executeQuery();

                Integer bid = Integer.valueOf(brokerVO.first().getAttribute("BrokerId").toString());
                if (dcShareLimit.getViewObject().getEstimatedRowCount() > 0) {
                   ViewObject sVO= dcShareLimit.getViewObject();
                    sVO.setWhereClause("broker_id=" + bid);
                    sVO.executeQuery();
                    sVO.setWhereClause(null);
                } else {
                    Map<String, String> paramValue = new HashMap<String, String>();
                    paramValue.put("BROKER_ID", bid.toString());
                    BaseBeanUtil.executeViewWithWhereClauseParam("ShareLimitVOIterator", paramValue);
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
    }

    public void fileTypeValueChange(ValueChangeEvent valueChangeEvent) {

        valueChangeEvent.getComponent().processUpdates(fct);
        cbDownLoadInv.setDisabled(true);
        cbDownLoad.setDisabled(true);

    }

    private String getSystemDateShare() {
        String sysDate = null;
        try {
            Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());
            sysDate = new SimpleDateFormat("dd-MMM-yyyy").format(rawDate);
        } catch (Exception e) {
            System.out.print("Date  Formetting Problem--Share !!");

        }
        return sysDate;
    }

    private Long getShareLimit(String freeBalance) {

        Double bLimit = Math.floor(Double.parseDouble(freeBalance));
        double mLimit = Math.floor(Double.parseDouble(bLimit.toString()));
        long csLimit = Long.valueOf(freeBalance).longValue() ;
        csLimit = csLimit / 1000;

        return csLimit;
    }

    private void getCurrentSettingsMBShare(String businessType) {
        
        try {

            DCIteratorBinding dcibBro = ADFUtils.findIterator("CSBrokerLOVIterator");
            DCIteratorBinding dcShareLimit = ADFUtils.findIterator("ShareLimitVOIterator");
            ViewObject csLimitVo = dcShareLimit.getViewObject();
            if (brokerName.getValue() != null) {

                dcibBro.getViewObject().setWhereClause("BROKER_NAME='" + brokerName.getValue().toString() + "'");
                dcibBro.getViewObject().executeQuery();

                Integer bid = Integer.valueOf(dcibBro.getViewObject().first().getAttribute("BrokerId").toString());

                if (dcShareLimit.getViewObject().getEstimatedRowCount() > 0) {
                    filterShareLimit(businessType, csLimitVo, bid);

                } else {
                    DCIteratorBinding dcibShare = ADFUtils.findIterator("ShareLimitVOIterator");
                    ViewObject csShareVo = dcibShare.getViewObject();
                    filterShareLimit(businessType, csShareVo, bid);
                }

            } else {
                // brokerName.getValue() = BROKER
                
                if (csLimitVo.getEstimatedRowCount() > 0) {
                    String withOwn = this.getWithOwnCB().getValue().toString();
                    if (withOwn.equalsIgnoreCase("false")){
                        csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND " +
                                                 "INSTRUMENT_TYPE <>'PREFERENCE'" + " AND ISIN IS NOT NULL" +
                                                 " AND PRODUCT_TYPE <>'OWN' AND SALABLE_QTY > 0 ");
                    }else{
                        csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND " +
                                                 "INSTRUMENT_TYPE <>'PREFERENCE'" + " AND ISIN IS NOT NULL" +
                                                 " AND SALABLE_QTY > 0 ");
                    }
                    csLimitVo.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
                    csLimitVo.executeQuery();
                    csLimitVo.setWhereClause(null);
                }
            }
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
    }

    private void filterShareLimit(String businessType, ViewObject csLimitVo, Integer bid) {
        String processAllBroker = "false";
        if (this.getProcessWithAllBrokerUI().getValue() != null) {
            processAllBroker = this.getProcessWithAllBrokerUI().getValue().toString();
        }
        if (processAllBroker.equalsIgnoreCase("true")) {
            String withOwn = this.getWithOwnCB().getValue().toString();
            if (withOwn.equalsIgnoreCase("false"))
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND " +
                                         "INSTRUMENT_TYPE <>'PREFERENCE'" + " AND ISIN IS NOT NULL" +
                                         " AND PRODUCT_TYPE <>'OWN' AND SALABLE_QTY > 0 ");
            else
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND " +
                                         "INSTRUMENT_TYPE <>'PREFERENCE'" + " AND ISIN IS NOT NULL" +
                                         " AND SALABLE_QTY > 0 ");

            csLimitVo.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
            csLimitVo.executeQuery();
            csLimitVo.setWhereClause(null);
        } else {
            String withOwn = this.getWithOwnCB().getValue().toString();
            if (withOwn.equalsIgnoreCase("false"))
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND " +
                                         "INSTRUMENT_TYPE <>'PREFERENCE'" + " AND ISIN IS NOT NULL" +
                                         " AND PRODUCT_TYPE <>'OWN'" + " AND SALABLE_QTY > 0 and broker_id=" + bid);
            else
                csLimitVo.setWhereClause("BUSINESS_TYPE='" + businessType + "' AND " +
                                         "INSTRUMENT_TYPE <>'PREFERENCE'" + " AND ISIN IS NOT NULL" +
                                         " AND SALABLE_QTY > 0 and broker_id=" + bid);

            csLimitVo.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
            csLimitVo.executeQuery();
            csLimitVo.setWhereClause(null);
        }
    }

    public void generateFileListener(FacesContext facesContext,
                                     OutputStream outputStream) throws UnsupportedEncodingException, IOException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();

        try {

            File processedfile = null;
            //CashLimit
            //ShareLimit
            if (ApplicationInfo.getBusinessType().equals("BROKER")) {
                processedfile = new File(shareLimitDir + "ShareLimit.txt");
            } else {
                if (brokerName.getValue() != null)
                    processedfile = new File(shareLimitDir + brokerName.getValue().toString() + " - ShareLimit - "+ fileTypeCSEDSE.getValue().toString() +".txt");
                else
                    processedfile = new File(shareLimitDir + "ShareLimit-" + ApplicationInfo.getSystemDate() + " - " + fileTypeCSEDSE.getValue().toString() + ".txt");
            }
            File userPC =
                new File(context.getRealPath("/") + File.separator + generatedFileNameUI.getValue().toString());
            FileInputStream fdownLoad;

            byte[] b;
            DCIteratorBinding dcIter = ADFUtils.findIterator("ShareLimitVOIterator");

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
                procesedfileInv = new File(shareLimitDirInv + "ShareLimit.txt");
            } else {
                if (brokerName.getValue() != null)
                    procesedfileInv =
                            new File(shareLimitDirInv + brokerName.getValue().toString() + " - ShareLimitInv - "+ fileTypeCSEDSE.getValue().toString() +".txt");
                else
                    procesedfileInv =
                            new File(shareLimitDirInv + "ShareLimitInv-" + ApplicationInfo.getSystemDate() + " - " + fileTypeCSEDSE.getValue().toString() + ".txt");
            }

            File userPCInv =
                new File(context.getRealPath("/") + File.separator + generatedFileInv.getValue().toString());


            FileInputStream fdownLoadInv;

            byte[] b;

            DCIteratorBinding dcIter = ADFUtils.findIterator("ShareLimitVOIterator");

            Long rows = (dcIter.getViewObject().getEstimatedRowCount());

            if (rows > 0) {
                try {
                    File f = new File(procesedfileInv.getPath());
                    fdownLoadInv = new FileInputStream(f);
                    int n;
                    while ((n = fdownLoadInv.available()) > 0) {
                        b = new byte[n];
                        int result = fdownLoadInv.read(b);
                        outputStream.write(b, 0, b.length);
                        if (result == -1)
                            break;
                    }

                    outputStream.flush();
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

        DCIteratorBinding dcIter = ADFUtils.findIterator("ShareLimitVOIterator");

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
                createShareLimit_new(businessType);
                cbDownLoad.setDisabled(false);
                cbDownLoadInv.setDisabled(false);

            }

        } else {
            // MBank flow
            this.mbankValidation(((RichSelectOneChoice)JSFUtils.findComponentInRoot("socBroker")).getValue(),
                                 this.getProcessWithAllBrokerUI().getValue());
            if ((((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue() != null)) {
                createShareLimit_new(businessType);
                cbDownLoad.setDisabled(false);
                cbDownLoadInv.setDisabled(false);

            }
        }
        return null;
    }

    private void mbankValidation(Object brokerVal, Object procAllVal) throws Exception {
        if ((procAllVal == null || procAllVal.toString().equals("false")) && brokerVal == null) {
            fct.addMessage("Info", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please Select Broker"));
            throw new Exception("Select A Broker !!");
        }
    }

    private void createShareLimit_new(String businessType) {
        File home_dir = null;
        File home_dirInv = null;
        DCIteratorBinding dcIter = ADFUtils.findIterator("ShareLimitVOIterator");
        ViewObject vo = dcIter.getViewObject();
        //vo.executeQuery();
        String holdingDate = getSystemDateShare();

        if (businessType.equalsIgnoreCase("MBANK")) {
        // for mbank
            String fileType =
                    ((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString();
            getCurrentSettingsMBShare(businessType);
            dcIter.setRangeSize((int)dcIter.getEstimatedRowCount());
            Row[] r = dcIter.getViewObject().getAllRowsInRange();
            
            int len = r.length;
            /*
            Long noOfLines1 = (dcIter.getViewObject().getEstimatedRowCount());
            String nolines = noOfLines1.toString();
            Integer noOfLines = Integer.valueOf(nolines);
            */

            if (len > 0) {
                try {
                    if (brokerName.getValue() != null) {
                        home_dir = new File(shareLimitDir + brokerName.getValue().toString() + " - ShareLimit - "+ fileType +".txt");
                        home_dirInv =
                                new File(shareLimitDirInv + brokerName.getValue().toString() + " - ShareLimitInv - "+ fileType +".txt");
                    } else {
                        home_dir = new File(shareLimitDir + "ShareLimit-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                        home_dirInv =
                                new File(shareLimitDirInv + "ShareLimitInv-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                    }

                    FileWriter stream = new FileWriter(home_dir);
                    Writer output = new BufferedWriter(stream);
                    FileWriter streamInv = new FileWriter(home_dirInv);
                    Writer outputInv = new BufferedWriter(streamInv);

                    String linegap = String.format("%n");

                    String investorCode = null;
                    String investorName = null;
                    String instrumnetName = null;
                    String instrumnetShortName = null;
                    String BOID = null;
                    String Isin = null;
                    String sellLimit = null;
                    String freeBalance = null;
                    String prdType = null;
                    List notMapTrdInv = new ArrayList();

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
                        /*  else {
                                    investorCode = preadRow.getAttribute("InvestorCode").toString();
                                }

                        } else {
                            investorCode = preadRow.getAttribute("InvestorCode").toString();
                        } */

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

                        if (preadRow.getAttribute("Isin") != null &&
                            !(preadRow.getAttribute("Isin").toString().equalsIgnoreCase(""))) {

                            Isin = preadRow.getAttribute("Isin").toString();
                        } else {
                            Isin = "na";
                        }

                        if (preadRow.getAttribute("InstrumentName") != null &&
                            !preadRow.getAttribute("InstrumentName").toString().equalsIgnoreCase("")) {
                            instrumnetName = preadRow.getAttribute("InstrumentName").toString();
                        } else {
                            instrumnetName = "na";
                        }

                        if (preadRow.getAttribute("ShortName") != null &&
                            !preadRow.getAttribute("ShortName").toString().equalsIgnoreCase("")) {
                            instrumnetShortName = preadRow.getAttribute("ShortName").toString();
                        } else {
                            instrumnetShortName = "na";
                        }
                        if (preadRow.getAttribute("TotalQty") != null &&
                            !(preadRow.getAttribute("TotalQty").toString().equalsIgnoreCase(""))) {
                            sellLimit = preadRow.getAttribute("TotalQty").toString();
                        }

                        if (preadRow.getAttribute("FreeBalance") != null &&
                            !(preadRow.getAttribute("FreeBalance").toString().equalsIgnoreCase(""))) {
                            freeBalance = preadRow.getAttribute("FreeBalance").toString();
                        }

                        /* String filyeType1 =
                            ((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString(); */

                        String printLine = null;
                        String printLineInv = null;

                        if (fileType.equalsIgnoreCase("DSE")) {

                            printLine =
                                    Isin + "~" + instrumnetShortName + "~" + BOID + "~" + investorName + "~" + sellLimit +
                                    "~" + freeBalance + "~" + investorCode + "~" + holdingDate;
                            //System.out.println(printLine);
                        } else {

                            //holdingDate = getSystemDateShare();
                            printLine = investorCode + "|" + BOID + "|" + Isin + "|" + freeBalance + "|" + holdingDate;
                            printLineInv =
                                    BOID + "~~~~" + investorName + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + investorCode;
                        }
                        //System.out.println("printLine: "+ printLine);
                        //System.out.println("printLineInv: "+ printLineInv);
                        
                        output.write(printLine + linegap);
                        outputInv.write(printLineInv + linegap);

                    }
                    output.close();
                    outputInv.close();
                    if (brokerName.getValue() != null) {
                        generatedFileNameUI.setValue(brokerName.getValue().toString() + " - ShareLimit - "+ fileType +".txt");
                        generatedFileInv.setValue(brokerName.getValue().toString() + " - ShareLimitInv - "+ fileType +".txt");
                    } else {
                        generatedFileNameUI.setValue("ShareLimit-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
                        generatedFileInv.setValue("ShareLimitInv-" + ApplicationInfo.getSystemDate() + " - " + fileType + ".txt");
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

                } catch (Exception e) {
                    e.getMessage();
                }

            } else {
                fct.addMessage("Complete Msg", new FacesMessage("No Data to generate"));
                generatedFileNameUI.setValue(brokerName.getValue().toString() + " - ShareLimit.txt");
            }

        } else {
            // for broker
            getCurrentSettingsMBShare(businessType);
            
            //dcIter.setRangeSize((int)dcIter.getEstimatedRowCount());
            
            Row[] r = dcIter.getViewObject().getAllRowsInRange();  // ShareLimitVO
            /*
            Long noOfLines1 = (dcIter.getViewObject().getEstimatedRowCount());
            String nolines = noOfLines1.toString();
            Integer noOfLines = Integer.valueOf(nolines);
            */

            int len = r.length;
            
            if (len > 0) {
                try {
                    home_dir = new File(shareLimitDir + "ShareLimit.txt");
                    FileWriter stream = new FileWriter(home_dir);
                    Writer output = new BufferedWriter(stream);

                    home_dirInv = new File(shareLimitDirInv + "ShareLimitInv.txt");
                    FileWriter streamInv = new FileWriter(home_dirInv);
                    Writer outputInv = new BufferedWriter(streamInv);

                    String linegap = String.format("%n");

                    String investorCode = null;
                    String investorName = null;
                    String instrumnetShortName = null;
                    String BOID = null;
                    String Isin = null;
                    String sellLimit = null;
                    String freeBalance = null;
                    String exchangeName = ((RichSelectOneChoice)JSFUtils.findComponentInRoot("socfileType")).getValue().toString();
                    
                    for (int i = 0; i < len; i++) {
                        Row preadRow = r[i];
                        
                        if (preadRow.getAttribute("FreeBalance") != null &&
                            !(preadRow.getAttribute("FreeBalance").toString().equalsIgnoreCase(""))) {
                            freeBalance = preadRow.getAttribute("FreeBalance").toString();
                            /*
                             for BESL omit divide by 1000

                            Long sLimit = Long.valueOf(freeBalance); //this.getShareLimit(preadRow.getAttribute("FreeBalance").toString());
                            sLimit = sLimit/1000;
                            freeBalance = sLimit.toString();
                            */
                            if(freeBalance.equalsIgnoreCase("0")){ // if no saleable share then ignore 
                                continue;
                            }
                            
                            
                            //preadRow.setAttribute("FreeBalance", sLimit.toString());
                            //freeBalance = preadRow.getAttribute("FreeBalance").toString();
                            
                        }

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

                        if (preadRow.getAttribute("Isin") != null &&
                            !(preadRow.getAttribute("Isin").toString().equalsIgnoreCase(""))) {

                            Isin = preadRow.getAttribute("Isin").toString();
                        } else {
                            Isin = "na";
                        }


                        if (preadRow.getAttribute("ShortName") != null &&
                            !preadRow.getAttribute("ShortName").toString().equalsIgnoreCase("")) {
                            instrumnetShortName = preadRow.getAttribute("ShortName").toString();
                        } else {
                            instrumnetShortName = "na";
                        }


                        if (preadRow.getAttribute("TotalQty") != null &&
                            !(preadRow.getAttribute("TotalQty").toString().equalsIgnoreCase(""))) {
                            sellLimit = preadRow.getAttribute("TotalQty").toString();
                        }

                        

                        String printLine = null;
                        String printLineInv = null;

                        if (exchangeName.equalsIgnoreCase("DSE")) {

                            printLine =
                                    Isin + "~" + instrumnetShortName + "~" + BOID + "~" + investorName + "~" + sellLimit +
                                    "~" + freeBalance + "~" + investorCode + "~" + holdingDate;
                        } else {
                            // for CSE
                            //holdingDate = getSystemDateShare();
                            printLine = investorCode + "|" + BOID + "|" + Isin + "|" + freeBalance + "|" + holdingDate;
                            printLineInv =
                                    BOID + "~~~~" + investorName + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + investorCode;
                        }
                        
                        //System.out.println("printLine: "+ printLine);
                        //System.out.println("printLineInv: "+ printLineInv);

                        output.write(printLine + linegap);
                        outputInv.write(printLineInv + linegap);
                    }
                    
                    output.close();
                    outputInv.close();
                    generatedFileNameUI.setValue("ShareLimit.txt");
                    generatedFileInv.setValue("ShareLimitInv.txt");
                    fct.addMessage("Complete Msg", new FacesMessage("File Processing done Successfully"));
                    stream.close();
                    streamInv.close();
                    
                    System.gc();
                    
                } catch (Exception e) {
                    e.getMessage();
                    }
            }
        }
    }


    public void setGeneratedFileNameUI(RichInputText generatedFileNameUI) {
        this.generatedFileNameUI = generatedFileNameUI;
    }

    public RichInputText getGeneratedFileNameUI() {
        return generatedFileNameUI;
    }

    public void setGeneratedFileInv(RichInputText generatedFileInv) {
        this.generatedFileInv = generatedFileInv;
    }

    public RichInputText getGeneratedFileInv() {
        return generatedFileInv;
    }


    public void setFileTypeCSEDSE(RichSelectOneChoice fileTypeCSEDSE) {
        this.fileTypeCSEDSE = fileTypeCSEDSE;
    }

    public RichSelectOneChoice getFileTypeCSEDSE() {
        return fileTypeCSEDSE;
    }


    public void setCbDownLoad(RichCommandButton cbDownLoad) {
        this.cbDownLoad = cbDownLoad;
    }

    public RichCommandButton getCbDownLoad() {
        return cbDownLoad;
    }

    public void setCbDownLoadInv(RichCommandButton cbDownLoadInv) {
        this.cbDownLoadInv = cbDownLoadInv;
    }

    public RichCommandButton getCbDownLoadInv() {
        return cbDownLoadInv;
    }


    public void setBrokerName(RichSelectOneChoice brokerName) {
        this.brokerName = brokerName;
    }

    public RichSelectOneChoice getBrokerName() {
        return brokerName;
    }

    public void setWithOwnCB(RichSelectBooleanCheckbox withOwnCB) {
        this.withOwnCB = withOwnCB;
    }

    public RichSelectBooleanCheckbox getWithOwnCB() {
        return withOwnCB;
    }

    public void setProcessWithAllBrokerUI(RichSelectBooleanCheckbox processWithAllBrokerUI) {
        this.processWithAllBrokerUI = processWithAllBrokerUI;
    }

    public RichSelectBooleanCheckbox getProcessWithAllBrokerUI() {
        return processWithAllBrokerUI;
    }
}
