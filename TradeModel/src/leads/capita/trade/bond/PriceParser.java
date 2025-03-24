package leads.capita.trade.bond;

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

import leads.capita.trade.model.view.EodTickerVOImpl;
import leads.capita.trade.model.view.EodTickerVORowImpl;
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

import oracle.jbo.domain.Timestamp;

public class PriceParser {
    
    private static final ADFLogger logger = ADFLogger.createADFLogger(TradeParser.class);

    private FacesContext fct;
    private DCBindingContainer bindings = (DCBindingContainer)BindingContext.getCurrent().getCurrentBindingsEntry();
    private Integer errorCode = 0;
    private List<String> errors = new ArrayList<String>();
    
    public PriceParser() {
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
    
    private Timestamp getTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }
    private boolean varifyDate(String fileDate, Date toDateVal) throws ParseException {

        String pattern = "ddMMyyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String dt = simpleDateFormat.format(new java.sql.Timestamp(toDateVal.getTime()));
        if (!fileDate.equalsIgnoreCase(dt)) {
            errors.add(" Not Current Dated File!!");
            return false;
        }
        
        return true;
    }
    public Integer parse(int exchangeId, String fileName, Date toDateVal, InputStream file, EodTickerVOImpl vo,
                         InstrumentsVOImpl instruments, 
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
        InstrumentsVORowImpl instrumentVoRowImpl;
        
        final ImportExtFilesVORowImpl importExtFilesVORow = (ImportExtFilesVORowImpl)importExtFilesVOImpl.createRow();
        importExtFilesVORow.setFileName(fileName);
        importExtFilesVORow.setFileDate(getTimestamp(toDateVal));
        importExtFilesVORow.setFileId(importFileId);
        
        String fileDate = this.getFileNameDatePart(fileName);
        
        if(!varifyDate(fileDate, toDateVal)){
            errorCode = 1;
            return errorCode;
        }
          
        while (tradeData.hasNext()) {

            line = tradeData.next();

            
            
            final String securityCode = line.get(PriceField.SECURITY);
            final String board = line.get(PriceField.BOARD); 
            final String price = line.get(PriceField.CLOSE_PRICE);
            
              
            instrumentVoRowImpl = validateInstrument(instruments, securityCode);
            if (instrumentVoRowImpl == null) {
                errorCode = 1;
                errors.add("Invalid Instruments " + securityCode + "Seq: " + orderNo);
                continue;
            }
            
            final EodTickerVORowImpl row = (EodTickerVORowImpl)vo.createRow();
            row.setFileId(Long.parseLong(fileId));
            row.setFileName(fileName);
            row.setTradeDate(fileDate);
            row.setSecurityCode(securityCode);
            row.setClosePrice(new BigDecimal(price));
            row.setCompulsorySpot(isSpotTrade);
            row.setInstrCategory("A");
            row.setHighPrice(new BigDecimal(0));
            row.setLowPrice(new BigDecimal(0));
            row.setOpenPrice(new BigDecimal(0));
            row.setExchangeId(10);
            row.setAssetClass("TB");
            row.setIsin(instrumentVoRowImpl.getIsin());
            row.setSector(instrumentVoRowImpl.getSector());

            counter++;

        }
        
        importExtFilesVORow.setTotalRec(counter);
        importExtFilesVORow.setValidateRec(counter - errors.size());
        importExtFilesVORow.setErrorRec(errors.size());
        importExtFilesVORow.setProcessedRec(0);
        
        return counter;
    }




    /*public static void main(String[] args) throws IOException {
        System.out.println("+++++++++++++++++++++Price Parser++++++++++++");
        Iterator<Map<String, String>> tradeData = null;//CSVParser.getData("C:\\Users\\momtajul.karim\\OneDrive\\dev\\gsec\\src\\main\\resources\\price_2022-07-06.csv");

        while (tradeData.hasNext()) {
            final Map<String, String> keyVals = tradeData.next();
            System.out.print(keyVals.get(TradeField.SECURITY) + " ");
            System.out.print(keyVals.get(TradeField.PRICE) + "\n");
        }
    }*/

    public Integer getErrorCode() {
        return errorCode;
    }

    public List<String> getErrors() {
        return errors;
    }
}
