package leads.capita.trade.view.backing;

import java.sql.SQLException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.model.view.TradesVOImpl;

import leads.capita.trade.model.view.TradesVORowImpl;

import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.input.RichInputDate;
import oracle.adf.view.rich.component.rich.output.RichOutputText;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.ApplicationModule;
import oracle.jbo.Row;
import oracle.jbo.ViewObject;
import oracle.jbo.client.Configuration;
import oracle.jbo.server.Entity;
import oracle.jbo.server.ViewRowImpl;

public class TrdCommChngForeignInv extends BaseBean{
    
        private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
        private FacesContext fct;
        private ResourceBundle messagebundle;
    private RichOutputText commissionChngUI;
    private boolean isRowNumberalid;

    public TrdCommChngForeignInv() {
        super();
        bindings = ADFUtils.getDCBindingContainer();
        fct = JSFUtils.getFacesContextApp();
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");
        
    }
    
    public static boolean isModifiedRow(TradesVORowImpl curRow) {
        boolean isModifyState = false;
       // DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        
            //Row r=curRow;
            switch (curRow.getEntity(0).getEntityState()) {
            case Entity.STATUS_MODIFIED:
                {
                    isModifyState = true;
                    break;
                }
            }
     
        return isModifyState;
    }

    public String foreignInvNewCommSave() {
        /* boolean isModified=false;
        System.out.println("Enterd");
        ApplicationModule appMod=
        Configuration.createRootApplicationModule("leads.capita.trade.model.am.TradeFileService", "AppModuleLocal");
        TradesVOImpl vo = (TradesVOImpl)appMod.findViewObject("TradesVO");
        //if(vo == null) return null;
        while(vo.hasNext()){
                TradesVORowImpl row = (TradesVORowImpl)vo.next();
           isModified= this.isModifiedRow(row);
         System.out.println("val--- "+isModified);
             
            Object impId=row.getImportFileId();
            System.out.println("getImportFileId@@@"+impId+"************");
            } */
        System.out.println("enter");
        
        BindingContainer bindings = getBindings();
        OperationBinding operationBinding = bindings.getOperationBinding("Commit");
        Object result = operationBinding.execute();
        if (!operationBinding.getErrors().isEmpty()) {
            return null;
        }
        
        return null;
    }

    public BindingContainer getBindings() {
        return BindingContext.getCurrent().getCurrentBindingsEntry();
    }

    public void commissionChangeDialogListner(DialogEvent dialogEvent) {
        String invCode = null;
        String instCode = null;
        Double newRate = 0.0;
        int exchng = 10;
        int product = 0;
        int importID = 0;
        String tradingDate=null;
        String buySell = null;
        if (dialogEvent.getOutcome().ok == DialogEvent.Outcome.ok) {
            DCIteratorBinding commsnIteratorBindings = ADFUtils.findIterator("TradeCommissionChargeVOIterator");
            DCIteratorBinding fileIterBingings = ADFUtils.findIterator("TradesVOIterator");
            Row commsnChngRow = commsnIteratorBindings.getCurrentRow();
            if (commsnChngRow.getAttribute("NewRate") != null && !commsnChngRow.getAttribute("NewRate").equals("")) {
                newRate = Double.parseDouble(commsnChngRow.getAttribute("NewRate").toString());
                System.out.println("check the Trade");
                if (fileIterBingings.getViewObject().first() != null) {
                    Row trdFileMsavo = fileIterBingings.getViewObject().first();
                    importID = Integer.parseInt(trdFileMsavo.getAttribute("ImportFileId").toString());
                    
                } else {
                    Row trdFileMsaRow = fileIterBingings.getCurrentRow();
                    if (trdFileMsaRow.getAttribute("ImportFileId") != null) {
                        importID = Integer.parseInt(trdFileMsaRow.getAttribute("ImportFileId").toString());
                        
                    }
                }
                System.out.println("Checking Start");
                if (commsnChngRow.getAttribute("InvestorCode") != null) {
                    invCode = commsnChngRow.getAttribute("InvestorCode").toString();
                }
                if (commsnChngRow.getAttribute("InstrumentId") != null) {
                    instCode = commsnChngRow.getAttribute("InstrumentId").toString();
                }
                if (commsnChngRow.getAttribute("NewRate") != null) {
                    newRate = Double.parseDouble(commsnChngRow.getAttribute("NewRate").toString());
                }
                if (commsnChngRow.getAttribute("Buy_Sell") != null) {
                    buySell = commsnChngRow.getAttribute("Buy_Sell").toString();
                }
                if (commsnChngRow.getAttribute("TradingDate") != null) {
                    tradingDate = this.getFormatedDate(commsnChngRow.getAttribute("TradingDate"));
                }
                if (commsnChngRow.getAttribute("ProductId") != null) {
                    product = Integer.parseInt(commsnChngRow.getAttribute("ProductId").toString());
                    //use try cacth,commit
                    /* TMPlsqlExecutor tmSql = new TMPlsqlExecutor();
                    try {
                        //tmSql.trdCommissionChangeProcess(exchng, importID, product, invCode, instCode, buySell , newRate);
                        this.refreshIterator("TradeCommissionChargeVOIterator");
                        ApplicationInfo.getCurrentUserDBTransaction().commit();
                        JSFUtils.addFacesInformationMessage(messagebundle.getString("leads_capita_trade_view_update_success"));
                        this.refreshIterator("TradeFileBtVOIterator");

                    } catch (Exception e) {
                        ApplicationInfo.getCurrentUserDBTransaction().rollback();
                        JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_update_error"));
                        e.printStackTrace();
                    } */
                } else {
                    JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_product_id"));
                }
            } else {
                JSFUtils.addFacesErrorMessage(messagebundle.getString("leads_capita_trade_view_new_rate"));
                return;
            }
        }
    }
    /////////////////DateConversion//////////////////////////
    public String getFormatedDate(Object dateObj) {
        String dat = null;
        if (dateObj instanceof oracle.jbo.domain.Timestamp) {
            oracle.jbo.domain.Timestamp t = (oracle.jbo.domain.Timestamp)dateObj;
            Date dt = new Date(t.getTime());
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            dat = df.format(new Date(dt.getTime()));

        } else if (dateObj instanceof java.sql.Timestamp) {
            java.sql.Timestamp t = (java.sql.Timestamp)dateObj;
            Date dt = new Date(t.getTime());
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            dat = df.format(new Date(dt.getTime()));

        } else if (dateObj instanceof oracle.jbo.domain.Date) {
            oracle.jbo.domain.Date t = (oracle.jbo.domain.Date)dateObj;
            Date dt = new Date(t.getValue().getTime());
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            dat = df.format(new Date(dt.getTime()));
        } else {
            Date dt = (Date)dateObj;
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            dat = df.format(new Date(dt.getTime()));
        }
        return dat;
    }

