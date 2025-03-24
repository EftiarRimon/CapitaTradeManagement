package leads.capita.trade.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import javax.mail.internet.AddressException;


import javax.mail.internet.InternetAddress;

import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.jbo.Row;

import java.net.InetAddress;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import org.jdom2.JDOMException;


/*
 * Utility class for Apache FTP Client
 * Created By Main Uddin Patowary
 */
public class FTPUtils {
    private static String FTP_PROTOCOL = null;
    private static String IS_ACTIVE_MODE = null;

    private static String FTP_HOST = null;
    private static String FTP_USER = null;
    private static String FTP_USER_PASSWORD = null;
    private static String FTP_FILE_FOLDER = null;
    private static Integer FTP_DEFAULT_PORT = 21;
    private static boolean IS_PROCESS_CURRENT_DATE_FILE = true;
    private FacesContext fct;
    private static ResourceBundle messagebundle;

    public FTPUtils() {
        fct = JSFUtils.getFacesContext();
        messagebundle = JSFUtils.getResourceBundle("leads.capita.trade.view.TradeUIBundle");
    }

    public static synchronized boolean ftpConnect(FTPClient ftpclient, String host, String username, String password,
                                                  String isActiveMode) throws IOException, AddressException {
        System.out.println("CAPITA :: Logging in FTP..");
        try {

            //ftpclient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            //FTPClient ftpclientPassive= new FTPClient();
            //ftpclientPassive.setDefaultPort(getFTPDefaultPort());
            // ftpclientPassive.connect(host);
            // ftpclientPassive.enterRemotePassiveMode();
            ftpclient.setDefaultPort(getFTPDefaultPort());
            //ftpclient.setDefaultPort(ftpclient.ACTIVE_REMOTE_DATA_CONNECTION_MODE);
            ftpclient.connect(host);
            //System.out.println("port-- " + ftpclient.getPassivePort());
           // System.out.println("port1-- " + ftpclient.ACTIVE_REMOTE_DATA_CONNECTION_MODE);
            //System.out.println(getFTPDefaultPort() + "--host--" + InetAddress.getByName(ftpclient.getPassiveHost()));
            if (getIsActiveMode().equalsIgnoreCase("Y")) {
                ftpclient.enterRemoteActiveMode(InetAddress.getByName(ftpclient.getPassiveHost()),
                                                getFTPDefaultPort());
                System.out.println("Entered active mode..");
            }


            // ftpclient.enterLocalActiveMode();

            int reply = ftpclient.getReplyCode();
            //System.out.println(ftpclient.getReplyCode() + " --- " + FTPReply.isPositiveCompletion(reply));
            if (!FTPReply.isPositiveCompletion(reply)) {
                JSFUtils.addFacesErrorMessage("Unable To Connect To The Remote Machine!");
                System.out.println("Unable To Connect To The Remote Machine!");
                ftpclient.disconnect();
                return false;
            }
            if (!ftpclient.login(username, password)) {
                throw new IOException("Supplied wrong credentials to FTP Server");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("FTP Client is not able to Connect to host");
            throw new IOException("FTP Client is not able to Connect to host");
        }
        System.out.println("CAPITA :: FTP Login Successful..");
        return true;
    }


    public static synchronized boolean ftpConnect(FTPClient ftpclient) throws IOException {
        System.out.println("CAPITA :: Logging in FTP..");
        try {
            //ftpclient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            ftpclient.connect(getFTPHost());
            int reply = ftpclient.getReplyCode();
            //System.out.println(ftpclient.getReplyCode() + " --- " + FTPReply.isPositiveCompletion(reply));
            if (!FTPReply.isPositiveCompletion(reply)) {
                JSFUtils.addFacesErrorMessage("Unable To Connect To The Remote Machine!");
                System.out.println("Unable To Connect To The Remote Machine!");
                ftpclient.disconnect();
                return false;
            }
            if (!ftpclient.login(getFTPUser(), getFTPUserPassword())) {
                throw new IOException("Supplied wrong credentials to FTP Server");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("FTP Client is not able to Connect to host");
            throw new IOException("FTP Client is not able to Connect to host");
        }
        System.out.println("CAPITA :: FTP Login Successful..");
        return true;
    }


    public static void ftpDisConnect(FTPClient ftpclient) throws IOException {
        System.out.println("CAPITA :: FTP Logging out..");
        if (ftpclient != null && ftpclient.isConnected()) {
            try {
                ftpclient.logout();
                ftpclient.disconnect();
            } catch (IOException f) {
                f.printStackTrace();
            } catch (Exception f) {
                f.printStackTrace();
            }
        }
        System.out.println("CAPITA :: FTP Disconnected Successfully..");
    }


    public static boolean downloadFile(FTPClient ftpclient, String sourcePath,
                                       String destinationPath) throws IOException {
        System.out.println("CAPITA :: RemoteFile download starts ..FTP SOURCE " + sourcePath + " DESTINATION " +
                           destinationPath);
        FileOutputStream fos = null;
        boolean result = false;
        try {
            final int DEFAULT_BUFFER_SIZE = 1024;
            ftpclient.setBufferSize(DEFAULT_BUFFER_SIZE);
            ftpclient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
            File fDestination = new File(destinationPath);
            fos = new FileOutputStream(fDestination);
            result = ftpclient.retrieveFile(sourcePath, fos);
            boolean success = ftpclient.completePendingCommand();
            if (result && success) {
                System.out.println("CAPITA :: RemoteFile download Completed..FTP " + sourcePath);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("FTP is not able to Download the files from host");
            throw new IOException("FTP is not able to Download the files from host");
        } finally {
            if (fos != null)
                fos.close();
        }
        return result;
    }

    /**
     * @param ftpclient
     * @param sourcePath
     * @param destinationPath
     * @throws Exception
     */
    public static synchronized boolean uploadFile(FTPClient ftpclient, String sourcePath,
                                                  String destinationPath) throws Exception {
        FileInputStream fis = null;
        boolean issuccess = false;
        try {
            FTPClientConfig config = new FTPClientConfig();
            //ftpclient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            //ftpclient.setDefaultTimeout(10000);
            ftpclient.enterLocalPassiveMode();
            ftpclient.setFileType(FTPClient.BINARY_FILE_TYPE);
            //ftpclient.setFileTransferMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
            /* ftpclient.enterLocalPassiveMode(); */
            File srcFile = new File(sourcePath);
            fis = new FileInputStream(srcFile);
            if (destinationPath == null)
                issuccess =
                        ftpclient.storeFile(ftpclient.printWorkingDirectory() + File.separator + srcFile.getName(), fis);
            else {

                issuccess = ftpclient.storeFile(destinationPath + File.separator + srcFile.getName(), fis);
            }
            // boolean success = ftpclient.completePendingCommand();
            fis.close();
            issuccess = true;


        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("FTP is not able to upload the files from host");
            throw new IOException("FTP is not able to upload the files from host");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (fis != null)
                fis.close();
        }
        return issuccess;


    }

    public static void ftpErrorFileDownload(FTPClient ftpclient, String ftpSourcePath, String localDestinationPath,
                                            String fileName) throws Exception {
        //get output stream
        OutputStream output = null;
        try {

            ftpclient.enterLocalPassiveMode();
            String fileNameToLoad = null;
            //System.out.println("Remote system is " + ftpclient.getSystemType());
            //change current directory
            // ftpclient.changeWorkingDirectory(destinationPath);
            System.out.println(fileName + " Current directory is " + ftpclient.printWorkingDirectory());

            //get list of filenames
            List<FTPFile> errFileList = FTPUtils.getErrFileFromFtp(ftpclient);
            System.out.println("--- " + errFileList.size());

            if (errFileList != null && errFileList.size() > 0) {
                //loop thru files
                for (FTPFile file : errFileList) {
                    System.out.println("---- " + file.getName());
                    if (!file.isFile()) {
                        continue;
                    } else {
                        if (file.getName().equalsIgnoreCase(fileName)) {
                            fileNameToLoad = file.getName();
                            break;
                        }
                    }

                }

                System.out.println("File is " + fileNameToLoad);

                output = new FileOutputStream(new File(localDestinationPath + File.separator + fileNameToLoad));
                //get the file from the remote system
                ftpclient.retrieveFile((ftpclient.printWorkingDirectory() + File.separator + fileNameToLoad), output);
                //close output stream
                output.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (output != null)
                output.close();
        }
    }

    public static void ftpTradeFileDownload(FTPClient ftpclient, String ftpSourcePath, String localDestinationPath,
                                            String fileName) throws Exception {
        //get output stream
        OutputStream output = null;
        try {

            ftpclient.enterLocalPassiveMode();
            String fileNameToLoad = null;
            //System.out.println("Remote system is " + ftpclient.getSystemType());
            //change current directory
            // ftpclient.changeWorkingDirectory(destinationPath);
            System.out.println(fileName + " Current directory is " + ftpclient.printWorkingDirectory());

            //get list of filenames
            List<FTPFile> tradeFileList = FTPUtils.getTradesFileFromFtp(ftpclient);
            System.out.println("--- " + tradeFileList.size());

            if (tradeFileList != null && tradeFileList.size() > 0) {
                //loop thru files
                for (FTPFile file : tradeFileList) {
                    System.out.println("---- " + file.getName());
                    if (!file.isFile()) {
                        continue;
                    } else {
                        if (file.getName().equalsIgnoreCase(fileName)) {
                            fileNameToLoad = file.getName();
                            break;
                        }
                    }

                }

                System.out.println("File is " + fileNameToLoad);

                output = new FileOutputStream(new File(localDestinationPath + File.separator + fileNameToLoad));
                //get the file from the remote system
                ftpclient.retrieveFile((ftpclient.printWorkingDirectory() + File.separator + fileNameToLoad), output);
                //close output stream
                output.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (output != null)
                output.close();
        }
    }

    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                System.out.println("SERVER: Reply" + aReply);
            }
        }
    }

    boolean checkDirectoryExists(FTPClient ftpClient, String dirPath) throws IOException {
        ftpClient.changeWorkingDirectory(dirPath);
        int returnCode = ftpClient.getReplyCode();
        if (returnCode == 550) {
            return false;
        }
        return true;
    }

    boolean checkFileExists(FTPClient ftpClient, String filePath) throws IOException {
        InputStream inputStream = ftpClient.retrieveFileStream(filePath);
        int returnCode = ftpClient.getReplyCode();
        if (inputStream == null || returnCode == 550) {
            return false;
        }
        return true;
    }


    public static URLConnection getFtpByURLConnection(String userName, String hostName, String password,
                                                      String remoteFileFolder) {
        boolean connect = false;
        URLConnection remoteConnection = null;
        try {
            URL url = null;
            if (remoteFileFolder != null)
                url =
new URL((getFTPProtocol() == null ? FTP_PROTOCOL : getFTPProtocol()) + "://" + userName + ":" + password + "@" +
        hostName + "/" + remoteFileFolder + ";type=i");
            else
                url =
new URL((getFTPProtocol() == null ? FTP_PROTOCOL : getFTPProtocol()) + "://" + userName + ":" + password + "@" +
        hostName + ";type=i");
            remoteConnection = url.openConnection();
            connect = true;
        } catch (Exception ex) {
            throw new RuntimeException("Connection Problem " + ex);
        }
        return remoteConnection;
    }


    public static void uploadRemoteFtpFile(URLConnection remoteConnection, String sourceFilePath) {
        try {
            InputStream inputStream = new FileInputStream(new File(sourceFilePath));
            BufferedInputStream read = new BufferedInputStream(inputStream);
            OutputStream out = remoteConnection.getOutputStream();
            byte[] buffer = new byte[1024];
            int readCount = 0;
            while ((readCount = read.read(buffer)) > 0) {
                out.write(buffer, 0, readCount);
            }
            out.flush();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadRemoteFtpFile(URLConnection remoteConnection, String savePath) { //this is a function
        final int BUFFER_SIZE = 1024;
        long startTime = System.currentTimeMillis();

        try {
            InputStream inputStream = remoteConnection.getInputStream();
            long filesize = remoteConnection.getContentLength();
            //System.out.println("Size of the file to download in kb is:-" + filesize/1024 ) ;
            FileOutputStream outputStream = new FileOutputStream(savePath);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("File downloaded");
            System.out.println("Download time in sec. is:-" + (endTime - startTime) / 1000);
            outputStream.close();
            inputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static List<FTPFile> getErrFileFromFtp(FTPClient ftpclient) throws IOException {
        List<FTPFile> errFiles = null;
        FTPFile[] files = ftpclient.listFiles();
        if (files.length > 0) {
            errFiles = new ArrayList<FTPFile>();
            for (FTPFile f : files) {
                if (f.getName().contains("log") || f.getName().contains("Log") || f.getName().contains("LOG")) {
                    if (IS_PROCESS_CURRENT_DATE_FILE) {
                        String appdateDatePart = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();

                        String ftpFileDatePart = FlexTradeFileUtil.getFileNameDatePart(f.getName());
                        System.out.println(appdateDatePart + "-----" + ftpFileDatePart);
                        if (f.getName().startsWith(appdateDatePart) ||
                            appdateDatePart.trim().equalsIgnoreCase(ftpFileDatePart.trim()))
                            errFiles.add(f);
                    } else
                        errFiles.add(f);
                }
            }
        } else {
            JSFUtils.addFacesInformationMessage("No files found!");
        }
        return errFiles;
    }

    public static List<FTPFile> getTradesFileFromFtp(FTPClient ftpclient) throws IOException {
        List<FTPFile> tradeFiles = null;
        FTPFile[] files = ftpclient.listFiles();
        if (files.length > 0) {
            tradeFiles = new ArrayList<FTPFile>();
            for (FTPFile f : files) {
                if (f.getName().contains("Trade") || f.getName().contains("trade") || f.getName().contains("TRADE") ||
                    f.getName().contains("Ticker") || f.getName().contains("TICKER") ||
                    f.getName().contains("ticker")) {
                    if (IS_PROCESS_CURRENT_DATE_FILE) {
                        String appdateDatePart = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();
                        String ftpFileDatePart = FlexTradeFileUtil.getFileNameDatePart(f.getName());
                        if (f.getName().startsWith(appdateDatePart) ||
                            appdateDatePart.trim().equalsIgnoreCase(ftpFileDatePart.trim()))
                            tradeFiles.add(f);
                    } else
                        tradeFiles.add(f);
                }
            }
        } else {
            JSFUtils.addFacesInformationMessage("No files found!");
        }
        return tradeFiles;
    }


    public static List<FTPFile> getPositionFileFromFtp(FTPClient ftpclient) throws IOException {
        List<FTPFile> positionFiles = null;
        FTPFile[] files = ftpclient.listFiles();
        if (files.length > 0) {
            positionFiles = new ArrayList<FTPFile>();
            for (FTPFile f : files) {
                if (f.getName().contains("Position") || f.getName().contains("position") ||
                    f.getName().contains("POSITION")) {
                    if (IS_PROCESS_CURRENT_DATE_FILE) {
                        String appdateDatePart = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();
                        String ftpFileDatePart = FlexTradeFileUtil.getFileNameDatePart(f.getName());
                        if (f.getName().startsWith(appdateDatePart) ||
                            appdateDatePart.trim().equalsIgnoreCase(ftpFileDatePart.trim()))
                            positionFiles.add(f);
                    } else
                        positionFiles.add(f);
                }
            }
        } else {
            JSFUtils.addFacesInformationMessage("No files found!");
        }
        return positionFiles;
    }

    public static List<FTPFile> getClientFileFromFtp(FTPClient ftpclient) throws IOException {
        List<FTPFile> clientFiles = null;
        FTPFile[] files = ftpclient.listFiles();
        if (files.length > 0) {
            clientFiles = new ArrayList<FTPFile>();
            for (FTPFile f : files) {
                if (f.getName().contains("Clients") || f.getName().contains("clients") ||
                    f.getName().contains("CLIENTS")) {
                    if (IS_PROCESS_CURRENT_DATE_FILE) {
                        String appdateDatePart = FlexTradeFileUtil.getApplicationDateStringyyyyMMdd();
                        String ftpFileDatePart = FlexTradeFileUtil.getFileNameDatePart(f.getName());
                        if (f.getName().startsWith(appdateDatePart) ||
                            appdateDatePart.trim().equalsIgnoreCase(ftpFileDatePart.trim()))
                            clientFiles.add(f);
                    } else
                        clientFiles.add(f);
                }
            }
        } else {
            JSFUtils.addFacesInformationMessage("No files found!");
        }
        return clientFiles;
    }


    public static String _getFtpConfigAttrValueFromIter(String attrName) {
        Row curRow = null;
        String attrVale = null;
        String ftpConfigVoIterName;
        try {
            if (messagebundle != null) {
                ftpConfigVoIterName = messagebundle.getString("leads_capita_ftp_congig_iter_name");
                System.out.println("--*** -- " + ftpConfigVoIterName);
            } else
                ftpConfigVoIterName = "FtpConfigViewIterator";
            DCIteratorBinding iterBinding = ADFUtils.findIterator(ftpConfigVoIterName);
            System.out.println("---- " + ftpConfigVoIterName);
            if (iterBinding != null && iterBinding.getEstimatedRowCount() > 0) {
                curRow = iterBinding.getCurrentRow();
                if (curRow == null)
                    curRow = iterBinding.getViewObject().first();
                if (curRow.getAttribute(attrName) != null)
                    attrVale = curRow.getAttribute(attrName).toString();
            } else {
                System.out.println("FTP Config iter. binding name should be FtpConfigViewIterator");
                JSFUtils.addFacesErrorMessage("Iterator Binding Not Found For FTP Config!!");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attrVale;
    }

    public static String getFTPUser() {
        if (FTP_USER == null || FTP_USER.equals(""))
            FTP_USER = _getFtpConfigAttrValueFromIter("UserName");
        return FTP_USER;
    }

    public static String getFTPUserPassword() {
        if (FTP_USER_PASSWORD == null || FTP_USER_PASSWORD.equals(""))
            FTP_USER_PASSWORD = _getFtpConfigAttrValueFromIter("UserPassword");
        return FTP_USER_PASSWORD;
    }

    public static String getFTPFolePath() {
        if (FTP_FILE_FOLDER == null || FTP_FILE_FOLDER.equals(""))
            FTP_FILE_FOLDER = _getFtpConfigAttrValueFromIter("FilePath");
        return FTP_FILE_FOLDER;
    }

    public static String getFTPHost() {
        if (FTP_HOST == null || FTP_HOST.equals(""))
            FTP_HOST = _getFtpConfigAttrValueFromIter("Host");
        return FTP_HOST;
    }

    public static String getFTPProtocol() {
        if (FTP_PROTOCOL == null || FTP_PROTOCOL.equals(""))
            FTP_PROTOCOL = _getFtpConfigAttrValueFromIter("FtpProtocol");
        return FTP_PROTOCOL;
    }

    public static String getIsActiveMode() {
        if (IS_ACTIVE_MODE == null || IS_ACTIVE_MODE.equals(""))
            IS_ACTIVE_MODE = _getFtpConfigAttrValueFromIter("IsActive");
        return IS_ACTIVE_MODE;
    }

    public static Integer getFTPDefaultPort() {
        Integer retVal = null;
        try {
            if (_getFtpConfigAttrValueFromIter("Port") != null)
                FTP_DEFAULT_PORT = Integer.valueOf(_getFtpConfigAttrValueFromIter("Port"));
            else
                return FTP_DEFAULT_PORT;
            System.out.println("---FTP default port-- " + FTP_DEFAULT_PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return FTP_DEFAULT_PORT;
    }


    /*
     * 192.168.20.73
     * user: ftpuser
     * pass: Open123
     *
     */

    public static void main(String[] args) throws JDOMException, IOException, AddressException {
        //System.out.println("*** " + getStringWithoutCDATA("<![CDATA[QSMDRYCELL]]>"));
        FTPClient ftpclient = new FTPClient();
        ftpConnect(ftpclient, "192.168.20.107", "mohin", "mainuddin", "Y");
        // FTPFile[] files = ftpclient.listFiles();
        //List<FTPFile> files=FTPUtils.getPositionFileFromFtp(ftpclient);
        List<FTPFile> files = FTPUtils.getClientFileFromFtp(ftpclient);
        //List<FTPFile> files = getErrFileFromFtp(ftpclient);
        for (FTPFile file : files) {
            System.out.println(file.getSize() / 1000 + "--^^^^ - " + file.getName());
        }
        ftpDisConnect(ftpclient);

    }
}
