package leads.capita.trade.view.backing;

import java.math.BigDecimal;

import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

import java.util.Map;

import java.util.ResourceBundle;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import javax.faces.event.ValueChangeEvent;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.bean.BaseBeanImpl;
import leads.capita.common.ui.bean.BaseBeanUtil;
import leads.capita.common.ui.exception.handler.CustomExceptionHandling;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.plsql.TMPlsqlExecutor;

import leads.capita.trade.view.validator.TMCustomValidator;

import oracle.adf.controller.AdfcIllegalArgumentException;
import oracle.adf.controller.ControllerContext;
import oracle.adf.controller.ViewPortContext;
import oracle.adf.model.BindingContext;
import oracle.adf.model.DataControlFrame;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.share.ADFContext;

import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.input.RichSelectBooleanCheckbox;
import oracle.adf.view.rich.event.DialogEvent;
import oracle.adf.view.rich.event.PopupFetchEvent;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.AlreadyLockedException;
import oracle.jbo.JboException;
import oracle.jbo.Row;
import oracle.jbo.RowAlreadyDeletedException;
import oracle.jbo.RowInconsistentException;
import oracle.jbo.ViewObject;
import oracle.jbo.domain.Timestamp;
import oracle.jbo.server.Entity;
import oracle.jbo.server.ViewRowImpl;

public class BdaOrderAllocManageBean {
    private RichPopup bdaBuyAddEditPopup;
    private RichPopup saleEditPopup;
    private ResourceBundle messagebundle;
    private FacesContext fct;
    private RichInputText investorCodesUI;
    private RichPopup searchPopupUI;
    private RichInputText salesInvestorCodesUI;
    private RichPopup salesSearchPopupUI;

    public BdaOrderAllocManageBean() {
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");
    }

    public synchronized BigDecimal selectedRowsColumnTotal(String iteratorName, String colName, String chkBoxAttr,
                                                           String chkBoxAttVal) {
        DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        iter.setRangeSize((int)iter.getEstimatedRowCount());
        Row rows[] = iter.getAllRowsInRange();
        BigDecimal total = new BigDecimal(0);
        if (rows != null && rows.length > 0) {
            for (int i = 0; i < rows.length; i++) {
                if (rows[i].getAttribute(chkBoxAttr) != null &&
                    rows[i].getAttribute(chkBoxAttr).toString().equalsIgnoreCase(chkBoxAttVal)) {
                    if (rows[i].getAttribute(colName) != null){
                        total = total.add(new BigDecimal(rows[i].getAttribute(colName).toString()));
                    }
                }
            }
        }
        return total;
    }


    private synchronized BigDecimal rowsColumnTotal(String iteratorName, String colName) {
        DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        Row rows[] = iter.getAllRowsInRange();
        BigDecimal total = new BigDecimal(0);
        if (rows != null && rows.length > 0) {
            for (int i = 0; i < rows.length; i++) {
                if (rows[i].getAttribute(colName) != null)
                    total = total.add(new BigDecimal(rows[i].getAttribute(colName).toString()));
            }
        }
        return total;
    }


    public void createNewOrder(ActionEvent actionEvent) {
        if (JSFUtils.getBindings().getOperationBinding("CreateInsert") != null)
            JSFUtils.getBindings().getOperationBinding("CreateInsert").execute();
        else
            JSFUtils.addFacesErrorMessage("Action binding problem !");
    }

    //-------BDA BUY

    public String saveAction() {
        if (_bdaAllocStatusChangeValidation("BdaBuyOrdersVOIterator", "BdaBuyOrdersDetailVOIterator",
                                            "AllocQuantity")) {
            boolean isSaved = commonSaveAction();
            try {
                if (isSaved)
                    bdaBuyAddEditPopup.hide();
            } catch (NullPointerException ne) {
            } catch (Exception ne) {
            }
        } else {
            JSFUtils.addFacesErrorMessage("Save Failed !");
        }
        return null;
    }


    //-------BDA Sale

    public String saveSaleAction() {
        if (_bdaAllocStatusChangeValidation("BdaOrdersVOIterator", "BdaOrderDetailVOIterator", "AllocQuantity")) {
            boolean isSaved = commonSaveAction();
            try {
                if (isSaved)
                    bdaBuyAddEditPopup.hide();
            } catch (NullPointerException ne) {
            } catch (Exception ne) {
            }
        } else {
            JSFUtils.addFacesErrorMessage("Save Failed !");
        }
        return null;
    }


    private boolean _bdaAllocStatusChangeValidation(String baseIter, String childIter, String attrNAme) {
        boolean isValid = false;
        try {
            BigDecimal total = new BigDecimal(0);
            String tradeQty = _getCurrentRowFieldValue(baseIter, "TradeQuantity");
            String status = _getCurrentRowFieldValue(baseIter, "Status");
            String allocDate = _getCurrentRowFieldValue(baseIter, "AllocDate");
            //total = rowsColumnTotal(childIter, attrNAme);
            String orderId = _getCurrentRowFieldValue(baseIter, "OrderId");
            Object[] params = { orderId, "ALLOC" }; //Sp Params
            BigDecimal totalAllocQty =
                new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_trade.get_total_bda_order_alloc_qty(?,?)", params);
            //System.out.print(tradeQty+"-----??----" +total.doubleValue());
            if (status.equalsIgnoreCase("ALLOC")) {
                if (allocDate != null) {
                    if (Double.valueOf(tradeQty).compareTo(totalAllocQty.doubleValue()) == 0) {
                        isValid = true;
                    } else {
                        JSFUtils.addFacesErrorMessage("Trade Qty and Total Allocation Qty should be same !");
                    }
                } else {
                    JSFUtils.addFacesErrorMessage("Allocation Date Required !");
                }
            } else {
                isValid = true;
            }
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }
        return isValid;
    }

    //-------BDA Sale

    public String saleSaveAction() {
        boolean isSaved = commonSaveAction();
        try {
            if (isSaved)
                saleEditPopup.hide();
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }
        return null;
    }


    //-------BDA BUY

    public String cancelAddEdit() {
        try {
            refreshCurrntRow("BdaBuyOrdersDetailVOIterator");
            bdaBuyAddEditPopup.hide();
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }
        return null;
    }


    //-------BDA SALE

    public String cancelSaleAddEdit() {
        try {
            refreshCurrntRow("BdaOrderDetailVOIterator");
            saleEditPopup.hide();
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }
        return null;
    }


    private void refreshCurrntRow(String iter) {
        try {
            ViewObject vo = null;
            vo = ADFUtils.findIterator(iter).getViewObject();
            prepareVOToUpdate(vo, true); //-------------------
        } catch (NullPointerException ne) {
        } catch (Exception e) {
        }
    }


    //SO-----------------for sales order

