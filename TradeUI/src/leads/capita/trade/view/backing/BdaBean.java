package leads.capita.trade.view.backing;

import java.math.BigDecimal;

import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.ResourceBundle;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.model.am.BdaServiceImpl;
import leads.capita.trade.model.view.BdaPurchaseOrderVO_Not_UsedImpl;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputText;

import oracle.binding.BindingContainer;

import oracle.jbo.JboException;
import oracle.jbo.Row;
import oracle.jbo.ViewCriteria;
import oracle.jbo.ViewCriteriaRow;
import oracle.jbo.ViewObject;


public class BdaBean {
    private static final String INVESTOR_LIST_FOR_BDA_SALES_ITERATOR = "InvestorListForBDASalesVOIterator";
    private static final String INVESTOR_LIST_FOR_BDA_PURCHASE_ITERATOR = "InvestorListForBDAPurchaseVOIterator";
    private static final String BDA_SALES_CRITERIA_VO_ITERATOR = "BDaSalesCriteriaVOIterator";
    private static final String BDA_ALLOCATION_VO_ITERATOR = "InvListForBdaAllocationVOIterator";
    private static final String BDA_TRANSIANT_VO_ITERATOR = "TransiantVOIterator";
    private static final String BDA_SEARCH_PURCHASE_ORD_VO_ITERATOR = "BDASearchPurchaseOrderVOIterator";
    private static final String BDA_INV_LIST_FOR_SALES_ALLOCATION_VO_ITERATOR =
        "InvestorListForBDASalesAllocationIterator";
    private FacesContext fct;
    private ResourceBundle messageBundle;
    private RichInputText priceUI;

    public BdaBean() {
        fct = JSFUtils.getFacesContext();
        //messageBundle = JSFUtils.getResourceBundle("leads.capita.im.view.InstrumentSetUpUIBundle");
    }

    //BDA Sales Order..

