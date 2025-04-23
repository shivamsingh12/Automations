package com.company;

import org.apache.commons.csv.CSVPrinter;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ECP {
    private final Map<String,Action> errorAndEcpMap;
    private final OnlineTool ONLINE_TOOL;
    private final CSVPrinter csvLogger;

    public ECP(Map<String , Action> map, OnlineTool onlineTool, CSVPrinter csvLogger){
        this.errorAndEcpMap = map;
        this.ONLINE_TOOL = onlineTool;
        this.csvLogger = csvLogger;
        //testing purpose
//        errorAndEcpMap.put("AOTA: Subscription Not Found by ICCID"+ "TLG"+"REST"+"10"+"0",Action.DEVICE_SCRIPT);
        errorAndEcpMap.put("No error code/string found for <CCS1> Logical Dvc"+"TLG"+"REST"+"10"+"4",Action.ANCESTOR_RESEND);

    }

    public boolean applyECP(Transaction transaction) throws Exception{
        Action action;
        Action actionForSpecificTrxType = errorAndEcpMap.get(transaction.ERROR+transaction.MARKET.name()+transaction.TRANSACTION_TYPE.name()+transaction.DIGIT+transaction.RETRIES);
        Action actionForRESTallTrxType = errorAndEcpMap.get(transaction.ERROR+transaction.MARKET.name()+Transaction.TransactionType.REST.name()+transaction.DIGIT+transaction.RETRIES);
        action = actionForSpecificTrxType != null ? actionForSpecificTrxType : actionForRESTallTrxType;
        boolean wasActionPerformed = false;
        if(action == null){
            System.out.println("no ecp for this error");
            return true;
        }
        else{
            switch (action){
                    case HANDLE:
                        //handle on switch tool
                        wasActionPerformed = ONLINE_TOOL.handle(transaction);
                        System.out.println(action.name());
                        if(wasActionPerformed) logTrx(transaction,action.name(),action.name());
                        break;

                    case RESEND:
                        //resend on switch tool
                        wasActionPerformed = ONLINE_TOOL.resend(transaction);
                        System.out.println(action.name());
                        if(wasActionPerformed) logTrx(transaction,action.name(),action.name());
                        break;

                    case CLEAN_RESEND:
                        //clean resend on switch tool
                        wasActionPerformed = ONLINE_TOOL.cleanResend(transaction);
                        System.out.println(action.name());
                        if(wasActionPerformed) logTrx(transaction,action.name(),action.name());
                        break;

                    case RSA_HANDLE:
                        //RSA on subservice
                        //Handle on switch tool
//                        System.out.println(action.name());
                        wasActionPerformed = ONLINE_TOOL.rsa(transaction);
                        if(wasActionPerformed){
                            wasActionPerformed = ONLINE_TOOL.handle(transaction);
                            System.out.println(action.name());
                            if(wasActionPerformed) logTrx(transaction,action.name(),action.name());
                        }
                        else{
                            wasActionPerformed = ONLINE_TOOL.handle(transaction);
                            System.out.println("Unable to RSA, error handled and Noted");
                            logTrx(transaction,action.name(),"NA");
                        }
                        break;

                    case ANCESTOR_RESEND:
                        wasActionPerformed = ONLINE_TOOL.ancestorResend(transaction);
                        logTrx(transaction,action.name(),action.name());
                        break;

                    case CONVERSATION_SCRIPT:
//                        ONLINE_TOOL.getConvScript(transaction);
                        break;

                    case DEVICE_SCRIPT:
                        ONLINE_TOOL.getDevScript(transaction);
                        break;

                    default:
                        System.out.println("ecp might not be supported");
            }
        }
        return wasActionPerformed;
    }

    private void logTrx(Transaction transaction, String ecp, String ecpDone) throws IOException {
        csvLogger.printRecord(LocalDateTime.now(ZoneId.systemDefault()),transaction.MARKET,transaction.TRANSACTION_TYPE,transaction.ERROR,transaction.CTN,transaction.TRANSACTION_NUMBER,transaction.RETRIES,transaction.DIGIT, ecp, ecpDone);
        csvLogger.flush();
    }

    public void closeLogger() throws IOException{
        csvLogger.flush();
        csvLogger.close();
    }

    public enum Action{
        RESEND, CLEAN_RESEND, HANDLE, RSA_HANDLE, ANCESTOR_RESEND, CONVERSATION_SCRIPT, DEVICE_SCRIPT
    }

    public boolean getAllInHandling(List<Transaction> transactions) throws Exception{
        List<Transaction> transactionsWithEcp = new ArrayList<>();
        for(Transaction transaction : transactions){
            Action action;
            Action actionForSpecificTrxType = errorAndEcpMap.get(transaction.ERROR + transaction.MARKET.name() + transaction.TRANSACTION_TYPE.name() + transaction.DIGIT + transaction.RETRIES);
            Action actionForRESTallTrxType = errorAndEcpMap.get(transaction.ERROR + transaction.MARKET.name() + Transaction.TransactionType.REST.name() + transaction.DIGIT + transaction.RETRIES);
            action = actionForSpecificTrxType != null ? actionForSpecificTrxType : actionForRESTallTrxType;
            if (action != null && !(List.of(Transaction.TransactionType.CCN, Transaction.TransactionType.RSA).contains(transaction.TRANSACTION_TYPE))){
                transactionsWithEcp.add(transaction);
            }
        }
        try{
            ONLINE_TOOL.getAllInHandling(transactionsWithEcp);
        }catch (Exception e){
            System.out.println("unable to get all in Handling");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    public void applyAll(List<Transaction> transactions) throws Exception{
        Map<ECP.Action,List<Transaction>> segregatedMap = getSegregatedList(transactions);
        ONLINE_TOOL.performActionOnAll(segregatedMap.get(Action.RESEND),Action.RESEND);
        Thread.sleep(500);
        ONLINE_TOOL.performActionOnAll(segregatedMap.get(Action.CLEAN_RESEND),Action.CLEAN_RESEND);
        Thread.sleep(500);
        ONLINE_TOOL.performActionOnAll(segregatedMap.get(Action.HANDLE),Action.HANDLE);
        Thread.sleep(500);

        List<Transaction> rsaList = segregatedMap.get(Action.RSA_HANDLE);
        for(Transaction transaction : rsaList){
            if(ONLINE_TOOL.rsa(transaction)){
                logTrx(transaction,Action.RSA_HANDLE.name(), Action.RSA_HANDLE.name());
            }else{
                logTrx(transaction,Action.RSA_HANDLE.name(), "NA");
            }
        }
        ONLINE_TOOL.performActionOnAll(rsaList,Action.HANDLE);
    }

    private Map<ECP.Action,List<Transaction>> getSegregatedList(List<Transaction> transactions) throws Exception{
        Map<ECP.Action,List<Transaction>> segragatedMap = new HashMap<>();
        segragatedMap.put(Action.RESEND,new ArrayList<>());
        segragatedMap.put(Action.CLEAN_RESEND,new ArrayList<>());
        segragatedMap.put(Action.HANDLE,new ArrayList<>());
        segragatedMap.put(Action.RSA_HANDLE,new ArrayList<>());

        for(Transaction transaction : transactions){
            Action action;
            Action actionForSpecificTrxType = errorAndEcpMap.get(transaction.ERROR + transaction.MARKET.name() + transaction.TRANSACTION_TYPE.name() + transaction.DIGIT + transaction.RETRIES);
            Action actionForRESTallTrxType = errorAndEcpMap.get(transaction.ERROR + transaction.MARKET.name() + Transaction.TransactionType.REST.name() + transaction.DIGIT + transaction.RETRIES);
            action = actionForSpecificTrxType != null ? actionForSpecificTrxType : actionForRESTallTrxType;
            if(action!=null){
                segragatedMap.get(action).add(transaction);
                if(action == Action.RESEND || action.equals(Action.CLEAN_RESEND) || action.equals(Action.HANDLE)){
                    logTrx(transaction,action.name(),action.name());
                }
            }
        }

        return segragatedMap;
    }

    //add predefined set of conditions in this like dont work on non (TLG and JSP) markt types,
    //error with more than 0 retires or CCN transaction types
    //main class will call applyEcp for all elements, this class should be responsibe for not calling the
    // switch online tool class for which above conditions are met and just ignore them
    // also anticipate that online tools could fail

    // load into a HashMap all errors and their ecps from a CSV
    // make an enum named action will all available ecp actions like Resend, clean resend, Handle, Rsa
    // add trx type, if sub is 15 digit, error message as KEY and enum with action enum as value

    // add an applyECP function which calls from the switchOnlineTool applys ecp
    // Switch online tools also generates log with CTN error message and ecp applied
    // add in this log whether you have applied ecp and also add whether you verified that you did apply ecp

    //preconditions should be abstract, use a template method pattern
    //also make a static factory for both of these classes

}