    public String trdesFilterAction() {
        String invCode =null;
        String trdType= null;
        String trdDate=null;
        int broker=0;
        int exchnge=0;
        String instrmnt=null;
        Double chngComm=0.0;
        DCIteratorBinding commisnIteratorBindings = ADFUtils.findIterator("ForignInvTradeCommisionChangeFilterIterator");
                Row row = commisnIteratorBindings.getCurrentRow();
        if(checkRequiredValue()==true){
                broker=Integer.parseInt(row.getAttribute("Broker").toString());
                exchnge=Integer.parseInt(row.getAttribute("Exchange").toString());
                trdDate=this.getFormatedDate(row.getAttribute("TradingDate"));
                if(row.getAttribute("InvCode")!=null){
                invCode=row.getAttribute("InvCode").toString();
                }
                if(row.getAttribute("ShortName")!=null){
                instrmnt=row.getAttribute("ShortName").toString();
                }
                if(row.getAttribute("TradeType")!=null){
                trdType=row.getAttribute("TradeType").toString();
                }
                chngComm=Double.parseDouble(row.getAttribute("ChangeComm").toString());
            }
        
        //DCIteratorBinding commisnFuncIteratorBindings = ADFUtils.findIterator("GetTradeCommChngFuncVOIterator");
        try {
            ViewObject commisnFuncVo = ADFUtils.findIterator("GetTradeCommChngFuncVOIterator").getViewObject();
            
            //System.out.println("Get View Object********");
            commisnFuncVo.setNamedWhereClauseParam("p_trading_date", trdDate);
            commisnFuncVo.setNamedWhereClauseParam("p_exchange_id", exchnge);
            commisnFuncVo.setNamedWhereClauseParam("p_broker_id", broker);
            commisnFuncVo.setNamedWhereClauseParam("p_investor_code", invCode);
            commisnFuncVo.setNamedWhereClauseParam("p_instrument_code", instrmnt);
            commisnFuncVo.setNamedWhereClauseParam("p_buy_sale", trdType);
            
            //System.out.println("The Query is@@@@@"+commisnFuncVo.getQuery());
            commisnFuncVo.executeQuery();
            commisnFuncVo.setWhereClause(null);
            this.getCommissionChngUI().setValue(chngComm);
            if(commisnFuncVo.getEstimatedRowCount()== 0){
                    JSFUtils.addFacesInformationMessage("Record Not Found !");
                    return null;
                }

        } catch (Exception e) {
            // TODO: Add catch code
            e.printStackTrace();
        }
        
        
        
        return null;
    }