    public void filterinvListByInstrAndTradeCode(ActionEvent actionEvent) {
        String instrumentId = null;
        String tradeCode = null;
        try {
            String orderId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "OrderId");
            filterWithExecParam("BdaOrdersFilterVOIterator", orderId); //----------------
            filterByBindVariable("BdaSalesOrderInvListVOIterator", "order_id", orderId); //-------------
            instrumentId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "InstrumentId");
            filterByBindVariable("BdaSalesOrderInvListVOIterator", "INSTRUMENT_ID", instrumentId);
            tradeCode = _getCurrentRowFieldValue("BdaOrdersVOIterator", "TradeCode");
            //System.out.print("<----> \n " + tradeCode + "&&&" + instrumentId);
            filterIteratorByInstrTradeCode("BdaSalesOrderInvListVOIterator", instrumentId, tradeCode); //------------
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //SA-----------------for sales Allocation

    public void filterInvListByInstrAndTradeCodeForSA(ActionEvent actionEvent) {
        String instrumentId = null;
        String tradeCode = null;
        try {
            String orderId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "OrderId");
            filterWithExecParam("BdaOrdersFilterVOIterator", orderId); //----------------
            instrumentId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "InstrumentId");
            tradeCode = _getCurrentRowFieldValue("BdaOrdersVOIterator", "TradeCode");
            //System.out.print("<----> \n " + tradeCode + "&&&" + instrumentId);
            filterIteratorByInstrTradeCode("BdaSalesAllocInvListIterator", instrumentId, tradeCode); //------------
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void filterIteratorByInstrTradeCode(String iter, String instrumentId, String tradeCode) {
        DCIteratorBinding it = ADFUtils.findIterator(iter);
        ViewObject VO = it.getViewObject();
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        if (tradeCode != null)
            whereClauseSql.append(" QRSLT.TRADE_CODE='" + tradeCode + "'");
        if (tradeCode != null && instrumentId != null)
            whereClauseSql.append(" AND QRSLT.INSTRUMENT_ID='" + instrumentId + "' ");
        if (tradeCode == null && instrumentId != null)
            whereClauseSql.append("  QRSLT.INSTRUMENT_ID='" + instrumentId + "' ");

        VO.setWhereClause(whereClauseSql.toString());
        VO.setRangeSize(-1);
        VO.addQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        //System.out.print("<----> \n " + VO.getQuery() + ADFUtils.findIterator(iter).getEstimatedRowCount());
        VO.executeQuery();
        VO.setWhereClause(null);
    }

    public void filterIteratorByTradeCode(String iter, String tradeCode) {
        DCIteratorBinding it = ADFUtils.findIterator(iter);
        ViewObject VO = it.getViewObject();
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        if (tradeCode != null)
            whereClauseSql.append(" QRSLT.TRADE_CODE='" + tradeCode + "'");
        VO.setWhereClause(whereClauseSql.toString());
        VO.setRangeSize(-1);
        VO.addQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        //System.out.print("<----> \n " + VO.getQuery() + ADFUtils.findIterator(iter).getEstimatedRowCount());
        VO.executeQuery();
        VO.setWhereClause(null);
    }


    public String _getCurrentRowFieldValue(String iterator, String fieldAttr) {
        DCIteratorBinding iter = ADFUtils.findIterator(iterator);
        String fieldValue = null;
        if (iter != null && iter.getEstimatedRowCount() > 0) {
            Row r = iter.getCurrentRow();
            if (r != null) {
                if (r.getAttribute(fieldAttr) != null)
                    fieldValue = r.getAttribute(fieldAttr).toString();
            }
        }
        //System.out.print("-----" + fieldValue);
        return fieldValue;
    }

    private void filterByBindVariable(String iterator, String varName, String bindVarValue) {
        DCIteratorBinding it = ADFUtils.findIterator(iterator);
        ViewObject VO = it.getViewObject();
        VO.setNamedWhereClauseParam(varName, bindVarValue);
        VO.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        VO.executeQuery();
    }
    //process

