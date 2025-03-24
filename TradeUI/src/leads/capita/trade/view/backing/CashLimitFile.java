package leads.capita.trade.view.backing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import javax.xml.parsers.ParserConfigurationException;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.file.FTPUtils;
import leads.capita.trade.file.FlexTradeFileUtil;
import leads.capita.trade.file.PayInOutFileUtil;
import leads.capita.trade.plsql.TMPlsqlExecutor;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.RichQuery;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.context.AdfFacesContext;
import oracle.adf.view.rich.event.DialogEvent;
import oracle.adf.view.rich.event.PopupFetchEvent;
import oracle.adf.view.rich.model.QueryDescriptor;
import oracle.adf.view.rich.model.QueryModel;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewCriteria;
import oracle.jbo.ViewCriteriaItem;
import oracle.jbo.ViewCriteriaItemHints;
import oracle.jbo.ViewCriteriaManager;
import oracle.jbo.ViewCriteriaRow;
import oracle.jbo.ViewObject;
import oracle.jbo.uicli.binding.JUCtrlListBinding;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.xml.sax.SAXException;

public class CashLimitFile {

    private FacesContext fct;
    private String newFileName = null;
    static String clientFileName = null;
    static String clientControlFileName = null;
    private RichInputText generatedClientFileUI;
    private RichInputText generatedClientControlFileUI;
    private static final String xsd_url_path = "flex-tradexsd/v7-10/";
    private static final String client_xsd_url_path = "Flextrade-BOS-Clients.xsd";
    TMPlsqlExecutor tmSqlExe = new TMPlsqlExecutor();
    private RichQuery richClientUIQuery;
    String bussinessType = ApplicationInfo.getBusinessType();
    private RichPopup generatePopUpUI;

    public CashLimitFile() {
        super();
        fct = JSFUtils.getFacesContextApp();
    }

    private DCIteratorBinding getIterator(String iterName) {
        BindingContext ctx = BindingContext.getCurrent();
        DCBindingContainer bc = (DCBindingContainer) ctx.getCurrentBindingsEntry();
        DCIteratorBinding iterator = bc.findIteratorBinding(iterName);
        return iterator;
    }

    public String generateClientData() {
        String fileOption = null;
        String deactivateAll = null;
        String brokerId = null;
        String appDate = new SimpleDateFormat("dd-MMM-yyyy").format(getSysDate());
        DCIteratorBinding clientParameterIter = getIterator("ClientParameterVOIterator");
        ViewObject clientParameterVo = clientParameterIter.getViewObject();
        Row clientParameterRow = clientParameterVo.getCurrentRow();
        if (clientParameterRow != null) {
            if (clientParameterRow.getAttribute("FileOption") != null && !(clientParameterRow.getAttribute("FileOption")
                                                                                             .toString()
                                                                                             .equalsIgnoreCase(""))) {
                fileOption = clientParameterRow.getAttribute("FileOption").toString();
                if (clientParameterRow.getAttribute("DeactivateAll") != null &&
                    !(clientParameterRow.getAttribute("DeactivateAll")
                                                                                                    .toString()
                                                                                                    .equalsIgnoreCase(""))) {
                    deactivateAll = clientParameterRow.getAttribute("DeactivateAll").toString();
                }
                if (clientParameterRow.getAttribute("BrokerId") != null)
                    brokerId = clientParameterRow.getAttribute("BrokerId").toString();
                if (deactivateAll != null && deactivateAll.equalsIgnoreCase("Y")) {
                    if (brokerId != null)
                        System.out.println("Pop Up With Broker Id");
                    else
                        System.out.println("Pop Up With Out Broker Id");
                    RichPopup.PopupHints hints = new RichPopup.PopupHints();
                    generatePopUpUI.show(hints);

                } else {
                    try {
                        tmSqlExe.ftiClientLimitProcCall(fileOption, appDate, deactivateAll, brokerId);
                        ApplicationInfo.getCurrentUserDBTransaction().commit();
                        fct.addMessage("Complete Msg", new FacesMessage("Generate Client Data Successfully"));
                        refreshVersion();
                        executeLovIterator("VersionNo");

                    } catch (Exception e) {
                        ApplicationInfo.getCurrentUserDBTransaction().rollback();
                        JSFUtils.addFacesErrorMessage(e.getMessage());
                    }
                }
            } else {
                JSFUtils.addFacesErrorMessage("File Option is Mandatory");
            }
        }
        return null;
    }

