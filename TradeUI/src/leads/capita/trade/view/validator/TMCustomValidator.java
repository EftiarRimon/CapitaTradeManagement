package leads.capita.trade.view.validator;

import java.math.BigDecimal;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;

import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.view.rich.component.rich.input.RichInputText;

public class TMCustomValidator implements Validator {
    FacesContext fctx = FacesContext.getCurrentInstance();

    public TMCustomValidator() {
        super();
    }

    /**---
     * @param facesContext
     * @param uIComponent
     * @param object
     * @throws ValidatorException
     */
    @Override
    public void validate(FacesContext facesContext, UIComponent uiComponent, Object object) {
    }

    public void bdaBuyAllocEditValidation(FacesContext facesContext, UIComponent uIComponent, Object object,
                                          String accountId, Double tradePrice, Double oredrQty) {
        if (object != null) {

            Double _totalPrice = new Double(0);
            Double allocQty = null;
            allocQty = Double.valueOf(object.toString());
            if (allocQty.compareTo(oredrQty) > 0) {
                FacesMessage message =
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, " ", "Allocation Qty. can not be greater than Order Qty. !");
                fctx.addMessage(uIComponent.getClientId(fctx), message);
                ((RichInputText)uIComponent).setValid(false);
            } else if (tradePrice <= 0) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, " ", "Trade Price Not Found !");
                fctx.addMessage(uIComponent.getClientId(fctx), message);
                ((RichInputText)uIComponent).setValid(false);
            } else {
                BigDecimal _purchasePower =
                    new TMPlsqlExecutor()._getInvestorPurchasePower("pkg_investor.get_purchase_power(?)", accountId);
                _totalPrice = (allocQty == null ? 0 : allocQty) * (tradePrice == null ? 0 : tradePrice);
                if (_totalPrice.compareTo(_purchasePower.doubleValue()) <= 0) {
                    ((RichInputText)uIComponent).setValid(true);
                } else {
                    String msg = null;
                    msg = " Not have enough purchase power  (" + _purchasePower + ") Of  This Investor !";
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, " ", msg);
                    fctx.addMessage(uIComponent.getClientId(fctx), message);
                    ((RichInputText)uIComponent).setValid(false);
                }
            }

        }
    }

    public void bdaSaleAllocEditValidation(FacesContext facesContext, UIComponent uIComponent, Object object,
                                           String accountId, Double tradePrice, Double oredrQty, String instrumentId) {
        if (object != null) {

            Double _totalPrice = new Double(0);
            Double allocQty = null;
            allocQty = Double.valueOf(object.toString());
            if (allocQty.compareTo(oredrQty) > 0) {
                FacesMessage message =
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, " ", "Allocation Qty. can't be greater than Order Qty. !");
                fctx.addMessage(uIComponent.getClientId(fctx), message);
                ((RichInputText)uIComponent).setValid(false);
            } else if (tradePrice <= 0) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, " ", "Trade Price Not Found !");
                fctx.addMessage(uIComponent.getClientId(fctx), message);
                ((RichInputText)uIComponent).setValid(false);
            } else {
                Object[] params =
                { "INSTRUMENT_BALANCES", "SALABLE_QTY", "INSTRUMENT_ID", instrumentId, "ACCOUNT_ID", accountId }; //Sp Params
                BigDecimal salableQty =
                    new TMPlsqlExecutor().getTMFunctionCallReturn("pkg_capita_utl.get_table_column_value(?,?,?,?,?,?)",
                                                                  params);
                if (allocQty.compareTo(salableQty.doubleValue()) <= 0) {
                    ((RichInputText)uIComponent).setValid(true);
                } else {
                    String msg = null;
                    msg =" Allocation Qty. can't be greater than  salable Qty.  (" + salableQty + ") Of  This Investor !";
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, " ", msg);
                    fctx.addMessage(uIComponent.getClientId(fctx), message);
                    ((RichInputText)uIComponent).setValid(false);
                }
            }

        }
    }
}
