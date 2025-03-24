package leads.capita.trade.view.backing;

import java.math.BigDecimal;

import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import javax.faces.event.ValueChangeEvent;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.adf.view.rich.event.PopupFetchEvent;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class BtFileBean extends BaseBean {

    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    int importId;

    private Integer shortSellCount;
    private Integer overPurchaseCount;
    private Integer unapprovedInstrumentCount;
    private Integer spotCount;
    private String procStatus;
    private int tracerNo;
    private Integer bdaCount;
    private Integer invNonMgrCount;
    private Integer insNonMgrCount;

    private RichSelectOneChoice opssuiStatus;
    private ResourceBundle messagebundle;


    public BtFileBean() {
        super();

        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");

        getImportId();

    }

    public String process() {

        try {
            Object params[] = { "11" }; /// 11 for CSE
            BigDecimal rVal =
                new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_trade.is_valid_date_to_trade(?)", params);
            if (rVal.compareTo(new BigDecimal(0)) > 0) {
                new TMPlsqlExecutor().processThisFile(importId,
                                                      Integer.parseInt(ApplicationInfo.getCurrentUserBranch()),
                                                      ApplicationInfo.getUser());
                //new TMPlsqlExecutor().processThisFile(importId);
                fct.addMessage("SuccessMsg",
                               new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The File has been processed!"));
            } else {
                JSFUtils.addFacesErrorMessage("Not a valid date to trade in CSE!! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
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
            e.printStackTrace();
        }

        return null;
    }

    public String allowAllOPSSUI() {

        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;

        try {
            new TMPlsqlExecutor().allowOPSSUI(procStatus, importId, 0);
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
        DCIteratorBinding btIteratorBindings = ADFUtils.findIterator("TradeFileBtVOIterator");
        ViewObject tradeFileBtVO = btIteratorBindings.getViewObject();
        Row row = tradeFileBtVO.getCurrentRow();
        tracerNo = Integer.valueOf(row.getAttribute("TracerNo").toString());
        //procStatus = JSFUtils.getFromSession("procStatus").toString();
        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;

        fct.addMessage("SuccessMsg", new FacesMessage(FacesMessage.SEVERITY_INFO, "", "tracerNo :" + tracerNo));
        try {
            new TMPlsqlExecutor().allowOPSSUI(procStatus, importId, tracerNo);
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The operation has been successfully completed!!"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Invalide operation!"));
            e.printStackTrace();
        }
        return null;
    }

    public String resetSingleOPSSUI() {

        DCIteratorBinding btIteratorBindings = ADFUtils.findIterator("TradeFileBtVOIterator");
        ViewObject tradeFilebtVO = btIteratorBindings.getViewObject();
        Row row = tradeFilebtVO.getCurrentRow();
        tracerNo = Integer.valueOf(row.getAttribute("TracerNo").toString());
        //procStatus = JSFUtils.getFromSession("procStatus").toString();
        procStatus = opssuiStatus.getValue() != null ? opssuiStatus.getValue().toString().toUpperCase() : null;

        try {
            new TMPlsqlExecutor().resetOPSSUI(procStatus, importId, tracerNo);
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
        DCIteratorBinding btIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        importId = Integer.parseInt(btIteratorBindings.getCurrentRow().getAttribute("ImportId").toString());
        return importId;
    }

    public void filterOnValidation(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        DCIteratorBinding btFileIteratorBindings = ADFUtils.findIterator("TradeFileBtVOIterator");
        DCIteratorBinding filesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");

        try {
            ViewObject btFileFilesVO = btFileIteratorBindings.getViewObject();

            if (valueChangeEvent.getNewValue().toString().equals("ss")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and substr(PROC_STATUS,1,5)=upper('SS') ");
                procStatus = "SS";
            } else if (valueChangeEvent.getNewValue().toString().equals("op")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and substr(PROC_STATUS,1,5)=upper('OP') ");
                procStatus = "OP";
            } else if (valueChangeEvent.getNewValue().toString().equals("ui")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and substr(PROC_STATUS,1,5)=upper('UI') ");
                procStatus = "UI";
            } else if (valueChangeEvent.getNewValue().toString().equals("insmg")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INSMG') ");
                procStatus = "INSMG";
            } else if (valueChangeEvent.getNewValue().toString().equals("invmg")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INVMG') ");
                procStatus = "INVMG";
            } else if (valueChangeEvent.getNewValue().toString().equals("n")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('N') ");
                procStatus = "N";
            } else if (valueChangeEvent.getNewValue().toString().equals("bda")) {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('BDA') ");
                procStatus = "BDA";
            }

            else {
                btFileFilesVO.setWhereClause("IMPORT_ID =" + importId);
            }
            btFileFilesVO.executeQuery();
            //System.out.println(btFileFilesVO.getQuery());

        } catch (NullPointerException ne) {
            ne.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error"));
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
        }
        // Add event code here...
    }


    public void pageRefesh() {

        DCIteratorBinding btIteratorBindings = ADFUtils.findIterator("TradeFileBtVOIterator");
        DCIteratorBinding filesIteratorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");

        try {
            ViewObject btFilesVO = btIteratorBindings.getViewObject();


            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('SS') ");
            btFilesVO.executeQuery();
            this.setShortSellCount(btFilesVO.getRowCount());
            // AdfFacesContext.getCurrentInstance().addPartialTarget(shortSellCount);


            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('OP') ");
            btFilesVO.executeQuery();
            this.setOverPurchaseCount(btFilesVO.getRowCount());


            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('UI') ");
            btFilesVO.executeQuery();
            this.setUnapprovedInstrumentCount(btFilesVO.getRowCount());

            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(market_type)='S' ");
            btFilesVO.executeQuery();
            this.setSpotCount(btFilesVO.getRowCount());

            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('BDA') ");
            btFilesVO.executeQuery();
            this.setBdaCount(btFilesVO.getRowCount());

            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INVMG') ");
            btFilesVO.executeQuery();
            this.setInvNonMgrCount(btFilesVO.getRowCount());

            btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('INSMG') ");
            btFilesVO.executeQuery();
            this.setInsNonMgrCount(btFilesVO.getRowCount());


            if (opssuiStatus.getValue().toString().equals("al")) {
                btFilesVO.setWhereClause("IMPORT_ID =" + importId);
                btFilesVO.executeQuery();
            } else {
                btFilesVO.setWhereClause("IMPORT_ID =" + importId + " and upper(PROC_STATUS)=upper('" +
                                         opssuiStatus.getValue().toString() + "') ");
                btFilesVO.executeQuery();
            }


            ViewObject importFilesVO = filesIteratorBindings.getViewObject();
            importFilesVO.setWhereClause("IMPORT_ID =" + importId);
            importFilesVO.executeQuery();

        } catch (NullPointerException ne) {
            ne.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error"));
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
        return "..";
    }


    public void setOpssuiStatus(RichSelectOneChoice opssuiStatus) {
        this.opssuiStatus = opssuiStatus;

    }

    public RichSelectOneChoice getOpssuiStatus() {
        return opssuiStatus;
    }

    public void setSpotCount(Integer spotCount) {
        this.spotCount = spotCount;
    }

    public Integer getSpotCount() {
        return spotCount;
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

    public void btFileBeanProcess(DialogEvent dialogEvent) {
        String invCode = null;
        String instCode = null;
        Double newRate = 0.0;
        int exchng = 11;
        int product = 0;
        int importID = 0;
        String buySell=null;

        if (dialogEvent.getOutcome().ok == DialogEvent.Outcome.ok) {
            DCIteratorBinding commsnIteratorBindings = ADFUtils.findIterator("TradeCommissionChargeVOIterator");
            DCIteratorBinding btFileIterBingings = ADFUtils.findIterator("TradeFileBtVOIterator");
            Row commsnChngRow = commsnIteratorBindings.getCurrentRow();

            if (commsnChngRow.getAttribute("NewRate") != null && !commsnChngRow.getAttribute("NewRate").equals("")) {
                newRate = Double.parseDouble(commsnChngRow.getAttribute("NewRate").toString());

                if (btFileIterBingings.getViewObject().first() != null) {
                    Row trdFileMsavo = btFileIterBingings.getViewObject().first();
                    importID = Integer.parseInt(trdFileMsavo.getAttribute("ImportId").toString());
                } else {
                    Row trdFileMsaRow = btFileIterBingings.getCurrentRow();
                    if (trdFileMsaRow.getAttribute("ImportId") != null) {
                        importID = Integer.parseInt(trdFileMsaRow.getAttribute("ImportId").toString());
                    }
                }
                if (commsnChngRow.getAttribute("InvestorCode") != null) {
                    invCode = commsnChngRow.getAttribute("InvestorCode").toString();
                }
                if (commsnChngRow.getAttribute("InstrumentId") != null) {
                    instCode = commsnChngRow.getAttribute("InstrumentId").toString();
                }
                if (commsnChngRow.getAttribute("NewRate") != null) {
                    newRate = Double.parseDouble(commsnChngRow.getAttribute("NewRate").toString());
                }
                if(commsnChngRow.getAttribute("Buy_Sell") !=null){
                        buySell=commsnChngRow.getAttribute("Buy_Sell").toString();
                    }
                if (commsnChngRow.getAttribute("ProductId") != null) {
                    product = Integer.parseInt(commsnChngRow.getAttribute("ProductId").toString());
                TMPlsqlExecutor tmSql = new TMPlsqlExecutor();
                try {
                    tmSql.trdCommissionChangeProcess(exchng, importID, product, invCode, instCode, buySell , newRate);
                    this.refreshIterator("TradeCommissionChargeVOIterator");
                    ApplicationInfo.getCurrentUserDBTransaction().commit();
                    JSFUtils.addFacesInformationMessage(messagebundle.getString("leads_capita_trade_view_update_success"));
                    this.refreshIterator("TradeFileBtVOIterator");

                } catch (Exception e) {
                    ApplicationInfo.getCurrentUserDBTransaction().rollback();
                    JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_update_error"));
                    e.printStackTrace();
                    }
                }else{
                             JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_product_id"));
                         }
            } else {
                JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_new_rate"));
                return;
            }
        }
    }
}
