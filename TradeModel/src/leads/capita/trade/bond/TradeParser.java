package leads.capita.trade.bond;

import java.io.File;
import java.io.IOException;

import java.io.InputStream;

import java.math.BigDecimal;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;

import javax.faces.context.FacesContext;

import leads.capita.common.application.ApplicationInfo;
import leads.capita.common.ui.util.JSFUtils;
import leads.capita.trade.model.view.FlexTradeDataVOImpl;

import leads.capita.trade.model.view.FlexTradeDataVORowImpl;

import leads.capita.trade.model.view.ImportExtFilesVOImpl;
import leads.capita.trade.model.view.ImportExtFilesVORowImpl;
import leads.capita.trade.model.view.InstrumentsVOImpl;
import leads.capita.trade.model.view.InstrumentsVORowImpl;

import leads.capita.trade.model.view.InvestorAccountsVOImpl;
import leads.capita.trade.model.view.InvestorAccountsVORowImpl;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.share.logging.ADFLogger;

import oracle.binding.BindingContainer;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

import oracle.jbo.domain.Timestamp;

import org.apache.myfaces.trinidad.model.UploadedFile;

public class TradeParser {

    private static final ADFLogger logger = ADFLogger.createADFLogger(TradeParser.class);

    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    private Integer errorCode = 0;
    private List<String> errors = new ArrayList<String>();

    public TradeParser() {
        super();

    }

    private DCIteratorBinding getBindingsIterator(String iteratorName) {
        DCIteratorBinding iterator = (DCIteratorBinding)bindings.get(iteratorName);
        return iterator;
    }

    public static String getFileNameDatePart(String fileName) {
        if (fileName != null || fileName.equals("")) {
            return fileName.substring(0, 8);
        } else
            return null;
    }

    public static String getUniqueValue() {
        SimpleDateFormat format = null;
        String valueString = null;
        try {
            format = new SimpleDateFormat("yyMMddhhmmss");
            valueString = format.format(new Date());
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        }
        return valueString;
    }

    private InstrumentsVORowImpl validateInstrument(InstrumentsVOImpl instrumentsVOImpl, String shortName) {

        instrumentsVOImpl.setWhereClause("QRSLT.SHORT_NAME = " + "\'" + shortName + "\'");
        instrumentsVOImpl.executeQuery();
        InstrumentsVORowImpl instrument = (InstrumentsVORowImpl)instrumentsVOImpl.first();
        return instrument != null ? instrument : null;
    }

    private InvestorAccountsVORowImpl validateInvestor(InvestorAccountsVOImpl investorVOImpl, String boId) {

        investorVOImpl.setWhereClause("QRSLT.BOID = " + "\'" + boId + "\'");
        investorVOImpl.executeQuery();
        InvestorAccountsVORowImpl investor = (InvestorAccountsVORowImpl)investorVOImpl.first();

        return investor != null ? investor : null;
    }

