package leads.capita.trade.view.backing;

import javax.faces.application.FacesMessage;

import javax.faces.context.FacesContext;

import javax.faces.event.ValueChangeEvent;

import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.nav.RichCommandButton;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class PriceFileBean extends BaseBean {

    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    int importId;
    private RichCommandButton cbProcessUI;
    private RichCommandButton cbValidatorUI;

    public PriceFileBean() {
        super();

        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        getImportId();
    }
 
    public String process() {

        try {
            new TMPlsqlExecutor().processThisPriceFile(importId);
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The File has been processed!"));
            //this.refreshIterator("TradePriceFileVOIterator");
            
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "The File has not been processed!"));
        }

        return null;
    }

    //No need to validate file with status validated(V) or processed(P)
    public boolean getProcessEnable() {
        boolean isEnable = false;
        String status = "V";
        String processed = "P";
        ViewObject tradePriceFleVO = ADFUtils.findIterator("TradePriceFileVOIterator").getViewObject();
        tradePriceFleVO.setWhereClause(" TradePriceFile.proc_status <> '" + status +
                                       "' and TradePriceFile.proc_status <> '" + processed + "'");
        tradePriceFleVO.executeQuery();
        Row[] rowset = tradePriceFleVO.getAllRowsInRange();
        tradePriceFleVO.setWhereClause(null);
        tradePriceFleVO.clearCache();
        if (rowset.length > 0) {
            isEnable = false;
        } else {
            isEnable = true;
        }
        return isEnable;
    } 

    public String validate() throws Exception {
        try {
            new TMPlsqlExecutor().validateThisPriceFile(importId);
            fct.addMessage("SuccessMsg",
                           new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The file has been validated!!"));
            //this.getCbProcessUI().setDisabled(false);
            //this.getCbValidatorUI().setDisabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "The file has not been validated!"));
        }
        //InstrumentPriceVOIterator
        return null;
    }

    public void filterOnValidation(ValueChangeEvent valueChangeEvent) {

        DCIteratorBinding priceFileIteratorBindings = ADFUtils.findIterator("TradePriceFileVOIterator");

        try {
            ViewObject priceFileVO = priceFileIteratorBindings.getViewObject();

            if (valueChangeEvent.getNewValue().toString() == "ss") {
                priceFileVO.setWhereClause("IMPORT_ID =" + importId + " and substr(FILE_ID,1,5)=upper('PRICE') ");
            } else if (valueChangeEvent.getNewValue().toString() == "op") {
                priceFileVO.setWhereClause("IMPORT_ID =" + importId + " and substr(FILE_ID,1,5)=upper('PRICE') ");
            } else if (valueChangeEvent.getNewValue().toString() == "ui") {
                priceFileVO.setWhereClause("IMPORT_ID =" + importId + " and substr(FILE_ID,1,5)=upper('PRICE') ");
            } else {
                priceFileVO.setWhereClause("IMPORT_ID =" + importId);
            }
            priceFileVO.executeQuery();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "File Search Error"));
        }
        // Add event code here...
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
        //importId=Integer.parseInt(ADFUtils.findControlBinding("ImportId").getInputValue().toString());
        return importId;
    }

    public void setCbProcessUI(RichCommandButton cbProcessUI) {
        this.cbProcessUI = cbProcessUI;
    }

    public RichCommandButton getCbProcessUI() {
        return cbProcessUI;
    }

    public void setCbValidatorUI(RichCommandButton cbValidatorUI) {
        this.cbValidatorUI = cbValidatorUI;
    }

    public RichCommandButton getCbValidatorUI() {
        return cbValidatorUI;
    }
}
