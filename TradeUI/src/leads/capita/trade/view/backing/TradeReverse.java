package leads.capita.trade.view.backing;


import java.util.Date;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;


import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.file.OperationExecutionUtil;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.RichPopup;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class TradeReverse extends BaseBean {
    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    private ResourceBundle messagebundle;
    private int sessionCount;
    private long sessionId;
    private int branchId;
    private TMPlsqlExecutor tmPlSqlExecutor = new TMPlsqlExecutor();
    private Date appDate;
    private RichPopup popConUser;

    public TradeReverse() {
        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");
    }

    void syncAppState() {

        try {
            branchId =
                    ApplicationInfo.getCurrentUserBranch() != null ? Integer.parseInt(ApplicationInfo.getCurrentUserBranch()) :
                    9;
            appDate = tmPlSqlExecutor.getAppDate(branchId);

            sessionId = ApplicationInfo.getSessionId() != null ? ApplicationInfo.getSessionId() : 1;

        } catch (Exception e) {
            JSFUtils.addFacesInformationMessage("Database connection problem!");
            e.printStackTrace();
        }
    }

    private int getSessionCount() {
        this.syncAppState();

        DCIteratorBinding conUserIteratorBindings = ADFUtils.findIterator("ConnectedUsersVOIterator");
        ViewObject connUserVO = conUserIteratorBindings.getViewObject();
        connUserVO.executeQuery();
        sessionCount = connUserVO.getRowCount();
        return sessionCount;
    }

    private void showConnUserPopup() {

        RichPopup.PopupHints hints = new RichPopup.PopupHints();
        popConUser.show(hints);
    }

    public String tradeReverse_action() throws Exception {
        
        OperationExecutionUtil operationExecutionUtil = new OperationExecutionUtil();
        
        if (this.getSessionCount() > 1) {
            showConnUserPopup();
        } else{
            Integer importID = null;
            DCIteratorBinding trdFilemsaIterBingings = ADFUtils.findIterator("TradeReverseFileListVOIterator");
            try{
                if (trdFilemsaIterBingings != null && trdFilemsaIterBingings.getEstimatedRowCount() > 0) {
                    operationExecutionUtil.setOperationExecutionMessage("true", "Sorry!! trade reverse process running", "Trade Reverse Running");
                    Row rows[] = trdFilemsaIterBingings.getAllRowsInRange();
                    for (int i = 0; i < rows.length; i++) {
                        Row trdFileMsaRow = null;
                        trdFileMsaRow = rows[i];
                        if (trdFileMsaRow != null) {
                                if (trdFileMsaRow != null) {
                                    if (trdFileMsaRow.getAttribute("ImportId") != null) {
                                        importID = Integer.parseInt(trdFileMsaRow.getAttribute("ImportId").toString());
                                        if (importID != null) {
                                            TMPlsqlExecutor tmSql = new TMPlsqlExecutor();
                                            try {
                                                tmSql.tradeReverseProcess(importID);
                                                ApplicationInfo.getCurrentUserDBTransaction().commit();
                                                JSFUtils.addFacesInformationMessage(messagebundle.getString("leads_capita_trade_view_process_success"));
                                                this.refreshIterator("TradeReverseFileListVOIterator");
                                                operationExecutionUtil.setOperationExecutionMessage("false", "null", "Trade Reverse Done");
                                            } catch (Exception e) {
                                                ApplicationInfo.getCurrentUserDBTransaction().rollback();
                                                JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_update_error"));
                                                e.printStackTrace();
                                                operationExecutionUtil.setOperationExecutionMessage("false", "null", "Trade Reverse Error");
                                            }
                                        } else
                                            JSFUtils.addFacesInformationMessage("Import Id Not Found");
                                            operationExecutionUtil.setOperationExecutionMessage("false", "null", "Trade Reverse Import Id Not Found");
                                    }
                                } else{
                                    JSFUtils.addFacesInformationMessage("Please load data before trade reverse");
                                    operationExecutionUtil.setOperationExecutionMessage("false", "null", "Trade Reverse Load Data");
                                }

                        }
                    }
                }
            }catch(Exception e){
                ApplicationInfo.getCurrentUserDBTransaction().rollback();
                JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_update_error" + "!!"));
                e.printStackTrace();
            }
        } 

        return null;
    }

    public String logoutall_action() {
        EodBean eodbean = new EodBean();
        eodbean.logoutAllSession();
        return null;
    }

    public String refresh() {
        this.refreshIterator("TradeReverseFileListVOIterator");
        return null;
    }

    public void setPopConUser(RichPopup popConUser) {
        this.popConUser = popConUser;
    }

    public RichPopup getPopConUser() {
        return popConUser;
    }
    
}