    private Timestamp getTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }
    private boolean varifyDate(String fileDate, Date toDateVal) throws ParseException {

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat formatterFileDate = new SimpleDateFormat("ddMMyyyy");
        String pattern = "yyyyMMdd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String dt = simpleDateFormat.format(new java.sql.Timestamp(toDateVal.getTime()));
        if (!fileDate.equalsIgnoreCase(dt)) {
            errors.add(" Not Current Dated File!!");
            return false;
        }
        
        /*Date tradeFileDate = new Date(formatterFileDate.parse(fileDate).getTime());
        System.out.println(tradeFileDate);

        if ((tradeFileDate != null) && (toDateVal.compareTo(formatterFileDate.format(tradeFileDate)) != 0)) {
             errors.add("File date(" + formatter.format(tradeFileDate) +
                                                     ") and uploaded date(" + formatter.format(toDateVal) +
                                                     ")  has been mismatched!");
             return false;
        }*/
        return true;
    }
    public Integer parse(int exchangeId, String fileName, Date toDateVal, InputStream file, FlexTradeDataVOImpl vo,
                         InstrumentsVOImpl instruments, InvestorAccountsVOImpl investorAccounts, 
                         ImportExtFilesVOImpl importExtFilesVOImpl, String importFileId) throws IOException,
                                                                                                        ParseException {
        int counter = 0;
        Iterator<Map<String, String>> tradeData = CSVParser.getData(file);
        Map<String, String> line;
        String fileId = this.getUniqueValue();
        String boId = null;
        String side = null;
        String traderId = null;
        String orderNo = null;
        String isSpotTrade = null;
        int buySell = 0;
        InstrumentsVORowImpl instrumentVoRowImpl;
        InvestorAccountsVORowImpl InvestorAccountsVORowImpl;
        
        final ImportExtFilesVORowImpl importExtFilesVORow = (ImportExtFilesVORowImpl)importExtFilesVOImpl.createRow();
        importExtFilesVORow.setFileName(fileName);
        importExtFilesVORow.setFileDate(getTimestamp(toDateVal));
        importExtFilesVORow.setFileId(importFileId);
        
        while (tradeData.hasNext()) {
            
            buySell = 0;
            counter++;
            line = tradeData.next();
            
            final String tradeNo = line.get(TradeField.TRADE_NO);
            final String buyOrderNo = line.get(TradeField.BUY_ORDER_NO);
            final String isForeign = line.get(TradeField.IS_FOREIGN);
            final String sellOrderNo = line.get(TradeField.SELL_ORDER_NO);
            final String isSpot = line.get(TradeField.SPOT);
            final String tradeTime = line.get(TradeField.TIME);
            final String buyBoid = line.get(TradeField.BUY_BOID);
            final String sellBoid = line.get(TradeField.SELL_BOID);
            final String buyTrader = line.get(TradeField.BUY_TRADER);
            final String sellTrader = line.get(TradeField.SELL_TRADER);
            final String securityCode = line.get(TradeField.SECURITY);
            final String price = line.get(TradeField.PRICE);
            final String board = line.get(TradeField.BOARD);
            final String quantity = line.get(TradeField.QUANTITY);
            final String tradeValue = line.get(TradeField.VALUE);
            final String tradeSource = line.get(TradeField.TRADE_SOURCE);
            final String yield = line.get(TradeField.YIELD);
            final String accruedInterest = line.get(TradeField.ACCRUED_INTEREST);

            String fileDate = this.getFileNameDatePart(tradeNo);
            
            if(!varifyDate(fileDate, toDateVal)){
                errorCode = 1;
                return errorCode;
            }
            
            if ((buyBoid != null && !buyBoid.equals("")) && (sellBoid != null && !sellBoid.equals(""))) {
                boId = buyBoid;
                traderId = buyTrader;
                orderNo = buyOrderNo;
                side    = "B";
                buySell = buySell + 2;
            }    
            else if (buyBoid != null && !buyBoid.equals("")) {
                boId = buyBoid;
                traderId = buyTrader;
                orderNo = buyOrderNo;
                side = "B";
                buySell = buySell + 1;
            } else if(sellBoid != null && !sellBoid.equals("")) {
                boId = sellBoid;
                traderId = sellTrader;
                orderNo = sellOrderNo;
                side = "S";
                buySell = buySell + 1;
            }else{
                errorCode = 1;
                errors.add("BoId not found. Line #" + counter);
            }

            instrumentVoRowImpl = validateInstrument(instruments, securityCode);
            if (instrumentVoRowImpl == null) {
                errorCode = 1;
                errors.add("Invalid Instruments " + securityCode + "Seq: " + orderNo);
                continue;
            }

            InvestorAccountsVORowImpl = validateInvestor(investorAccounts, boId);

            if (InvestorAccountsVORowImpl == null) {
                errorCode = 1;
                errors.add("Inavlid Investors " + boId + "Seq: " + orderNo);
                continue;
            }
            
            if(isSpot.equals("FALSE")){
                isSpotTrade = "N";
            }else{
                isSpotTrade = "Y";
            }

            for(int i = 0; i<buySell; i++){
                
                // Following statement for both buy sell in same row. For sell column set as "S" 
                if(i == 1)
                    side = "S";
                
                final FlexTradeDataVORowImpl row = (FlexTradeDataVORowImpl)vo.createRow();
                row.setFileId(Long.parseLong(fileId));
                row.setFileName(fileName);
                row.setBoard(board);
                row.setBoid(boId);
                row.setSecurityCode(securityCode);
                row.setPrice(new BigDecimal(price));
                row.setQuantity(Long.parseLong(quantity));
                row.setTraderDealerId(traderId);
                row.setSide(side);
                row.setFileDate(fileDate);
                row.setForeignFlag(isForeign);
                row.setCompulsorySpot(isSpotTrade);
                row.setCompCategory(tradeSource == null? "A" : tradeSource);
                row.setTradeContractNo(tradeNo);
                row.setAction("EXEC");
                row.setStatus("FILL");
                row.setTradeValue(new BigDecimal(tradeValue));
                row.setFileTime(tradeTime);
                row.setBranchId("11");
                row.setAssetclass("TB");
                row.setOrderId(orderNo);
                row.setOwnerDealerId(traderId);
                row.setExecid(orderNo);
                row.setRefOrderId(orderNo);
                row.setClientCode(InvestorAccountsVORowImpl.getInvestorCode());
                row.setIsin(instrumentVoRowImpl.getIsin());
                row.setFillType("FILL");
                row.setYield(new BigDecimal(yield));
                row.setAccruedInterest(new BigDecimal(accruedInterest));
                
            }
            
        }
        
        importExtFilesVORow.setTotalRec(counter);
        importExtFilesVORow.setValidateRec(counter - errors.size());
        importExtFilesVORow.setErrorRec(errors.size());
        importExtFilesVORow.setProcessedRec(0);
        
        return counter;
    }


    /*public static void main(String[] args) throws IOException {
        System.out.println("+++++++++++++++++++++Trade Parser++++++++++++");
        Iterator<Map<String, String>> tradeData =
            CSVParser.getData("D:\\Clients\\Sprint\\G-Sec\\trade_2022-07-06.csv");
        Map<String, String> row;
        while (tradeData.hasNext()) {
            row = tradeData.next();
            System.out.print(row.get(TradeField.SECURITY) + " ");
            System.out.print(row.get(TradeField.PRICE) + " ");
            System.out.println(row.get(TradeField.STATUS));
        }
    }*/

    public Integer getErrorCode() {
        return errorCode;
    }

    public List<String> getErrors() {
        return errors;
    }
}
