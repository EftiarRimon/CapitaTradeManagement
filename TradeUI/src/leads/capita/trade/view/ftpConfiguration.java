package leads.capita.trade.view;

/*Created by : Ipsheta Saha */

import java.io.IOException;

import javax.faces.application.FacesMessage;

import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.file.FTPUtils;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputText;

import oracle.adf.view.rich.component.rich.input.RichSelectBooleanCheckbox;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.ApplicationModule;
import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import oracle.jbo.server.Entity;
import oracle.jbo.server.ViewRowImpl;

import org.apache.commons.net.ftp.FTPClient;

public class ftpConfiguration {
    private RichInputText hostUI;
    private RichInputText userNameUI;
    private RichInputText userPasswordUI;
    private RichInputText confPasswordUI;
    private RichSelectBooleanCheckbox isActiveModeUI;

    public ftpConfiguration() {
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

    public boolean isNewConfig() {
        boolean isNewRow = false;
        isNewRow = this.isNewStateRow("FtiFtpConfigurationVOIterator");
        return isNewRow;
    }

    //not used

    public String get_newMode() {
        BindingContainer bindings = ADFUtils.getDCBindingContainer();
        OperationBinding operationBinding = bindings.getOperationBinding("CreateInsert");
        if (operationBinding != null) {
            operationBinding.execute();
        } else {
            JSFUtils.addFacesErrorMessage("CreateInsert Binding Problem !!");
        }
        return null;
    }

    public String cbSave_Action() {
        String pass = null;
        String confPass = null;
        boolean isValidPass = false;
        if (getUserPasswordUI().getValue() != null && getConfPasswordUI().getValue() != null) {
            pass = this.getUserPasswordUI().getValue().toString();

            confPass = this.getConfPasswordUI().getValue().toString();
            if (pass.equalsIgnoreCase(confPass)) {
                isValidPass = true;
            } else {
                JSFUtils.addFacesErrorMessage("Password & Confirm Password Does Not Match");
                return null;
            }
        }
        if (changesExists()) {
            if (isValidPass) {
                OperationBinding operationCommit = null;
                operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
                if (operationCommit != null) {
                    try {
                        operationCommit.execute();
                        if (operationCommit.getErrors().isEmpty()) {
                            JSFUtils.addFacesInformationMessage("Saved Successfully");
                        } else {
                            System.out.println("-----" + operationCommit.getErrors());
                            JSFUtils.addFacesErrorMessage("Save Failed !!");
                            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        e.getMessage();
                    }
                }
            }
        } else {
            JSFUtils.addFacesInformationMessage("Nothing To Save..");
        }
        return null;
    }

    public String testConnection() throws IOException {
        String ftpHost = null;
        String ftpUser = null;
        String ftpPass = null;
        String isActiveMode = null;
        boolean testOk = false;
        FTPClient ftpClient = new FTPClient();
        if (getHostUI().getValue() != null && getUserNameUI().getValue() != null &&
            getUserPasswordUI().getValue() != null) {
            ftpHost = getHostUI().getValue().toString();
            ftpUser = getUserNameUI().getValue().toString();
            ftpPass = getUserPasswordUI().getValue().toString();
            String isActive=getIsActiveModeUI().getValue().toString();
            if(isActive.equalsIgnoreCase("true"))
                isActiveMode="Y";
            else
                isActiveMode="N";     
                
            System.out.println("new val.. "+isActiveMode);
        }

        try {
            if (!ftpClient.isConnected()) {
                testOk = FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
            }
            if (testOk) {
                JSFUtils.addFacesInformationMessage("FTP Connection Tested Successfully..");
            } else {
                JSFUtils.addFacesErrorMessage("Problem in FTP Connection !!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage(e.getMessage());
        } finally {
            FTPUtils.ftpDisConnect(ftpClient);
        }

        return null;
    }


    public void setUserPasswordUI(RichInputText userPasswordUI) {
        this.userPasswordUI = userPasswordUI;
    }

    public RichInputText getUserPasswordUI() {
        return userPasswordUI;
    }

    public void setConfPasswordUI(RichInputText confPasswordUI) {
        this.confPasswordUI = confPasswordUI;
    }

    public RichInputText getConfPasswordUI() {
        return confPasswordUI;
    }

    public void setUserNameUI(RichInputText userNameUI) {
        this.userNameUI = userNameUI;
    }

    public RichInputText getUserNameUI() {
        return userNameUI;
    }

    public void setHostUI(RichInputText hostUI) {
        this.hostUI = hostUI;
    }

    public RichInputText getHostUI() {
        return hostUI;
    }

    public void setIsActiveModeUI(RichSelectBooleanCheckbox isActiveModeUI) {
        this.isActiveModeUI = isActiveModeUI;
    }

    public RichSelectBooleanCheckbox getIsActiveModeUI() {
        return isActiveModeUI;
    }
}