    public void executeLovIterator(String versionNo) {
        try {
            BindingContext bctx = BindingContext.getCurrent();
            BindingContainer bindings = bctx.getCurrentBindingsEntry();
            JUCtrlListBinding list = (JUCtrlListBinding) bindings.get(versionNo);
            if (list != null) {
                DCIteratorBinding iter = list.getListIterBinding();
                if (iter != null && iter.getEstimatedRowCount() > 0) {
                    iter.executeQuery();
                }
            }
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshVersion() {
        RichQuery queryComp = getRichClientUIQuery();
        QueryModel queryModel = queryComp.getModel();
        QueryDescriptor queryDescriptor = queryComp.getValue();
        queryModel.reset(queryDescriptor);
        queryComp.refresh(FacesContext.getCurrentInstance());
        AdfFacesContext.getCurrentInstance().addPartialTarget(queryComp);

    }

    public String processClientLimit() throws ParseException, JDOMException, IOException, SQLException, Exception {

        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
        String shortName = null;

        //String bussinessType = ApplicationInfo.getBusinessType();
        if (bussinessType.equalsIgnoreCase("BROKER"))
            shortName = FlexTradeFileUtil._getAttrValueFromIter("BrokerLOVIterator", "ShortName");
        else
            shortName = FlexTradeFileUtil._getFirstRowAttrValueFromIter("ClientLimitVOIterator", "ShortName");

        newFileName =
            destFolderPath + File.separator +
  FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType
                                                                                                                          .CLIENTS
                                                                                                                          .getValue(),
                                                   null);
        newFileName = newFileName + "-" + shortName + ".xml";

        File generatedfile = new File(newFileName);

        /* clientFileName =
                FlexTradeFileUtil.getGeneratedFileNameCommonPart(FlexTradeFileUtil.FlexTradeFileType.CLIENTS.getValue(),
                                                                 null) + "-" + shortName + ".xml";
        clientControlFileName = FlexTradeFileUtil.appendStringWithFileName(generatedfile.getName(), "-ctrl.xml");*/


        if (generateClient(generatedfile)) {
            try {
                clientFileName = generatedfile.getName() + "-" + shortName;
                clientFileName = FlexTradeFileUtil.appendStringWithFileName(clientFileName, ".xml");
                clientControlFileName =
                    FlexTradeFileUtil.appendStringWithFileName(generatedfile.getName(), "-ctrl.xml");
                generateClientControl(generatedfile);
                JSFUtils.addFacesInformationMessage("File Processing done Successfully");

                //fct.addMessage("Complete Msg", new FacesMessage("File Processing done Successfully"));

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private boolean isValid(Row row) {
        if (row.getAttribute("InvestorCode") == null || (row.getAttribute("InvestorCode")
                                                            .toString()
                                                            .equalsIgnoreCase(""))) {
            JSFUtils.addFacesErrorMessage("Investor Code can not be empty!!");
            return false;
        }
        if (row.getAttribute("Boid") == null || (row.getAttribute("Boid")
                                                    .toString()
                                                    .equalsIgnoreCase(""))) {
            JSFUtils.addFacesErrorMessage("Boid can not be empty!!");
            return false;
        }
        if (row.getAttribute("Category") == null || (row.getAttribute("Category")
                                                        .toString()
                                                        .equalsIgnoreCase(""))) {
            JSFUtils.addFacesErrorMessage("Category can not be empty!!");
            return false;
        }
        /*if (row.getAttribute("TraderId") == null || (row.getAttribute("TraderId").toString().equalsIgnoreCase(""))) {
            JSFUtils.addFacesErrorMessage("Trader ID not found for (" + row.getAttribute("InvestorCode") + ")!");
            return false;
        }*/
        if (bussinessType.equalsIgnoreCase("MBANK")) {
            if (row.getAttribute("TradingCode") == null || (row.getAttribute("TradingCode")
                                                               .toString()
                                                               .equalsIgnoreCase(""))) {
                JSFUtils.addFacesErrorMessage("Trading Code is Mandatory for (" + row.getAttribute("InvestorCode") +
                                              ")!");
                return false;
            }
        }
        row = null;
        return true;
    }

    private boolean isValidTraderId(Row[] rows) {
        StringBuffer sb = new StringBuffer();
        boolean firstTime = true;
        for (int i = 0; i < rows.length; i++) {
            Row row = rows[i];
            if (row.getAttribute("TraderId") == null || (row.getAttribute("TraderId")
                                                            .toString()
                                                            .equalsIgnoreCase(""))) {

                if (!firstTime) {
                    sb.append(",  ");
                }
                sb.append(row.getAttribute("InvestorCode"));
                firstTime = false;
            }
        }
        if (sb.length() > 0)
            JSFUtils.addFacesErrorMessage("Trader ID not found for (" + sb + ")!");
        return firstTime;
    }

    private boolean generateClient(File generatedfile) throws JDOMException, SQLException, Exception {

        Map<String, String> nsNameValue = null;
        Map<Object, String> clientLimit = null;
        Map<String, Integer> invPurchasePower = null;

        String deactivateAll = null;
        String fileOption = null;
        Double buyLimit = 0.0;
        DCIteratorBinding clientXmlIter = getIterator("ClientLimitVOIterator");
        /* try {
            clientXmlIter.setRangeSize((int)clientXmlIter.getEstimatedRowCount());
        } catch (Exception e) {
            e.printStackTrace();
        } */
        Row[] rows = clientXmlIter.getAllRowsInRange();

        Long noOfLines1 = (clientXmlIter.getViewObject().getEstimatedRowCount());
        String nolines = noOfLines1.toString();
        Integer noOfLines = Integer.valueOf(nolines);


        DCIteratorBinding clientParameterIter = getIterator("ClientParameterVOIterator");
        ViewObject clientParameterVo = clientParameterIter.getViewObject();
        Row clientParameterRow = clientParameterVo.getCurrentRow();

        if (rows != null && rows.length > 0) {
            if (rows[0].getAttribute("IsDeactivateAll") != null && !(clientParameterRow.getAttribute("IsDeactivateAll")
                                                                                     .toString()
                                                                                     .equalsIgnoreCase(""))) {
                deactivateAll = clientParameterRow.getAttribute("IsDeactivateAll").toString();
            }
        }

        if (clientParameterRow != null) {
            if (clientParameterRow.getAttribute("BuyLimit") != null && !(clientParameterRow.getAttribute("BuyLimit")
                                                                                           .toString()
                                                                                           .equalsIgnoreCase(""))) {
                buyLimit = Double.parseDouble(clientParameterRow.getAttribute("BuyLimit").toString());
            }
        }
        fileOption = FlexTradeFileUtil._getAttrValueFromIter("ClientLimitVOIterator", "MornIntraday");
        DCIteratorBinding boardLimitIter = getIterator("InvestorBoardLimitVOIterator");
        Row[] boardRows = boardLimitIter.getAllRowsInRange();

        Long noOfRows = (boardLimitIter.getViewObject().getEstimatedRowCount());
        String temp = noOfRows.toString();
        Integer boardLines = Integer.valueOf(temp);

        invPurchasePower = new HashMap<String, Integer>();

        if (noOfLines > 0) {
            BufferedWriter bfw = null;
            try {
                if (!isValidTraderId(rows))
                    return false;
                clientLimit = new HashMap<Object, String>();
                nsNameValue = new HashMap<String, String>();
                nsNameValue.put("ProcessingMode", "BatchInsertOrUpdate");
                nsNameValue.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
                nsNameValue.put("xsi:noNamespaceSchemaLocation", "Flextrade-BOS-Clients.xsd");
                nsNameValue.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

                String rootElemenet = FlexTradeFileUtil.generateRootWithSchema("Clients", null, nsNameValue);

                Document doc = new SAXBuilder().build(new StringReader(rootElemenet));
                Element root = doc.getRootElement();
                if (deactivateAll != null && deactivateAll.equalsIgnoreCase("Y")) {
                    if (fileOption.equalsIgnoreCase("R")) {
                        Element deactivateAllClients = new Element("DeactivateAllClients");
                        doc.getRootElement().addContent(deactivateAllClients);

                        for (int i = 0; i < rows.length; i++) {
                            Row r = rows[i];

                            if (isValid(r)) {
                                generateInvRegistration(clientLimit, doc, r, root);
                            } else {
                                return false;
                            }
                        }
                    } else {
                        JSFUtils.addFacesErrorMessage("<Deactivate All> is allowed only in Registration");
                        return false;
                    }
                } else {
                    if ((fileOption.equalsIgnoreCase("R"))) {
                        for (int i = 0; i < rows.length; i++) {
                            Row r = rows[i];

                            if (isValid(r)) {
                                generateInvRegistration(clientLimit, doc, r, root);
                            } else {
                                return false;
                            }
                        }
                    }
                }

                if (fileOption.equalsIgnoreCase("R")) {
                    for (int i = 0; i < rows.length; i++) {
                        Row r = rows[i];
                        if (!clientLimit.containsKey(r.getAttribute("InvestorCode"))) {
                            generateInvLimit(buyLimit, doc, r, root, invPurchasePower);
                            //clientLimit.remove(r.getAttribute("InvestorCode"));
                        }
                    }

                    if (boardLines > 0) {
                        for (int i = 0; i < boardRows.length; i++) {
                            Row r = boardRows[i];
                            if (invPurchasePower.containsKey(r.getAttribute("InvCode")))
                                generateInvBoardLimit(r, root,
                                                      invPurchasePower.get(r.getAttribute("InvCode").toString()));
                        }
                    }

                } else if (fileOption.equalsIgnoreCase("M") || fileOption.equalsIgnoreCase("I")) {
                    //Registration file write
                    for (int i = 0; i < rows.length; i++) {
                        Row r = rows[i];

                        if (isValid(r)) {
                            generateInvRegistration(clientLimit, doc, r, root);
                        } else {
                            return false;
                        }
                    }
                    //Limit file write
                    for (int i = 0; i < rows.length; i++) {

                        Row r = rows[i];
                        if (!clientLimit.containsKey(r.getAttribute("InvestorCode"))) {
                            generateInvLimit(buyLimit, doc, r, root, invPurchasePower);
                            //clientLimit.remove(r.getAttribute("InvestorCode"));
                        }
                    }

                    if (boardLines > 0) {
                        for (int i = 0; i < boardRows.length; i++) {
                            Row r = boardRows[i];
                            if (invPurchasePower.containsKey(r.getAttribute("InvCode")))
                                generateInvBoardLimit(r, root,
                                                      invPurchasePower.get(r.getAttribute("InvCode").toString()));

                        }
                    }

                }
                bfw = new BufferedWriter(new FileWriter(generatedfile.getPath()));

                XMLOutputter xmlOutput = new XMLOutputter();
                xmlOutput.setFormat(Format.getPrettyFormat());
                xmlOutput.output(doc, bfw);

                nsNameValue.clear();
                clientLimit.clear();
                System.gc();

                if (bfw != null) {
                    bfw.flush();
                    bfw.close();
                    xmlOutput = null;
                    nsNameValue = null;
                    clientLimit = null;
                    doc = null;
                    rows = null;
                    bfw = null;
                }

            } catch (IOException io) {
                nsNameValue.clear();
                clientLimit.clear();
                nsNameValue = null;
                clientLimit = null;
                io.printStackTrace();
            } catch (Exception e) {
                nsNameValue.clear();
                clientLimit.clear();
                nsNameValue = null;
                clientLimit = null;
                e.printStackTrace();
            }
        } else {
            fct.addMessage("Complete Msg", new FacesMessage("No Data to generate"));
            clientFileName = null;
            clientControlFileName = null;
            return false;
        }

        return true;
    }

    private void generateInvRegistration(Map<Object, String> clientLimit, Document doc, Row r, Element root) {

        String invAccountStatus = null;
        String isNew = null;
        String operationType = null;
        String icNo = null;
        String phone = null;
        String traderId = null;
        String name = null;
        String address = null;
        String accCategory = null;

        if (r.getAttribute("AccountStatus") != null)
            invAccountStatus = r.getAttribute("AccountStatus").toString(); //Current status
        if (r.getAttribute("IsNew") != null)
            isNew = r.getAttribute("IsNew").toString();

        if (r.getAttribute("OperationType") != null && !(r.getAttribute("OperationType")
                                                          .toString()
                                                          .equalsIgnoreCase("")))
            operationType = r.getAttribute("OperationType").toString();

        if (r.getAttribute("Name") != null && !(r.getAttribute("Name")
                                                 .toString()
                                                 .equalsIgnoreCase(""))) {
            name = r.getAttribute("Name").toString();
            if (r.getAttribute("Name")
                 .toString()
                 .length() > 50)
                name = name.substring(0, 50);
        }
        if (r.getAttribute("Address") != null && !(r.getAttribute("Address")
                                                    .toString()
                                                    .equalsIgnoreCase(""))) {
            address = r.getAttribute("Address").toString();
            if (r.getAttribute("Address")
                 .toString()
                 .length() > 160)
                address = address.substring(0, 160);
        }

        if (r.getAttribute("NationalId") != null && !(r.getAttribute("NationalId")
                                                       .toString()
                                                       .equalsIgnoreCase(""))) {
            icNo = r.getAttribute("NationalId").toString();
            if (r.getAttribute("NationalId")
                 .toString()
                 .length() > 14)
                icNo = icNo.substring(0, 14);
        }

        if (r.getAttribute("Mobile") != null && !(r.getAttribute("Mobile")
                                                   .toString()
                                                   .equalsIgnoreCase(""))) {
            phone = r.getAttribute("Mobile").toString();
            if (r.getAttribute("Mobile")
                 .toString()
                 .length() > 20)
                phone = phone.substring(0, 20);
        }

        if (r.getAttribute("Category") != null && !(r.getAttribute("Category")
                                                     .toString()
                                                     .equalsIgnoreCase("")))
            accCategory = r.getAttribute("Category").toString();

        if (r.getAttribute("TraderId") != null && !(r.getAttribute("TraderId")
                                                     .toString()
                                                     .equalsIgnoreCase("")))
            traderId = r.getAttribute("TraderId").toString();
        if (isNew.equals("Y")) {

            Element register = new Element("Register");
            if (bussinessType.equalsIgnoreCase("MBANK"))
                register.addContent(new Element("ClientCode")
                                    .setText(r.getAttribute("TradingCode") == null ? null :
                                             r.getAttribute("TradingCode").toString()));
            else
                register.addContent(new Element("ClientCode")
                                    .setText(r.getAttribute("InvestorCode") == null ? null :
                                             r.getAttribute("InvestorCode").toString()));
            if (r.getAttribute("TraderId")
                 .toString()
                 .contains("DLR"))
                register.addContent(new Element("DealerID")
                                    .setText(r.getAttribute("TraderId") == null ? null :
                                             (r.getAttribute("TraderId").toString())));
            else
                register.addContent(new Element("DealerID")
                                    .setText(r.getAttribute("TraderId") == null ? null :
                                             FlexTradeFileUtil.getFixedTengthTraderId(r.getAttribute("TraderId")
                                                                                      .toString())));
            register.addContent(new Element("BOID")
                                .setText(r.getAttribute("Boid") == null ? null : r.getAttribute("Boid").toString()));
            register.addContent(new Element("WithNetAdjustment").setText("Yes"));
            register.addContent(new Element("Name").addContent(new CDATA(name)));
            register.addContent(new Element("ShortName").addContent(new CDATA("")));
            register.addContent(new Element("Address")
                                .addContent(new CDATA(r.getAttribute("Address") == null ? null : address)));
            register.addContent(new Element("Tel").addContent(new CDATA(phone)));
            register.addContent(new Element("ICNo").setText(icNo));
            /* register.addContent(new Element("AccountType").addContent(new CDATA(r.getAttribute("Category") ==
                                                                                            null ? null : r.getAttribute("Category").toString()))); */
            String accountType = accountCategory(accCategory);
            if (accountType != null)
                register.addContent(new Element("AccountType").addContent(new CDATA(accountType)));
            else {
                if (operationType.equalsIgnoreCase("CORPORATE"))
                    register.addContent(new Element("AccountType").addContent(new CDATA("I")));
                else
                    register.addContent(new Element("AccountType").addContent(new CDATA("N")));
            }
            register.addContent(new Element("ShortSellingAllowed").setText("No"));
            doc.getRootElement().addContent(register);
            //clientLimit.put(r.getAttribute("InvestorCode"), r.getAttribute("InvestorCode").toString());
        } else if (isNew.equals("S")) {
            Element suspend = new Element("Suspend");
            //Suspend Element:NoChange,Suspend,Resume
            if (bussinessType.equalsIgnoreCase("MBANK"))
                suspend.addContent(new Element("ClientCode")
                                   .setText(r.getAttribute("TradingCode") == null ? null :
                                            r.getAttribute("TradingCode").toString()));
            else
                suspend.addContent(new Element("ClientCode")
                                   .setText(r.getAttribute("InvestorCode") == null ? null :
                                            r.getAttribute("InvestorCode").toString()));
            suspend.addContent(new Element("Sell_Suspend").setText("Suspend"));
            suspend.addContent(new Element("Buy_Suspend").setText("Suspend"));
            suspend.addContent(new Element("Remark").setText(""));

            doc.getRootElement().addContent(suspend);
            //r.setAttribute("CalculatedPurchasePower", "0");
            clientLimit.put(r.getAttribute("InvestorCode"), r.getAttribute("InvestorCode").toString());
        } else if (isNew.equals("R")) {
            Element suspend = new Element("Suspend");
            //Suspend Element:NoChange,Suspend,Resume
            if (bussinessType.equalsIgnoreCase("MBANK"))
                suspend.addContent(new Element("ClientCode")
                                   .setText(r.getAttribute("TradingCode") == null ? null :
                                            r.getAttribute("TradingCode").toString()));
            else
                suspend.addContent(new Element("ClientCode")
                                   .setText(r.getAttribute("InvestorCode") == null ? null :
                                            r.getAttribute("InvestorCode").toString()));
            suspend.addContent(new Element("Sell_Suspend").setText("Resume"));
            suspend.addContent(new Element("Buy_Suspend").setText("Resume"));
            suspend.addContent(new Element("Remark").setText(""));

            doc.getRootElement().addContent(suspend);
            //clientLimit.put(r.getAttribute("InvestorCode"), r.getAttribute("InvestorCode").toString());
        } else if (isNew.equals("D")) {
            Element deactivate = new Element("Deactivate");
            if (bussinessType.equalsIgnoreCase("MBANK"))
                deactivate.addContent(new Element("ClientCode")
                                      .setText(r.getAttribute("TradingCode") == null ? null :
                                               r.getAttribute("TradingCode").toString()));
            else
                deactivate.addContent(new Element("ClientCode")
                                      .setText(r.getAttribute("InvestorCode") == null ? null :
                                               r.getAttribute("InvestorCode").toString()));
            doc.getRootElement().addContent(deactivate);
            //r.setAttribute("CalculatedPurchasePower", "0");
            clientLimit.put(r.getAttribute("InvestorCode"), r.getAttribute("InvestorCode").toString());
        } else if (isNew.equals("E")) {
            //clientLimit.put(r.getAttribute("InvestorCode"), r.getAttribute("InvestorCode").toString());
        }

        invAccountStatus = null;
        isNew = null;
        operationType = null;
        icNo = null;
        phone = null;
        traderId = null;
        name = null;
        address = null;
        accCategory = null;
    }

    private void generateInvLimit(Double buyLimit, Document doc, Row r, Element root,
                                  Map<String, Integer> invPurchasePower) {
        Double purchasePower = 0.0;
        int marginId = 0;
        int isMarginable = 0;
        Long bLimit = 0L;

        if (r.getAttribute("PurchasePower") != null && !(r.getAttribute("PurchasePower")
                                                          .toString()
                                                          .equalsIgnoreCase(""))) {
            purchasePower = Double.parseDouble(r.getAttribute("PurchasePower").toString());
        }
        if (r.getAttribute("MarginId") != null && !(r.getAttribute("MarginId")
                                                     .toString()
                                                     .equalsIgnoreCase(""))) {
            marginId = Integer.parseInt(r.getAttribute("MarginId").toString());
        }

        if (r.getAttribute("Ismarginable") != null && !(r.getAttribute("Ismarginable")
                                                         .toString()
                                                         .equalsIgnoreCase(""))) {
            isMarginable = Integer.parseInt(r.getAttribute("Ismarginable").toString());
        }

        Element limits = new Element("Limits");

        if (bussinessType.equalsIgnoreCase("MBANK")) {
            limits.addContent(new Element("ClientCode")
                              .setText(r.getAttribute("TradingCode") == null ? null :
                                       r.getAttribute("TradingCode").toString()));
            //limits.addContent(new Element("ClientCode").setText(r.getAttribute("TradingCode").toString()));
        } else {
            limits.addContent(new Element("ClientCode")
                              .setText(r.getAttribute("InvestorCode") == null ? null :
                                       r.getAttribute("InvestorCode").toString()));
            //limits.addContent(new Element("ClientCode").setText(r.getAttribute("InvestorCode").toString()));
        }
        if (purchasePower.compareTo(0.0) > 0) {

            //limits.addContent(new Element("Cash").setText(r.getAttribute("PurchasePower").toString()));
            bLimit = this.getBuyLimit(buyLimit, r);
            String buylimit_set = null;
            if (bLimit == null || bLimit == 0)
                buylimit_set = "0";
            else
                buylimit_set = bLimit.toString();

            //r.setAttribute("CalculatedPurchasePower", buylimit_set);
            limits.addContent(new Element("Cash").setText(buylimit_set));
        } else {
            //r.setAttribute("CalculatedPurchasePower", "0");
            limits.addContent(new Element("Cash").setText("0"));
        }
        //limits.addContent(new Element("Cash").setText("0"));*/
        if (isMarginable > 0) {
            if (marginId > 0)
                limits.addContent(new Element("Margin")
                                  .setAttribute("MarginRatio", r.getAttribute("RatioRate").toString()));
            else
                limits.addContent(new Element("Margin"));
        }
        invPurchasePower.put(r.getAttribute("InvestorCode").toString(), bLimit.intValue());

        root.addContent(limits);
    }

    public String getNewGenFile() {
        return clientFileName;
    }

    public String getNewGenControlFile() {
        return clientControlFileName;
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

    private void generateClientControl(File generatedClientFile) throws JDOMException, IOException {

        Map<String, String> nsNameValue = null;

        String positionFileHashCode = FlexTradeFileUtil.getMD5HashContentForFile(generatedClientFile.getPath());
        nsNameValue = new HashMap<String, String>();
        nsNameValue.put("Hash", positionFileHashCode);
        nsNameValue.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        nsNameValue.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        nsNameValue.put("xsi:noNamespaceSchemaLocation", "Flextrade-BOS-Control.xsd");
        nsNameValue.put("Method", "MD5");

        File generatedControlfileName =
            new File(FlexTradeFileUtil.appendStringWithFileName(generatedClientFile.getName(), "-ctrl.xml"));

        String rootElemenet = FlexTradeFileUtil.generateRootWithSchema("Control", null, nsNameValue);

        Document doc = new SAXBuilder().build(new StringReader(rootElemenet));
        FileWriter fw =
            new FileWriter(generatedClientFile.getParent() + File.separator + generatedControlfileName.getName());
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, fw);

        fw.close();
        xmlOutput = null;
        doc = null;
    }

    public void generatedClientListener(FacesContext facesContext, OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext) fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
        File home_dirFile =
            new File(destFolderPath + File.separator + getGeneratedClientFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator + getGeneratedClientFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            fdownload = new FileInputStream(f);
            int n;
            while ((n = fdownload.available()) > 0) {
                b = new byte[n];
                int result = fdownload.read(b);
                outputStream.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            outputStream.flush();
            new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
            fdownload.close();
            outputStream.close();
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        } catch (NullPointerException e) {
            JSFUtils.addFacesErrorMessage("No File Found !");
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }

    }

    public void generatedClientControlListener(FacesContext facesContext,
                                               OutputStream outputStream) throws ParseException {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext) fctx.getExternalContext().getContext();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
        File home_dirFile =
            new File(destFolderPath + File.separator + getGeneratedClientControlFileUI().getValue().toString());
        File weRoot_dirFile =
            new File(context.getRealPath("/") + File.separator +
                     getGeneratedClientControlFileUI().getValue().toString());
        FileInputStream fdownload;
        byte[] b;
        try {
            // _writeFile(); //write date to file before download....
            File f = new File(home_dirFile.getPath());
            fdownload = new FileInputStream(f);
            int n;
            while ((n = fdownload.available()) > 0) {
                b = new byte[n];
                int result = fdownload.read(b);
                outputStream.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            outputStream.flush();
            new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
            fdownload.close();
            outputStream.close();
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Occured !");
        } catch (NullPointerException e) {
            JSFUtils.addFacesErrorMessage("No File Found !");
        } catch (Exception e) {
            JSFUtils.addFacesErrorMessage(e.getMessage());
        }

    }

    public void setGeneratedClientFileUI(RichInputText generatedClientFileUI) {
        this.generatedClientFileUI = generatedClientFileUI;
    }

    public RichInputText getGeneratedClientFileUI() {
        return generatedClientFileUI;
    }

    public void setGeneratedClientControlFileUI(RichInputText generatedClientControlFileUI) {
        this.generatedClientControlFileUI = generatedClientControlFileUI;
    }

    public RichInputText getGeneratedClientControlFileUI() {
        return generatedClientControlFileUI;
    }

    public String sendFileThroughFTP() throws IOException, Exception {
        FTPClient ftpClient = new FTPClient();
        FTPClient ftpClient2 = new FTPClient();
        if (!ftpClient.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
        }

        if (!ftpClient2.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            FTPUtils.ftpConnect(ftpClient2, ftpHost, ftpUser, ftpPass, isActiveMode);
        }


        try {
            String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
            //FTPUtils.ftpConnect(ftpClient, "192.168.20.107", "akila", "akila");
            //FTPUtils.ftpConnect(ftpClient, "192.168.20.107", "mohin", "mainuddin");
            File home_dirFile =
                new File(destFolderPath + File.separator + getGeneratedClientFileUI().getValue().toString());
            File home_dirFileControl =
                new File(destFolderPath + File.separator + getGeneratedClientControlFileUI().getValue().toString());
            if (ftpClient != null)
                ftpClient.setDefaultTimeout(10000);

            // ftpClient.completePendingCommand();
            //ftpClient.changeWorkingDirectory("/positions");
            boolean isTrasfered = FTPUtils.uploadFile(ftpClient, home_dirFile.getPath(), null);
            //boolean isTrasfered2 = FTPUtils.uploadFile(ftpClient2, home_dirFileControl.getPath(), null);

            /* ftpClient.completePendingCommand();
            ftpClient2.completePendingCommand();
             int r1 = ftpClient.getReplyCode();
            int r2 = ftpClient2.getReplyCode(); */
            if (isTrasfered) {
                ftpClient.logout();
                if (ftpClient != null && ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }

            }
            //ftpClient.logout();

            Thread.currentThread();
            Thread.currentThread().sleep(5000);

            if (ftpClient2 != null)
                ftpClient2.setDefaultTimeout(20000);
            boolean isTrasfered2 = FTPUtils.uploadFile(ftpClient2, home_dirFileControl.getPath(), null);
            //ftpClient2.logout();
            if (isTrasfered2) {
                ftpClient2.logout();
                if (ftpClient2 != null && ftpClient2.isConnected()) {
                    ftpClient2.disconnect();
                }
            }
            /* if (isTrasfered && isTrasfered2) {
                if (ftpClient != null && ftpClient.isConnected())
                    ftpClient.disconnect();
                if (ftpClient2 != null && ftpClient2.isConnected())
                    ftpClient2.disconnect();
                fct.addMessage("Complete Msg", new FacesMessage("File Send Successfully"));
            } */
            if (isTrasfered && isTrasfered2)
                fct.addMessage("Complete Msg", new FacesMessage("File Send Successfully"));
        } catch (Exception e) {
            if (ftpClient != null && ftpClient.isConnected())
                ftpClient.disconnect();
            if (ftpClient2 != null && ftpClient2.isConnected())
                ftpClient2.disconnect();
            JSFUtils.addFacesErrorMessage("File Send Error");
            e.printStackTrace();
        }

        return null;
    }

    public String validateClientLimitWithXsd() throws ParseException, ParserConfigurationException, IOException,
                                                      SAXException {
        try {
            boolean isValidate = false;
            String generatedClientFilePath = null;
            String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Limit");
            String destXsdFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Xsd");
            if (getGeneratedClientFileUI().getValue() != null) {
                HttpServletRequest request = (HttpServletRequest) JSFUtils.getFacesContext()
                                                                          .getExternalContext()
                                                                          .getRequest();
                generatedClientFilePath =
                    destFolderPath + File.separator + getGeneratedClientFileUI().getValue().toString();
                //String clientFileXsdPath = destFolderPath + File.separator + "Flextrade-BOS-Clients.xsd";
                String urlPath =
                    FlexTradeFileUtil.getURLWithContextPath(request) + "/" + xsd_url_path + client_xsd_url_path;

                String clientFileXsdPath = destXsdFolderPath + File.separator + client_xsd_url_path;
                FlexTradeFileUtil.downloadUrlFileUsingStream(urlPath, clientFileXsdPath);
                isValidate = FlexTradeFileUtil.validateWithExtXSDUsingSAX(generatedClientFilePath, clientFileXsdPath);
                if (isValidate)
                    fct.addMessage("Complete Msg", new FacesMessage("File Validate Successfully"));
                else
                    JSFUtils.addFacesErrorMessage("File Validate Error");
            } else
                fct.addMessage("Complete Msg", new FacesMessage("No File Found To Validate"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private Long getBuyLimit(Double buyLimit, Row preadRow) {
        Double bLimit = 0.0;
        Double cLimit = 0.0;
        cLimit = Double.parseDouble(preadRow.getAttribute("PurchasePower").toString());
        if (buyLimit != null && buyLimit.doubleValue() > 0.0) {
            bLimit = (buyLimit / 100.0) * (cLimit);
        } else {
            if (cLimit.doubleValue() < 1.0) {
                bLimit = 0.0;
            } else {
                bLimit = cLimit;
            }
        }

        double mLimit = Math.floor(Double.parseDouble(bLimit.toString()));
        long csLimit = (long) mLimit;
        //csLimit = csLimit / 1000;
        return csLimit;
    }

    private Long getBoardBuyLimit(Integer limit, String percentage) {

        double percent = Double.parseDouble(percentage);
        double bLimit = 0.0;
        bLimit = (limit * percent) / 100.0;
        double mLimit = Math.floor(bLimit);
        return (long) mLimit;
    }


    public void showRemoteClientFileFetchListener(PopupFetchEvent popupFetchEvent) {
        int ctr = 0;
        //Map<String, String> fileTypeMap =new HashMap<String, String>();
        //fileTypeMap.put(fileTypeRadio,fileTypeRadio);
        try {
            FTPClient ftpClient = new FTPClient();
            if (!ftpClient.isConnected()) {
                String ftpUser = FTPUtils.getFTPUser();
                String ftpHost = FTPUtils.getFTPHost();
                String ftpPass = FTPUtils.getFTPUserPassword();
                String isActiveMode = FTPUtils.getIsActiveMode();
                String ftpProtocol = FTPUtils.getFTPProtocol();
                FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
            }
            List<FTPFile> clientFileList = FTPUtils.getClientFileFromFtp(ftpClient);

            DCIteratorBinding clientFilesIter = ADFUtils.findIterator("FtiFtpClientFilesVOIterator");
            ViewObject clientFilesVO = clientFilesIter.getViewObject();
            if (clientFilesVO != null) {
                Map<String, String> existingFiles =
                    FlexTradeFileUtil.getVOAttrValueInMap("FtiFtpClientFilesVOIterator", "FileName");
                int counter = 0;
                for (FTPFile fList : clientFileList) {
                    if (!existingFiles.containsKey(fList.getName())) {
                        counter++;
                        Row clientFilesRow = clientFilesVO.createRow();
                        String fileId = FlexTradeFileUtil.getUniqueValue();
                        clientFilesRow.setAttribute("FtiFileId", fileId + counter);
                        clientFilesRow.setAttribute("FileName", fList.getName());
                        clientFilesRow.setAttribute("FileType", "Client");
                        clientFilesRow.setAttribute("FileSize", fList.getSize() / 1000);
                        ctr++;
                    }
                }
            }
            OperationBinding operationCommit = null;
            operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
            if (ctr > 0 && operationCommit != null) {
                try {
                    operationCommit.execute();
                    if (operationCommit.getErrors().isEmpty()) {
                        JSFUtils.addFacesInformationMessage("File(s) Loaded From FTP Successfully");
                        clientFilesVO.executeQuery();
                        FTPUtils.ftpDisConnect(ftpClient);
                    } else {
                        JSFUtils.addFacesErrorMessage("Problem in Loading Trade Files");
                        JSFUtils.getBindings()
                                .getOperationBinding("Rollback")
                                .execute();
                    }
                } catch (Exception e) {
                    JSFUtils.getBindings()
                            .getOperationBinding("Rollback")
                            .execute();
                    e.printStackTrace();
                    e.getMessage();
                }
            } else if (ctr == 0) {
                JSFUtils.addFacesInformationMessage("No New File(s) to Load from FTP..");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showRemoteClientFileDialog(DialogEvent dialogEvent) {
        if (!dialogEvent.getOutcome().equals(DialogEvent.Outcome.ok)) {
            return;
        }
    }

    private String accountCategory(String accType) {

        if (accType != null) {
            if (accType.equalsIgnoreCase("N"))
                return FlexTradeFileUtil.FlexAccountType
                                        .N
                                        .getValue();
            else if (accType.equalsIgnoreCase("D"))
                return FlexTradeFileUtil.FlexAccountType
                                        .D
                                        .getValue();
            else if (accType.equalsIgnoreCase("I"))
                return FlexTradeFileUtil.FlexAccountType
                                        .I
                                        .getValue();
            else if (accType.equalsIgnoreCase("MF"))
                return FlexTradeFileUtil.FlexAccountType
                                        .MF
                                        .getValue();
            else if (accType.equalsIgnoreCase("RB"))
                return FlexTradeFileUtil.FlexAccountType
                                        .RB
                                        .getValue();
            else if (accType.equalsIgnoreCase("F"))
                return FlexTradeFileUtil.FlexAccountType
                                        .F
                                        .getValue();
            else if (accType.equalsIgnoreCase("NRB"))
                return FlexTradeFileUtil.FlexAccountType
                                        .NRB
                                        .getValue();
            else if (accType.equalsIgnoreCase("OM"))
                return FlexTradeFileUtil.FlexAccountType
                                        .OM
                                        .getValue();
            else if (accType.equalsIgnoreCase("ASI"))
                return FlexTradeFileUtil.FlexAccountType
                                        .ASI
                                        .getValue();
            else if (accType.equalsIgnoreCase("O"))
                return FlexTradeFileUtil.FlexAccountType
                                        .O
                                        .getValue();
            else
                return null;
        } else
            return null;
    }

    public void setRichClientUIQuery(RichQuery richClientUIQuery) {
        this.richClientUIQuery = richClientUIQuery;
    }

    public RichQuery getRichClientUIQuery() {
        return richClientUIQuery;
    }


    private void hideCriteriaItem(String viewCriteriaName, String criteriaItemName, boolean condition,
                                  String showHint) {
        if (viewCriteriaName != null) {
            ViewCriteria v = this.getViewCriteria(viewCriteriaName);
            if (v != null) {
                boolean found = false;
                while (v.hasNext() && !found) {
                    ViewCriteriaRow vcr = (ViewCriteriaRow) v.next();
                    if (vcr != null) {
                        ViewCriteriaItem[] vcis = vcr.getCriteriaItemArray();
                        if (vcis != null && vcis.length > 0) {
                            for (int j = 0; j < vcis.length && !found; j++) {
                                ViewCriteriaItem vci = vcis[j];
                                if (vci != null && criteriaItemName != null && criteriaItemName.equals(vci.getName())) {
                                    found = true;
                                    if (bussinessType.equalsIgnoreCase("MBANK")) {
                                        vci.setRequiredString(ViewCriteriaItem.VCITEM_REQUIRED_STR);
                                    }
                                    vci.setProperty(ViewCriteriaItemHints.CRITERIA_RENDERED_MODE,
                                                    condition ? ViewCriteriaItemHints.CRITERIA_RENDERED_MODE_NEVER :
                                                    showHint);
                                    v.saveState();
                                }
                            }
                        }
                    }
                    if (found)
                        break;
                }
            }
        }
    }

    public String getRenderBrokerIdFromViewCriteria() {
        hideCriteriaItem("ClientLimitVOCriteria", "BrokerId", bussinessType.equalsIgnoreCase("MBANK") ? false : true,
                         ViewCriteriaItemHints.CRITERIA_RENDERED_MODE_DEFAULT);
        return null;

    }

    private ViewCriteria getViewCriteria(String string) {
        ViewCriteria vc = null;
        try {
            DCIteratorBinding iter = ADFUtils.findIterator("ClientLimitVOIterator");
            ViewObject vo = iter.getViewObject();
            ViewCriteriaManager vcr = vo.getViewCriteriaManager();
            vc = vcr.getViewCriteria(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vc;
    }


    private void getBrokerId(String viewCriteriaName, String criteriaItemName) {
        if (viewCriteriaName != null) {
            ViewCriteria v = this.getViewCriteria(viewCriteriaName);
            if (v != null) {
                boolean found = false;
                while (v.hasNext() && !found) {
                    ViewCriteriaRow vcr = (ViewCriteriaRow) v.next();
                    if (vcr != null) {
                        ViewCriteriaItem[] vcis = vcr.getCriteriaItemArray();
                        if (vcis != null && vcis.length > 0) {
                            for (int j = 0; j < vcis.length && !found; j++) {
                                ViewCriteriaItem vci = vcis[j];
                                if (vci != null) {
                                    found = true;
                                    //if (bussinessType.equalsIgnoreCase("MBANK")) {
                                    //}
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    public void setGeneratePopUpUI(RichPopup generatePopUpUI) {
        this.generatePopUpUI = generatePopUpUI;
    }

    public RichPopup getGeneratePopUpUI() {
        return generatePopUpUI;
    }

    public void generateMsgDialog(DialogEvent dialogEvent) {

        if (dialogEvent.getOutcome() != DialogEvent.Outcome.ok) {
            return;
        } else {
            String fileOption = null;
            String deactivateAll = null;
            String brokerId = null;
            String appDate = new SimpleDateFormat("dd-MMM-yyyy").format(getSysDate());
            DCIteratorBinding clientParameterIter = getIterator("ClientParameterVOIterator");
            ViewObject clientParameterVo = clientParameterIter.getViewObject();
            Row clientParameterRow = clientParameterVo.getCurrentRow();

            try {
                if (clientParameterRow != null) {
                    if (clientParameterRow.getAttribute("FileOption") != null &&
                        !(clientParameterRow.getAttribute("FileOption")
                                                                                                     .toString()
                                                                                                     .equalsIgnoreCase(""))) {
                        fileOption = clientParameterRow.getAttribute("FileOption").toString();
                    }
                    if (clientParameterRow.getAttribute("DeactivateAll") != null &&
                        !(clientParameterRow.getAttribute("DeactivateAll")
                                                                                                        .toString()
                                                                                                        .equalsIgnoreCase(""))) {
                        deactivateAll = clientParameterRow.getAttribute("DeactivateAll").toString();
                    }
                    if (clientParameterRow.getAttribute("BrokerId") != null)
                        brokerId = clientParameterRow.getAttribute("BrokerId").toString();
                }
                tmSqlExe.ftiClientLimitProcCall(fileOption, appDate, deactivateAll, brokerId);
                ApplicationInfo.getCurrentUserDBTransaction().commit();
                fct.addMessage("Complete Msg", new FacesMessage("Generate Client Data Successfully"));
                refreshVersion();
                executeLovIterator("VersionNo");

            } catch (Exception e) {
                ApplicationInfo.getCurrentUserDBTransaction().rollback();
                JSFUtils.addFacesErrorMessage(e.getMessage());
            }
        }
    }

    private boolean getVersinoNo(String viewCriteriaName, String criteriaItemName) {
        boolean found = true;
        if (viewCriteriaName != null) {
            ViewCriteria v = this.getViewCriteria(viewCriteriaName);
            if (v != null) {
                found = false;
                ViewCriteriaRow vcr = (ViewCriteriaRow) v.first();
                if (vcr != null) {
                    ViewCriteriaItem[] vcis = vcr.getCriteriaItemArray();
                    if (vcis != null && vcis.length > 0) {
                        for (int j = 0; j < vcis.length && !found; j++) {
                            ViewCriteriaItem vci = vcis[j];
                            if (vci != null && vci.getName().equals(criteriaItemName)) {
                                if (vci.getValue() != null)
                                    found = false;
                                else
                                    found = true;
                            }
                        }
                    }
                }
            }
        }
        return found;
    }


    public boolean getExecuteLovIteratorVersion() {

        String viewCriteriaName = "ClientLimitVOCriteria";
        String criteriaItemName = "VersionNo";
        boolean found = true;
        found = this.getVersinoNo(viewCriteriaName, criteriaItemName);
        return found;
    }

    private void generateInvBoardLimit(Row r, Element root, Integer purchasePowerCalc) {

        String marketType = null;
        String limitPercentage = null;
        String investorCode = null;

        if (r.getAttribute("BoardRefCode") != null && !(r.getAttribute("BoardRefCode")
                                                         .toString()
                                                         .equalsIgnoreCase(""))) {
            marketType = r.getAttribute("BoardRefCode").toString();
        }

        if (r.getAttribute("LimitPercentage") != null && !(r.getAttribute("LimitPercentage")
                                                            .toString()
                                                            .equalsIgnoreCase(""))) {
            limitPercentage = r.getAttribute("LimitPercentage").toString();
        }
        if (r.getAttribute("InvCode") != null && !(r.getAttribute("InvCode")
                                                    .toString()
                                                    .equalsIgnoreCase(""))) {
            investorCode = r.getAttribute("InvCode").toString();
        }

        Element withMarket = new Element("LimitsWithMarket");

        withMarket.setAttribute("Market", marketType);

        withMarket.addContent(new Element("ClientCode").setText(investorCode));
        if (r.getAttribute("MbankId") == null) {

            if (purchasePowerCalc > 0) {

                Long bLimit = this.getBoardBuyLimit(purchasePowerCalc, limitPercentage);
                String buylimit_set = null;
                if (bLimit == null || bLimit == 0)
                    buylimit_set = "0";
                else
                    buylimit_set = bLimit.toString();

                withMarket.addContent(new Element("MaxCapitalBuy").setText(buylimit_set));
            } else {
                withMarket.addContent(new Element("MaxCapitalBuy").setText("0"));
            }
        } else {
            if (r.getAttribute("MbankLimit") != null)
                withMarket.addContent(new Element("MaxCapitalBuy").setText(r.getAttribute("MbankLimit").toString()));
        }

        root.addContent(withMarket);
    }
}
