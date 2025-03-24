package leads.capita.trade.view.backing;

/*Created by : Ipsheta Saha */

import java.io.File;
import java.io.IOException;

import java.math.BigDecimal;

import java.sql.SQLException;

import java.text.ParseException;

import java.util.Date;
import java.util.List;

import java.util.Map;

import javax.faces.event.ValueChangeEvent;

import javax.mail.internet.AddressException;

import javax.naming.NamingException;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.ADFUtils;
import leads.capita.common.ui.JSFUtils;
import leads.capita.trade.exception.TradeFileDateMismatchException;

import leads.capita.trade.file.FTPUtils;
import leads.capita.trade.file.FlexTradeFileUtil;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;

import oracle.binding.OperationBinding;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import oracle.sql.DATE;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.myfaces.trinidad.model.UploadedFile;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class FlexErrorFileBean {

    private UploadedFile _file;
    private String currentFilePath = null;

    public FlexErrorFileBean() {
    }

    public void fileSelectValChangeListener(ValueChangeEvent valueChangeEvent) {
        _file = (UploadedFile)valueChangeEvent.getNewValue();
        this.setFile(_file);
        if (!_file.getContentType().equals("text/xml")) {
            _file = null;
            System.out.println("No file has selected!");
        }
    }

    public String doUploadErrFile() throws IOException, NamingException, TradeFileDateMismatchException, SQLException,
                                           ParseException, Exception {
        UploadedFile file = this.getFile();
        String fileName = null;
        if (file != null) {
            fileName = file.getFilename();
        } else if (file == null || file.getLength() <= 0) {
            JSFUtils.addFacesErrorMessage("There is an error on uploading the file. Please make sure your file is a valid one.");
            return null;
        }

        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Error");
        FlexTradeFileUtil.fileUploadAndSave(destFolderPath, file.getFilename(), file);
        currentFilePath = destFolderPath + File.separator + file.getFilename();
        File newGenFile = new File(currentFilePath);
        // System.out.println("---" + currentFilePath);
        if (!newGenFile.exists() || newGenFile.length() < 1) {
            JSFUtils.addFacesErrorMessage(" File upload fail!!");
            return null;
        }
        this.dumpDataFromErrorLogFile(newGenFile, fileName, null);

        return null;
    }

    public String loadErrFileFromFTP() throws IOException, AddressException {
        int ctr = 0;
        //192.168.20.107
        //mohin
        //mainuddin
        FTPClient ftpClient = new FTPClient();
        if (!ftpClient.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            //System.out.println(ftpHost + " --- " + ftpUser + " " + ftpPass + " ** " + ftpProtocol);
            FTPUtils.ftpConnect(ftpClient, ftpHost, ftpUser, ftpPass, isActiveMode);
        }
        List<FTPFile> errFileList = FTPUtils.getErrFileFromFtp(ftpClient);

        DCIteratorBinding errFilesIter = ADFUtils.findIterator("FtiErrFilesVOIterator");
        ViewObject errFilesVO = errFilesIter.getViewObject();
        if (errFilesVO != null) {
            Map<String, String> existingFiles =
                FlexTradeFileUtil.getVOAttrValueInMap("FtiErrFilesVOIterator", "FileName");
            for (FTPFile fList : errFileList) { //file once loaded will not be loaded again
                if (!existingFiles.containsKey(fList.getName())) {
                    Row errFilesRow = errFilesVO.createRow();
                    errFilesRow.setAttribute("FileName", fList.getName());
                    errFilesRow.setAttribute("FileType", "Error");
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
                    FTPUtils.ftpDisConnect(ftpClient);
                } else {
                    System.out.println("--22---" + operationCommit.getErrors());
                    JSFUtils.addFacesErrorMessage("Problem in Loading Files");
                    JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
                FTPUtils.ftpDisConnect(ftpClient);
                e.getMessage();
            }

        } else if (ctr == 0) {
            JSFUtils.addFacesInformationMessage("No File(s) to Load from FTP...");
            FTPUtils.ftpDisConnect(ftpClient);
        }
        return null;
    }

    public String uploadErrFile() throws IOException, ParseException, Exception {
        boolean isDumpOk = false;
        String filename = null;
        DCIteratorBinding errFilesIter = ADFUtils.findIterator("FtiErrFilesVOIterator");
        Row errFileRow = errFilesIter.getCurrentRow();
        if (errFileRow.getAttribute("FileName") != null) {
            filename = errFileRow.getAttribute("FileName").toString();
        }
        FTPClient ftpclient = new FTPClient();
        String destFolderPath = FlexTradeFileUtil.getFlexTradeFileFolder("Error");
        if (!ftpclient.isConnected()) {
            String ftpUser = FTPUtils.getFTPUser();
            String ftpHost = FTPUtils.getFTPHost();
            String ftpPass = FTPUtils.getFTPUserPassword();
            String isActiveMode = FTPUtils.getIsActiveMode();
            String ftpProtocol = FTPUtils.getFTPProtocol();
            FTPUtils.ftpConnect(ftpclient, ftpHost, ftpUser, ftpPass, isActiveMode);
        }
        FTPUtils.ftpErrorFileDownload(ftpclient, null, destFolderPath, filename);
        isDumpOk = dumpErrorFileData(destFolderPath + File.separator + filename, errFileRow);
        //System.out.println("file location----"+destFolderPath + File.separator + filename);
        if (isDumpOk) {
            ftpclient.disconnect();
        } else {
            ftpclient.disconnect();
        }
        return null;
    }

    public boolean dumpErrorFileData(String srcFilePath, Row curErrFileRow) {
        boolean dumpOK = false;
        File newGenFile = new File(srcFilePath);
        String filename = newGenFile.getName();
        dumpOK = this.dumpDataFromErrorLogFile(newGenFile, filename, curErrFileRow);
        return dumpOK;
    }

    public boolean dumpDataFromErrorLogFile(File newErrorFile, String fileName, Row ftpErrFileRow) {
        boolean dumpOk = false;
        long parentSeqId = 0l;
        boolean validSum = false;
        boolean validDetail = false;
        String fileVersion = null;
        String fileGenTime = FlexTradeFileUtil.getCurSystemTime(null);
        DCIteratorBinding errSumIter = ADFUtils.findIterator("FtiErrSummaryVOIterator");
        DCIteratorBinding errDetailsIter = ADFUtils.findIterator("FtiErrSummaryDetailsVOIterator");
        SAXBuilder saxbuilder = new SAXBuilder();
        DCIteratorBinding fileTypeLOVIter = ADFUtils.findIterator("FlexFileTypeVOIterator");

        if (fileTypeLOVIter.getCurrentRow().getAttribute("FileVersion") != null) {
            fileVersion = fileTypeLOVIter.getCurrentRow().getAttribute("FileVersion").toString();
            System.out.println("version----" + fileVersion);
        } else {
            System.out.println("version else----");
        }

        try {
            Document document = (Document)saxbuilder.build(newErrorFile);
            Element rootNode = document.getRootElement();
            ViewObject sumVO = errSumIter.getViewObject();
            ViewObject detailVO = errDetailsIter.getViewObject();
            List<Element> rootChilds = rootNode.getChildren();
            for (Element e : rootChilds) {
                if (e.getName().equalsIgnoreCase("Summary")) {
                    Row errSumRow = sumVO.createRow();
                    List<Element> list = e.getChildren();
                    for (int i = 0; i < list.size(); i++) {
                        Element node = (Element)list.get(i);
                        if (node.getName().equalsIgnoreCase("FileType")) {
                            errSumRow.setAttribute("FileType", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("FileStatus")) {
                            errSumRow.setAttribute("FileStatus", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("ProcessingMode")) {
                            errSumRow.setAttribute("ProcessingMode", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("DateProcessed")) {
                            errSumRow.setAttribute("DateProcessed", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("TotalDuration")) {
                            errSumRow.setAttribute("TotalDuration", node.getText() == null ? null : node.getText());
                        } else if (node.getName().equalsIgnoreCase("Record")) {
                            errSumRow.setAttribute("NoOfRecord", node.getText() == null ? 0 : node.getText());
                        } else if (node.getName().equalsIgnoreCase("Successful")) {
                            errSumRow.setAttribute("SuccessfulRecord", node.getText() == null ? 0 : node.getText());
                        } else if (node.getName().equalsIgnoreCase("Wrong")) {
                            errSumRow.setAttribute("Wrong", node.getText() == null ? 0 : node.getText());
                        }
                        errSumRow.setAttribute("FileName", fileName);
                        errSumRow.setAttribute("FileGenTime", fileGenTime);
                        errSumRow.setAttribute("FileVersion", fileVersion);
                    }
                    OperationBinding operationCommit = null;
                    operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
                    if (operationCommit != null) {
                        operationCommit.execute();
                        if (operationCommit.getErrors().isEmpty()) {
                            errSumRow.refresh(Row.REFRESH_CONTAINEES);
                            errSumIter.prepareForInput();
                            System.out.println(errSumRow.getAttribute("RowSeqId") + "--err summary saved---");
                            if (errSumRow.getAttribute("RowSeqId") != null) {
                                parentSeqId = Long.valueOf(errSumRow.getAttribute("RowSeqId").toString());
                                //System.out.println("seqid---"+parentSeqId);
                            }
                            validSum = true;
                        } else {
                            System.out.println("--00---" + operationCommit.getErrors());
                            JSFUtils.addFacesErrorMessage("Problem in Loading Error File");
                            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        }
                    }
                } else if (e.getName().equalsIgnoreCase("ErrorLog")) {
                    List<Element> errChilds = e.getChildren(); //Data_Error
                    for (Element err : errChilds) {
                        Row errDetailRow = detailVO.createRow(); //create a row for each Data_Error
                        List<Attribute> attrs = err.getAttributes();
                        for (Attribute attr : attrs) {
                            errDetailRow.setAttribute("SumSeqId", parentSeqId);
                            if (attr.getName().equalsIgnoreCase("errCode")) {
                                errDetailRow.setAttribute("ErrCode", attr.getValue() == null ? null : attr.getValue());
                            } else if (attr.getName().equalsIgnoreCase("lineNum")) {
                                errDetailRow.setAttribute("LineNum", attr.getValue() == null ? 0 : attr.getValue());
                            } else if (attr.getName().equalsIgnoreCase("colNum")) {
                                errDetailRow.setAttribute("ColNum", attr.getValue() == null ? 0 : attr.getValue());
                            } else if (attr.getName().equalsIgnoreCase("errText")) {
                                errDetailRow.setAttribute("ErrText", attr.getValue() == null ? 0 : attr.getValue());
                            }
                            errDetailRow.setAttribute("XmlTag",
                                                      err.getName() == null ? null : err.getName()); //save tag name(data_error)
                        }
                    }
                    if (ftpErrFileRow != null) {
                        ftpErrFileRow.setAttribute("Status",
                                                   "P"); //set Status ='P' when respective Error file is loaded..
                        dumpOk = true;
                    } else {
                        dumpOk = true;
                    }
                    OperationBinding operationCommit = null;
                    operationCommit = JSFUtils.getBindings().getOperationBinding("Commit");
                    if (operationCommit != null) {
                        operationCommit.execute();
                        if (operationCommit.getErrors().isEmpty()) {
                            System.out.println("--err details saved---");
                            validDetail = true;
                        } else {
                            System.out.println("--11---" + operationCommit.getErrors());
                            JSFUtils.addFacesErrorMessage("Problem in Loading Error File");
                            JSFUtils.getBindings().getOperationBinding("Rollback").execute();
                        }
                    }
                }
            }
            if (validSum & validDetail & dumpOk) {
                JSFUtils.addFacesInformationMessage("File Loaded Successfully..");
                this.refreshIterator("FtiErrSummaryVOIterator");
                this.refreshIterator("FtiErrSummaryDetailsVOIterator");
            } else {
                JSFUtils.addFacesErrorMessage("File Upload Failed");
            }

        } catch (IOException io) {
            io.printStackTrace();
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            jdomex.printStackTrace();
            System.out.println(jdomex.getMessage());
        }
        return dumpOk;
    }

    public void refreshIterator(String iteratorName) {
        if (iteratorName != null) {
            ADFUtils.findIterator(iteratorName).getViewObject().clearCache();
            ADFUtils.findIterator(iteratorName).executeQuery();
            ADFUtils.findIterator(iteratorName).refreshIfNeeded();
        }
        return;
    }

    public void setFile(UploadedFile _file) {
        this._file = _file;
    }

    public UploadedFile getFile() {
        return _file;
    }

}
