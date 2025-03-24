package leads.capita.trade.view.backing;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import javax.servlet.http.HttpServletRequest;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.file.OperationExecutionUtil;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.controller.ControllerContext;
import oracle.adf.controller.ViewPortContext;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.ADFContext;
import oracle.adf.view.rich.component.rich.RichPopup;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;

public class EodBean {
    private DateFormat formatter = new SimpleDateFormat("dd-MMM-yy");
    private Date appDate;
    private long sessionId;
    private int branchId;
    private TMPlsqlExecutor tmPlSqlExecutor = new TMPlsqlExecutor();
    private RichPopup popConUser;
    private int sessionCount;
    boolean isPassAll = true;

    public EodBean() {
    }

    public String getDayStartStatus() {
        int counter = 0;
        String status = "The process is not executed yet";
        DCIteratorBinding dayStartIteratorBindings = ADFUtils.findIterator("DayStartVOIterator");
        ViewObject dayStartVO = dayStartIteratorBindings.getViewObject();

        this.syncAppState();
        dayStartVO.setWhereClause(" to_char(EXEC_DATE,'dd-mon-yy') = '" + formatter.format(appDate).toLowerCase() +
                                  "'");
        dayStartVO.executeQuery();

        // System.out.println(dayStartVO.getQuery());
        for (Row row : dayStartVO.getAllRowsInRange()) {
            counter++;
            //if (row.getAttribute("Status").equals("Pass")) {
            if ((row.getAttribute("Status") != null) && (row.getAttribute("Status").equals("Pass"))) {
            } else {
                isPassAll = false;
                break;
            }
        }

        if (isPassAll) {
            status = "The day start process has been done!";
        } else {
            status = "The day start process has been failed!";
        }

        if (counter == 0) {
            status = "The process is not execute yet!";
        }

        return status + "(" + appDate.toString() + ")";
    }

    public String executeDayStart() {
        try {
            this.syncAppState();
            tmPlSqlExecutor.dayStart(formatter.format(appDate), branchId, sessionId);
            ApplicationInfo.getCurrentUserDBTransaction().commit();
            //JSFUtils.addFacesInformationMessage(this.getDayStartStatus());
            JSFUtils.addFacesInformationMessage("Process has completed successfully !");

            // make the DAY_START_REQUIRED flag false
            ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
            HttpServletRequest request = (HttpServletRequest) ectx.getRequest();
            request.getSession().setAttribute("DAY_START_REQUIRED", "false");
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Process failed !");
        }
        return "";
    }

    public boolean isAllowDayStartPrevilige() {
        String user = ApplicationInfo.getUser();
        return hasSpecialPermission("DAY_START_PREVILIGE", user, "SpecialFunctionPermissionVOIterator");
    }

    public String getDayEndStatus() throws Exception {
        int counter = 0;
        int nullCounter = 0;
        String status = "The process is not executed yet";
        try {
            DCIteratorBinding dayEndIteratorBindings = ADFUtils.findIterator("DayEndVOIterator");
            ViewObject dayEndVO = dayEndIteratorBindings.getViewObject();
            this.syncAppState();
            dayEndVO.setWhereClause("to_char(EXEC_DATE,'dd-mon-yy')  ='" + formatter.format(appDate).toLowerCase() +
                                    "'");
            dayEndVO.executeQuery();

            DCIteratorBinding conUserIteratorBindings = ADFUtils.findIterator("ConnectedUsersVOIterator");
            ViewObject connUserVO = conUserIteratorBindings.getViewObject();
            //connUserVO.setWhereClause(" trunc(login_time)  ='" + formatter.format(appDate).toLowerCase() + "'");
            connUserVO.executeQuery();
            sessionCount = connUserVO.getRowCount();
            if (sessionCount > 1) {
                showConnUserPopup();
            }

            for (Row row : dayEndVO.getAllRowsInRange()) {
                counter++;
                if ((row.getAttribute("Status") != null) && (row.getAttribute("Status").equals("Pass"))) {
                } else {
                    if ((row.getAttribute("Status") == null)) {
                        nullCounter++;
                    }
                    isPassAll = false;
                    break;
                }
            }
            if (isPassAll) {
                status = "The day End process has been done!";
            } else {
                status = "The day end process has been failed!";
            }
            if (counter == 0) {
                status = "Day end process is not initiate!";
            } else {
                if (nullCounter == counter) {
                    status = "The process is not executed yet!";
                }
            }
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage("--------!" + e.getMessage());
            throw e;
        }
        return status + "(" + appDate.toString() + ")";
    }


