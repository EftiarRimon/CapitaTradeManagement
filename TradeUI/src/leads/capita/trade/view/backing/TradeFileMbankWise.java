package leads.capita.trade.view.backing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.faces.application.FacesMessage;

import javax.faces.context.FacesContext;

import javax.servlet.ServletContext;

import leads.capita.common.ui.util.ADFUtils;
import leads.capita.common.ui.util.JSFUtils;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.input.RichInputDate;
import oracle.adf.view.rich.component.rich.input.RichSelectOneChoice;

import oracle.adf.view.rich.component.rich.nav.RichCommandButton;

import oracle.jbo.Row;
import oracle.jbo.ViewObject;

public class TradeFileMbankWise {
    static String user_home = System.getProperty("user.home");
    static String tradeFmbankDir = user_home + File.separator + "TradeFile.txt";
    private FacesContext fct;
    private RichSelectOneChoice mbankIdUI;
    private RichInputDate tradingDateUI;
    private RichCommandButton downloadFileBtnUI;

    public TradeFileMbankWise() {
        super();
        fct = JSFUtils.getFacesContextApp();
    }

    private void copyFile(File src, File dst) throws IOException {
        DCIteratorBinding dcIter = ADFUtils.findIterator("TradeFileMbankWiseVOIterator");
        Long rows = (dcIter.getViewObject().getEstimatedRowCount());
        if (rows > 0) {
            try {
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dst);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fct.addMessage("!Info", new FacesMessage("No data to read"));
        }
    }

    // write file

