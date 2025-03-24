package leads.capita.trade.file;

import java.io.InputStream;

import java.math.BigDecimal;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.plsql.TMPlsqlExecutor;

public class DataDump {
    public DataDump() {
        super();
    }

    public void dumpMSAData(int exchangeId, String fileName, String fileDate, InputStream content) {
        new TMPlsqlExecutor().tradeFileLoad(exchangeId, "MSA", fileName, fileDate, content);
    }

    public void dumpBTData(int exchangeId, String fileName, String fileDate, InputStream content) {
        new TMPlsqlExecutor().tradeFileLoad(exchangeId, "BT", fileName, fileDate, content);
    }

    public void dumpMSAPlusData(int exchangeId, String fileName, String fileDate, InputStream content) {
        new TMPlsqlExecutor().tradeFileLoad(exchangeId, "MSAPLUS", fileName, fileDate, content);
    }

    public void dumpPriceData(int exchangeId, String fileName, String fileDate, InputStream content) {
        new TMPlsqlExecutor().tradeFileLoad(exchangeId, "PRICE_DSE_REPORT", fileName, fileDate, content);
    }

    public boolean dumpFlexPriceData(String fileId) {
        boolean isCalled = false;
        try {
            new TMPlsqlExecutor().dumpEODTickerToPrice(fileId);
            isCalled = true;
        } catch (Exception e) {
            // ApplicationInfo.getCurrentUserDBTransaction().executeCommand("delete from eod_ticker t where t.file_id =" + fileId);
            //ApplicationInfo.getCurrentUserDBTransaction().commit();
            e.printStackTrace();
            JSFUtils.addFacesErrorMessage("Process Error ! ");
            isCalled = false;
        }
        return isCalled;
    }

    public boolean dumpFlexTradeData(String fileId) throws Exception {
        boolean isCalled = false;
        String msg = null;
        try {
            new TMPlsqlExecutor().dumpFlexTradeToMSA(fileId);
            isCalled = true;
        } catch (Exception e) {
            /*  ApplicationInfo.getCurrentUserDBTransaction().executeCommand("delete from flex_trade_data t where t.file_id =" + fileId);
            ApplicationInfo.getCurrentUserDBTransaction().commit();*/
            e.printStackTrace();
            if (e.getMessage() != null) {
                msg = e.getMessage().substring(e.getMessage().indexOf("<") + 1, e.getMessage().indexOf(">"));
                JSFUtils.addFacesErrorMessage(msg);
                //if duplicate file name found on tha same date then remove data from flex_trade_data
                if (msg.equalsIgnoreCase("Duplicate data or file name found!!")) {
                     ApplicationInfo.getCurrentUserDBTransaction().executeCommand("delete from flex_trade_data t where t.file_id =" +
                                                                                 fileId);
                    ApplicationInfo.getCurrentUserDBTransaction().commit(); 
                }

                isCalled = false;
            }
            throw e;
        }
        return isCalled;
    }

    public boolean dumpMergeCashFileData(String fileId, String fileFormat, String fileOption,
                                         String extOrg) throws Exception {
        boolean isCalled = false;
        try {
            new TMPlsqlExecutor().dumpMrgCashFileData(fileId, fileFormat, fileOption, extOrg);
            isCalled = true;
        } catch (Exception e) {
            e.printStackTrace();
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesErrorMessage("Process Error ! Transaction Rolled Back!");
            isCalled = false;
            throw e;
        }
        return isCalled;
    }

    public boolean dumpMergeShareFileData(String fileId, String fileFormat, String fileOption,
                                          String extOrg) throws Exception {
        boolean isCalled = false;
        try {
            new TMPlsqlExecutor().dumpMrgShareFileData(fileId, fileFormat, fileOption, extOrg);
            isCalled = true;
        } catch (Exception e) {
            e.printStackTrace();
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesErrorMessage("Process Error !! Transaction Rolled Back!!");
            isCalled = false;
            throw e;
        }
        return isCalled;
    }

    public boolean dumpMergeCashFileDataDealer(String fileId, String fileFormat, String fileOption,
                                               String extOrg) throws Exception {
        boolean isCalled = false;
        try {
            new TMPlsqlExecutor().dumpMrgCashFileDataDealer(fileId, fileFormat, fileOption, extOrg);
            isCalled = true;
        } catch (Exception e) {
            e.printStackTrace();
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesErrorMessage("Process Error ! Transaction Rolled Back!");
            isCalled = false;
            throw e;
        }
        return isCalled;
    }

    public boolean dumpMergeShareFileDataDealer(String fileId, String fileFormat, String fileOption,
                                                String extOrg) throws Exception {
        boolean isCalled = false;
        try {
            new TMPlsqlExecutor().dumpMrgShareFileDataDealer(fileId, fileFormat, fileOption, extOrg);
            isCalled = true;
        } catch (Exception e) {
            e.printStackTrace();
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesErrorMessage("Process Error !! Transaction Rolled Back!!");
            isCalled = false;
            throw e;
        }
        return isCalled;
    }
    
    public int validTradeFile(){        
        BigDecimal value = new TMPlsqlExecutor().tradeFileValidation("pkg_trade_file.get_price_file_process_value()");
        int returnValue = value.intValueExact();
        return returnValue;
    }
    

}