    public void executeDayEnd(ActionEvent actionEvent) throws Exception {
        OperationExecutionUtil operationExecutionUtil = null;
        try {
            this.syncAppState();

            DCIteratorBinding conUserIteratorBindings = ADFUtils.findIterator("ConnectedUsersVOIterator");
            ViewObject connUserVO = conUserIteratorBindings.getViewObject();
            connUserVO.executeQuery();
            sessionCount = connUserVO.getRowCount();
            if (sessionCount > 1) {
                showConnUserPopup();
            } else {
                operationExecutionUtil = new OperationExecutionUtil();
                operationExecutionUtil.setOperationExecutionMessage("true", "Sorry!! day end process running",
                                                                    "Day End Running");
                tmPlSqlExecutor.dayEnd(formatter.format(appDate), branchId, sessionId);
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                //JSFUtils.addFacesInformationMessage("Process has completed successfully !");
                // HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
                // req.getSession().invalidate();
                this.getDayEndStatus();
                if (isPassAll) {
                    try {
                        ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
                        HttpServletRequest request = (HttpServletRequest) ectx.getRequest();
                        String temp =
                            //request.getContextPath() + "/adfAuthentication?logout=true&end_url=/faces/Login.jspx";
                            request.getContextPath() + "/adfAuthentication?logout=true&end_url=/faces/login.html";
                        ControllerContext context = ControllerContext.getInstance();
                        ViewPortContext currentRootViewPort = context.getCurrentRootViewPort();
                        if (currentRootViewPort != null) {
                            if (currentRootViewPort.isExceptionPresent()) {
                                currentRootViewPort.clearException();
                            }
                        }
                        ectx.redirect(temp);
                        operationExecutionUtil.setOperationExecutionMessage("false", "null", "Day end process done !");
                    } catch (Exception ex) {
                        ApplicationInfo.getCurrentUserDBTransaction().rollback();
                        //added by forhan
                        //System.out.println("inside pass");
                        if (ex.getMessage().contains("Price File has not processed yet!")) {
                            JSFUtils.addFacesErrorMessage("Price File has not processed yet!");
                        } else if ((ex.getMessage().contains("Price File not found!"))) {
                            JSFUtils.addFacesErrorMessage("Price File not found!");
                        } else {
                            JSFUtils.addFacesErrorMessage("Process failed !");
                        }
                        //ended by forhan
                        operationExecutionUtil.setOperationExecutionMessage("false", "null", "Process failed !");
                        ex.printStackTrace();
                        throw ex;
                    }
                }
            }
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            //added by forhan
            //System.out.println("outside pass"+e.getMessage());
            if (e.getMessage().contains("Price File has not processed yet!")) {
                JSFUtils.addFacesErrorMessage("Price File has not processed yet!");
            } else if ((e.getMessage().contains("Price File not found!"))) {
                JSFUtils.addFacesErrorMessage("Price File not found!");
            } else {
                JSFUtils.addFacesErrorMessage("Process failed !");
            }
            //ended by forhan
            //JSFUtils.addFacesErrorMessage("Process failed !");  //commented by forhan
            operationExecutionUtil.setOperationExecutionMessage("false", "null", "Day end process failed !");
            e.printStackTrace();
        }
    }

    /*
    public String eodProcessAction() throws SQLException {
        String branchCode = null;
        String _currentUser = null;
        _currentUser = getCurrentUser();
        Row r = _getCurrentRow("EODVOIterator");
        if (r != null) {
            if (r.getAttribute("Branch") != null)
                branchCode = r.getAttribute("Branch").toString();
        }
        System.out.print("Branch Code **" + getCurrentBranch() + " **----Current User name " + getCurrentUser() +
                         " -----");
        processEOD(ApplicationInfo.getCurrentUserBranch(), ApplicationInfo.getUser());
        //processEOD("11", "admin");
        return null;
    }
    */

    private void showConnUserPopup() {
        RichPopup.PopupHints hints = new RichPopup.PopupHints();
        popConUser.show(hints);
    }

    /*
    public void processEOD(String branchCode, String userId) {
        boolean isProcessed = false;
        TMPlsqlExecutor imPlSqlExecutor = null;
        imPlSqlExecutor = new TMPlsqlExecutor();
        try {
            isProcessed = imPlSqlExecutor.processEOD(branchCode, userId);
            if (isProcessed)
                JSFUtils.addFacesInformationMessage("Process completed successfully..");
            else
                JSFUtils.addFacesErrorMessage("Process failed !");
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage("Process failed !");
            System.out.println(e.getMessage());
        }
    }
*/

    public Row _getCurrentRow(String iteratorname) {
        DCIteratorBinding it = ADFUtils.findIterator(iteratorname);
        Row curRow = null;
        if (it != null) {
            curRow = it.getCurrentRow();
        }
        return curRow;
    }

    public String getCurrentUser() {
        String _currentUser = null;
        //_currentUser = ADFContext.getCurrent().getSecurityContext().getUserName();
        Object loggedUser = ADFContext.getCurrent()
                                      .getSessionScope()
                                      .get("CURRENT_USER");
        if (loggedUser != null)
            _currentUser = ADFContext.getCurrent()
                                     .getSessionScope()
                                     .get("CURRENT_USER")
                                     .toString();
        return _currentUser;
    }

