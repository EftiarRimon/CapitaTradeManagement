package leads.capita.trade.view.backing;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import javax.faces.event.ActionEvent;

import javax.faces.event.ValueChangeEvent;

import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.exception.handler.CustomExceptionHandling;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.input.RichInputDate;

import oracle.adf.view.rich.component.rich.input.RichSelectOneRadio;

import oracle.adf.view.rich.event.DialogEvent;

import oracle.jbo.JboException;
import oracle.jbo.ViewObject;
import oracle.jbo.Row;


public class ManualSettlement extends BaseBean {
    
    private FacesContext fct;
    private DCBindingContainer bindings;
    private RichInputDate toDate;
    private RichSelectOneRadio filterType;
    private String dateValue;
    private String howlaOrForeign;
    private String whereClause;
    private DateFormat formatter = new SimpleDateFormat("dd-MMM-yy");
    private DCIteratorBinding instruSettlemetVOIteratorBindings;
    private DCIteratorBinding fundSettlemetVOIteratorBindings;
    private RichInputDate settleDate;

    public ManualSettlement() {
        super();
        
        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        howlaOrForeign ="F";
        fundSettlemetVOIteratorBindings = ADFUtils.findIterator("FundSettlemetVOIterator");
        instruSettlemetVOIteratorBindings = ADFUtils.findIterator("InstrumentSettlementsVOIterator");
    }
    
    public void pupulateFundList(){
 
        ViewObject fundSettlemetVO = fundSettlemetVOIteratorBindings.getViewObject();
        fundSettlemetVO.setWhereClause(this.getWhereClause());
        fundSettlemetVO.executeQuery();   
        System.out.println(fundSettlemetVO.getQuery());
    }
    

    public void pupulateInstrumentList(){

        ViewObject instruSettlemetVO = instruSettlemetVOIteratorBindings.getViewObject();
        instruSettlemetVO.setWhereClause(this.getWhereClause());
        instruSettlemetVO.executeQuery();   
    }


    public String getLoadText(){
        
        if ((toDate == null) || (this.toDate.getValue() == null)) {
        
            if (toDate == null) {
                this.toDate = new RichInputDate();
            }
            this.toDate.setValue(new Date());
            dateValue = formatter.format(new Date());
        } else {
            dateValue = formatter.format((Date)toDate.getValue());
        }
        
        //filterType.setValue("F");
     
        //this.pupulateFundList();
        //this.pupulateInstrumentList();   
        
        return "Manual Sattlement";
    }

    public String getWhereClause(){
        
        
        dateValue = formatter.format((Date)toDate.getValue());
        howlaOrForeign=filterType.getValue().toString();
            
        whereClause = "TRADING_DATE ='" + dateValue+"'";
        
        if (howlaOrForeign.equals("F")){
            whereClause += " AND IS_FOREIGN='T'";
        }

        if (howlaOrForeign.equals("D")){
            whereClause += "  AND  HOWLA_TYPE='D'";
        }            
        
        return whereClause;
    }
    

    public void setToDate(RichInputDate toDate) {
        this.toDate = toDate;
    }

    public RichInputDate getToDate() {
        return toDate;
    }

    public void setFilterType(RichSelectOneRadio filterType) {
        this.filterType = filterType;
    }

    public RichSelectOneRadio getFilterType() {
        return filterType;
    }

    public void fundAction(ActionEvent actionEvent) {
        this.pupulateFundList();
        
    }
    
    public void instrumentAction(ActionEvent actionEvent) { 
        this.pupulateInstrumentList();   
    }

    public void filterChange(ValueChangeEvent valueChangeEvent) {
        this.pupulateFundList();
        this.pupulateInstrumentList();   
    }
    
    
    public void popFundSettle(DialogEvent dialogEvent) {
        try {
            String sDate=  formatter.format((Date)settleDate.getValue());
            if (sDate!=null){
                  if (dialogEvent.getOutcome() == DialogEvent.Outcome.ok ) {
                     Row row = fundSettlemetVOIteratorBindings.getCurrentRow();
                      new TMPlsqlExecutor().setFundSettleDate( Long.valueOf(row.getAttribute("Id").toString()), sDate);
                  }                          
            }
        } catch (FacesException ex) {
            ex.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(ex.getMessage()));
        }
    }
    
    
    public void popInstruSettle(DialogEvent dialogEvent) {
        try {
            String sDate=  formatter.format((Date)settleDate.getValue());
            if (sDate!=null){
                  if (dialogEvent.getOutcome() == DialogEvent.Outcome.ok ) {
                     Row row = fundSettlemetVOIteratorBindings.getCurrentRow();
                      new TMPlsqlExecutor().setInstruSettleDate( Long.valueOf(row.getAttribute("Id").toString()), sDate);
                  }                          
            }
        } catch (FacesException ex) {
            ex.printStackTrace();
            fct.addMessage("ErrorMsg", new FacesMessage(ex.getMessage()));
        }
    }

    public void setSettleDate(RichInputDate settleDate) {
        this.settleDate = settleDate;
    }

    public RichInputDate getSettleDate() {
        return settleDate;
    }
}