    public String createTradeFile_new() {
        // fetch mbank id
        Row mBankRow = ADFUtils.findIterator("MerchantBankLOVIterator").getViewObject().getCurrentRow();
        String mbankId = mBankRow.getAttribute("MbankId") == null ? "" : mBankRow.getAttribute("MbankId").toString();

        // fetch trading date
        Date myDate = (Date)this.getTradingDateUI().getValue();
        SimpleDateFormat ft = new SimpleDateFormat("dd-MMM-yyyy");
        String docUploadDate = ft.format(myDate);
        ft = new SimpleDateFormat("dd-MMM-yyyy");
        Date tradingDate = null;
        try {
            tradingDate = ft.parse(docUploadDate);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        File home_dir = null;
        DCIteratorBinding dcIter = ADFUtils.findIterator("TradeFileMbankWiseVOIterator");
        ViewObject vo = dcIter.getViewObject();
        vo.setNamedWhereClauseParam("mbankId", mbankId);
        vo.setNamedWhereClauseParam("tradingDate", tradingDate);
        vo.executeQuery();

        Row[] r = vo.getAllRowsInRange();
        Long noOfLines = vo.getEstimatedRowCount();
        if (noOfLines > 0) {
            try {
                home_dir = new File(tradeFmbankDir);
                //home_dir = new File("D:/tradefile.txt");
                FileWriter stream = new FileWriter(home_dir);
                Writer output = new BufferedWriter(stream);

                String linegap = String.format("%n");

                String born = null;
                String instrmntCode = null;
                String isIn = null;
                String usrId = null;
                String buySellFlag = null;
                String tradeQty = null;
                String tradePrice = null;
                String eventDate = null;
                String eventTime = null;
                String mktType = null;
                String fillType = null;
                String howlaType = null;
                String foreignFlag = null;
                String customer = null;
                String boId = null;
                String tradeContractNo = null;
                String compSpotID = null;
                String instrumentCategory = null;
                

                String printLine = null;

                for (int i = 0; i < r.length; i++) {
                    Row curRow = r[i];
                    if (curRow.getAttribute("Born") != null) {
                        born = curRow.getAttribute("Born").toString();
                    }
                    if (curRow.getAttribute("InstrumentCode") != null) {
                        instrmntCode = curRow.getAttribute("InstrumentCode").toString();
                    }
                    if (curRow.getAttribute("Isin") != null) {
                        isIn = curRow.getAttribute("Isin").toString();
                    }
                    if (curRow.getAttribute("TraderId") != null) {
                        usrId = curRow.getAttribute("TraderId").toString();
                    }
                    if (curRow.getAttribute("BuySell") != null) {
                        buySellFlag = curRow.getAttribute("BuySell").toString();
                    }
                    if (curRow.getAttribute("TradeQuantity") != null) {
                        tradeQty = curRow.getAttribute("TradeQuantity").toString();
                    }
                    if (curRow.getAttribute("Price") != null) {
                        tradePrice = curRow.getAttribute("Price").toString();
                    }
                    if (curRow.getAttribute("TradeDate") != null) {
                        
                        SimpleDateFormat ftt = new SimpleDateFormat("yyyy-MM-dd");
                        Date docUploadDatee = ftt.parse(curRow.getAttribute("TradeDate").toString());
                        ftt = new SimpleDateFormat("dd-MM-yyyy");
                        eventDate=ftt.format(docUploadDatee);
                        
                        
                        
                        //eventDate = curRow.getAttribute("TradeDate").toString();
                    }
                    System.out.println("trade date " + eventDate);
                    if (curRow.getAttribute("TradeTime") != null) {
                        eventTime = curRow.getAttribute("TradeTime").toString();
                    }
                    if (curRow.getAttribute("MarketType") != null) {
                        mktType = curRow.getAttribute("MarketType").toString();
                    }
                    if (curRow.getAttribute("FillType") != null) {
                        fillType = curRow.getAttribute("FillType").toString();
                    }
                    if (curRow.getAttribute("HowlaType") != null) {
                        howlaType = curRow.getAttribute("HowlaType").toString();
                    }
                    if (curRow.getAttribute("ForeignFlag") != null) {
                        foreignFlag = curRow.getAttribute("ForeignFlag").toString();
                    }
                    if (curRow.getAttribute("Customer") != null) {
                        customer = curRow.getAttribute("Customer").toString();
                    }
                    if (curRow.getAttribute("Boid") != null) {
                        boId = curRow.getAttribute("Boid").toString();
                    }
                    if (curRow.getAttribute("ContractNo") != null) {
                        tradeContractNo = curRow.getAttribute("ContractNo").toString();
                    }
                    if (curRow.getAttribute("CompulsorySpot") != null) {
                        compSpotID = curRow.getAttribute("CompulsorySpot").toString();
                    }
                    if (curRow.getAttribute("InstrumentGroup") != null) {
                        instrumentCategory = curRow.getAttribute("InstrumentGroup").toString();
                    }
                    printLine =
                            born + "~" + instrmntCode + "~" + isIn + "~" + usrId + "~" + buySellFlag + "~" + tradeQty +
                            "~" + tradePrice + "~" + eventDate + "~" + eventTime + "~" + mktType + "~" + fillType +
                            "~" + howlaType + "~" + foreignFlag + "~" + customer + "~" + boId + "~" + tradeContractNo +
                            "~" + compSpotID + "~" + instrumentCategory;

                    output.write(printLine + linegap);
                }
                output.close();
                fct.addMessage("Complete Msg", new FacesMessage("File is ready to be downloaded !!"));
                this.getDownloadFileBtnUI().setDisabled(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fct.addMessage("!Info", new FacesMessage("No data to read"));
            this.getDownloadFileBtnUI().setDisabled(true);
        }
        return null;
    }

    // download file

    public void generateFileListener(FacesContext facesContext, OutputStream outputStream) {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ServletContext context = (ServletContext)fctx.getExternalContext().getContext();
        File processedfile = new File(tradeFmbankDir);
        File userPC = new File(context.getRealPath("/") + File.separator + "tradefile.txt");
        FileInputStream fdownLoad;
        byte[] b;
        try {
            File f = new File(processedfile.getPath());
            fdownLoad = new FileInputStream(f);
            int n;
            while ((n = fdownLoad.available()) > 0) {
                b = new byte[n];
                int result = fdownLoad.read(b);
                outputStream.write(b, 0, b.length);
                if (result == -1)
                    break;
            }
            outputStream.flush();
            outputStream.close();
            // new PayInOutFileUtil().copy(home_dirFile, weRoot_dirFile);
            copyFile(processedfile, userPC);
            //System.out.println("done");
        } catch (IOException e) {
            JSFUtils.addFacesErrorMessage("Error Reading file");
        }
    }

    public void setMbankIdUI(RichSelectOneChoice mbankIdUI) {
        this.mbankIdUI = mbankIdUI;
    }

    public RichSelectOneChoice getMbankIdUI() {
        return mbankIdUI;
    }

    public void setTradingDateUI(RichInputDate tradingDateUI) {
        this.tradingDateUI = tradingDateUI;
    }

    public RichInputDate getTradingDateUI() {
        return tradingDateUI;
    }

    public void setDownloadFileBtnUI(RichCommandButton downloadFileBtnUI) {
        this.downloadFileBtnUI = downloadFileBtnUI;
    }

    public RichCommandButton getDownloadFileBtnUI() {
        return downloadFileBtnUI;
    }
}