    public String getCurrentBranch() {
        String _branchId = null;
        Object loggedUserBranch = ADFContext.getCurrent()
                                            .getSessionScope()
                                            .get("CURRENT_USER_BRANCH");
        if (loggedUserBranch != null)
            _branchId = ADFContext.getCurrent()
                                  .getSessionScope()
                                  .get("CURRENT_USER_BRANCH")
                                  .toString();
        return _branchId;
    }


    private ActionEvent acEvent = null;

    public void setAcEvent(ActionEvent acEvent) {
        this.acEvent = acEvent;
    }

    public ActionEvent getAcEvent() {
        return acEvent;
    }

    public void toggleBusyPopup(boolean isShown) {
        FacesContext context = FacesContext.getCurrentInstance();
        RichPopup popup = (RichPopup) JSFUtils.findComponentInRoot("spinnerPopUp");
        ExtendedRenderKitService service = Service.getRenderKitService(context, ExtendedRenderKitService.class);
        if (isShown) {
            service.addScript(context,
                              "var popup = AdfPage.PAGE.findComponent(\"" + popup.getClientId(context) +
                              "\"); popup.show();");
        } else {
            service.addScript(context,
                              "var popup = AdfPage.PAGE.findComponent(\"" + popup.getClientId(context) +
                              "\"); popup.hide();");
        }
        return;
    }

    void syncAppState() {
        try {
            //appDate=ApplicationInfo.getSystemDate()!=null? formatter.parse( new SimpleDateFormat("dd-MMM-yy").format(ApplicationInfo.getSystemDate())):appDate;
            branchId =
                ApplicationInfo.getCurrentUserBranch() != null ?
                Integer.parseInt(ApplicationInfo.getCurrentUserBranch()) : 9;
            appDate = tmPlSqlExecutor.getAppDate(branchId);
            //System.out.println(String.valueOf(ApplicationInfo.getSessionId().toString()));
            sessionId = ApplicationInfo.getSessionId() != null ? ApplicationInfo.getSessionId() : 1;
        } catch (Exception e) {
            // JSFUtils.addFacesInformationMessage("Application Date or Branch or Session Synchronization Problem.");
            JSFUtils.addFacesInformationMessage("Database connection problem!");
            e.printStackTrace();
        }
    }

    public void processAction(ActionEvent acEvent) {
        setAcEvent(acEvent); // save teh query event for the method that really fires the query to use.
        toggleBusyPopup(true); // Fires the popup, which when shown, fires a server listener that fires the query.
    }

    /*
    public void ProcessAction(ClientEvent clientEvent) {
        try {
            eodProcessAction();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        toggleBusyPopup(false);
    }
*/

    public void setPopConUser(RichPopup popConUser) {
        this.popConUser = popConUser;
    }

    public RichPopup getPopConUser() {
        return popConUser;
    }

    public String logoutAllSession() {
        try {
            /* populate session list that is being invalidated. We will use this list to force them out of the system
             * next user performs any action*/
            DCIteratorBinding conUserIteratorBindings = ADFUtils.findIterator("ConnectedUsersVOIterator");
            ViewObject connUserVO = conUserIteratorBindings.getViewObject();
            connUserVO.setRangeSize((int) connUserVO.getEstimatedRowCount());
            Row[] row = connUserVO.getAllRowsInRange();
            for (int i = 0; i < row.length; i++) {
                if (!row[i].getAttribute("SessionId")
                           .toString()
                           .equals(String.valueOf(ApplicationInfo.getSessionId()))) {
                    ApplicationInfo.getInvalidSessions().add(row[i].getAttribute("SessionId").toString());
                }
            }
            tmPlSqlExecutor.logoutAllSession(ApplicationInfo.getSessionId());
            conUserIteratorBindings.executeQuery();
            JSFUtils.addFacesInformationMessage("Process has completed !");
        } catch (Exception e) {
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Process failed !");
        }
        return null;
    }
    //  ====  Check whether login user has the permission or not
    public static boolean hasSpecialPermission(String functionType, String userName, String iteratorName) {
        boolean hasAccess = false;
        DCIteratorBinding iterBinding = ADFUtils.findIterator(iteratorName);
        try {
            if (iterBinding != null && iterBinding.getEstimatedRowCount() > 0) {
                ViewObject vo = iterBinding.getViewObject();
                if (vo != null) {
                    vo.setWhereClause("qrslt.function_type='" + functionType + "' and qrslt.user_name='" + userName +
                                      "'");
                    //System.out.println("---  " + vo.getQuery());
                    vo.executeQuery();
                    if (vo.getEstimatedRangePageCount() > 0)
                        hasAccess = true;
                }
                vo.setWhereClause(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasAccess = false;
        }
        return hasAccess;
    }
}
