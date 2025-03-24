package leads.capita.trade.view.backing;

import java.sql.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.faces.application.FacesMessage;

import javax.faces.context.FacesContext;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.common.ui.BaseBean;

import leads.capita.trade.plsql.PPPlsqlExecutor;

import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputDate;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;

import oracle.adf.view.rich.component.rich.nav.RichCommandButton;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;
import oracle.jbo.server.AssociationDefBase;
import oracle.jbo.server.DBTransaction;


public class YearEndProcessor {
    private RichInputDate yearEndDateUI;
    private RichSelectOneChoice cobYearEndBranceName;
    private RichSelectOneChoice btnYearEnd_action;
    private RichCommandButton btnYearEnd_action1;
    private RichCommandButton btnReverseYE;
    private RichCommandButton btnReverseYE_action;
    private FacesContext fct;
    //private DateFormat formatter = new SimpleDateFormat("dd-MMM-yy");


    public YearEndProcessor() {
        fct = JSFUtils.getFacesContextApp();
    }


    public String btnYearEnd_action() {

        PPPlsqlExecutor sq = new PPPlsqlExecutor();
        String date = null;
        Integer branchID = 0;
        //java.sql.Timestamp dts = null;
        //date=this.getYearEndDateUI().getValue().toString();
        if (this.getYearEndDateUI().getValue() != null ||
            !(this.getYearEndDateUI().getValue().toString().equals(""))) {
            // System.out.println("1");
            // date= this.getYearEndDateUI().getValue().toString();
            Timestamp activateDateNewValue = Timestamp.valueOf((this.getYearEndDateUI()).getValue().toString());
            //System.out.println(" ------------------" + activateDateNewValue.getTime());
            date = new SimpleDateFormat("dd-MMM-yyyy").format(new Date(activateDateNewValue.getTime()));
            //System.out.println("year end " + date);
            //System.out.println("XXXX");

            //        Timestamp activateDateNewValue =
            //            Timestamp.valueOf((this.getYearEndDateUI()).getValue().toString());
            //            //System.out.println("2 ");
            //        String date = new SimpleDateFormat("dd-MMM-yyyy").format(new Date(activateDateNewValue.getTime()));

            //        if (this.getCobYearEndBranceName().getValue().equals("")) {
            //            //System.out.println("2 ");
            //        }

            if (this.getCobYearEndBranceName().getValue().equals("")) {

                branchID = -1;

            } else {
                //System.out.println("3  ");
                branchID = Integer.parseInt(this.getCobYearEndBranceName().getValue().toString());
                //System.out.println("4");
            }

            //System.out.println("year end " + date);
            //System.out.println("branches " + branchID);
            try {

                boolean isProcess = sq.processYearEnd(date, branchID);

                if (isProcess) {

                    fct.addMessage("SuccessMsg",
                                   new FacesMessage(FacesMessage.SEVERITY_INFO, "", "Year End Completed Success!!"));
                    //System.out.println("Success!!!!!!!!");
                    //ApplicationInfo.getCurrentUserDBTransaction().commit();
                } else {
                    //ApplicationInfo.getCurrentUserDBTransaction().rollback();
                    //System.out.println("Failed!!!!!!!!");
                    fct.addMessage("ErrorMsg",
                                   new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Year End Process Failed!"));
                }
            } catch (Exception e) {
                /*fct.addMessage("ErrorMsg",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage().substring(10,
                                                                                                          e.getMessage().indexOf("!"))));*/

                fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage()));
                
                // System.out.println("Failed!!!!!!!!");
                //ApplicationInfo.getCurrentUserDBTransaction().rollback();
                e.printStackTrace();
            }

        } else {

            // System.out.println("XXXX****");

            fct.addMessage("ErrorMsg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", " Date Can Not be Empty!"));

        }
        return null;
    }

    public String btnReverseYE_action() {

        PPPlsqlExecutor sq = new PPPlsqlExecutor();
        // DCIteratorBinding dcIter = ADFUtils.findIterator("YearEndRevokeIterator");
        String workingDate = null;
        int branchId = 0;
        Row row = ADFUtils.findIterator("YearEndRevokeIterator").getViewObject().getCurrentRow();

        if (row.getAttribute("TranDate") != null) {
            Timestamp date = Timestamp.valueOf(row.getAttribute("TranDate").toString());

            workingDate = new SimpleDateFormat("dd-MMM-yyyy").format(new Date(date.getTime()));

        }
        if (row.getAttribute("BranchId") != null) {
            branchId = Integer.parseInt(row.getAttribute("BranchId").toString());
        }

        try {
            boolean isProcess = sq.processYearEndReverse(workingDate, branchId);
            if (isProcess) {

                fct.addMessage("SuccessMsg",
                               new FacesMessage(FacesMessage.SEVERITY_INFO, "", "Reverse Year End Completed Successfully !!"));
                //System.out.println("Success!!!!!!!!");
                //ApplicationInfo.getCurrentUserDBTransaction().commit();
            } else {
                fct.addMessage("ErrorMsg",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Reverse Year End Process failed!"));
                //ApplicationInfo.getCurrentUserDBTransaction().rollback();
                // System.out.println("Failed!!!!!!!!");
            }
        } catch (Exception e) {

            //System.out.println("Failed!!!!!!!!");

            fct.addMessage("ErrorMsg",
                           new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Reverse Year End Process failed!"));

            //ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
        }

        // Add event code here...
        return null;
    }

    public void setYearEndDateUI(RichInputDate yearEndDateUI) {
        this.yearEndDateUI = yearEndDateUI;
    }

    public RichInputDate getYearEndDateUI() {
        return yearEndDateUI;
    }

    public void setCobYearEndBranceName(RichSelectOneChoice cobYearEndBranceName) {
        this.cobYearEndBranceName = cobYearEndBranceName;
    }

    public RichSelectOneChoice getCobYearEndBranceName() {
        return cobYearEndBranceName;
    }

    public void setBtnYearEnd_action(RichSelectOneChoice btnYearEnd_action) {
        this.btnYearEnd_action = btnYearEnd_action;
    }

    public RichSelectOneChoice getBtnYearEnd_action() {
        return btnYearEnd_action;
    }

    public void setBtnYearEnd_action1(RichCommandButton btnYearEnd_action1) {
        this.btnYearEnd_action1 = btnYearEnd_action1;
    }

    public RichCommandButton getBtnYearEnd_action1() {
        return btnYearEnd_action1;
    }

    public void setBtnReverseYE(RichCommandButton btnReverseYE) {
        this.btnReverseYE = btnReverseYE;
    }

    public RichCommandButton getBtnReverseYE() {
        return btnReverseYE;
    }

    public void setBtnReverseYE_action(RichCommandButton btnReverseYE_action) {
        this.btnReverseYE_action = btnReverseYE_action;
    }

    public RichCommandButton getBtnReverseYE_action() {
        return btnReverseYE_action;
    }

    public String btnRefresh() {
        BaseBean bb = new BaseBean();
        bb.refreshIterator("YearEndRevokeIterator");
        return null;
    }
}
