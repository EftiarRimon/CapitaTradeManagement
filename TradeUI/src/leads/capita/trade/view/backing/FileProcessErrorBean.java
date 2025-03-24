package leads.capita.trade.view.backing;

import java.io.Console;

import javax.faces.application.FacesMessage;

import javax.faces.context.FacesContext;

import leads.capita.common.am.CapitaDBServiceImpl;
import leads.capita.common.ui.BaseBean;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.TMPlsqlExecutor;
import leads.capita.common.application.ApplicationInfo;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.nav.RichCommandToolbarButton;


import oracle.adf.view.rich.event.DialogEvent;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class FileProcessErrorBean extends BaseBean {
    int importId;
    String fileID;
    private FacesContext fct;

    public FileProcessErrorBean() {
        super();
        fct = JSFUtils.getFacesContextApp();
        //importId = getImportId();
        // fileID = getFileID();
    }


    public int getImportId() {
        DCIteratorBinding fileprocessErrorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        if (fileprocessErrorBindings.getCurrentRow().getAttribute("ImportId") != null) {
            importId = Integer.parseInt(fileprocessErrorBindings.getCurrentRow().getAttribute("ImportId").toString());
        }
        return importId;
    }


    public String getFileID() {
        DCIteratorBinding fileprocessErrorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        if (fileprocessErrorBindings.getCurrentRow().getAttribute("FileId") != null) {
            fileID = fileprocessErrorBindings.getCurrentRow().getAttribute("FileId").toString();
        }
        return fileID;
    }


    public boolean updateTotalRecInfo(int counter) {
        int totalError = 0, totalRec = 0;

        DCIteratorBinding fileprocessErrorBindings = ADFUtils.findIterator("ImportExtFilesVOIterator");
        try {
            if (fileprocessErrorBindings.getCurrentRow().getAttribute("ImportId") != null) {
                importId =
                        Integer.parseInt(fileprocessErrorBindings.getCurrentRow().getAttribute("ImportId").toString());
                totalRec =
                        Integer.parseInt(fileprocessErrorBindings.getCurrentRow().getAttribute("TotalRec").toString());
                totalError =
                        Integer.parseInt(fileprocessErrorBindings.getCurrentRow().getAttribute("ErrorRec").toString());

                // System.out.println("Before: " +" tr :" + totalRec + " TE: "+ totalError + "counter: " +counter);

                if (totalRec > 0 & totalError > 0 & counter > 0) {
                    totalRec = totalRec - counter;
                    totalError = totalError - counter;

                    //   System.out.println("Inside: " +" tr :" + totalRec + " TE: "+ totalError + "counter: " +counter);

                    ApplicationInfo.getCurrentUserDBTransaction().executeCommand("update import_ext_files t set t.total_rec =" +
                                                                                 totalRec + " , t.error_rec =" +
                                                                                 totalError + " where t.import_id= " +
                                                                                 importId);
                    return true;

                }
            }

        } catch (NumberFormatException nfe) {
            // TODO: Add catch code
            nfe.printStackTrace();
            return false;
        }
        return false;
    }

    public void fileProcErrDelDialogListener(DialogEvent dialogEvent) {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        } else {
            this.deleteBtn_action();
        }
    }

    public String deleteBtn_action() {
        // Add event code here...
        DCIteratorBinding fileprocessErrorBindings = ADFUtils.findIterator("FileProcessErrorVOIterator");
        ViewObject fileProcessErrorVO = fileprocessErrorBindings.getViewObject();
        Row row = fileProcessErrorVO.getCurrentRow();
        importId = getImportId();
        fileID = getFileID();
        Long totalrow = (fileprocessErrorBindings.getViewObject().getEstimatedRowCount());
        Integer totalLine = Integer.valueOf(totalrow.toString());
        String paramED = "";
        int success = 1;
        TMPlsqlExecutor tmpe = new TMPlsqlExecutor();
        String errType = null;

        try {

            if (row.getAttribute("ErrType") != " " || row.getAttribute("ErrType") != null) {

                errType = row.getAttribute("ErrType").toString();
                if (errType.equalsIgnoreCase("INVESTOR")) {

                    try {
                        tmpe.delete_file_process_error(importId, errType, fileID);
                        ApplicationInfo.getCurrentUserDBTransaction().commit();
                    } catch (Exception e) {
                        success = -1;
                        e.printStackTrace();
                       ApplicationInfo.getCurrentUserDBTransaction().rollback();
                    }

                } else {
                    JSFUtils.addFacesErrorMessage(" You are allowed to delete Investor  type Error Only");
                }

            } else {
                JSFUtils.addFacesErrorMessage(" Nothing to Delete ");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //}


        if (!updateTotalRecInfo(totalLine)) {
            success = -1;
        }

        if (success > 0) {
            ApplicationInfo.getCurrentUserDBTransaction().commit();
            refreshIterator("FileProcessErrorVOIterator");
            refreshIterator("TradeFileMsaPlusVOIterator");
            JSFUtils.addFacesInformationMessage("Deleted Successfully");
        } else {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesInformationMessage("Not Deleted Successfully");
        }

        return null;
    }

    public String validateBtn_action() {
        // Add event code here...
        return null;
    }

}
