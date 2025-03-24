package leads.capita.trade.view.backing;

import java.math.BigDecimal;

import java.sql.Timestamp;

import javax.faces.event.ActionEvent;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.ADFUtils;
import leads.capita.common.ui.exception.handler.CustomExceptionHandling;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.file.FlexTradeFileUtil;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.event.DialogEvent;

import oracle.adf.view.rich.event.PopupFetchEvent;

import oracle.binding.OperationBinding;

import oracle.jbo.ApplicationModule;
import oracle.jbo.JboException;
import oracle.jbo.Row;
import oracle.jbo.ViewObject;
import oracle.jbo.server.Entity;
import oracle.jbo.server.ViewRowImpl;

public class PurchasePowerLimit {
    public PurchasePowerLimit() {
        super();
    }

    public String purchasePowerLimitSaveAction() throws Exception {
        if (changesExists()) {
            String ukId = FlexTradeFileUtil.getUniqueValue();
            System.out.println("ukId-- " + ukId);
            DCIteratorBinding headerIter = ADFUtils.findIterator("PurchasePowerLimitHeaderVOIterator");
            Row headerRow = headerIter.getViewObject().getCurrentRow();
            if (headerRow != null)
                headerRow.setAttribute("UkId", ukId);
            this.saveAction();
            this.refreshIterator("PurchasePowerLimitHeaderVOIterator");
        } else {
            JSFUtils.addFacesInformationMessage("Nothing To Save....");
        }
        return null;
    }

    public void saveAction() throws Exception {
        OperationBinding operationBinding = JSFUtils.getBindings().getOperationBinding("Commit");
        try {
            if (operationBinding != null) {
                operationBinding.execute();
                if (operationBinding.getErrors().isEmpty()) {
                    JSFUtils.addFacesInformationMessage("Saved Successfully..");
                } else {
                    System.out.println("--- "+operationBinding.getErrors());
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
    }

    public void refreshIterator(String iteratorName) {
        if (iteratorName != null) {
            ADFUtils.findIterator(iteratorName).executeQuery();
            ADFUtils.findIterator(iteratorName).getViewObject().clearCache();
            ADFUtils.findIterator(iteratorName).refresh(DCIteratorBinding.RANGESIZE_UNLIMITED);
        }
        return;
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

    public void newPurchasePowerLimitAction(ActionEvent actionEvent) {
        DCBindingContainer bindings = ADFUtils.getDCBindingContainer();
        OperationBinding headerBinding = bindings.getOperationBinding("CreateInsertHeader");
        if (headerBinding != null) {
            headerBinding.execute();
        } else
            JSFUtils.addFacesErrorMessage("CreateInsert Binding Problem !!");
    }

    public void newSlabSaveDialogListener(DialogEvent dialogEvent) {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok))
            return;
        else
            this.newSlabSaveAction();
    }

    public void newSlabSaveAction() {
        String headerId = null;
        long accId = 0l;
        DCIteratorBinding slabIter = ADFUtils.findIterator("PurchasePowerLimitSlabVOIterator");

        DCIteratorBinding investorIter = ADFUtils.findIterator("InvestorAccLOVIterator");
        Row[] invRows = investorIter.getAllRowsInRange();
        // Row curInvRow = investorIter.getViewObject().getCurrentRow();

        try {
            if (invRows != null && invRows.length > 0) {
                for (int i = 0; i < invRows.length; i++) {
                    Row curInvRow = null;
                    curInvRow = invRows[i];
                    if (curInvRow.getAttribute("SelectedRow") != null &&
                        curInvRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        System.out.println("select Val-- " + curInvRow.getAttribute("SelectedRow").toString());
                        BigDecimal additionPct = new BigDecimal(0);
                        headerId =
                                FlexTradeFileUtil._getAttrValueFromIter("PurchasePowerLimitHeaderVOIterator", "UkId");
                        accId = Long.valueOf(curInvRow.getAttribute("AccountId").toString());
                        String idConcat= String.valueOf(accId);
                        String slabId = FlexTradeFileUtil.getUniqueValue().concat(idConcat);

                        if (curInvRow.getAttribute("Pct") != null || curInvRow.getAttribute("Pct").equals("")) {
                            additionPct = BigDecimal.valueOf(Double.valueOf(curInvRow.getAttribute("Pct").toString()));
                        }
                        ViewObject slabVo = slabIter.getViewObject();
                        if (slabVo != null) {
                            Row newSlabRow = slabVo.createRow();
                            newSlabRow.setAttribute("SlabId", slabId);
                            newSlabRow.setAttribute("UkId", headerId);
                            newSlabRow.setAttribute("AccountId", accId);
                            newSlabRow.setAttribute("Pct", additionPct);
                            System.out.println(slabId + "-- " + headerId + "-- " + accId + "-- " + additionPct);
                        }
                    }
                }
            }
            this.saveAction();
            this.refreshIterator("PurchasePowerLimitSlabVOIterator");
        } catch (NumberFormatException nfe) {
            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
            this.refreshIterator("PurchasePowerLimitSlabVOIterator");
            nfe.printStackTrace();
        } catch (Exception e) {
            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
            this.refreshIterator("PurchasePowerLimitSlabVOIterator");
            e.printStackTrace();
        }
    }

    public boolean isNewHeader() {
        boolean isNewRow = false;
        isNewRow = this.isNewStateRow("PurchasePowerLimitHeaderVOIterator");
        return isNewRow;
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

    public void newSlabPopupFetchListener(PopupFetchEvent popupFetchEvent) {
        try {
            System.out.println("Entered fetch listener..");
            DCIteratorBinding headerIter = ADFUtils.findIterator("PurchasePowerLimitHeaderVOIterator");
            Row r = headerIter.getViewObject().getCurrentRow();
            String headerId = r.getAttribute("UkId").toString();
            ViewObject invVO = ADFUtils.findIterator("InvestorAccLOVIterator").getViewObject();
            invVO.setNamedWhereClauseParam("headerId", headerId);
            invVO.executeQuery();
            System.out.println("fetch-- " + headerId);
            invVO.setWhereClause(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String refreshSlab() {
        this.refreshIterator("PurchasePowerLimitSlabVOIterator");
        return null;
    }
}