    public String _bdaProcessSalesOrder() {
        DCIteratorBinding bdaSalesDefaultIterBinding = ADFUtils.findIterator(BDA_SALES_CRITERIA_VO_ITERATOR);
        DCIteratorBinding bdaSalesInvListIterBinding = ADFUtils.findIterator(INVESTOR_LIST_FOR_BDA_SALES_ITERATOR);
        Row r[] = bdaSalesInvListIterBinding.getAllRowsInRange();
        int counter = 0;

        Row invsetRow = null;
        int invalidCounter = 0;
        Row dbRow = null;
        try {
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    invsetRow = r[i];
                    if (invsetRow.getAttribute("SelectedRow") != null &&
                        invsetRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        if (!isValidForSales(invsetRow)) {
                            invalidCounter++;

                            break;
                        } else {
                            Row newRow = null;
                            Integer accId = Integer.valueOf(invsetRow.getAttribute("AccountId").toString());
                            // System.out.print("------"+accId);
                            Integer intsId = Integer.valueOf(invsetRow.getAttribute("InstrumentId").toString());
                            dbRow =
                                    _getDatabaseRowForSales(BDA_SALES_CRITERIA_VO_ITERATOR, accId, intsId, getSysDate());
                            if (dbRow == null) {
                                ViewObject vo = bdaSalesDefaultIterBinding.getViewObject();
                                createRecord(); //to create new empty row..
                                newRow = bdaSalesDefaultIterBinding.getCurrentRow();
                                newRow.setAttribute("AccountId", accId);
                                newRow.setAttribute("InstrumentId", intsId);
                                newRow.setAttribute("Status", "ORDER");
                                newRow.setAttribute("OrderDate", getSysDate());
                                newRow.getAttribute("AccountId");
                                newRow.setAttribute("OrderQty", invsetRow.getAttribute("Quantity"));
                                newRow.setAttribute("TradeCode", invsetRow.getAttribute("TradeCode"));
                                counter++;
                            } else {
                                Integer orderQty = Integer.valueOf(invsetRow.getAttribute("Quantity").toString());
                                Integer orderedQty = Integer.valueOf(dbRow.getAttribute("OrderQty").toString());
                                dbRow.setAttribute("OrderQty", (orderQty + orderedQty));
                                counter++;
                            }
                        }
                    }
                }
                if (counter > 0 && invalidCounter == 0) {
                    _save_action();
                    ADFUtils.findIterator(INVESTOR_LIST_FOR_BDA_SALES_ITERATOR).executeQuery();
                    JSFUtils.addFacesInformationMessage(counter + "  rows have been processed successfully..");
                } else {
                    ADFUtils.findIterator(INVESTOR_LIST_FOR_BDA_SALES_ITERATOR).getViewObject().reset();
                    JSFUtils.addFacesInformationMessage("No row created !");
                }
            }
        } catch (NumberFormatException nFE) {
            System.out.println("Number Format Problem From BDA Sales process " + nFE.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public BigDecimal getSelectedRowsTotalSalesInstruments() {
        return selectedRowsColumnTotal(INVESTOR_LIST_FOR_BDA_SALES_ITERATOR, "Quantity", "SelectedRow", "true");
    }

    public BigDecimal getSelectedRowsTotalPurchaseInstruments() {
        return selectedRowsColumnTotal(INVESTOR_LIST_FOR_BDA_PURCHASE_ITERATOR, "Quantity", "SelectedRow", "true");
    }


    public synchronized BigDecimal selectedRowsColumnTotal(String iteratorName, String colName, String chkBoxAttr,
                                                           String chkBoxAttVal) {
        DCIteratorBinding iter = ADFUtils.findIterator(iteratorName);
        Row rows[] = iter.getAllRowsInRange();
        BigDecimal total = new BigDecimal(0);
        if (rows != null && rows.length > 0) {
            for (int i = 0; i < rows.length; i++) {
                if (rows[i].getAttribute(chkBoxAttr) != null &&
                    rows[i].getAttribute(chkBoxAttr).toString().equalsIgnoreCase(chkBoxAttVal)) {
                    if (rows[i].getAttribute(colName) != null)
                        total = total.add(new BigDecimal(rows[i].getAttribute(colName).toString()));
                }
            }
        }
        return total;
    }


    public boolean checkForInteger(String toChkVal) {
        boolean isValid = false;
        try {
            Integer chkval = Integer.valueOf(toChkVal);
            isValid = true;
        } catch (NumberFormatException nFE) {
            System.out.println("Integer Format Problem From  " + nFE.getMessage());
        }
        return isValid;
    }

    public boolean checkForDouble(String toChkVal) {
        boolean isValid = false;
        try {
            Double chkval = Double.valueOf(toChkVal);
            isValid = true;
        } catch (NumberFormatException nFE) {
            System.out.println("Double Format Problem From  " + nFE.getMessage());
        }
        return isValid;
    }
    //BDA Purchase Order..

    public String _processPurchaseOrder() {
        DCIteratorBinding bdaSalesDefaultIterBinding = ADFUtils.findIterator(BDA_SEARCH_PURCHASE_ORD_VO_ITERATOR);
        DCIteratorBinding bdaSalesInvListIterBinding = ADFUtils.findIterator(INVESTOR_LIST_FOR_BDA_PURCHASE_ITERATOR);
        Row r[] = bdaSalesInvListIterBinding.getAllRowsInRange();
        Row investorRow = null;
        int invalidCounter = 0;
        int newCreatedRows = 0;
        Row dbRow = null;
        try {
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    investorRow = r[i];
                    if (investorRow.getAttribute("SelectedRow") != null &&
                        investorRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        if (!isValidForPurchase(investorRow)) {
                            invalidCounter++;
                            break;
                        } else {
                            ViewObject vo = bdaSalesDefaultIterBinding.getViewObject();
                            Double price = null;
                            Integer instrumentId = null;
                            instrumentId =
                                    Integer.valueOf(_getCurrentRow(BDA_TRANSIANT_VO_ITERATOR).getAttribute("InstrumentId").toString());
                            Integer accId = Integer.valueOf(investorRow.getAttribute("AccountId").toString());
                            dbRow =
                                    _getdatabaseRow(BDA_SEARCH_PURCHASE_ORD_VO_ITERATOR, accId, instrumentId, getSysDate());
                            price =
                                    Double.valueOf(_getCurrentRow(BDA_TRANSIANT_VO_ITERATOR).getAttribute("Price").toString());

                            if (dbRow == null) {
                                createRecord(); //to create new empty row..
                                Row newRow = bdaSalesDefaultIterBinding.getCurrentRow();
                                newRow.setAttribute("AccountId", investorRow.getAttribute("AccountId").toString());
                                newRow.setAttribute("InstrumentId", instrumentId);
                                newRow.setAttribute("Status", "ORDER");
                                newRow.setAttribute("TradeCode", investorRow.getAttribute("TradeCode").toString());
                                newRow.setAttribute("OrderDate", getSysDate());
                                newRow.setAttribute("OrderQty", investorRow.getAttribute("Quantity"));
                                newRow.setAttribute("Price", price);
                                newCreatedRows++; //counter
                            } else {
                                Integer purchaseQty = Integer.valueOf(investorRow.getAttribute("Quantity").toString());
                                Integer orederedQty =
                                    Integer.valueOf(investorRow.getAttribute("Orderedqty").toString());
                                dbRow.setAttribute("OrderQty", (purchaseQty) + (orederedQty));
                                dbRow.setAttribute("Price", price);
                                newCreatedRows++; //counter
                            }
                        }
                    }
                }
                if (newCreatedRows > 0 && invalidCounter == 0) {
                    _save_action();
                    ADFUtils.findIterator(INVESTOR_LIST_FOR_BDA_PURCHASE_ITERATOR).executeQuery();
                    ADFUtils.findIterator(BDA_TRANSIANT_VO_ITERATOR).executeQuery();
                    JSFUtils.addFacesInformationMessage(newCreatedRows + " row/s have been processed successfully..");
                } else {
                    JSFUtils.addFacesInformationMessage("No row created !");
                }
            }
        } catch (NumberFormatException nFE) {
            JSFUtils.addFacesErrorMessage(nFE.getMessage());
        } catch (NullPointerException nE) {
            JSFUtils.addFacesErrorMessage(nE.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //BDA SALES..

    public void selectAllValueChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        Boolean selectRow = false;
        selectRow = (Boolean)valueChangeEvent.getNewValue();
        this.selectAllAction(INVESTOR_LIST_FOR_BDA_SALES_ITERATOR, "SelectedRow", selectRow);
    }

    //BDA PURCHASE...

    public void bdaPurchaseSelectAllValueChange(ValueChangeEvent valueChangeEvent) {
        valueChangeEvent.getComponent().processUpdates(fct);
        Boolean selectRow = false;
        selectRow = (Boolean)valueChangeEvent.getNewValue();
        this.selectAllAction(INVESTOR_LIST_FOR_BDA_PURCHASE_ITERATOR, "SelectedRow", selectRow);
        //AdfFacesContext.getCurrentInstance().addPartialTarget(JSFUtils.findComponentInRoot("t1"));
        // fct.renderResponse();
    }

    public void selectAllAction(String iteratorName, String selectedRowAttr, boolean select) {
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

    //BDA Sales validation....

    private boolean isValidForSales(Row investorRow) {
        boolean isValid = false;
        Double salableQty = null;
        try {
            if (investorRow.getAttribute("Salable") != null)
                salableQty =
                        Double.valueOf(investorRow.getAttribute("Salable").toString()); //Remaining for bda sales..
            if (investorRow.getAttribute("Quantity") == null ||
                Integer.valueOf(investorRow.getAttribute("Quantity").toString()) < 1) {
                JSFUtils.addFacesErrorMessage(" Quantity required ! and should be at least 1..!");
                isValid = false;
            } else {
                if (Integer.valueOf(investorRow.getAttribute("Quantity").toString()) > salableQty) {
                    isValid = false;
                    JSFUtils.addFacesErrorMessage("Maximum quantity exceeded! for investor code " +
                                                  investorRow.getAttribute("InvestorCode"));
                } else {
                    isValid = true;
                }

            }
        } catch (NumberFormatException nFE) {
            System.out.println("Number Format Problem From BDA SALES " + nFE.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isValid;
    }
    //BDA Purchase validation....

    private boolean isValidForPurchase(Row investorRow) {
        boolean isValid = false;
        Double price = null;
        try {
            Object instrumentId = null;
            instrumentId = _getCurrentRow(BDA_TRANSIANT_VO_ITERATOR).getAttribute("InstrumentId");
            if (priceUI.getValue() != null)
                price = Double.valueOf(priceUI.getValue().toString());
            //System.out.print("=====" + price);
            if (investorRow.getAttribute("Quantity") == null ||
                Integer.valueOf(investorRow.getAttribute("Quantity").toString()) < 1) {
                isValid = false;
                JSFUtils.addFacesErrorMessage(" Quantity required ! and should be at least 1.. ");

            } else if (instrumentId == null) {
                isValid = false;
                JSFUtils.addFacesErrorMessage(" Instrument required ! ");

            } else if (price == null || price < 1) {
                isValid = false;
                JSFUtils.addFacesErrorMessage(" Price required ! ");

            } else {
                //Validation
                Integer qty = null;
                Integer orderedQty = null;
                Double _purchasePower = null;
                Double remainingpurchasepower = null;
                if (investorRow.getAttribute("Purchasepower") != null)
                    _purchasePower = Double.valueOf(investorRow.getAttribute("Purchasepower").toString());
                if (investorRow.getAttribute("RemainingPurchasePower") != null)
                    remainingpurchasepower =
                            Double.valueOf(investorRow.getAttribute("RemainingPurchasePower").toString());
                qty = Integer.valueOf(investorRow.getAttribute("Quantity").toString());
                // if (investorRow.getAttribute("Orederedqty") != null)
                //  orderedQty = Integer.valueOf(investorRow.getAttribute("Orederedqty").toString());
                Double _totalPrice = null;
                Integer _totalQty = (qty == null ? new Integer(0) : qty);
                _totalPrice = (_totalQty * (price == null ? new Double(0) : price));
                if (_totalPrice.compareTo(remainingpurchasepower) > 0) {
                    isValid = false;
                    JSFUtils.addFacesErrorMessage(" Maximum Purchase power Exceeded ! for investor code " +
                                                  investorRow.getAttribute("InvestorCode"));
                } else
                    isValid = true;
            }
        } catch (NumberFormatException nFE) {
            System.out.println("Number Format Problem From BDA Purchase " + nFE.getMessage());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isValid;
    }

    public BindingContainer getBindings() {
        return BindingContext.getCurrent().getCurrentBindingsEntry();
    }

    public Row _getCurrentRow(String iteratorname) {
        DCIteratorBinding it = ADFUtils.findIterator(iteratorname);
        Row curRow = null;
        if (it != null) {
            curRow = it.getCurrentRow();
        }
        return curRow;
    }


    private BdaServiceImpl getBdaServiceAm() {
        FacesContext fc = FacesContext.getCurrentInstance();
        Application app = fc.getApplication();
        ExpressionFactory elFactory = app.getExpressionFactory();
        ELContext elContext = fc.getELContext();
        ValueExpression valueExp =
            elFactory.createValueExpression(elContext, "#{data.BdaServiceDataControl.dataProvider}", Object.class);
        return (BdaServiceImpl)valueExp.getValue(elContext);
    }


    //BDA Purchase Order __Used to get rows for a particular instrument for a particular day---(Update)

    public synchronized Row _getdatabaseRow(String iter, Integer accountId, Integer instrumentId, Date date) {
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


    //BDA Sales Order __Used to get rows for a particular instrument for a particular day---(Update)

    public synchronized Row _getDatabaseRowForSales(String iter, Integer accountId, Integer instrumentId, Date date) {
        DCIteratorBinding dcIter = ADFUtils.findIterator(iter);
        //System.out.print("<--__--> \n ");
        ViewObject VO = dcIter.getViewObject();
        String d = new SimpleDateFormat("dd-MMM-yyyy").format(date);
        StringBuffer whereClauseSql = null;
        whereClauseSql = new StringBuffer();
        whereClauseSql.append("BdaSalesOrder.ACCOUNT_ID='" + accountId + "'");
        whereClauseSql.append(" AND BdaSalesOrder.INSTRUMENT_ID='" + instrumentId + "'");
        whereClauseSql.append(" AND to_char(BdaSalesOrder.ORDER_DATE,'dd-MON-yyyy') = upper('" + d + "')");
        VO.setWhereClause(whereClauseSql.toString());
        //System.out.print("<--__--> \n " + VO.getQuery());
        VO.executeQuery();
        Row curRow = null;
        if (VO.hasNext()) {
            curRow = VO.next();

        }
        return curRow;
    }

    // get row from public offer details table ----<N><O><T>

    public synchronized Row genericViewCriteriaRow(Integer accountId, Integer instrumentId, String date) {
        BdaServiceImpl am = null;
        Row filteredRow = null;
        String d = new SimpleDateFormat("dd-MMM-yyyy").format(date);
        BdaPurchaseOrderVO_Not_UsedImpl bdaPurchaseOrderVO;
        am = getBdaServiceAm();
        ViewCriteria vc = null;
        bdaPurchaseOrderVO = null;
        bdaPurchaseOrderVO.setWhereClause(null);
        vc = bdaPurchaseOrderVO.createViewCriteria();
        ViewCriteriaRow vCrInstrumentId = vc.createViewCriteriaRow();
        vCrInstrumentId.setAttribute("InstrumentId", instrumentId);
        vc.add(vCrInstrumentId);
        ViewCriteriaRow vCrAccountId = vc.createViewCriteriaRow();
        vCrAccountId.setAttribute("AccountId", accountId);
        vCrAccountId.setConjunction(vCrAccountId.VC_CONJ_AND);
        vc.add(vCrAccountId);

        ViewCriteriaRow vCrOrderDate = vc.createViewCriteriaRow();
        vCrOrderDate.setAttribute("to_char(order_date,'dd-MON-yyyy')", "upper(" + date + ")");
        vCrOrderDate.setConjunction(vCrAccountId.VC_CONJ_AND);
        vc.add(vCrOrderDate);

        bdaPurchaseOrderVO.applyViewCriteria(vc);
        vc.setCriteriaMode(ViewCriteria.CRITERIA_MODE_QUERY);
        bdaPurchaseOrderVO.setQueryMode(ViewObject.QUERY_MODE_SCAN_DATABASE_TABLES);
        bdaPurchaseOrderVO.executeQuery();
        //vc.resetCriteria();
        if (bdaPurchaseOrderVO.hasNext()) {
            filteredRow = bdaPurchaseOrderVO.next();
        }
        vc.resetCriteria();
        return filteredRow;
    }

    public String newInsert() {
        this.createRecord();
        //ADFUtils.findIterator("BdaPurchaseOrderVOIterator").prepareForInput();
        return null;
    }

    public String _save_action() {
        try {
            if (JSFUtils.getBindings().getOperationBinding("Commit") != null)
                JSFUtils.getBindings().getOperationBinding("Commit").execute();
            else
                JSFUtils.addFacesErrorMessage("Commit binding problem!");
            return null;
        } catch (JboException ex) {
            throw new JboException(ex);
        }
    }

    public java.lang.String customRollBack(ActionEvent evt) {
        if (JSFUtils.getBindings().getOperationBinding("Rollback") != null)
            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
        else
            JSFUtils.addFacesErrorMessage("Rollback binding problem!");
        return null;
    }

    public java.lang.String createRecord() {
        if (JSFUtils.getBindings().getOperationBinding("CreateInsert") != null)
            JSFUtils.getBindings().getOperationBinding("CreateInsert").execute();
        else
            JSFUtils.addFacesErrorMessage("CreateInsert binding problem!");
        return null;
    }

    public void setPriceUI(RichInputText priceUI) {
        this.priceUI = priceUI;
    }

    public RichInputText getPriceUI() {
        return priceUI;
    }

    //Purchase Allocation...............

    public String _bdaAllocationProcess() {
        DCIteratorBinding bdaPurchaseDefaultIterBinding = ADFUtils.findIterator(BDA_SEARCH_PURCHASE_ORD_VO_ITERATOR);
        DCIteratorBinding bdaAllocationInvListIterBinding = ADFUtils.findIterator(BDA_ALLOCATION_VO_ITERATOR);
        Row r[] = bdaAllocationInvListIterBinding.getAllRowsInRange();
        Row investorRow = null;
        int invalidCounter = 0;
        int newCreatedRows = 0;
        Row dbRow = null;
        try {
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    investorRow = r[i];
                    if (investorRow.getAttribute("SelectedRow") != null &&
                        investorRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        if (!isValidForAllocation(investorRow)) {
                            invalidCounter++;
                            break;
                        } else {
                            ViewObject vo = bdaPurchaseDefaultIterBinding.getViewObject();
                            Integer accId = Integer.valueOf(investorRow.getAttribute("AccountId").toString());
                            Integer intsId = Integer.valueOf(investorRow.getAttribute("InstrumentId").toString());
                            dbRow = _getdatabaseRow(BDA_SEARCH_PURCHASE_ORD_VO_ITERATOR, accId, intsId, getSysDate());
                            if (dbRow == null) {
                                String x = "nothing happen"; //counter
                            } else {
                                Integer appliedQty =
                                    Integer.valueOf(investorRow.getAttribute("AppliedQty").toString());
                                Integer OrderQty = Integer.valueOf(dbRow.getAttribute("OrderQty").toString());
                                dbRow.setAttribute("AllocationQty", appliedQty);
                                dbRow.setAttribute("AllocationDate", getSysDate());
                                dbRow.setAttribute("Price", investorRow.getAttribute("TradePrice"));
                                dbRow.setAttribute("TradeCode", investorRow.getAttribute("TradeCode"));
                                //dbRow.setAttribute("AllocationComplete", dbRow.getAttribute("AllocationComplete"));
                                dbRow.setAttribute("Status", "ALLOCATED");
                                //dbRow.setAttribute("Price", 0);
                                newCreatedRows++; //counter
                            }
                        }
                    }
                }
                if (newCreatedRows > 0 && invalidCounter == 0) {
                    _save_action();
                    ADFUtils.findIterator(BDA_ALLOCATION_VO_ITERATOR).executeQuery();
                    JSFUtils.addFacesInformationMessage(newCreatedRows + " row/s have been processed successfully..");
                } else {
                    JSFUtils.addFacesInformationMessage("No row created !");
                }
            }
        } catch (NumberFormatException nFE) {
            System.out.println("Number Format Problem From BDA Purchase process " + nFE.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //BAD -- Allocation validator.... purchase

    private boolean isValidForAllocation(Row investorRow) {
        Integer appliedQty = null;
        Integer orderQty = null;
        Double _orderPrice = null;
        Double _purchasePower = null;
        Double _totalPrice = null;
        Integer tradeQty = null; //TradeQty
        boolean isValid = false;
        if (investorRow.getAttribute("AppliedQty") != null)
            appliedQty = Integer.valueOf(investorRow.getAttribute("AppliedQty").toString());
        if (investorRow.getAttribute("OrderQty") != null)
            orderQty = Integer.valueOf(investorRow.getAttribute("OrderQty").toString());
        if (investorRow.getAttribute("TradeQty") != null)
            orderQty = Integer.valueOf(investorRow.getAttribute("TradeQty").toString());
        if (investorRow.getAttribute("Price") != null)
            _orderPrice = Double.valueOf(investorRow.getAttribute("Price").toString());
        //        if (investorRow.getAttribute("PurchasePower") != null)
        //            _purchasePower = Double.valueOf(investorRow.getAttribute("PurchasePower").toString());
        //        _totalPrice =
        //                (appliedQty == null ? new Integer(0) : appliedQty) * (_orderPrice == null ? new Double(0) : _orderPrice);

        if (investorRow.getAttribute("AppliedQty") == null) {
            JSFUtils.addFacesErrorMessage("Applied quantity should be at least 1 ! ");
        } else if (appliedQty.compareTo(orderQty) > 0) {
            JSFUtils.addFacesErrorMessage("Applied quantity should be less than Order qty.! ");
        } else {
            isValid = true;
        }
        return isValid;
    }

    public String bdaDistribution() throws SQLException, ParseException {
        DCIteratorBinding bdaAllocationInvListIterBinding = ADFUtils.findIterator(BDA_ALLOCATION_VO_ITERATOR);
        Row r[] = bdaAllocationInvListIterBinding.getAllRowsInRange();
        Row investorRow = null;
        // Map
        if (r.length > 0 && r != null) {
            for (int i = 0; i < r.length; i++) {
                investorRow = r[i];
                new TMPlsqlExecutor().bdaInstrumentDistribution(investorRow.getAttribute("ShortName").toString(),
                                                                investorRow.getAttribute("TradeCode").toString(),
                                                                new SimpleDateFormat("dd-MMM-yyyy").format(getSysDate()));
            }
        }
        return null;
    }


    //Sales Allocation...........

    public String _bdaSalesAllocationProcess() {
        DCIteratorBinding bdaPurchaseDefaultIterBinding = ADFUtils.findIterator("BdaSalesOrderEntitySearchVOIterator");
        DCIteratorBinding bdaAllocationInvListIterBinding =
            ADFUtils.findIterator(BDA_INV_LIST_FOR_SALES_ALLOCATION_VO_ITERATOR);
        Row r[] = bdaAllocationInvListIterBinding.getAllRowsInRange();
        Row investorRow = null;
        int invalidCounter = 0;
        int newCreatedRows = 0;
        Row dbRow = null;
        try {
            if (r.length > 0 && r != null) {
                for (int i = 0; i < r.length; i++) {
                    investorRow = r[i];
                    if (investorRow.getAttribute("SelectedRow") != null &&
                        investorRow.getAttribute("SelectedRow").toString().equalsIgnoreCase("true")) {
                        if (!isValidForBdaSalesAllocation(investorRow)) {
                            invalidCounter++;
                            break;
                        } else {
                            ViewObject vo = bdaPurchaseDefaultIterBinding.getViewObject();
                            Integer accId = Integer.valueOf(investorRow.getAttribute("AccountId").toString());
                            Integer intsId = Integer.valueOf(investorRow.getAttribute("InstrumentId").toString());
                            dbRow =
                                    _getDatabaseRowForSales("BdaSalesOrderEntitySearchVOIterator", accId, intsId, getSysDate());
                            if (dbRow == null) {
                                String x = "nothing happen"; //counter
                            } else {
                                Integer appliedQty =
                                    Integer.valueOf(investorRow.getAttribute("AppliedQty").toString());
                                Integer OrderQty = Integer.valueOf(dbRow.getAttribute("OrderQty").toString());
                                dbRow.setAttribute("AllocationQty", appliedQty);
                                dbRow.setAttribute("AllocationDate", getSysDate());
                                dbRow.setAttribute("TradeCode", dbRow.getAttribute("TradeCode"));
                                //dbRow.setAttribute("AllocationComplete", dbRow.getAttribute("AllocationComplete"));
                                dbRow.setAttribute("Status", "ALLOCATED");
                                dbRow.setAttribute("Price", investorRow.getAttribute("Tradeprice"));
                                newCreatedRows++; //counter
                            }
                        }
                    }
                }
                if (newCreatedRows > 0 && invalidCounter == 0) {
                    _save_action();
                    ADFUtils.findIterator(BDA_INV_LIST_FOR_SALES_ALLOCATION_VO_ITERATOR).executeQuery();
                    JSFUtils.addFacesInformationMessage(newCreatedRows + " row/s have been processed successfully..");
                } else {
                    JSFUtils.addFacesInformationMessage("No row created !");
                }
            }
        } catch (NumberFormatException nFE) {
            System.out.println("Number Format Problem From BDA Purchase process " + nFE.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
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

    private boolean isValidForBdaSalesAllocation(Row investorRow) {
        Integer appliedQty = null;
        Integer orderQty = null;
        Double _orderPrice = null;
        Double _purchasePower = null;
        Double _totalPrice = null;
        boolean isValid = false;
        if (investorRow.getAttribute("AppliedQty") != null)
            appliedQty = Integer.valueOf(investorRow.getAttribute("AppliedQty").toString());
        if (investorRow.getAttribute("OrderQty") != null)
            orderQty = Integer.valueOf(investorRow.getAttribute("OrderQty").toString());
        // if (investorRow.getAttribute("Price") != null)
        // _orderPrice = Double.valueOf(investorRow.getAttribute("Price").toString());
        //        if (investorRow.getAttribute("PurchasePower") != null)
        //            _purchasePower = Double.valueOf(investorRow.getAttribute("PurchasePower").toString());
        //        _totalPrice =
        //                (appliedQty == null ? new Integer(0) : appliedQty) * (_orderPrice == null ? new Double(0) : _orderPrice);

        if (investorRow.getAttribute("AppliedQty") == null) {
            JSFUtils.addFacesErrorMessage("Applied quantity should be at least 1 ! ");
        } else if (appliedQty.compareTo(orderQty) > 0) {
            JSFUtils.addFacesErrorMessage("Applied quantity should be more than Order qty.! ");
        } else {
            isValid = true;
        }
        return isValid;
    }


    public BigDecimal getTotalSelectedFieldValue(String iter, String field, String selectedAttr) {
        DCIteratorBinding itera = ADFUtils.findIterator(iter);
        Row r[] = itera.getAllRowsInRange();
        BigDecimal totalAmount = new BigDecimal(0);
        if (r != null && r.length > 0) {
            for (int i = 0; i < r.length; i++) {
                if (r[i].getAttribute(selectedAttr) != null &&
                    r[i].getAttribute(selectedAttr).toString().equalsIgnoreCase("true")) {
                    if (r[i].getAttribute(field) != null)
                        totalAmount = totalAmount.add(new BigDecimal(r[i].getAttribute(field).toString()));
                }
            }
        }
        // System.out.print("---------"+totalAmount);
        return totalAmount;
    }


    public BigDecimal getTotalFieldValue(String iter, String field) {
        DCIteratorBinding itera = ADFUtils.findIterator(iter);
        Row r[] = itera.getAllRowsInRange();
        BigDecimal totalAmount = new BigDecimal(0);
        if (r != null && r.length > 0) {
            for (int i = 0; i < r.length; i++) {
                if (r[i].getAttribute(field) != null)
                    totalAmount = totalAmount.add(new BigDecimal(r[i].getAttribute(field).toString()));
            }
        }
        // System.out.print("---------"+totalAmount);
        return totalAmount;
    }
    //purchase -----

    public BigDecimal getTotalAllocatedQty() {
        return getTotalFieldValue("BdaPurchaseOrderVOIterator", "AllocationQty");
    }

    //Sales----

    public BigDecimal getTotalAllocatedsalesQty() {
        return getTotalFieldValue("BdaSalesOrderVOIterator", "AllocationQty");
    }

}
