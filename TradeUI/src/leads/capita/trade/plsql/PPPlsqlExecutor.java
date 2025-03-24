package leads.capita.trade.plsql;

import java.io.InputStream;

import java.math.BigDecimal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import java.sql.Types;

import leads.capita.common.am.CapitaDBServiceImpl;
import leads.capita.common.plsql.PlsqlExecutor;
import leads.capita.common.ui.util.JSFUtils;


public class PPPlsqlExecutor extends PlsqlExecutor {
    private static final String COMMON_SESSION_DB_SERVICE = "CommonSessionDbService";

    public PPPlsqlExecutor() {
        super();
    }

    public void tradeFileLoad(String fileType, String fileName, String fileDate, InputStream content) {
        //getDbService().callStoredProcedure("pkg_trade.trade_file_load(?,?,?,?)", new Object[] {fileType, fileName, fileDate, content }) ;
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_trade.trade_file_load(?,?,?,?); end;",
                                                                      0);
        try {
            callableStatement.setObject(1, fileType);
            callableStatement.setObject(2, fileName);
            callableStatement.setObject(3, fileDate);
            callableStatement.setAsciiStream(4, content);
            callableStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void priceFileLoad(String fileType, String fileName, String fileDate, InputStream content) {
        //getDbService().callStoredProcedure("pkg_trade.trade_file_load(?,?,?,?)", new Object[] {fileType, fileName, fileDate, content }) ;
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_trade.trade_file_load(?,?,?,?); end;",
                                                                      0);
        try {
            callableStatement.setObject(1, fileType);
            callableStatement.setObject(2, fileName);
            callableStatement.setObject(3, fileDate);
            callableStatement.setAsciiStream(4, content);
            callableStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public boolean processEOD(String branchCode, String userId) {
        boolean isProcessed = false;
        CallableStatement callableStatement =
            getSessionCommonDbService().getDBTransaction().createCallableStatement("begin pkg_eod.eod(?,?); end;", 0);
        try {
            callableStatement.setObject(1, branchCode);
            callableStatement.setObject(2, userId);
            callableStatement.execute();
            isProcessed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isProcessed;
    }
    
//    public void processYearEnd(String executionDate, int branchId) throws Exception {
//
//        getDbService().callStoredProcedure("begin pkg_eod.day_end" + "(?,?); end ",
//                                           new Object[] { executionDate, branchId }, false);
//
//        /*
//        CallableStatement callableStatement =
//            getDbService().getDBTransaction().createCallableStatement("begin pkg_eod.day_end(?,?,?); end;",0);
//            callableStatement.setObject(1, executionDate);
//            callableStatement.setObject(2, branchId);
//            callableStatement.setObject(3, sessionId);
//            callableStatement.execute();
//    */
//
//    }
    
    public boolean processYearEnd(String yDate, int branchID) throws Exception  {
        boolean isProcessed = false;
        
        //System.out.println("From Insilde "+yDate);
        //System.out.println("From Insilde "+branchID);
        
        CallableStatement callableStatement =
            getSessionCommonDbService().getDBTransaction().createCallableStatement("begin PKG_YEAR_END.year_end(?,?); end;", 0);
        //System.out.println("From Insilde 1");
        try {
            callableStatement.setObject(1,yDate);
            callableStatement.setObject(2, branchID);
          //  System.out.println("From Insilde 2");
          //  System.out.println(callableStatement.toString());
            callableStatement.execute();
           // System.out.println("From Insilde 3");
            isProcessed = true;
        } catch (Exception e) {
            throw e;
        }
        return isProcessed;
    }
    
    public boolean processYearEndReverse(String yDate, int branchID) {
        boolean isProcessed = false;
        
      //  System.out.println("From Insilde "+yDate);
      //  System.out.println("From Insilde "+branchID);
        
        CallableStatement callableStatement =
            getSessionCommonDbService().getDBTransaction().createCallableStatement("begin PKG_YEAR_END.reverse_year_end(?); end;", 0);
       // System.out.println("From Insilde 1");
        try {
            callableStatement.setObject(1,yDate);
            //callableStatement.setObject(2, branchID);
          //  System.out.println("From Insilde 2");
         //   System.out.println(callableStatement.toString());
            callableStatement.execute();
           // System.out.println("From Insilde 3");
            isProcessed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isProcessed;
    }
    public boolean processPurchasePowerUpdate(String accountId) throws SQLException {
        boolean isProcessed = false;
        CallableStatement callableStatement = null;
        try {
            callableStatement =
                getSessionCommonDbService().getDBTransaction().createCallableStatement("begin pkg_portfolio.update_purchase_power(?); end;",
                                                                          0);
            callableStatement.setObject(1, accountId);
            callableStatement.execute();
            isProcessed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            if(callableStatement !=null){
                callableStatement.close();
            }
        }
        return isProcessed;
    }


    public CapitaDBServiceImpl getSessionCommonDbService() {
        CapitaDBServiceImpl dbService = null;
        if (JSFUtils.getFromSession(COMMON_SESSION_DB_SERVICE) != null)
            dbService = (CapitaDBServiceImpl)JSFUtils.getFromSession(COMMON_SESSION_DB_SERVICE);
        else {
            dbService = getDbService();
            JSFUtils.storeOnSession(COMMON_SESSION_DB_SERVICE, dbService);
        }
        return dbService;
    }
    
    public String getDepositoryCode(String var) {
        String returnVal = null;
        try {
            Object[] params = { var};
            String packg_function = "pkg_capita_sys.get_depository_code(?)";
            returnVal =(String) getSessionCommonDbService().callStoredFunction(Types.VARCHAR, packg_function, params, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return returnVal;
    }

}
