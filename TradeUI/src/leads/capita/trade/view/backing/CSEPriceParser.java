package leads.capita.trade.view.backing;

import java.io.BufferedReader;
import java.io.IOException;

import java.math.BigDecimal;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import leads.capita.trade.model.view.ImportExtFilesVOImpl;
import leads.capita.trade.model.view.ImportExtFilesVORowImpl;
import leads.capita.trade.model.view.InstrumentsVOImpl;
import leads.capita.trade.model.view.InstrumentsVORowImpl;
import leads.capita.trade.model.view.common.TradePriceFileVO;
import leads.capita.trade.model.view.common.TradePriceFileVORow;

import oracle.jbo.domain.Timestamp;


public class CSEPriceParser {
    

    public CSEPriceParser() {

    }

    public List<String> parse(BufferedReader bufferedReader, ImportExtFilesVOImpl importExtFilesVOImpl,
                              InstrumentsVOImpl instruments, TradePriceFileVO tradePriceFileVO, String fileName,
                              String fileId, Date tradeDate) throws IOException {

        List<String> invalidInstruments = new ArrayList<String>();
        InstrumentsVORowImpl instrumentVoRowImpl;

        int dashLineCounter = 0;
        int totalRecord = 0;
        int tracerNo;
        String line;
        SimpleDateFormat tradeDateFormatter = new SimpleDateFormat("dd-MMM-yy");

        ImportExtFilesVORowImpl importExtFilesVORow = (ImportExtFilesVORowImpl)importExtFilesVOImpl.createRow();
        importExtFilesVORow.setFileName(fileName);
        importExtFilesVORow.setFileDate(getTimestamp(tradeDate));
        importExtFilesVORow.setFileId(fileId);

        tradePriceFileVO.setOrderByClause("tracer_no desc");
        tradePriceFileVO.setWhereClause("trade_time = " + "\'" + tradeDateFormatter.format(tradeDate) + "\'");
        tradePriceFileVO.executeQuery();
        
        if (((TradePriceFileVORow)tradePriceFileVO.first()) != null)
            tracerNo = ((TradePriceFileVORow)tradePriceFileVO.first()).getTracerNo();
        else
            tracerNo = 0;

        StringBuffer lineBuf;
        while (true) {
            line = bufferedReader.readLine();

            if (line.contains("----"))
                dashLineCounter++;

            if (dashLineCounter == 2) {
                if (line.contains("----") || line.isEmpty())
                    continue;
                totalRecord++;
                lineBuf = new StringBuffer(line);

                List<String> recordList =
                    Arrays.asList(lineBuf.replace(13, 64, "").toString().trim().replaceAll(" +", " ").split(" "));
                instrumentVoRowImpl = validateInstrument(instruments, recordList.get(0));
                if (instrumentVoRowImpl != null) {
                    TradePriceFileVORow tradePriceFileVORow =
                        (TradePriceFileVORow)importExtFilesVORow.getTradePriceFileVO().createRow();

                    tradePriceFileVORow.setTracerNo(++tracerNo);
                    tradePriceFileVORow.setSeqNo(tradePriceFileVORow.getTracerNo());
                    tradePriceFileVORow.setInstrCode(instrumentVoRowImpl.getShortName());
                    tradePriceFileVORow.setInstrGroup(instrumentVoRowImpl.getInstrumentGroup());
                    tradePriceFileVORow.setInstrIsin(instrumentVoRowImpl.getIsin());
                    tradePriceFileVORow.setExchangeId(11);
//                    tradePriceFileVORow.setFileId(Integer.valueOf(new SimpleDateFormat("DMMyy").format(tradeDate) +
//                                                                  tradePriceFileVORow.getTracerNo()));
                    tradePriceFileVORow.setTradeTime(tradeDateFormatter.format(tradeDate.getTime()));
                    tradePriceFileVORow.setPriceDate(getTimestamp(tradeDate));
                    tradePriceFileVORow.setProcDate(getTimestamp(tradeDate));

                    if (instrumentVoRowImpl.getStatus().equals("ACTIVE") &&
                        instrumentVoRowImpl.getInstrumentGroup() != null) {
                        tradePriceFileVORow.setValidStatus("TRUE");
                        tradePriceFileVORow.setProcStatus("V");
                    }
                    tradePriceFileVORow.setOpenPrice(getBigDecimal(recordList.get(1)));
                    tradePriceFileVORow.setHighPrice(getBigDecimal(recordList.get(2)));
                    tradePriceFileVORow.setLowPrice(getBigDecimal(recordList.get(3)));
                    tradePriceFileVORow.setClosePrice(getBigDecimal(recordList.get(4)));
                    tradePriceFileVORow.setMarketType("P");
                } else
                    invalidInstruments.add(recordList.get(0));

            } else if (dashLineCounter == 3)
                break;
        }

        importExtFilesVORow.setTotalRec(totalRecord);
        importExtFilesVORow.setValidateRec(totalRecord - invalidInstruments.size());
        importExtFilesVORow.setErrorRec(invalidInstruments.size());
        importExtFilesVORow.setProcessedRec(0);
        
        return invalidInstruments;

    }


    //    private boolean isValidateInstrument(InstrumentsVOImpl instrumentsVOImpl, String shortName) {
    //
    //        instrumentsVOImpl.setWhereClause("SHORT_NAME = " + "\'" + shortName + "\'");
    //        instrumentsVOImpl.executeQuery();
    //        InstrumentsVORowImpl instrument = (InstrumentsVORowImpl)instrumentsVOImpl.first();
    //
    //        return instrument.getInstrumentId() != 0 ? true : false;
    //    }

    private InstrumentsVORowImpl validateInstrument(InstrumentsVOImpl instrumentsVOImpl, String shortName) {

        instrumentsVOImpl.setWhereClause("SHORT_NAME = " + "\'" + shortName + "\'");
        instrumentsVOImpl.executeQuery();
        InstrumentsVORowImpl instrument = (InstrumentsVORowImpl)instrumentsVOImpl.first();

        return instrument != null ? instrument : null;
    }

    private BigDecimal getBigDecimal(String value) throws NumberFormatException {
        return BigDecimal.valueOf(Double.valueOf(value)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private Timestamp getTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }
}
