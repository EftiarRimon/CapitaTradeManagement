package leads.capita.trade.plsql;

import java.io.InputStream;

import java.math.BigDecimal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import java.util.Date;

import leads.capita.common.am.CapitaDBServiceImpl;
import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.plsql.PlsqlExecutor;
import leads.capita.common.ui.util.JSFUtils;

import oracle.jbo.JboException;

import oracle.jdbc.OracleTypes;


public class TMPlsqlExecutor extends PlsqlExecutor {
    private static final String COMMON_SESSION_DB_SERVICE = "CommonSessionDbService";

    public TMPlsqlExecutor() {
        super();
    }

    public Date getAppDate(int branchId) {
        int returnType = 1;

        try {

            CallableStatement callableStatement =
                getDbService().getDBTransaction().createCallableStatement("begin ? :=pkg_capita_sys.get_current_system_date(?); end;",
                                                                          0);
            callableStatement.registerOutParameter(1, Types.DATE);
            callableStatement.setObject(2, branchId);
            callableStatement.execute();

            return (Date)callableStatement.getObject(1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public void tradeFileLoad(int exchangeId, String fileType, String fileName, String fileDate, InputStream content) {
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_trade.trade_file_load(?,?,?,?,?); end;",
                                                                      0);
        try {
            callableStatement.setObject(1, exchangeId);
            callableStatement.setObject(2, fileType);
            callableStatement.setObject(3, fileName);
            callableStatement.setObject(4, fileDate);
            callableStatement.setAsciiStream(5, content);
            callableStatement.execute();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public void priceFileLoad(int exchangeId, String fileType, String fileName, String fileDate, InputStream content) {

        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_trade_file.trade_file_load(?,?,?,?,?); end;",
                                                                      0);
        try {
            callableStatement.setObject(1, exchangeId);
            callableStatement.setObject(2, fileType);
            callableStatement.setObject(3, fileName);
            callableStatement.setObject(4, fileDate);
            callableStatement.setAsciiStream(5, content);
            callableStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void dayStart(String executionDate, int branchId, long sessionId) throws SQLException, Exception {
        /*  getDbService().callStoredProcedure("begin pkg_eod.day_start(?,?,?); end ",
                                           new Object[] { executionDate, branchId, sessionId }, false); */
        try {
            new CapitaDBServiceImpl().callStoredProcedure("pkg_eod.day_start(?,?,?)",
                                                          new Object[] { executionDate, branchId, sessionId }, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            throw e;
        }

        /*
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_eod.day_start(?,?,?); end;",0);
            callableStatement.setObject(1, executionDate);
            callableStatement.setObject(2, branchId);
            callableStatement.setObject(3, sessionId);
            callableStatement.execute();
        */
    }

    public void dayEnd(String executionDate, int branchId, long sessionId) throws Exception {

        /*  getDbService().callStoredProcedure("begin pkg_eod.day_end" + "(?,?,?); end ",
                                           new Object[] { executionDate, branchId, sessionId }, false); */
        try {
            new CapitaDBServiceImpl().callStoredProcedure("pkg_eod.day_end(?,?,?)",
                                                          new Object[] { executionDate, branchId, sessionId }, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            throw e;
        }

        /*
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_eod.day_end(?,?,?); end;",0);
            callableStatement.setObject(1, executionDate);
            callableStatement.setObject(2, branchId);
            callableStatement.setObject(3, sessionId);
            callableStatement.execute();
    */

    }

    /*
    public boolean processEOD(String branchCode, String userId) throws Exception{
        boolean isProcessed = false;
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_eod.eod(?,?); end;", 0);
        //try {
            callableStatement.setObject(1, branchCode);
            callableStatement.setObject(2, userId);
            callableStatement.execute();
            isProcessed = true;
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
        return isProcessed;
    }
*/

    public void logoutAllSession(long sessionId) throws Exception {


        getDbService().callStoredProcedure("begin pkg_eod.logout_all_session(?); end", new Object[] { sessionId },
                                           false);

        /*
        CallableStatement callableStatement =
            getDbService().getDBTransaction().createCallableStatement("begin pkg_eod.logout_all_session(?); end;", 0);

            callableStatement.setObject(1, sessionId);
            callableStatement.execute();
        */
    }

    public void deleteThisFile(int importId) throws Exception {
        /* CallableStatement callableStatement =
                getDbService().getDBTransaction().createCallableStatement("begin pkg_cdbl_prc.delete_file(?); end;", 0);
            //try {
                callableStatement.setObject(1, importId);

               // callableStatement.execute();
            */
        getDbService().callStoredProcedure("begin pkg_cdbl_prc.delete_file" + "(?); end", new Object[] { importId });
    }

    public void deleteTradeFile(int importId) throws Exception {
        getDbService().callStoredProcedure("begin pkg_trade_file.delete_file" + "(?); end", new Object[] { importId });
    }

    public void deleteTBFile(String file_type, int importId) throws Exception {
        getDbService().callStoredProcedure("begin pkg_trade_file.delete_tb_file" + "(?, ?); end",
                                           new Object[] { file_type, importId });
    }

    public void validateThisFile(int importId) throws Exception {
        getDbService().callStoredProcedure("begin pkg_trade.trade_file_validate" + "(?); end",
                                           new Object[] { importId }, false);
    }

    public void validateThisPriceFile(int importId) throws Exception {
        getDbService().callStoredProcedure("begin pkg_trade.price_file_validate(?); end", new Object[] { importId },
                                           false);
    }

    public void processThisPriceFile(int importId) throws Exception {
        getDbService().callStoredProcedure("begin pkg_trade.price_file_process" + "(?); end",
                                           new Object[] { importId });

        //getDbService().callStoredProcedure("begin pkg_trade.price_file_process" + "(?); end;", new Object[] { importId },false);
    }

    public void processThisFile(int importId) throws Exception {
        int branch = 10;
        String businessType = "MBANK";

        getDbService().callStoredProcedure("begin pkg_trade.trade_process" + "(?, ?, ?); end",
                                           new Object[] { importId, branch, businessType });
        //getDbService().callStoredProcedure("begin pkg_trade.trade_process" + "(?, ?, ?); end;", new Object[] { importId, branch, businessType},false);
    }

    public void processThisFile(int importId, int branchId, String businessType) throws Exception {

        //System.out.println("p5----" + branchId);
        //getDbService().callStoredProcedure("begin pkg_trade.trade_process" + "(?, ?, ?); end", new Object[] { importId, branch, businessType});
        getDbService().callStoredProcedure("begin pkg_trade.trade_process" + "(?, ?, ?); end",
                                           new Object[] { importId, branchId, businessType }, false);

        //System.out.println("p6----");
    }

    public boolean processPurchasePowerUpdate(String accountId) throws SQLException {
        boolean isProcessed = false;
        try {
            getDbService().callStoredProcedure("begin pkg_portfolio.update_purchase_power(?); end",
                                               new Object[] { accountId }, false);
            /*
            CallableStatement callableStatement =
                getSessionCommonDbService().getDBTransaction().createCallableStatement("begin pkg_portfolio.update_purchase_power(?); end;",
                                                                          0);
            callableStatement.setObject(1, accountId);
            callableStatement.execute();
            */

            isProcessed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isProcessed;
    }


    public boolean bdaInstrumentDistribution(String accountId, String instrumentId,
                                             String tradeDate) throws SQLException {
        boolean isProcessed = false;
        try {
            getDbService().callStoredProcedure("begin pkg_portfolio.update_purchase_power(?); end",
                                               new Object[] { accountId, instrumentId, tradeDate }, false);

            /*
            CallableStatement callableStatement =
                getSessionCommonDbService().getDBTransaction().createCallableStatement("begin pkg_instr.distribute_bda_allocation(?,?,?); end;",
                                                                          0);
            callableStatement.setObject(1, accountId);
            callableStatement.setObject(2, instrumentId);
            callableStatement.setObject(3, tradeDate);
            callableStatement.execute();
            */
            isProcessed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isProcessed;
    }

    public boolean callnonTradeHolidayChecker(int exchangeId, Date tradeDate, boolean status) throws SQLException {
        boolean isProcessed = false;
        try {

            // System.out.println("INSIDE CHEKER-----T");
            CallableStatement callableStatement =
                getSessionCommonDbService().getDBTransaction().createCallableStatement("begin pkg_capit_sys.is_nonTradeDay(?,?,?); end;",
                                                                                       0);
            callableStatement.setObject(1, exchangeId);
            callableStatement.setObject(2, tradeDate);
            //System.out.println("################-----" + "OTHER TYPE");
            callableStatement.registerOutParameter(3, OracleTypes.BOOLEAN);
            //System.out.println("################-----" + "BEFORE EXECUTE");
            callableStatement.execute();
            // System.out.println("################-----" + "BEFORE SET");
            // callableStatement.setBoolean(3, status);
            System.out.println("################-----" + status);
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isProcessed;
    }

    public void allowOPSSUI(String procStatus, int importId, int tracerNo) {
        getDbService().callStoredProcedure("begin pkg_trade_file.allow_op_ss_ui" + "(?,?,?); end",
                                           new Object[] { procStatus, importId, tracerNo });
    }

    public void resetOPSSUI(String procStatus, int importId, int tracerNo) {
        getDbService().callStoredProcedure("begin pkg_trade_file.reset_op_ss_ui" + "(?,?,?); end",
                                           new Object[] { procStatus, importId, tracerNo });
    }

    public void setFundSettleDate(long id, String newSettleDate) {
        getDbService().callStoredProcedure("begin pkg_trade.set_fund_settle_date" + "(?, ?); end",
                                           new Object[] { id, newSettleDate });
    }

    public void setInstruSettleDate(long id, String newSettleDate) {
        getDbService().callStoredProcedure("begin pkg_trade.set_instrument_settle_date" + "(?, ?); end",
                                           new Object[] { id, newSettleDate });
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


    public BigDecimal getTradePriceAndQuantity(String packg_function, String tradeCode, String tradeDate,
                                               String instrumentId, String exchangeId) {
        BigDecimal rVal = null;
        try {
            Object[] params = { tradeCode, tradeDate, instrumentId, exchangeId };
            rVal =
(BigDecimal)new CapitaDBServiceImpl().callStoredFunction(Types.NUMERIC, packg_function, params, false);
        } catch (Exception e) {
            System.out.print(e.getMessage());
            throw new JboException(e);
        }
        return rVal == null ? new BigDecimal(0) : rVal;
    }

    //Purchase Capacity of an investor

    public BigDecimal _getInvestorPurchaseCapacity(String packg_function, String accId) {
        BigDecimal rVal = null;
        try {
            Object[] params = { accId };
            rVal =
(BigDecimal)new CapitaDBServiceImpl().callStoredFunction(Types.NUMERIC, packg_function, params, false);
        } catch (Exception e) {
            System.out.print(e.getMessage());
            throw new JboException(e);
        }
        return rVal == null ? new BigDecimal(0) : rVal;
    }

    //Purchase Power of an investor

    public BigDecimal _getInvestorPurchasePower(String packg_function, String accId) {
        BigDecimal rVal = null;
        try {
            Object[] params = { accId };
            rVal =
(BigDecimal)new CapitaDBServiceImpl().callStoredFunction(Types.NUMERIC, packg_function, params, false);
        } catch (Exception e) {
            System.out.print(e.getMessage());
            throw new JboException(e);
        }
        return rVal == null ? new BigDecimal(0) : rVal;
    }


    //It returns only number

    public BigDecimal getTMFunctionCallReturn(String packg_function, Object[] params) {
        BigDecimal rVal = null;
        try {
            // System.out.println("p2----");
            rVal =
(BigDecimal)new CapitaDBServiceImpl().callStoredFunction(Types.NUMERIC, packg_function, params, false);
        } catch (Exception e) {
            // System.out.println("p3----");
            System.out.print(e.getMessage());
            //throw new JboException(e);
        }
        return rVal == null ? new BigDecimal(0) : rVal;
    }

    public void dumpFlexTradeToMSA(String fileID) throws SQLException, Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_trade_file.dump_flex_trade_to_msa(?); end",
                                                          new Object[] { fileID });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void dumpEODTickerToPrice(String fileID) throws SQLException, Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_trade_file.dump_eod_ticker_to_price(?); end",
                                                          new Object[] { fileID });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void dumpMrgCashFileData(String fileID, String fileFormat, String fileOption,
                                    String extOrg) throws SQLException, Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.process_omnibus_cash_file(?, ?, ?, ?); end",
                                                          new Object[] { fileID, fileFormat, fileOption, extOrg });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void dumpMrgShareFileData(String fileID, String fileFormat, String fileOption,
                                     String extOrg) throws SQLException, Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.process_omnibus_share_file(?, ?, ?, ?); end",
                                                          new Object[] { fileID, fileFormat, fileOption, extOrg });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void dumpMrgCashFileDataDealer(String fileID, String fileFormat, String fileOption,
                                          String extOrg) throws SQLException, Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.process_dealer_cash_file(?, ?, ?, ?); end",
                                                          new Object[] { fileID, fileFormat, fileOption, extOrg });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void dumpMrgShareFileDataDealer(String fileID, String fileFormat, String fileOption,
                                           String extOrg) throws SQLException, Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.process_dealer_share_file(?, ?, ?, ?); end",
                                                          new Object[] { fileID, fileFormat, fileOption, extOrg });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void ftiClientLimitProcCall(String fileOption, String appDate, String deActivateFlag) throws SQLException,
                                                                                                        Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.gen_flex_trade_limit(?,?,?); end",
                                                          new Object[] { fileOption, appDate, deActivateFlag });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void ftiClientLimitProcCall(String fileOption, String appDate, String deActivateFlag,
                                       String brokerId) throws SQLException, Exception {
        String pkg_proc = null;
        try {
            String bizOption = ApplicationInfo.getBusinessType();
            System.out.println(" Flex bizOption " + bizOption);
            if (bizOption.equals("BROKER")) {
                if (fileOption.equals("R"))
                    pkg_proc = "pkg_flex_trade.gen_org_client_new_reg(?,?,?,?)";
                else
                    pkg_proc = "pkg_flex_trade.gen_flex_trade_limit(?,?,?,?)";
            } else {
                if (fileOption.equals("R"))
                    pkg_proc = "pkg_flex_trade_mbank.gen_org_client_new_reg(?,?,?,?)";
                else
                    pkg_proc = "pkg_flex_trade_mbank.gen_flex_trade_limit(?,?,?,?)";
            }
            new CapitaDBServiceImpl().callStoredProcedure("begin " + pkg_proc + "; end",
                                                          new Object[] { fileOption, appDate, deActivateFlag,
                                                                         brokerId });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void ftiPositionLimitProcCall(String fileOption, String appDate, String deActivateFlag) throws SQLException,
                                                                                                          Exception {
        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.gen_flex_trade_pos_limit(?,?,?); end",
                                                          new Object[] { fileOption, appDate, deActivateFlag });
        } catch (Exception e) {
            throw e;
        }
    }

    public static String getdbCallStringReturn(String packg_function, Object[] params) {
        String rVal = "N";
        try {
            Object dbValue =
                new CapitaDBServiceImpl().callStoredFunction(Types.VARCHAR, packg_function, params, false).toString();
            if (dbValue != null)
                rVal = dbValue.toString();
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
        return rVal;
    }

    public void delete_file_process_error(int importId, String pErrType, String fileID) throws Exception {

        try {
            new CapitaDBServiceImpl().callStoredProcedure("begin  pkg_trade_file.delete_process_file_error" +
                                                          "(?,?,?); end ", new Object[] { importId, pErrType, fileID },
                                                          false);
        } catch (SQLException sqle) {
            throw sqle;
        } catch (Exception e) {
            throw e;
        }
    }

    public static void generateInstrPayInOut(String payInOutType, String payInOutDate) throws SQLException, Exception {
        try {
            String packg_procedure = "pkg_trade.generate_instr_payin_out(?,?)";
            Object[] params = { payInOutType, payInOutDate };
            new CapitaDBServiceImpl().callStoredProcedure(packg_procedure, params, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public static void generateInstrPayInOutBeforeTrade(String payInOutType, String payInOutDate, Number exchange) throws SQLException,
                                                                                                         Exception {
        try {
            String packg_procedure = "pkg_trade.gen_payin_out_before_trade(?,?,?)";
            Object[] params = { payInOutType, payInOutDate, exchange };
            new CapitaDBServiceImpl().callStoredProcedure(packg_procedure, params, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public static void trdCommissionChangeProcess(int exchangeId, int importId, int prodId, String invCode,
                                                  String insCode, String buySell, Double commRate) throws SQLException,
                                                                                                          Exception {
        String pck_proc = "pkg_trade_file.trade_commission_change(?,?,?,?,?,?,?)";
        Object[] paramas = { exchangeId, importId, prodId, invCode, insCode, buySell, commRate };
        try {
            new CapitaDBServiceImpl().callStoredProcedure(pck_proc, paramas, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public static void trdCommissionChangeForeignInv(String trdDate, int exchnge, int broker, String invCode,
                                                     String instrmnt, String trdType,
                                                     Double chngComm) throws SQLException, Exception {
        String pck_proc = "trade_change_ui.insert_trade_change(?,?,?,?,?,?,?)";
        Object[] paramas = { trdDate, exchnge, broker, invCode, instrmnt, trdType, chngComm };
        try {
            new CapitaDBServiceImpl().callStoredProcedure(pck_proc, paramas, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public static void trdCommissionChangeForeignInvProcess(String trdDate) throws SQLException, Exception {
        String pck_proc = "trade_change.process(?)";
        Object[] paramas = { trdDate };
        try {
            new CapitaDBServiceImpl().callStoredProcedure(pck_proc, paramas, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesErrorMessage("Process Error !");
            e.printStackTrace();
            throw e;
        }
    }

    public BigDecimal tradeFileValidation(String packg_function) {
        BigDecimal rVal = null;
        try {
            rVal =
(BigDecimal)new CapitaDBServiceImpl().callStoredFunction(Types.NUMERIC, packg_function, null, false);
        } catch (Exception e) {
            System.out.print(e.getMessage());
            throw new JboException(e);
        }
        return rVal == null ? new BigDecimal(0) : rVal;
    }

    public static void tradeReverseProcess(Integer pimportId) throws SQLException, Exception {
        String pck_proc = "TRADE_REVERSE.TRADE_REVERSE_PROCESS(?)";
        Object[] paramas = { pimportId };
        try {
            new CapitaDBServiceImpl().callStoredProcedure(pck_proc, paramas, false);
        } catch (Exception e) {
            ApplicationInfo.getCurrentUserDBTransaction().rollback();
            JSFUtils.addFacesErrorMessage("Process Error !");
            e.printStackTrace();
            throw e;
        }
    }

    public void createInvFromOmnibusLimitFileData(String fileID, String fileType, String mbankShortName,
                                                  String orgBrId, String productId, String subProductId,
                                                  String traderId) throws Exception {
        try {
            /* System.out.println("Proc params: " + fileID+" -- "+fileType + " -- " + mbankShortName + " -- " + orgBrId + " -- " +
                               productId + " -- " + subProductId + " -- " + traderId); */
            new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.create_inv_from_om_limit_file(?, ?, ?, ?, ?, ?, ?); end",
                                                          new Object[] { fileID, fileType, mbankShortName, orgBrId,
                                                                         productId, subProductId, traderId });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /* public void deleteOmnibusLimitFileData(String fileID, String fileType, String mbankShortName) throws Exception {
            try {
                 //System.out.println("Proc params: " + fileID+" -- "+fileType + " -- " + mbankShortName);
                new CapitaDBServiceImpl().callStoredProcedure("begin pkg_flex_trade.delete_omnibus_limit_file(?, ?, ?); end",
                                                              new Object[] { fileID, fileType, mbankShortName });
            }  catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        } */

}