    public String bulkSalesOrder() {
        try {
            DCIteratorBinding bdaSalesInvListIterBinding = ADFUtils.findIterator("BdaSalesOrderInvListVOIterator");
            int counter = 0;
            Row invsetRow = null;
            String orderId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderId");
            bdaSalesInvListIterBinding.setRangeSize((int)bdaSalesInvListIterBinding.getEstimatedRowCount());
            Row r[] = bdaSalesInvListIterBinding.getAllRowsInRange();
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    invsetRow = r[i];
                    if (invsetRow.getAttribute("SelectedRow") != null &&
                        invsetRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        if (saleOrderValidation(invsetRow)) {
                            commonInsertMethod("CreateInsert"); //----------
                            Row curRow = _getCurrentRowFrmIter("BdaOrderDetailVOIterator");
                            if (curRow != null) {
                                curRow.setAttribute("AccountId", invsetRow.getAttribute("AccountId"));
                                curRow.setAttribute("OrderQuantity", invsetRow.getAttribute("OrderFor"));
                                counter++;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                if (counter > 0) {
                    commonSaveAction(); //------------
                    //refreshiterator("BdaSalesOrderInvListVOIterator");
                    //filterInvListByOrderId("BdaOrdersFilterVOIterator", "BdaSalesOrderInvListVOIterator",
                    //"OrderId"); //-----------
                    filterByBindVariable("BdaSalesOrderInvListVOIterator", "order_id", orderId); //-------------
                    String instrumentId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "InstrumentId");
                    String tradeCode = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "TradeCode");
                    filterIteratorByInstrTradeCode("BdaSalesOrderInvListVOIterator", instrumentId,
                                                   tradeCode); //------------
                }
            }
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
        return null;
    }
    //

    private void commonInsertMethod(String insertActionBind) {
        if (JSFUtils.getBindings().getOperationBinding(insertActionBind) != null) {
            JSFUtils.getBindings().getOperationBinding(insertActionBind).execute();
        } else {
            JSFUtils.addFacesErrorMessage("Action binding problem !");
        }
    }


    private boolean commonSaveAction() {
        boolean isSaved = false;
        OperationBinding operationBinding = JSFUtils.getBindings().getOperationBinding("Commit");
        if (operationBinding != null) {
            operationBinding.execute();
            if (operationBinding.getErrors().isEmpty()) {
                JSFUtils.addFacesInformationMessage("Saved Successfully");
                isSaved = true;
            } else {
                new CustomExceptionHandling().handleException((JboException)operationBinding.getErrors().get(0));
            }
        } else {
            JSFUtils.addFacesErrorMessage("Commit binding problem");
        }
        return isSaved;
    }

    private boolean saveActionWithoutMsg() {
        boolean isSaved = false;
        OperationBinding operationBinding = JSFUtils.getBindings().getOperationBinding("Commit");
        if (operationBinding != null) {
            operationBinding.execute();
            if (operationBinding.getErrors().isEmpty()) {
                isSaved = true;
            } else {
                new CustomExceptionHandling().handleException((JboException)operationBinding.getErrors().get(0));
            }
        } else {
            JSFUtils.addFacesErrorMessage("Commit binding problem");
        }
        return isSaved;
    }

    public Row _getCurrentRowFrmIter(String iterator) {
        DCIteratorBinding iter = ADFUtils.findIterator(iterator);
        Row r = null;
        if (iter != null && iter.getEstimatedRowCount() > 0) {
            r = iter.getCurrentRow();
        }
        return r;
    }

    public String bdaSalesAllocationProcess() {
        try {
            DCIteratorBinding bdaSalesInvListIterBinding = ADFUtils.findIterator("BdaSalesAllocInvListIterator");
            int counter = 0;
            Row invsetRow = null;
            String detailId = null;
            Integer _tradeQty = new Integer(0);
            Integer totalAllocation = new Integer(0);
            BigDecimal totalApplyFor = new BigDecimal(0);
            bdaSalesInvListIterBinding.setRangeSize((int)bdaSalesInvListIterBinding.getEstimatedRowCount());
            Row r[] = bdaSalesInvListIterBinding.getAllRowsInRange();
            if (r.length > 0 && r != null) {
                String orderId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderId");
                _tradeQty = Integer.valueOf(_getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "TradeQuantity"));
                Object[] params = { orderId, "ALLOC" }; //Sp Params
                BigDecimal totalAppliedQty =
                    new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_trade.get_total_bda_order_alloc_qty(?,?)",
                                                                  params);
                totalApplyFor =
                        selectedRowsColumnTotal("BdaSalesAllocInvListIterator", "ApplyFor", "SelectedRow", "true");
                totalAllocation = totalAppliedQty.intValue() + totalApplyFor.intValue();
                if (totalAllocation.compareTo(_tradeQty) <= 0) {
                    for (int i = 0; i < r.length; i++) {
                        invsetRow = r[i];
                        if (invsetRow.getAttribute("SelectedRow") != null &&
                            invsetRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                            if (saleAllocationValidation(invsetRow)) {
                                if (invsetRow.getAttribute("DetailId") != null)
                                    detailId = invsetRow.getAttribute("DetailId").toString();
                                Row dbRow = _getdbRow("BdaOrderDetailDefaultVOIterator", detailId);
                                if (dbRow != null) {
                                    dbRow.setAttribute("AllocQuantity", invsetRow.getAttribute("ApplyFor"));
                                    counter++;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    if (counter > 0) {
                        commonSaveAction(); //------------
                        refreshiterator("BdaSalesAllocInvListIterator");
                        // filterInvListByOrderId("BdaOrdersFilterVOIterator", "BdaSalesAllocInvListIterator",
                        // "OrderId"); //-----------
                        filterInvListByOrderId("BdaOrdersFilterVOIterator", "BdaSalesAllocInvListIterator",
                                               "OrderId"); //-----------
                    }
                } else {
                    JSFUtils.addFacesErrorMessage(" Total allocation ( " + totalAllocation +
                                                  " ) can't be greater than total trade quantity !");
                }
            }

        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
        return null;
    }


    private Row _getdbRow(String iter, String detailId) {
        DCIteratorBinding dcIter = ADFUtils.findIterator(iter);
        ViewObject VO = dcIter.getViewObject();
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        whereClauseSql.append(" BdaOrderDetail.DETAIL_ID='" + detailId + "'");
        VO.setWhereClause(whereClauseSql.toString());
        VO.setRangeSize(-1);
        VO.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        VO.executeQuery();
        Row curRow = null;
        if (VO.hasNext()) {
            curRow = VO.next();
        }
        return curRow;
    }

    private synchronized Row _getdatabaseRow(String iter, Integer accountId, Integer instrumentId, Date date) {
        DCIteratorBinding dcIter = ADFUtils.findIterator(iter);
        ViewObject VO = dcIter.getViewObject();
        String d = new SimpleDateFormat("dd-MMM-yyyy").format(getSysDate());
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        whereClauseSql.append("BdaPurchaseOrder.ACCOUNT_ID='" + accountId + "'");
        whereClauseSql.append(" AND BdaPurchaseOrder.INSTRUMENT_ID='" + instrumentId + "'");
        whereClauseSql.append(" AND to_char(BdaPurchaseOrder.order_date,'dd-MON-yyyy') = upper('" + d + "')");
        VO.setWhereClause(whereClauseSql.toString());
        //System.out.print("<----> \n " + VO.getQuery());
        VO.executeQuery();
        Row curRow = null;
        while (VO.hasNext()) {
            curRow = VO.next();
        }
        return curRow;
    }


    public static Date getSysDate() {
        Date sDate = null;
        try {
            sDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return sDate;
    }


    public void rollbackCall(ActionEvent actionEvent) {
        cbCancel_action();
    }

    public String cbCancel_action() {
        try {
            BindingContainer bindings = JSFUtils.getBindings();
            OperationBinding operationBinding = bindings.getOperationBinding("Rollback");
            if (operationBinding != null) {
                operationBinding.execute();
                if (!operationBinding.getErrors().isEmpty()) {
                    return null;
                }
                rollbackTransaction();
            } else {
                rollbackTransaction();
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (FacesException fe) {
            fe.printStackTrace();
        } catch (JboException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void rollbackTransaction() {
        BindingContext context = BindingContext.getCurrent();
        String dcFrameName = context.getCurrentDataControlFrame();
        DataControlFrame dcFrame = context.findDataControlFrame(dcFrameName);
        dcFrame.rollback();
    }

    private void refreshiterator(String iter) {
        try {
            DCIteratorBinding dcIter = ADFUtils.findIterator(iter);
            if (dcIter != null && dcIter.getEstimatedRowCount() > 0) {
                dcIter.executeQuery();
                dcIter.getViewObject().clearCache();
            }
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }
    }


    private void putValueInSession(String key, String value) {
        ADFContext adfCtx = ADFContext.getCurrent();
        Map scope = adfCtx.getSessionScope();
        scope.put(key, value);
    }

    private String getValueFromSession(String key) {
        ADFContext adfCtx = ADFContext.getCurrent();
        String val = null;
        Map scope = adfCtx.getSessionScope();
        Object obj = scope.get(key);
        if (obj != null) {
            val = obj.toString();
        }
        return val;
    }

    public void investorValueChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(JSFUtils.getFacesContext());
        DCIteratorBinding dcIter = ADFUtils.findIterator("BdaSalesOrderInvListVOIterator");
        String accId = _getCurrentRowFieldValue("InvestorListForBDALOVIterator", "AccountId");
        ViewObject VO = dcIter.getViewObject();
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        whereClauseSql.append(" QRSLT.ACCOUNT_ID='" + accId + "' ");
        VO.setWhereClause(whereClauseSql.toString());
        VO.executeQuery();
    }


    //------------------POA------------

    //PO-----------------for Buy order

    public void filterinvListByInstrAndTradeCodeForPO(ActionEvent actionEvent) {
        String instrumentId = null;
        String tradeCode = null;
        String orderId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "OrderId");
        instrumentId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "InstrumentId");
        filterWithExecParam("BdaOrdersFilterVOIterator", orderId); //----------------
        filterByBindVariable("BdaBuyOrderInvListIterator", "order_id", orderId); //-------------
        filterByBindVariable("BdaBuyOrderInvListIterator", "p_instrument_id", instrumentId);
        tradeCode = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "TradeCode");
        //System.out.print(orderId+"<----> \n " + tradeCode + " &&&" + instrumentId);
        filterIteratorByTradeCode("BdaBuyOrderInvListIterator", tradeCode); //------------

    }

    //PA-----------------for Purchase Allocation

    public void filterInvListByInstrAndTradeCodeForPA(ActionEvent actionEvent) {
        // String instrumentId = null;
        //String tradeCode = null;
        String orderId = null;
        try {
            //instrumentId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "InstrumentId");
            //tradeCode = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "TradeCode");
            orderId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "OrderId");
            filterWithExecParam("BdaOrdersFilterVOIterator", orderId); //----------------
            //System.out.print("<----> \n " + tradeCode + "&&&" + instrumentId);
            filterIteratorByOrderId("BdaBuyAllocInvListIterator", orderId); //------------
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
    //PA-------by order id----------for Purchase Allocation investor List Filter

    public void filterInvListByOrderIdForPA(String masterIter,String childIter,String attrName) {
        String orderId = null;
        try {
            orderId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderId");//---BdaOrdersFilterVOIterator
            filterIteratorByOrderId("BdaBuyAllocInvListIterator", orderId); //------------
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

  */
    // filterInvListByOrderId("BdaOrdersFilterVOIterator","BdaBuyAllocInvListIterator,"OrderId");
    //Child iterator filter------by order id----------for  investor List Filter

    public void filterInvListByOrderId(String masterIter, String childIter, String attrName) {
        String orderId = null;
        try {
            orderId = _getCurrentRowFieldValue(masterIter, attrName); //---BdaOrdersFilterVOIterator
            filterIteratorByOrderId(childIter, orderId); //------------
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void filterIteratorByOrderId(String iter, String orderId) {
        DCIteratorBinding it = ADFUtils.findIterator(iter);
        ViewObject VO = it.getViewObject();
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        if (orderId != null)
            whereClauseSql.append(" QRSLT.ORDER_ID='" + orderId + "'");
        VO.setWhereClause(whereClauseSql.toString());
        VO.setRangeSize(-1);
        VO.addQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        // System.out.print("<----> \n " + VO.getQuery() + ADFUtils.findIterator(iter).getEstimatedRowCount());
        VO.executeQuery();
        VO.setWhereClause(null);
    }

    // BDA PO process

    public String processBuyOrder() {
        try {
            DCIteratorBinding bdaSalesInvListIterBinding = ADFUtils.findIterator("BdaBuyOrderInvListIterator");
            int counter = 0;
            Row invsetRow = null;
            String orderId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderId");
            bdaSalesInvListIterBinding.setRangeSize((int)bdaSalesInvListIterBinding.getEstimatedRowCount());
            Row r[] = bdaSalesInvListIterBinding.getAllRowsInRange();
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    invsetRow = r[i];
                    if (invsetRow.getAttribute("SelectedRow") != null &&
                        invsetRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        if (buyOrderValidation(invsetRow)) {
                            commonInsertMethod("CreateInsert"); //----------
                            Row curRow = _getCurrentRowFrmIter("BdaOrderDetailForSearchVOIterator");
                            if (curRow != null) {
                                curRow.setAttribute("AccountId", invsetRow.getAttribute("AccountId"));
                                curRow.setAttribute("OrderQuantity", invsetRow.getAttribute("OrderFor"));
                                curRow.setAttribute("OrderId", orderId);
                                counter++;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                if (counter > 0) {
                    commonSaveAction(); //------------
                    refreshiterator("BdaBuyOrderInvListIterator");
                    //filterInvListByOrderId("BdaOrdersFilterVOIterator", "BdaBuyOrderInvListIterator", "OrderId");
                    //filterByBindVariable("BdaBuyOrderInvListIterator", "order_id", orderId); //-------------
                }
            }
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
        return null;

    }

    public BigDecimal getSelectedRowsTotalOrder() {
        return selectedRowsColumnTotal("BdaBuyOrderInvListIterator", "OrderFor", "SelectedRow", "true");
    }
    
    public BigDecimal getSelectedRowsTotalSalesOrder() {
        return selectedRowsColumnTotal("BdaSalesOrderInvListVOIterator", "OrderFor", "SelectedRow", "true");
    }
    
    public BigDecimal getSelectedRowsTotalBuyAlloc() {
        return selectedRowsColumnTotal("BdaBuyAllocInvListIterator", "ApplyFor", "SelectedRow", "true");
    }
    
    public BigDecimal getSelectedRowsTotalSalesAlloc() {
        return selectedRowsColumnTotal("BdaSalesAllocInvListIterator", "ApplyFor", "SelectedRow", "true");
    }

    public void filterWithExecParam(String iterToFilter, String orderId) {
        BindingContext bindingctx = BindingContext.getCurrent();
        BindingContainer bindings = bindingctx.getCurrentBindingsEntry();
        DCBindingContainer bindingsImpl = (DCBindingContainer)bindings;
        DCIteratorBinding iter = bindingsImpl.findIteratorBinding(iterToFilter);
        if (iter != null) {
            Integer orderIdParam = Integer.valueOf(orderId);
            OperationBinding operationBinding = bindings.getOperationBinding("ExecuteWithParams");
            operationBinding.getParamsMap().put("orderId", orderIdParam);
            operationBinding.execute();
        }
    }

    public String processBuyAllocation() throws SQLException {
        try {
            DCIteratorBinding bdaSalesInvListIterBinding = ADFUtils.findIterator("BdaBuyAllocInvListIterator");
            int counter = 0;
            Row invsetRow = null;
            String detailId = null;
            Integer _tradeQty = new Integer(0);
            Integer totalAllocation = new Integer(0);
            bdaSalesInvListIterBinding.setRangeSize((int)bdaSalesInvListIterBinding.getEstimatedRowCount());
            Row r[] = bdaSalesInvListIterBinding.getAllRowsInRange();
            String orderId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderId");
            BigDecimal totalApplyFor = new BigDecimal(0);
            _tradeQty = Integer.valueOf(_getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "TradeQuantity"));
            Object[] params = { orderId, "ALLOC" }; //Sp Params
            BigDecimal totalAppliedQty =
                new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_trade.get_total_bda_order_alloc_qty(?,?)", params);
            totalApplyFor = selectedRowsColumnTotal("BdaBuyAllocInvListIterator", "ApplyFor", "SelectedRow", "true");
            totalAllocation = totalAppliedQty.intValue() + totalApplyFor.intValue();
            //  System.out.print("***^^__***" + totalAllocation);
            if (r != null && r.length > 0) {
                if (totalAllocation.compareTo(_tradeQty) <= 0) {
                    for (int i = 0; i < r.length; i++) {
                        invsetRow = r[i];
                        if (invsetRow.getAttribute("SelectedRow") != null &&
                            invsetRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                            if (buyAllocationValidation(invsetRow)) {
                                if (invsetRow.getAttribute("DetailId") != null)
                                    detailId = invsetRow.getAttribute("DetailId").toString();
                                Row dbRow = _getdbRow("BdaOrderDetailForSearchVOIterator", detailId);
                                if (dbRow != null) {
                                    dbRow.setAttribute("AllocQuantity", invsetRow.getAttribute("ApplyFor"));
                                    counter++;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    if (counter > 0) {
                        commonSaveAction(); //------------
                        //refreshiterator("BdaBuyAllocInvListIterator");
                        filterInvListByOrderId("BdaOrdersFilterVOIterator", "BdaBuyAllocInvListIterator",
                                               "OrderId"); //-----------
                    }
                } else {
                    JSFUtils.addFacesErrorMessage(" Total allocation ( " + totalAllocation +
                                                  " ) can't be greater than total trade quantity !");
                }

            }

        } catch (NullPointerException ne) {
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }
        return null;
    }


    public BindingContainer getBindings() {
        return BindingContext.getCurrent().getCurrentBindingsEntry();
    }


    // BDA BUY

    public String newDetailInsert() {
        BindingContainer bindings = getBindings();
        OperationBinding operationBinding = bindings.getOperationBinding("CreateInsert");
        if (operationBinding != null) {
            Object result = operationBinding.execute();
            if (!operationBinding.getErrors().isEmpty()) {
                return null;
            }
            /* RichPopup addEditPopup = getBdaBuyAddEditPopup();
            RichPopup.PopupHints hints = new RichPopup.PopupHints();
            if (addEditPopup != null)
                addEditPopup.show(hints);
            else
                JSFUtils.addFacesErrorMessage("Popup  binding problem !"); */
        } else {
            JSFUtils.addFacesErrorMessage("Action binding problem !");
        }
        return null;
    }


    public void addEditBdaSalePopupListener(DialogEvent dialogEvent) throws SQLException {
        if (dialogEvent.getOutcome() == DialogEvent.Outcome.ok) {
            commonSaveAction();
        } else {
            try {
                ViewObject vo = null;
                vo = ADFUtils.findIterator("BdaBuyOrdersDetailVOIterator").getViewObject();
                prepareVOToUpdate(vo, false); //-------------------
                // refreshiterator("BdaBuyOrdersDetailVOIterator"); //---------------
            } catch (NullPointerException ne) {
            } catch (Exception ne) {
            }
        }
    }

    private void prepareVOToUpdate(ViewObject vo, boolean execute) {
        Row currow = vo.getCurrentRow();
        if (currow != null) {
            try {
                currow.refresh(Row.REFRESH_REMOVE_NEW_ROWS | Row.REFRESH_WITH_DB_FORGET_CHANGES |
                               Row.REFRESH_UNDO_CHANGES | Row.REFRESH_WITH_DB_ONLY_IF_UNCHANGED |
                               Row.REFRESH_CONTAINEES | Row.REFRESH_FORGET_NEW_ROWS);
                if (execute)
                    vo.executeQuery();
                vo.clearCache();
                controllerExceptionHandler(); //------------
            } catch (RowAlreadyDeletedException rade) {
                throw new JboException("Request row already deleted by another user ", "25019", null);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            } catch (RowInconsistentException rie) {
                throw new JboException("Request row already modified by another user ", "25014", null);
            } catch (AlreadyLockedException rle) {
                throw new JboException(rle);
            } catch (Exception ex) {
                throw new JboException(ex);
            }
        }
    }

    public boolean isNewStateRow(String iteratorName) {
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


    private void controllerExceptionHandler() {
        try {
            ControllerContext context = ControllerContext.getInstance();
            ViewPortContext currentRootViewPort = context.getCurrentRootViewPort();
            if (currentRootViewPort != null) {
                if (currentRootViewPort.isExceptionPresent()) {
                    currentRootViewPort.clearException();
                }
            }
        } catch (NullPointerException npex) {
        } catch (Exception ex) {
        }
    }

    //--------------  Validation-------------


    private boolean saleOrderValidation(Row invsetRow) {
        boolean isValid = false;
        Integer accountId = null;
        Double orderQty = null;
        if (invsetRow != null) {
            String instrumentId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "InstrumentId");

            if (invsetRow.getAttribute("AccountId") != null)
                accountId = Integer.valueOf(invsetRow.getAttribute("AccountId").toString());

            if (invsetRow.getAttribute("OrderFor") == null) {
                JSFUtils.addFacesErrorMessage(" Order Quantity is required ! For Investor -> " +
                                              invsetRow.getAttribute("InvestorCode"));
            } else if (Double.valueOf(invsetRow.getAttribute("OrderFor").toString()).doubleValue() < 1) {
                JSFUtils.addFacesErrorMessage(" Invalid order quantity found ! For " +
                                              invsetRow.getAttribute("InvestorCode"));
            } else {
                if (invsetRow.getAttribute("OrderFor") != null)
                    orderQty = Double.valueOf(invsetRow.getAttribute("OrderFor").toString());
                Object[] params =
                { "INSTRUMENT_BALANCES", "SALABLE_QTY", "INSTRUMENT_ID", instrumentId, "ACCOUNT_ID", invsetRow.getAttribute("AccountId").toString() }; //Sp Params
                BigDecimal salableQty =
                    new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_capita_utl.get_table_column_value(?,?,?,?,?,?)",
                                                                  params);
                if (orderQty.compareTo(salableQty.doubleValue()) > 0) {
                    JSFUtils.addFacesErrorMessage(" Order quantity can't be greater than salable (" + salableQty +
                                                  " ) quantity !");
                } else {
                    isValid = true;
                }
            }

        }
        return isValid;
    }


    private boolean saleAllocationValidation(Row invsetRow) {
        boolean isValid = false;
        Integer applyFor = new Integer(0);
        Integer orderFor = new Integer(0);
        Integer accountId = null;
        if (invsetRow != null) {
            String instrumentId = _getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "InstrumentId");
            if (invsetRow.getAttribute("ApplyFor") != null)
                applyFor = Integer.valueOf(invsetRow.getAttribute("ApplyFor").toString());
            if (invsetRow.getAttribute("OrderQuantity") != null)
                orderFor = Integer.valueOf(invsetRow.getAttribute("OrderQuantity").toString());
            if (invsetRow.getAttribute("AccountId") != null)
                accountId = Integer.valueOf(invsetRow.getAttribute("AccountId").toString());
            if (invsetRow.getAttribute("ApplyFor") == null)
                JSFUtils.addFacesErrorMessage(" Applied Quantity is required ! For Investor -> " +
                                              invsetRow.getAttribute("InvestorCode"));
            /*  else if (applyFor.compareTo(orderFor) > 0)
                JSFUtils.addFacesErrorMessage(" Applied quantity can't be greater than Order Qty. ! For Investor -> " +
                                              invsetRow.getAttribute("InvestorCode")); */
            else {
                Object[] params =
                { "INSTRUMENT_BALANCES", "SALABLE_QTY", "INSTRUMENT_ID", instrumentId, "ACCOUNT_ID", invsetRow.getAttribute("AccountId").toString() }; //Sp Params
                BigDecimal salableQty =
                    new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_capita_utl.get_table_column_value(?,?,?,?,?,?)",
                                                                  params);
                if (applyFor.compareTo(orderFor) > 0 && applyFor.compareTo(salableQty.intValue()) > 0) {
                    JSFUtils.addFacesErrorMessage(" Applied quantity can't be greater than Order Qty. or salable Qty. ( " +
                                                  salableQty + " ) ! For Investor -> " +
                                                  invsetRow.getAttribute("InvestorCode"));
                } else {
                    isValid = true;
                }
            }
        }
        return isValid;
    }


    private boolean buyOrderValidation(Row invsetRow) {
        boolean isValid = false;
        Integer accountId = null;
        Integer orderFor = new Integer(0);
        Double orderAmnt = new Double(0);
        Double purchsaePower = new Double(0);
        Double orderPrice = new Double(0);
        if (_getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderPrice") != null)
            orderPrice = Double.valueOf(_getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "OrderPrice"));
        if (invsetRow != null) {
            if (invsetRow.getAttribute("AccountId") != null)
                accountId = Integer.valueOf(invsetRow.getAttribute("AccountId").toString());
            if (invsetRow.getAttribute("RemainingPurchasePower") != null)
                purchsaePower = Double.valueOf(invsetRow.getAttribute("RemainingPurchasePower").toString());
            if (invsetRow.getAttribute("OrderFor") != null)
                orderFor = Integer.valueOf(invsetRow.getAttribute("OrderFor").toString());
            orderAmnt = orderFor * orderPrice; //-------------calculated
            if (invsetRow.getAttribute("OrderFor") == null)
                JSFUtils.addFacesErrorMessage(" Order Quantity is required ! For " +
                                              invsetRow.getAttribute("InvestorCode"));
            else if (Double.valueOf(invsetRow.getAttribute("OrderFor").toString()).doubleValue() < 1) {
                JSFUtils.addFacesErrorMessage(" Invalid order quantity found ! For " +
                                              invsetRow.getAttribute("InvestorCode"));
            } else if (orderAmnt.compareTo(purchsaePower) > 0)
                /* JSFUtils.addFacesErrorMessage(" Maximum Purchase Power Exceeds ! For Investor -> " +
                                              invsetRow.getAttribute("InvestorCode")); */
                isValid = true;
            else
                isValid = true;

        }
        return isValid;
    }

    private boolean buyAllocationValidation(Row invsetRow) {
        boolean isValid = false;
        Integer applyFor = new Integer(0);
        Integer orderFor = new Integer(0);
        Integer accountId = null;
        Double tradePrice = null;
        Double _totalPrice = new Double(0);
        Double purchasePower = new Double(0);
        if (invsetRow != null) {
            tradePrice = Double.valueOf(_getCurrentRowFieldValue("BdaOrdersFilterVOIterator", "TradePrice"));
            if (invsetRow.getAttribute("ApplyFor") != null)
                applyFor = Integer.valueOf(invsetRow.getAttribute("ApplyFor").toString());
            if (invsetRow.getAttribute("OrderQuantity") != null)
                orderFor = Integer.valueOf(invsetRow.getAttribute("OrderQuantity").toString());
            if (invsetRow.getAttribute("AccountId") != null)
                accountId = Integer.valueOf(invsetRow.getAttribute("AccountId").toString());
            if (invsetRow.getAttribute("PurchasePower") != null)
                purchasePower = Double.valueOf(invsetRow.getAttribute("PurchasePower").toString());
            if (invsetRow.getAttribute("ApplyFor") == null)
                JSFUtils.addFacesErrorMessage(" Applied Quantity is required ! For Investor -> " +
                                              invsetRow.getAttribute("InvestorCode"));
            else if (applyFor.compareTo(orderFor) > 0)
                JSFUtils.addFacesErrorMessage(" Applied quantity can't be greater than Order Qty. ! For Investor -> " +
                                              invsetRow.getAttribute("InvestorCode"));
            else {

                Object[] params = { getSystemDate(), invsetRow.getAttribute("AccountId").toString() }; //Sp Params
                BigDecimal _purchasePower =
                    new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_trade.get_bda_purchase_capacity(?,?)", params);
                // System.out.print("$$$$$$^^^*****"+_purchasePower);
                _totalPrice = (applyFor == null ? 0 : applyFor) * (tradePrice == null ? 0 : tradePrice);
                if (_totalPrice.compareTo(_purchasePower.doubleValue()) <= 0) {
                    isValid = true;
                } else
                    /*JSFUtils.addFacesErrorMessage(" Not have enough purchase power  (" + purchasePower +
                                                  ") For Investor " + invsetRow.getAttribute("InvestorCode"));*/
                    isValid = true;
            }
        }
        return isValid;
    }


    //--------------  Validation-------------

    public void setBdaBuyAddEditPopup(RichPopup bdaBuyAddEditPopup) {
        this.bdaBuyAddEditPopup = bdaBuyAddEditPopup;
    }

    public RichPopup getBdaBuyAddEditPopup() {
        return bdaBuyAddEditPopup;
    }

    public void setSaleEditPopup(RichPopup saleEditPopup) {
        this.saleEditPopup = saleEditPopup;
    }

    public RichPopup getSaleEditPopup() {
        return saleEditPopup;
    }


    public void bdaSalesDetailDeleteListener(DialogEvent dialogEvent) {
        if (dialogEvent.getOutcome() != DialogEvent.Outcome.ok) {
            return;
        } else {
            boolean isDeleted;
            try {
                isDeleted =
                        new BaseBeanImpl("leads.capita.trade.view.TradeUIBundle").manualdelete("BdaOrderDetailVOIterator",
                                                                                               new String[] { "DetailId" },
                                                                                               new String[] { "DETAIL_ID" },
                                                                                               "bda_order_detail");
                if (isDeleted) {
                    ADFUtils.findIterator("BdaOrderDetailVOIterator").executeQuery();
                    ADFUtils.findIterator("BdaOrderDetailVOIterator").getViewObject().clearCache();
                    JSFUtils.addFacesInformationMessage(messagebundle.getString("leads_capita_trade_view_delete_Message"));
                }
            } catch (SQLException se) {
            } catch (NullPointerException ne) {
            } catch (Exception ne) {
            }

        }
    }


    public void bdaBuyDetailDeleteListener(DialogEvent dialogEvent) {
        if (dialogEvent.getOutcome() != DialogEvent.Outcome.ok) {
            return;
        } else {
            boolean isDeleted;
            try {
                isDeleted =
                        new BaseBeanImpl("leads.capita.trade.view.TradeUIBundle").manualdelete("BdaBuyOrdersDetailVOIterator",
                                                                                               new String[] { "DetailId" },
                                                                                               new String[] { "DETAIL_ID" },
                                                                                               "bda_order_detail");
                if (isDeleted) {
                    ADFUtils.findIterator("BdaBuyOrdersDetailVOIterator").executeQuery();
                    ADFUtils.findIterator("BdaBuyOrdersDetailVOIterator").getViewObject().clearCache();
                    JSFUtils.addFacesInformationMessage(messagebundle.getString("leads_capita_trade_view_delete_Message"));
                }
            } catch (SQLException se) {
            } catch (NullPointerException ne) {
            } catch (Exception ne) {
            }

        }
    }

    public String loadTradePriceQtyForBDABuy() throws SQLException {
        String tradeCode = null;
        String tradeDate = null;
        String instrumentId = null;
        String exchangeId = null;
        instrumentId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "InstrumentId");
        tradeCode = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "TradeCode");
        //instrumentId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "InstrumentId");
        exchangeId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "ExchangeId");
        try {
            // System.out.println(instrumentId+"------------"+exchangeId+"******"+tradeCode+"&&&"+getSystemDate());
            if (tradeCode != null && instrumentId != null && exchangeId != null) {
                BigDecimal tfbuyPrice =
                    new TMPlsqlExecutor().getTradePriceAndQuantity("pkg_trade_file.get_bda_buy_price(?,?,?,?)",
                                                                   tradeCode, getSystemDate(), instrumentId,
                                                                   exchangeId);
                copyFieldValue(tfbuyPrice.toString(), "#{bindings.TradePrice.inputValue}");

                BigDecimal tfbuyQty =
                    new TMPlsqlExecutor().getTradePriceAndQuantity("pkg_trade_file.get_bda_buy_quantity(?,?,?,?)",
                                                                   tradeCode, getSystemDate(), instrumentId,
                                                                   exchangeId);
                copyFieldValue(tfbuyQty.toString(), "#{bindings.TradeQuantity.inputValue}");
                saveActionWithoutMsg();
            } else {
                JSFUtils.addFacesErrorMessage("Trade Code ,Instrument and Exchange are required !");
            }
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }
        return null;
    }

    public String loadTradePriceQtyForBDASale() throws SQLException {
        String tradeCode = null;
        String tradeDate = null;
        String instrumentId = null;
        String exchangeId = null;
        instrumentId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "InstrumentId");
        tradeCode = _getCurrentRowFieldValue("BdaOrdersVOIterator", "TradeCode");
        //instrumentId = _getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "InstrumentId");
        exchangeId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "ExchangeId");
        try {
            // System.out.println(instrumentId+"------------"+exchangeId+"******"+tradeCode+"&&&"+getSystemDate());
            if (tradeCode != null && instrumentId != null && exchangeId != null) {
                BigDecimal tfbuyPrice =
                    new TMPlsqlExecutor().getTradePriceAndQuantity("pkg_trade_file.get_bda_sell_price(?,?,?,?)",
                                                                   tradeCode, getSystemDate(), instrumentId,
                                                                   exchangeId);
                copyFieldValue(tfbuyPrice.toString(), "#{bindings.TradePrice.inputValue}");

                BigDecimal tfbuyQty =
                    new TMPlsqlExecutor().getTradePriceAndQuantity("pkg_trade_file.get_bda_sell_quantity(?,?,?,?)",
                                                                   tradeCode, getSystemDate(), instrumentId,
                                                                   exchangeId);
                copyFieldValue(tfbuyQty.toString(), "#{bindings.TradeQuantity.inputValue}");
                saveActionWithoutMsg();
            } else {
                JSFUtils.addFacesErrorMessage("Trade Code ,Instrument and Exchange are required !");
            }
        } catch (NullPointerException ne) {
        } catch (Exception ne) {
        }

        return null;
    }

    public String getSystemDate() {
        String sysDate = null;
        try {
            Date rawDate = new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate());
            sysDate = new SimpleDateFormat("dd-MMM-yyyy").format(rawDate);
        } catch (Exception e) {
            System.out.print("Date  Formetting Problem ---TM BDA  !!");
        }
        return sysDate;
    }


    public String copyFieldValue(String fromValueBinding, String toValueBinding) {
        fct = JSFUtils.getFacesContextApp();
        Application app = fct.getApplication();
        ExpressionFactory elFactory = app.getExpressionFactory();
        ELContext elContext = fct.getELContext();
        ValueExpression valueExpField1 = elFactory.createValueExpression(elContext, fromValueBinding, String.class);
        ValueExpression valueExpField2 = elFactory.createValueExpression(elContext, toValueBinding, String.class);
        valueExpField2.setValue(elContext, valueExpField1.getValue(elContext));
        return null;
    }

    //pop up field

    public void buyAllocationValidator(FacesContext facesContext, UIComponent uIComponent, Object object) {
        Double tradePrice = new Double(0);
        try {
            String accountId = _getCurrentRowFieldValue("BdaBuyOrdersDetailVOIterator", "AccountId");
            Double orderQty =
                Double.valueOf(_getCurrentRowFieldValue("BdaBuyOrdersDetailVOIterator", "OrderQuantity"));
            tradePrice = Double.valueOf(_getCurrentRowFieldValue("BdaBuyOrdersVOIterator", "TradePrice"));
            new TMCustomValidator().bdaBuyAllocEditValidation(facesContext, uIComponent, object, accountId, tradePrice,
                                                              orderQty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //pop up field

    public void saleAllocationValidator(FacesContext facesContext, UIComponent uIComponent, Object object) {
        Double tradePrice = new Double(0);
        try {
            String accountId = _getCurrentRowFieldValue("BdaOrderDetailVOIterator", "AccountId");
            String instrumentId = _getCurrentRowFieldValue("BdaOrdersVOIterator", "InstrumentId");
            Double orderQty = Double.valueOf(_getCurrentRowFieldValue("BdaOrderDetailVOIterator", "OrderQuantity"));
            tradePrice = Double.valueOf(_getCurrentRowFieldValue("BdaOrdersVOIterator", "TradePrice"));
            new TMCustomValidator().bdaSaleAllocEditValidation(facesContext, uIComponent, object, accountId,
                                                               tradePrice, orderQty, instrumentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isNotOnDateForSale() {
        return bdaDateValidation("BdaOrdersVOIterator");
    }

    public boolean isNotOnDateForBuy() {
        return bdaDateValidation("BdaBuyOrdersVOIterator");
    }
    //Exactly on date

    private boolean bdaDateValidation(String bastIterator) {
        DCIteratorBinding iter = null;
        iter = ADFUtils.findIterator(bastIterator);
        Timestamp orderDate = null;
        boolean isValid = false;
        if (iter != null && iter.getAllRowsInRange().length > 0) {
            Row currentRow = iter.getCurrentRow();
            if (currentRow.getAttribute("OrderDate") != null)
                orderDate = (oracle.jbo.domain.Timestamp)currentRow.getAttribute("OrderDate");
            if (orderDate != null) {
                try {
                    java.sql.Date bdaOrderDateDate = (java.sql.Date)orderDate.dateValue();
                    if (ApplicationInfo.getSystemDate() != null) {
                        if (bdaOrderDateDate.compareTo(new SimpleDateFormat("yyyy-MM-dd").parse(ApplicationInfo.getSystemDate())) ==
                            0)
                            isValid = true;
                        else
                            isValid = false;
                    }

                } catch (SQLException e) {
                    isValid = false;
                    System.out.print("Date  Cast Problem ---BDA Master  !!");
                } catch (Exception e) {
                    isValid = false;
                    System.out.print("Date  Formatting Problem ---BDA Master  !!");
                    e.printStackTrace();
                }
            }
        }
        return isValid;
    }
    
    private static Object evaluateEL(String el) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();
        ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();
        ValueExpression exp = expressionFactory.createValueExpression(elContext, el, Object.class);
        return exp.getValue(elContext);
    }
    
    public void selectAllBdaSalesAlloc(ValueChangeEvent valueChangeEvent) {

        boolean isSelected = ((Boolean)valueChangeEvent.getNewValue()).booleanValue();
        DCBindingContainer dcb = (DCBindingContainer)evaluateEL("#{bindings}");
        DCIteratorBinding dciter = dcb.findIteratorBinding("BdaSalesAllocInvListIterator");

        ViewObject vo = dciter.getViewObject();
        int i = 0;
        Row row = null;
        vo.reset();
        while (vo.hasNext()) {
            if (i == 0)
                row = vo.first();
            else
                row = vo.next();

            if (isSelected) {
                row.setAttribute("SelectedRow", true);
            } else {
                row.setAttribute("SelectedRow", false);
            }
            i++;
        }
    }
    
    public void selectAllBdaBuyAlloc(ValueChangeEvent valueChangeEvent) {

        boolean isSelected = ((Boolean)valueChangeEvent.getNewValue()).booleanValue();
        DCBindingContainer dcb = (DCBindingContainer)evaluateEL("#{bindings}");
        DCIteratorBinding dciter = dcb.findIteratorBinding("BdaBuyAllocInvListIterator");

        ViewObject vo = dciter.getViewObject();
        int i = 0;
        Row row = null;
        vo.reset();
        while (vo.hasNext()) {
            if (i == 0)
                row = vo.first();
            else
                row = vo.next();

            if (isSelected) {
                row.setAttribute("SelectedRow", true);
            } else {
                row.setAttribute("SelectedRow", false);
            }
            i++;
        }
    }
    
    
    /*public synchronized BigDecimal selectedRowsColumnTotal(String iteratorName, String colName, String chkBoxAttr,
                                                           String chkBoxAttVal, boolean isProcessAllSelected) {
        DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        iter.setRangeSize((int)iter.getEstimatedRowCount());
        BigDecimal total = new BigDecimal(0);
        try {
            Row rows[] = iter.getAllRowsInRange();
            if (rows != null && rows.length > 0) {
                for (int i = 0; i < rows.length; i++) {
                    if(isProcessAllSelected){
                        if (rows[i].getAttribute(colName) != null)
                            total = total.add(new BigDecimal(rows[i].getAttribute(colName).toString()));
                    }else{
                        if (rows[i].getAttribute(chkBoxAttr) != null &&
                            rows[i].getAttribute(chkBoxAttr).toString().equalsIgnoreCase(chkBoxAttVal)) {
                            if (rows[i].getAttribute(colName) != null)
                                total = total.add(new BigDecimal(rows[i].getAttribute(colName).toString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }*/

    public boolean isNewSalesOrder() {
        return isNewStateRow("BdaOrdersVOIterator");
    }

    public boolean isNewBuyOrder() {
        return isNewStateRow("BdaBuyOrdersVOIterator");
    }
    
    public String buyInvSearchAction() {
        String codes= null;
        if (this.getInvestorCodesUI().getValue() != null) {
            codes = this.getInvestorCodesUI().getValue().toString();
        }
        manualSearchAction("BdaBuyOrderInvListIterator", "INVESTOR_CODE", searchPopupUI, codes);
        return null;
    }
    
    public String salesInvSearchAction() {
        String codes= null;
        if (this.getSalesInvestorCodesUI().getValue() != null) {
            codes = this.getSalesInvestorCodesUI().getValue().toString();
        }
        manualSearchAction("BdaSalesOrderInvListVOIterator", "INVESTOR_CODE", salesSearchPopupUI, codes);
        return null;
    }
    

    public String manualSearchAction(String pIterator, String pColumnName, RichPopup pPopupName, String pCodes) {
        String inString = getInString(pCodes);
        if(inString != null){
            try {
                DCIteratorBinding dcibBro = ADFUtils.findIterator(pIterator);
                dcibBro.getViewObject().setWhereClause(pColumnName + " in " + inString);
                dcibBro.getViewObject().executeQuery();
                dcibBro.getViewObject().setWhereClause(null);
                //RichPopup.PopupHints hints = new RichPopup.PopupHints();
                pPopupName.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
                pPopupName.hide();
                JSFUtils.addFacesErrorMessage("Please enter investor codes");
            }
        return null;
    }

    private String getInString(String pCodes) {
        try {
            if (pCodes != null) {
                StringBuffer str = new StringBuffer("(");
                int count = 0;
                String[] codeArr = pCodes.split("[,;]");
                for (String cd : codeArr) {
                    if (cd != null && !cd.equals("")) {
                        str.append("'" + cd + "'");
                        if (codeArr.length - 1 != count)
                            str.append(",");
                        count++;
                    }
                }
                str.append(")");
                return str.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public void setInvestorCodesUI(RichInputText investorCodesUI) {
        this.investorCodesUI = investorCodesUI;
    }

    public RichInputText getInvestorCodesUI() {
        return investorCodesUI;
    }

    public void setSearchPopupUI(RichPopup searchPopupUI) {
        this.searchPopupUI = searchPopupUI;
    }

    public RichPopup getSearchPopupUI() {
        return searchPopupUI;
    }

    public void setSalesInvestorCodesUI(RichInputText salesInvestorCodesUI) {
        this.salesInvestorCodesUI = salesInvestorCodesUI;
    }

    public RichInputText getSalesInvestorCodesUI() {
        return salesInvestorCodesUI;
    }

    public void setSalesSearchPopupUI(RichPopup salesSearchPopupUI) {
        this.salesSearchPopupUI = salesSearchPopupUI;
    }

    public RichPopup getSalesSearchPopupUI() {
        return salesSearchPopupUI;
    }

    public String refreshInvestorCodes() {
        oracle.adf.view.rich.util.ResetUtils.reset(salesInvestorCodesUI);
        return null;
    }

    public String refreshBuyInvestorCodes() {
        oracle.adf.view.rich.util.ResetUtils.reset(investorCodesUI);
        return null;
    }
}
