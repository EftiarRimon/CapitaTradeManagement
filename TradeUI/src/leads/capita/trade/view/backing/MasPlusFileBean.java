package leads.capita.trade.view.backing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.math.BigDecimal;

import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.ValueChangeEvent;


import leads.capita.common.application.ApplicationInfo;

import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;
import oracle.adf.view.rich.context.AdfFacesContext;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.adf.view.rich.event.PopupFetchEvent;

import oracle.binding.AttributeBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import org.apache.myfaces.trinidad.event.AttributeChangeEvent;

public class MasPlusFileBean extends BaseBean {

    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    int importId;
    private Integer shortSellCount;
    private Integer overPurchaseCount;
    private Integer unapprovedInstrumentCount;
    private Integer bdaCount;
    private Integer invNonMgrCount;
    private Integer insNonMgrCount;
    private Integer totalBuyHowla;
    private Integer totalSellHowla;


    private String procStatus;
    private int tracerNo;
    private RichSelectOneChoice opssuiStatus;
    private RichSelectOneChoice instrumentIdUI;
    private RichSelectOneChoice productIdUI;
    private ResourceBundle messagebundle;
    private ResourceBundle masterMessagebundle;
    private RichPopup allowInstrumentForTrdPopUP;

    public MasPlusFileBean() {
        super();

        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");
        getImportId();

    }