    public void setCommissionChngUI(RichOutputText commissionChngUI) {
        this.commissionChngUI = commissionChngUI;
    }

    public RichOutputText getCommissionChngUI() {
        return commissionChngUI;
    }

    public String saveTrdCommForeignInv() {
        String invCode =null;
        String trdType= null;
        String trdDate=null;
        int broker=0;
        int exchnge=0;
        String instrmnt=null;
        Double chngComm=0.0;

        if (checkRequiredValue() == true) {
            DCIteratorBinding commisnIteratorBindings =
                ADFUtils.findIterator("ForignInvTradeCommisionChangeFilterIterator");
            Row row = commisnIteratorBindings.getCurrentRow();

            broker = Integer.parseInt(row.getAttribute("Broker").toString());
            exchnge = Integer.parseInt(row.getAttribute("Exchange").toString());
            trdDate = this.getFormatedDate(row.getAttribute("TradingDate"));
            if (row.getAttribute("InvCode") != null) {
                invCode = row.getAttribute("InvCode").toString();
            }
            if (row.getAttribute("ShortName") != null) {
                instrmnt = row.getAttribute("ShortName").toString();
            }
            if (row.getAttribute("TradeType") != null) {
                trdType = row.getAttribute("TradeType").toString();
            }
            chngComm = Double.parseDouble(row.getAttribute("ChangeComm").toString());

        }
        TMPlsqlExecutor tmPlSqlExecutor = new TMPlsqlExecutor();
        try {
            tmPlSqlExecutor.trdCommissionChangeForeignInv(trdDate, exchnge, broker, invCode, instrmnt, trdType, chngComm);
            ApplicationInfo.getCurrentUserDBTransaction().commit();
        } catch (SQLException e) {
        } catch (Exception e) {
        }
        
        //JSFUtils.addFacesInformationMessage(this.getDayStartStatus());
        JSFUtils.addFacesInformationMessage("Successfully Saved !");

        return null;
    }
    public boolean checkRequiredValue(){
        boolean value =false;
            DCIteratorBinding commisnIteratorBindings = ADFUtils.findIterator("ForignInvTradeCommisionChangeFilterIterator");
            Row row = commisnIteratorBindings.getCurrentRow();
            if(row.getAttribute("TradingDate")==null ||row.getAttribute("TradingDate").equals("")){
                
            if(row.getAttribute("Broker")==null && (row.getAttribute("Exchange")==null) && (row.getAttribute("TradingDate")==null)
                && (row.getAttribute("ChangeComm")==null)){  
                value=false;
            }
            }else{
                value = true;
            }
        return value;
        }

    public String procssForeignInvTradeChanges() {
        String trdDate=null;
        DCIteratorBinding commisnIteratorBindings =
            ADFUtils.findIterator("ForignInvTradeCommisionChangeFilterIterator");
        Row row = commisnIteratorBindings.getCurrentRow();
        
        if(row != null && row.getAttribute("TradingDate")!= null){
                trdDate = this.getFormatedDate(row.getAttribute("TradingDate"));
                TMPlsqlExecutor tmPlSqlExecutor = new TMPlsqlExecutor();
                try {
                    System.out.println("Date:"+trdDate);
                    tmPlSqlExecutor.trdCommissionChangeForeignInvProcess(trdDate);
                    ApplicationInfo.getCurrentUserDBTransaction().commit();
                    JSFUtils.addFacesInformationMessage("Process has completed successfully !");
                } catch (Exception e) {
                    JSFUtils.addFacesErrorMessage("Process Error !");
                }
                //JSFUtils.addFacesInformationMessage(this.getDayStartStatus());
        } else
            JSFUtils.addFacesInformationMessage("Trading Date Not Found");
                
        return null;
    }
    public boolean isEnableProcessbutton() {
           
        if(countRowInViewObject()){
                return true;
        }else 
            return false;
    }
    public boolean countRowInViewObject(){
        String tradeDate = null;
        DCIteratorBinding iterator = ADFUtils.findIterator("ForignInvTradeCommisionChangeFilterIterator");
        Row row = iterator.getCurrentRow();
        tradeDate = row.getAttribute("TradingDate")==null?null:this.getFormatedDate(row.getAttribute("TradingDate"));
        DCIteratorBinding commisnIteratorBindings = ADFUtils.findIterator("TradeChangesViewIterator");
        ViewObject vo = commisnIteratorBindings.getViewObject();
        vo.setWhereClause("TRADE_DATE ='" + tradeDate + "' and STATUS ='N'");
        vo.executeQuery();
        vo.setWhereClause(null);
        long rowCount = vo.getEstimatedRowCount();
        if (rowCount > 0) {
            return false;
        } else
        return true; 

    }

   
}
