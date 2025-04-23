package com.company;

import org.apache.commons.csv.CSVPrinter;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ECPBasicErrors extends ECP{
    private static ECPBasicErrors ecpBasicErrors;

    private ECPBasicErrors(Map<String, Action> map, OnlineTool onlineTool, CSVPrinter csvPrinter){
        super(map,onlineTool, csvPrinter);
    }

    // ECP is singleton because having multipe ecp objects doesnt make sense
    public static ECPBasicErrors getEcpBasicErrors(Map<String, Action> map, OnlineTool onlineTool, CSVPrinter csvPrinter){
        if(ecpBasicErrors == null){
            ecpBasicErrors = new ECPBasicErrors(map,onlineTool, csvPrinter);
        }
        return  ecpBasicErrors;
    }

    @Override
    public boolean applyECP(Transaction transaction) throws Exception {
        if(satisfiesPreconditions(transaction)){
            return super.applyECP(transaction);
        }
        else {
            System.out.println("error handling blocked by preconditions set { RSA | CCN trx }");
            return true;
        }
    }

    private boolean satisfiesPreconditions(Transaction transaction) {
        return !(List.of(Transaction.TransactionType.CCN ,Transaction.TransactionType.RSA).contains(transaction.TRANSACTION_TYPE)||transaction.NO_OF_DEVICE_ERRORS>3);
    }
}
