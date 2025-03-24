package leads.capita.trade.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Properties;

public class OperationExecutionUtil {
    
    /* config.properties file path for client
        \\WLS_APPS\\user_projects\\domains\\base_domain_broker

    config.properties file path for development pc
        C:\\Users\\uaer_name\\AppData\\Roaming\\JDeveloper\\system11.1.2.1.38.60.81\\DefaultDomain
    */
    
    private static final String OPERATION_PROP_FILE_NAME = "config.properties";
    private static final String LEADS_CAPITA_OPERATION_EXECUTION = "leads_capita_operation_execution";
    private static final String LEADS_CAPITA_OPERATION_EXECUTION_LOGIN_MESSAGE = "leads_capita_operation_execution_login_message";
    
    public OperationExecutionUtil() {
        super();
    }
    
    public void setOperationExecutionMessage(String isExecuted, String executionMessage, String headerMesage) throws Exception{
        Properties properties = new Properties();
        OutputStream outPutStrm = null;
        FileInputStream inputStrm = null;
        File file = null;
        try {
            file = new File(OPERATION_PROP_FILE_NAME);
            if(!file.exists())
                file.createNewFile();
            inputStrm = new FileInputStream(file);
            properties.load(inputStrm);
            properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION, isExecuted);
            properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION_LOGIN_MESSAGE,
                                   executionMessage);
            outPutStrm = new FileOutputStream(file);
            properties.save(outPutStrm, headerMesage);
        } catch (IOException ex) {
            System.out.println("Exception from process.." + ex.getMessage());
            inputStrm = new FileInputStream(file);
            properties.load(inputStrm);
            properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION, "false");
            properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION_LOGIN_MESSAGE,
                                   "null");
            outPutStrm = new FileOutputStream(file);
            properties.save(outPutStrm, "Exception Occur!");
            if (outPutStrm != null) {
                outPutStrm.close();
            }
        } catch (Exception e) {
            System.out.println("Exception from process.." + e.getMessage());
            inputStrm = new FileInputStream(file);
            properties.load(inputStrm);
            properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION, "false");
            properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION_LOGIN_MESSAGE,
                                   "null");
            outPutStrm = new FileOutputStream(file);
            properties.save(outPutStrm, "Exception Occur");
            if (outPutStrm != null) {
                outPutStrm.close();
            }
        } finally {
            if (outPutStrm != null) {
                outPutStrm.close();
            }
        }  
    }
    
    public String getOperationExecutionMessage(String keyParam) throws Exception{
        Properties properties = new Properties();
        FileInputStream inputStrm = null;
        File file = null;
        try {
            file = new File(OPERATION_PROP_FILE_NAME);
            
            if(!file.exists()){
                file.createNewFile();
                inputStrm = new FileInputStream(file);
                properties.load(inputStrm);
                OutputStream outPutStrm = null;
                properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION, "false");
                properties.setProperty(LEADS_CAPITA_OPERATION_EXECUTION_LOGIN_MESSAGE,
                                       "null");
                outPutStrm = new FileOutputStream(file);
                properties.save(outPutStrm, "New File Created!"); 
            }else{
                inputStrm = new FileInputStream(file);
                properties.load(inputStrm);
            }
            
            
        } catch (IOException ex) {
            System.out.println("Exception from process.." + ex.getMessage());
            if (inputStrm != null) {
                inputStrm.close();
            }
        } catch (Exception e) {
            System.out.println("Exception from process.." + e.getMessage());
            if (inputStrm != null) {
                inputStrm.close();
            }
        } finally {
            if (inputStrm != null) {
                inputStrm.close();
            }
        }  
        return properties.getProperty(keyParam);
    }
}
