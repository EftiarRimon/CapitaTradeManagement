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
import oracle.adf.view.rich.event.DialogEvent;

import oracle.jbo.ViewObject;

public class InstrumentPriceBean extends BaseBean{
    
    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    int importId;
    
    public InstrumentPriceBean() {
        super();

        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        
    }

    public String process() {
        
        try {
                new TMPlsqlExecutor().processThisPriceFile(importId); 
                fct.addMessage("SuccessMsg", new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The File has been processed!"));
            }
        catch (Exception e)
        {
            e.printStackTrace();
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "The File has not been processed!"));
        }
        
        return null;
    }

    public String validate() {
        
        try {
                new TMPlsqlExecutor().processThisPriceFile(importId); 
                fct.addMessage("SuccessMsg", new FacesMessage(FacesMessage.SEVERITY_INFO, "", "The file has been validate!!"));
            }
        catch (Exception e)
        {
            e.printStackTrace();
            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "The file has not been validate!"));
        }
        //InstrumentPriceVOIterator
        return null;
    }
    
    public void filterOnValidation(ValueChangeEvent valueChangeEvent) {
        
        DCIteratorBinding priceFileIteratorBindings = ADFUtils.findIterator("InstrumentPriceVOIterator");
    
            try {
                    ViewObject priceFileVO = priceFileIteratorBindings.getViewObject();
                
                    if (valueChangeEvent.getNewValue().toString()=="ss")
                    {
                        priceFileVO.setWhereClause("IMPORT_ID =" +importId+ " and substr(FILE_ID,1,5)=upper('PRICE') ");    
                    }
                    else if (valueChangeEvent.getNewValue().toString()=="op")
                    {
                        priceFileVO.setWhereClause("IMPORT_ID =" +importId+ " and substr(FILE_ID,1,5)=upper('PRICE') ");    
                    }
                    else if (valueChangeEvent.getNewValue().toString()=="ui")
                    {
                        priceFileVO.setWhereClause("IMPORT_ID =" +importId+ " and substr(FILE_ID,1,5)=upper('PRICE') ");    
                    }
                    else
                    {
                        priceFileVO.setWhereClause("IMPORT_ID =" +importId);    
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
        return importId;
    }
}
