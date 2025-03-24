package leads.capita.trade.view.backing;


import java.sql.SQLException;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.PPPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.DataControlFrame;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.render.ClientEvent;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;


public class PurchasePowerBean extends BaseBean{
    private FacesContext fct;

    public PurchasePowerBean(){
        fct = JSFUtils.getFacesContextApp();
    }

    public String purchasePowerUpdateAction() {
        DCIteratorBinding iter = ADFUtils.findIterator("InvestorAccountListVOIterator");
        ViewObject vO = iter.getViewObject();
        Row r[] = iter.getAllRowsInRange();
        Row row = null;
        int totalCounter = 0; //updated
        int totalFailCounter = 0; //fail to uodate
        try {
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    row = r[i];
                    if (row.getAttribute("SelectedRow") != null &&
                        row.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        String accountId = null;
                        accountId = row.getAttribute("AccountId").toString();
                        iter.setCurrentRowWithKey(row.getKey().toStringFormat(true));
                        boolean isUpdate = false;
                        isUpdate = dbSpCallForPurchasePowerUpdate(accountId); //DB SP Call...
                        if (isUpdate) {
                            totalCounter++;
                        } else {
                            totalFailCounter++;
                        }
                    }
                }
               new PPPlsqlExecutor().getSessionCommonDbService().getDBTransaction().commit();
            }
        } catch (Exception e) {
            new PPPlsqlExecutor().getSessionCommonDbService().getDBTransaction().commit();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, null, e.getMessage()));
        }
        if (totalCounter > 0 && totalFailCounter > 0) {
            JSFUtils.addFacesInformationMessage(totalCounter + " records has been  updated successfully .. and " +
                                                totalFailCounter + " records fail to update!! ");
        } else if (totalCounter > 0 && totalFailCounter == 0) {
            JSFUtils.addFacesInformationMessage(totalCounter + " records has been  updated successfully ..");
            this.refresh();
        } else if (totalCounter == 0 && totalFailCounter > 0) {
            JSFUtils.addFacesInformationMessage(totalFailCounter + " records fail to update!! ");
        } else {
            JSFUtils.addFacesInformationMessage("Update fail !!");
        }
        return null;
    }

    //RollBack

    public String cbCancel_action() {
        BindingContainer bindings = JSFUtils.getBindings();
        OperationBinding operationBinding = bindings.getOperationBinding("Rollback");
        if (operationBinding != null) {
            operationBinding.execute();
            if (!operationBinding.getErrors().isEmpty()) {
                return null;
            }
            rollbackTransaction();
        }
        return null;
    }


    public static void rollbackTransaction() {
        BindingContext context = BindingContext.getCurrent();
        String dcFrameName = context.getCurrentDataControlFrame();
        DataControlFrame dcFrame = context.findDataControlFrame(dcFrameName);
        dcFrame.rollback();
    }

    public void selectAllValueChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        Boolean selectRow = false;
        selectRow = (Boolean)valueChangeEvent.getNewValue();
        selectAllAcion("InvestorAccountListVOIterator", "SelectedRow", selectRow);
    }


    public void selectAllAcion(String iteratorName, String selectedRowAttr, boolean select) {
        DCIteratorBinding iterBinding = ADFUtils.findIterator(iteratorName);
        Row[] rows;
        rows = iterBinding.getAllRowsInRange();
        try {
            if (rows != null && rows.length > 0) {
                for (int i = 0; i < rows.length; i++) {
                    Row row = rows[i];
                    row.setAttribute(selectedRowAttr, select);
                }
            }
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
    }


    private boolean dbSpCallForPurchasePowerUpdate(String accountId) throws SQLException {
        boolean isUpdate = false;
        try {
            isUpdate = new PPPlsqlExecutor().processPurchasePowerUpdate(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isUpdate;
    }


    //Busy State code.....Start......
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
        purchasePowerUpdateAction();
       /*  setAcEvent(acEvent); // save teh query event for the method that really fires the query to use.
        toggleBusyPopup(true); // Fires the popup, which when shown, fires a server listener that fires the query. */

    }

    public void ProcessAction(ClientEvent clientEvent) {
        purchasePowerUpdateAction();
        toggleBusyPopup(false);
    }
    //Busy State code...Finish........
    
    public String refresh() {
        this.refreshIterator("InvestorAccountListVOIterator");
        return null;
    }
  
}
