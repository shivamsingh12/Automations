package com.company;

import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Transaction {
    public final String ERROR;
    public final Market MARKET;
    public final TransactionType TRANSACTION_TYPE;
    public final String DIGIT;
    public final String CTN;
    public final String TRANSACTION_NUMBER;
    public final int RETRIES;
    public final String DATE_TIME;
    public final int NO_OF_DEVICE_ERRORS;
    public final WebElement WEB_ELEMENT;
    public final String X_PATH;

    public Transaction(String transactionNumber, String error, String ctn, String market, String transactionType, String retries, String dateTime, String noOfDeviceErrors ,WebElement webElement, String xPath){
        this.MARKET = getMarket(Market.valueOf(market));
        this.CTN = ctn;
        this.DIGIT = Integer.toString(ctn.length());
        this.TRANSACTION_NUMBER = transactionNumber;
        this.TRANSACTION_TYPE = TransactionType.valueOf(transactionType);
        this.RETRIES = Integer.parseInt(retries);
        this.WEB_ELEMENT = webElement;
        this.ERROR = error;
        this.DATE_TIME = dateTime;
        this.NO_OF_DEVICE_ERRORS = Integer.parseInt(noOfDeviceErrors);
        this.X_PATH = xPath;
    }

    @Override
    public String toString(){
        String transactionToString = String.format(" DateTime : %s | TrxNo : %s | CTN : %s | Market : %s | TrxType %s | Error : %s |",DATE_TIME,TRANSACTION_NUMBER,CTN,MARKET.name(),TRANSACTION_TYPE.name(),ERROR);
        return transactionToString;
    }

    private Market getMarket(Market market){
        List<String> tlgMarkets = Stream.of(TLGMarket.values())
                .map(TLGMarket::name).toList();
        if (tlgMarkets.contains(market.name()))
            return Market.TLG;
        else
            return market;
    }

    // this enum also has all the 32 markets so that we can pass the 32 market as a Market rather than
    // TLGMarket and then simply replace them with Market.TLG
    // it contains all the markets there are
    public enum Market{
        BOS, HCL, MWR, ARK, GAC, GPL, IND, NWS,
        GLF, MTZ, PAC, TNK, NCA, AUS, GLR, PHI,
        SNE, FLP, ILL, MNY, TUL, WTX, DLS, NYR,
        COR, LAR, OKC, RGV, WAS, ALH, SAN, STL,
        CSI, EOD, GEO, WLL, NSM, TLG, CKT, JSP,
        FEM, NOT
    }

    // used to represent all the 32 tlg markets, which we can check against when we fetch
    // error details and simply replace them with Market.TLG
    private enum TLGMarket{
        BOS, HCL, MWR, ARK, GAC, GPL, IND, NWS,
        GLF, MTZ, PAC, TNK, NCA, AUS, GLR, PHI,
        SNE, FLP, ILL, MNY, TUL, WTX, DLS, NYR,
        COR, LAR, OKC, RGV, WAS, STL, SAN, ALH
    }

    // the REST field is used in the ECP class to represent when a group of transaction type have same ecp
    // so that we wont need to have seperate entries for each of them in the csv sheet, we can just represnt them
    // by REST and also represent the 32 tlg markets with simply TLG and also while fetching the ecp from the Map,
    // first we check for the trx type we have got from error
    // if we dont have ecp for that one, we try for REST, if still there is not ecp available for trx that means
    // manual intervention is needed
    public enum TransactionType{
        ACT, CCD, CCN, CCY, CHS, CMM, DAC, RIC,
        RSC, RSM, RSO, RST, SUS, MIA, MID, RLD,
        FAC, FCG, FCH, FCN, FDA, FFV, FLU, FNT,
        FSD, FSE, FTF, FUW, JAC, JCS, JCN, JDA,
        RLA, RSA, FPO, FWO, FRG, FPR, FSC, REST,
        CAT, JPC, UNC, RPA, UTR, RSE, CNU, NTY
    }
}