    public String process() {

        try {
             Object params[] = { "10" }; // 10 For DSE
            BigDecimal rVal =
                new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_trade.is_valid_date_to_trade(?)", params);
            if (rVal.compareTo(new BigDecimal(0)) > 0) {
                new TMPlsqlExecutor().processThisFile(importId,
                                                      Integer.parseInt(ApplicationInfo.getCurrentUserBranch()),
                                                      ApplicationInfo.getUser());
                //new TMPlsqlExecutor().processThisFile(importId);
                fct.addMessage("SuccessMsg",
                               new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The File has been processed!"));
                refreshIterator("TradeFileMsaPlusVOIterator");

            } else {
                JSFUtils.addFacesErrorMessage("Not a valid date to trade in DSE!! ");
            } 
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
            /*
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "The File has not been processed!"));
            */
        }

        return null;
    }

    public String validate() {
        try {
            new TMPlsqlExecutor().validateThisFile(importId);
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The File has been validated!!"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
            /*
            * fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "The File has not validated!"));
            */

        }

        return null;
    }


    public String allowAllOPSSUI() {

        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;

        try {
            new TMPlsqlExecutor().allowOPSSUI(procStatus, importId, 0);
            this.allowInstrumentForTrdPopUP.hide();
            this.pageRefesh();
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The operation has been successfully completed!!"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Invalide operation!"));
            e.printStackTrace();
        }
        return null;
    }

    public String resetAllOPSSUI() {
        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;
        try {
            new TMPlsqlExecutor().resetOPSSUI(procStatus, importId, 0);
            this.pageRefesh();
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The operation has been successfully completed!!"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Invalide operation!"));
            e.printStackTrace();
        }
        return null;
    }

    public String allowSingleOPSSUI() {
        DCIteratorBinding msaplusIteratorBindings = ADFUtils.findIterator("TradeFileMsaPlusVOIterator");
        ViewObject tradeFileMsaPlusVO = msaplusIteratorBindings.getViewObject();
        Row row = tradeFileMsaPlusVO.getCurrentRow();
        tracerNo = Integer.valueOf(row.getAttribute("TracerNo").toString());
        //procStatus = JSFUtils.getFromSession("procStatus").toString();
        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;

        //fct.addMessage("SuccessMsg", new FacesMessage(FacesMessage.SEVERITY_INFO, "", "tracerNo :" + tracerNo));
        try {
            new TMPlsqlExecutor().allowOPSSUI(procStatus, importId, tracerNo);

            fct.addMessage("SuccessMsg", new FacesMessage(FacesMessage.SEVERITY_INFO, "", "tracerNo :" + tracerNo));
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The operation has been successfully completed!!"));
            this.pageRefesh();
            /* fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The operation has been successfully completed!!")); */
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Invalide operation!"));
            e.printStackTrace();
        }
        return null;
    }

    public String resetSingleOPSSUI() {

        DCIteratorBinding msaplusIteratorBindings = ADFUtils.findIterator("TradeFileMsaPlusVOIterator");
        ViewObject tradeFileMsaPlusVO = msaplusIteratorBindings.getViewObject();
        Row row = tradeFileMsaPlusVO.getCurrentRow();
        tracerNo = Integer.valueOf(row.getAttribute("TracerNo").toString());
        //procStatus = JSFUtils.getFromSession("procStatus").toString();
        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;

        try {
            new TMPlsqlExecutor().resetOPSSUI(procStatus, importId, tracerNo);
            this.pageRefesh();
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The operation has been successfully completed!!"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Invalide operation!"));
            e.printStackTrace();
        }
        return null;
    }

    public void showPopupRecDeletion(DialogEvent dialogEvent) {
        this.deleteConfirmation(dialogEvent, "Delete");
    }

    public void setImportId(int importId) {
        this.importId = importId;
    }

    public int getImportId() {
        DCIteratorBinding msaPlusIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        importId = Integer.parseInt(msaPlusIteratorBindings.getCurrentRow().getAttribute("ImportId").toString());
        return importId;
    }

    public void filterOnValidation(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        DCIteratorBinding msaPlusIteratorBindings = ADFUtils.findIterator("TradeFileMsaPlusVOIterator");
        DCIteratorBinding filesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        //AttributeBinding  fileDateBindings = ADFUtils.findControlBinding("FileDate");

        try {
            ViewObject msaPlusFilesVO = msaPlusIteratorBindings.getViewObject();

            // System.out.println(valueChangeEvent.getNewValue().toString());

            if (valueChangeEvent.getNewValue().toString().equals("ss")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('SS') ");
                procStatus = "SS";
            }

            else if (valueChangeEvent.getNewValue().toString().equals("op")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('OP') ");
                procStatus = "OP";
            } else if (valueChangeEvent.getNewValue().toString().equals("ui")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('UI') ");
                procStatus = "UI";
            } else if (valueChangeEvent.getNewValue().toString().equals("insmg")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INSMG') ");
                procStatus = "INSMG";
            } else if (valueChangeEvent.getNewValue().toString().equals("invmg")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INVMG') ");
                procStatus = "INVMG";
            } else if (valueChangeEvent.getNewValue().toString().equals("n")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('N') ");
                procStatus = "N";
            } else if (valueChangeEvent.getNewValue().toString().equals("bda")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('BDA') ");
                procStatus = "BDA";
            } else {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId);
                procStatus = "";
            }
            //JSFUtils.storeOnSession("procStatus", procStatus);

            msaPlusFilesVO.executeQuery();


        } catch (NullPointerException ne) {
            ne.printStackTrace();
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error " + ne.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
        }
        // Add event code here...
    }


    public void pageRefesh() {

        DCIteratorBinding msaPlusIteratorBindings = ADFUtils.findIterator("TradeFileMsaPlusVOIterator");
        DCIteratorBinding filesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");

        try {
            ViewObject msaPlusFilesVO = msaPlusIteratorBindings.getViewObject();


            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('SS') ");
            msaPlusFilesVO.executeQuery();
            this.setShortSellCount(msaPlusFilesVO.getRowCount());
            // AdfFacesContext.getCurrentInstance().addPartialTarget(shortSellCount);


            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('OP') ");
            msaPlusFilesVO.executeQuery();
            this.setOverPurchaseCount(msaPlusFilesVO.getRowCount());

            // System.out.println(msaPlusFilesVO.getRowCount());

            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('UI') ");
            msaPlusFilesVO.executeQuery();
            this.setUnapprovedInstrumentCount(msaPlusFilesVO.getRowCount());

            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('BDA') ");
            msaPlusFilesVO.executeQuery();
            this.setBdaCount(msaPlusFilesVO.getRowCount());

            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INVMG') ");
            msaPlusFilesVO.executeQuery();
            this.setInvNonMgrCount(msaPlusFilesVO.getRowCount());

            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INSMG') ");
            msaPlusFilesVO.executeQuery();
            this.setInsNonMgrCount(msaPlusFilesVO.getRowCount());

            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(BUY_SELL)=upper('B') ");
            msaPlusFilesVO.executeQuery();
            this.setTotalBuyHowla(msaPlusFilesVO.getRowCount());

            msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(BUY_SELL)=upper('s') ");
            msaPlusFilesVO.executeQuery();
            this.setTotalSellHowla(msaPlusFilesVO.getRowCount());

            //procStatus = JSFUtils.getFromSession("procStatus").toString();
            //if (JSFUtils.getFromSession("procStatus")==null)
            // {
            //  msaPlusFilesVO.setWhereClause("IMPORT_ID =" +importId);
            //   msaPlusFilesVO.executeQuery();
            // }

            if (opssuiStatus.getValue().toString().equals("al")) {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId);
                msaPlusFilesVO.executeQuery();
            } else {
                msaPlusFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('" +
                                              opssuiStatus.getValue().toString() + "') ");
                msaPlusFilesVO.executeQuery();
            }


            ViewObject importFilesVO = filesIteratorBindings.getViewObject();
            importFilesVO.setWhereClause("IMPORT_ID =" + importId);
            importFilesVO.executeQuery();

        } catch (NullPointerException ne) {
            ne.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error..."));
        }
        // Add event code here...

    }

    public void refreshCall(ActionEvent actionEvent) {
        try {
            DCIteratorBinding msaPlusIteratorBindings = ADFUtils.findIterator("TradeFileMsaPlusVOIterator");
            msaPlusIteratorBindings.refresh(DCIteratorBinding.RANGESIZE_UNLIMITED);
        } catch (Exception e) {
            this.customRollBack();
            e.printStackTrace();
        }

    }

    public void setShortSellCount(Integer shortSellCount) {
        this.shortSellCount = shortSellCount;
    }

    public Integer getShortSellCount() {
        return shortSellCount;
    }

    public void setOverPurchaseCount(Integer overPurchaseCount) {
        this.overPurchaseCount = overPurchaseCount;
    }

    public Integer getOverPurchaseCount() {
        return overPurchaseCount;
    }

    public void setUnapprovedInstrumentCount(Integer unapprovedInstrumentCount) {
        this.unapprovedInstrumentCount = unapprovedInstrumentCount;
    }

    public Integer getUnapprovedInstrumentCount() {
        return unapprovedInstrumentCount;
    }

    public String getOnloadText() {
        this.pageRefesh();
        return "::";
    }


    public void setOpssuiStatus(RichSelectOneChoice opssuiStatus) {
        this.opssuiStatus = opssuiStatus;

    }

    public RichSelectOneChoice getOpssuiStatus() {
        return opssuiStatus;
    }

    public void setBdaCount(Integer bdaCount) {
        this.bdaCount = bdaCount;
    }

    public Integer getBdaCount() {
        return bdaCount;
    }

    public void setInvNonMgrCount(Integer invNonMgrCount) {
        this.invNonMgrCount = invNonMgrCount;
    }

    public Integer getInvNonMgrCount() {
        return invNonMgrCount;
    }

    public void setInsNonMgrCount(Integer insNonMgrCount) {
        this.insNonMgrCount = insNonMgrCount;
    }

    public Integer getInsNonMgrCount() {
        return insNonMgrCount;
    }

    public void trdCommsionChange(DialogEvent dialogEvent) {
        String invCode = null;
        String instCode = null;
        Double newRate = 0.0;
        int exchng = 10;
        int product = 0;
        int importID = 0;
        String buySell = null;

        if (dialogEvent.getOutcome().ok == DialogEvent.Outcome.ok) {
            DCIteratorBinding commsnIteratorBindings = ADFUtils.findIterator("TradeCommissionChargeVOIterator");
            DCIteratorBinding trdFilemsaIterBingings = ADFUtils.findIterator("TradeFileMsaPlusVOIterator");
            Row commsnChngRow = commsnIteratorBindings.getCurrentRow();

            if (commsnChngRow.getAttribute("NewRate") != null && !commsnChngRow.getAttribute("NewRate").equals("")) {
                newRate = Double.parseDouble(commsnChngRow.getAttribute("NewRate").toString());

                if (trdFilemsaIterBingings.getViewObject().first() != null) {
                    Row trdFileMsavo = trdFilemsaIterBingings.getViewObject().first();
                    importID = Integer.parseInt(trdFileMsavo.getAttribute("ImportId").toString());
                } else {
                    Row trdFileMsaRow = trdFilemsaIterBingings.getCurrentRow();
                    if (trdFileMsaRow.getAttribute("ImportId") != null) {
                        importID = Integer.parseInt(trdFileMsaRow.getAttribute("ImportId").toString());
                    }
                }
                if (commsnChngRow.getAttribute("InvestorCode") != null) {
                    invCode = commsnChngRow.getAttribute("InvestorCode").toString();
                }
                if (this.getInstrumentIdUI().getValue() != null) {
                    instCode = this.getInstrumentIdUI().getValue().toString();
                }
                if (commsnChngRow.getAttribute("Buy_Sell") != null) {
                    buySell = commsnChngRow.getAttribute("Buy_Sell").toString();
                }

                if (this.getProductIdUI().getValue() != null && !this.getProductIdUI().getValue().equals("")) {
                    product = Integer.parseInt(this.getProductIdUI().getValue().toString());
                    //}
                    //use try cacth,commit
                    TMPlsqlExecutor tmSql = new TMPlsqlExecutor();
                    try {
                        tmSql.trdCommissionChangeProcess(exchng, importID, product, invCode, instCode, buySell,
                                                         newRate);
                        ApplicationInfo.getCurrentUserDBTransaction().commit();
                        JSFUtils.addFacesInformationMessage(messagebundle.getString("leads_capita_trade_view_update_success"));
                        this.refreshIterator("TradeFileMsaPlusVOIterator");
                    } catch (Exception e) {
                        ApplicationInfo.getCurrentUserDBTransaction().rollback();
                        JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_update_error"));
                        e.printStackTrace();
                    }
                } else {
                    JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_product_id"));

                }
            } else {
                JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_new_rate"));
            }
        }
    }

    public void setInstrumentIdUI(RichSelectOneChoice instrumentIdUI) {
        this.instrumentIdUI = instrumentIdUI;
    }

    public RichSelectOneChoice getInstrumentIdUI() {
        return instrumentIdUI;
    }

    public void setProductIdUI(RichSelectOneChoice productIdUI) {
        this.productIdUI = productIdUI;
    }

    public RichSelectOneChoice getProductIdUI() {
        return productIdUI;
    }

    public void statusRefreshDiallogListnr(DialogEvent dialogEvent) {
        this.pageRefesh();
        RichPopup popup = this.allowInstrumentForTrdPopUP;
        popup.hide();
    }

    public void setTotalBuyHowla(Integer totalBuyHowla) {
        this.totalBuyHowla = totalBuyHowla;
    }

    public Integer getTotalBuyHowla() {
        return totalBuyHowla;
    }

    public void setTotalSellHowla(Integer totalSellHowla) {
        this.totalSellHowla = totalSellHowla;
    }

    public Integer getTotalSellHowla() {
        return totalSellHowla;
    }

    public void setAllowInstrumentForTrdPopUP(RichPopup allowInstrumentForTrdPopUP) {
        this.allowInstrumentForTrdPopUP = allowInstrumentForTrdPopUP;
    }

    public RichPopup getAllowInstrumentForTrdPopUP() {
        return allowInstrumentForTrdPopUP;
    }

}
