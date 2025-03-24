package leads.capita.trade.view.backing;


import java.io.IOException;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import java.math.BigDecimal;

import java.math.RoundingMode;

import java.text.DateFormat;
import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.Date;

import java.util.logging.Level;

import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import leads.capita.common.ui.util.JSFUtils;

import leads.capita.trade.file.FlexTradeFileUtil;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;

import oracle.adf.view.rich.component.rich.input.RichInputText;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class OmgeoTradeFileBean {
    
    public OmgeoTradeFileBean(){
        this.setGeneratedFileNameUI(generatedFileNameUI);    
    }
    
    private static final Logger logger = Logger.getLogger(OmgeoTradeFileBean.class.getName());
    private String generatedFileNameUI;    
    
    public void OmgeoTradeFileDownload(FacesContext facesContext,
                                       OutputStream outputStream) throws UnsupportedEncodingException, IOException {
        
        //block_keys.
        String blockKeys = null;
        String blockver = null;
        String instrparty = null;
        String execbroker = null;
        String masterref = null;

        //trade_security_id.
        String tradeSecurityId = null;
        String agencyCode = null;
        String securityCode = null;
        String securityTYpe = null;

        //trade_info
        String tradeInfo = null;
        String buyOrSell = null;
        String indicatorncurr = null;
        double rate = 0;
        int quantity = 0;
        double tradeAmount = 0;
        String tradeDate = null;
        String settlementDate = null;

        //block_total.
        String blockTotals = null;
        String totalsettlamt = null;
        String settlcurrcode = null;
        double totalNetCash = 0;
        String autocreateconfrm = null;

        //block_commissions.
        String blockCommission = null;
        String commindicator = null;
        String commType = null;
        double commAmount = 0;

        //block_charges_taxes.
        String blockChargesTaxes = null;
        String chagTaxType = null;
        double chrgAmt = 0;

        //block_ssi_keys
        String blockSSIKeys = null;
        String instrIndicator = null;
        String alrtcountrycode = null;
        String alrtMethodType = null;

        String block_keys_line = null;
        String trade_security_id_line = null;
        String trade_info_line = null;
        String block_total_line = null;
        String block_commissions_line = null;
        String block_charges_taxes_line = null;
        String block_ssi_keys_line = null;
        
        String heading = null;
        OutputStreamWriter w = new OutputStreamWriter(outputStream, "UTF-8");
        BindingContext ctx = BindingContext.getCurrent();
        DCBindingContainer bc = (DCBindingContainer)ctx.getCurrentBindingsEntry();
        DCIteratorBinding iterator = bc.findIteratorBinding("OmgeoTradeFileVOIterator");       
   

        if (iterator != null) {
            Row row[] = iterator.getAllRowsInRange();

            /* StringBuilder sb = new StringBuilder();
            w.write(sb.toString());
            w.write(System.getProperty("line.separator")); */
            heading = "import_eb_block";
            //w.write(heading + System.getProperty("line.separator"));
            for (Row r : row) {
                int count = 0;
                //block_keys.
                if(r.getAttribute("BlockKeys") != null)
                    blockKeys = r.getAttribute("BlockKeys").toString();
                if(r.getAttribute("Blockver") != null)
                    blockver = r.getAttribute("Blockver").toString();
                if(r.getAttribute("Instrparty") != null)
                    instrparty = r.getAttribute("Instrparty").toString();
                
                if(r.getAttribute("Execbroker") != null)
                    execbroker = r.getAttribute("Execbroker").toString();        
                if(r.getAttribute("Masterref") != null)
                    masterref = r.getAttribute("Masterref").toString();
                
                //trade_security_id.   
                if(r.getAttribute("TradeSecurityId") !=null)
                    tradeSecurityId = r.getAttribute("TradeSecurityId").toString();
                if(r.getAttribute("Agencycode") !=null)
                    agencyCode = r.getAttribute("Agencycode").toString();
                if(r.getAttribute("Securitycode") !=null)
                    securityCode = r.getAttribute("Securitycode").toString();
                if(r.getAttribute("Securitytype") !=null)
                    securityTYpe = r.getAttribute("Securitytype").toString();                
                
                //trade_info
                if(r.getAttribute("TradeInfo") !=null)
                    tradeInfo = r.getAttribute("TradeInfo").toString();
                if(r.getAttribute("BuyOrSell") !=null)
                    buyOrSell = r.getAttribute("BuyOrSell").toString();
                if(r.getAttribute("Indicatorncurr") !=null)
                    indicatorncurr = r.getAttribute("Indicatorncurr").toString();
                if(r.getAttribute("Rate") !=null)
                    //rate = Double.parseDouble(r.getAttribute("Rate").toString());
                    rate =BigDecimal.valueOf(Double.parseDouble(r.getAttribute("Rate").toString())).setScale(4, RoundingMode.HALF_UP).doubleValue();
                    //System.out.println("Rate: "+rate);
                //Double truncatedDouble = BigDecimal.valueOf(toBeTruncated).setScale(3, RoundingMode.HALF_UP).doubleValue();
                    
                    
                if(r.getAttribute("Quantity") !=null)
                    quantity = Integer.parseInt(r.getAttribute("Quantity").toString());
                if(r.getAttribute("Tradeamount") !=null)
                    //tradeAmount = Double.parseDouble(r.getAttribute("Tradeamount").toString());                 
                    
                    tradeAmount =BigDecimal.valueOf(Double.parseDouble(r.getAttribute("Tradeamount").toString())).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    //System.out.println("Trade Amount: "+tradeAmount);
                if(r.getAttribute("Tradedate") !=null)
                    tradeDate = getFormatedDate(r.getAttribute("Tradedate"));
                if(r.getAttribute("Settlementdate") !=null)
                    settlementDate = getFormatedDate(r.getAttribute("Settlementdate"));
                
                //block_total.
                if(r.getAttribute("BlockTotals") !=null)
                    blockTotals = r.getAttribute("BlockTotals").toString();
                /* if(r.getAttribute("Totalsettlamt") !=null)
                    totalsettlamt = r.getAttribute("Totalsettlamt").toString(); */
                /* if(r.getAttribute("Settlcurrcode") !=null)
                    settlcurrcode = r.getAttribute("Settlcurrcode").toString(); */
                if(r.getAttribute("Totalnetcash") !=null)
                    totalNetCash = Double.parseDouble(r.getAttribute("Totalnetcash").toString());
                if(r.getAttribute("Autocreateconfrm") !=null)
                    autocreateconfrm = r.getAttribute("Autocreateconfrm").toString();
                
                //block_commissions.
                if(r.getAttribute("BlockCommissions") !=null)
                    blockCommission = r.getAttribute("BlockCommissions").toString();                
                if(r.getAttribute("Commindicator") !=null)
                    commindicator = r.getAttribute("Commindicator").toString();                
                if(r.getAttribute("Commtype") !=null)
                    commType = r.getAttribute("Commtype").toString();
                if(r.getAttribute("Commamount") !=null)
                    commAmount = Double.parseDouble(r.getAttribute("Commamount").toString());
                
                //block_charges_taxes.
                if(r.getAttribute("BlockChargesTaxes") !=null)
                    blockChargesTaxes = r.getAttribute("BlockChargesTaxes").toString();    
                if(r.getAttribute("Chagtaxtype") !=null)
                    chagTaxType = r.getAttribute("Chagtaxtype").toString();    
                if(r.getAttribute("Chrgamt") !=null)
                    chrgAmt = Double.parseDouble(r.getAttribute("Chrgamt").toString());                
                
                //block_ssi_keys
                if(r.getAttribute("BlockSsiKeys") !=null)
                    blockSSIKeys = r.getAttribute("BlockSsiKeys").toString();    
                if(r.getAttribute("Instrindicator") !=null)
                    instrIndicator = r.getAttribute("Instrindicator").toString(); 
                if(r.getAttribute("Alrtcountrycode") !=null)
                    alrtcountrycode = r.getAttribute("Alrtcountrycode").toString();    
                if(r.getAttribute("Alrtmethodtype") !=null)
                    alrtMethodType = r.getAttribute("Alrtmethodtype").toString();                               
              

                block_keys_line = blockKeys + blockver + instrparty + execbroker + masterref;             
                trade_security_id_line = tradeSecurityId + agencyCode + securityCode + securityTYpe;          
                trade_info_line = tradeInfo + buyOrSell + indicatorncurr + rate + "," + quantity + "," +
                                  tradeAmount + "," + tradeDate + "," + settlementDate;                
                block_total_line = blockTotals + ",," + totalNetCash + autocreateconfrm;                                
                block_commissions_line = blockCommission + commindicator  + commType + commAmount;         
                block_charges_taxes_line = blockChargesTaxes + chagTaxType + chrgAmt;            
                block_ssi_keys_line = blockSSIKeys + instrIndicator + alrtcountrycode + alrtMethodType;
                

                w.write(heading + System.getProperty("line.separator") + block_keys_line 
                                + System.getProperty("line.separator") + trade_security_id_line
                                + System.getProperty("line.separator") + trade_info_line
                                + System.getProperty("line.separator") + block_total_line
                                + System.getProperty("line.separator") + block_commissions_line
                                + System.getProperty("line.separator") + block_charges_taxes_line
                                + System.getProperty("line.separator") + block_ssi_keys_line 
                                + System.getProperty("line.separator"));
                count++;
            }
            w.flush();
            w.close(); 
            this.getGeneratedFileNameUI();            
            
        }

    }
    
    
    public static String getUniqueValue() {
        SimpleDateFormat format = null;
        String valueString = null;
        try {
            format = new SimpleDateFormat("yyyyMMddhhmmss");
            valueString = format.format(new Date());
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
            
        }
        return valueString;
    }
    

    public static String getFormatedDate(Object dateObj) {
        String dat = null;
        if (dateObj instanceof oracle.jbo.domain.Timestamp) {
            oracle.jbo.domain.Timestamp t = (oracle.jbo.domain.Timestamp)dateObj;
            Date dt = new Date(t.getTime());
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            dat = df.format(new Date(dt.getTime()));
        } else if (dateObj instanceof java.sql.Timestamp) {
            java.sql.Timestamp t = (java.sql.Timestamp)dateObj;
            Date dt = new Date(t.getTime());
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            dat = df.format(new Date(dt.getTime()));
        } else {
            Date dt = (Date)dateObj;
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            dat = df.format(new Date(dt.getTime()));
        }
        return dat;
    }

    public void setGeneratedFileNameUI(String generatedFileNameUI) {
        this.generatedFileNameUI = "Trade_BRACEPSL_" +getUniqueValue()+".txt";
    }
    public String getGeneratedFileNameUI() {
        return generatedFileNameUI;
    }
}
