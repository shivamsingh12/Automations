package com.company;

import org.apache.commons.csv.CSVPrinter;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public abstract class ECP {
    private final Map<String,Action> errorAndEcpMap;
    private final OnlineTool ONLINE_TOOL;
    private final CSVPrinter csvLogger;
    private final int MAX_NO_OF_ENTRIES_IN_REPORT_CACHE = 1000; // max no of entries in the hashmap mainting cache of all trxs in the report
    private final Map<String,String> reportCacheMap = new LinkedHashMap<>(){ // hashmap for a O(1) fetch time to check for a duplicate entry
        @Override                                                      // linked for a queue like behaviour, to remove oldest entry and
        protected boolean removeEldestEntry(final Map.Entry eldest) {  // prevent hogging of memory
            return size() > MAX_NO_OF_ENTRIES_IN_REPORT_CACHE;         // in short behaving like a cache with constant size and constant lookup time
        }
    };

    public ECP(Map<String , Action> map, OnlineTool onlineTool, CSVPrinter csvLogger){
        this.errorAndEcpMap = map;
        this.ONLINE_TOOL = onlineTool;
        this.csvLogger = csvLogger;
        //testing purpose
        //errorAndEcpMap.put("CTS Check conv log for faillure reason"+ "TLG"+"CCN"+"10"+"0",Action.ANCESTER_RESEND);
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

                    default:
                        System.out.println("ecp might not be supported");
            }
        }
        return wasActionPerformed;
    }

    private void logTrx(Transaction transaction, String ecp, String ecpDone) throws IOException {
        String reportCacheMapKey = transaction.CTN + transaction.TRANSACTION_NUMBER + transaction.RETRIES;
        System.out.println("inside logTrx");
        if(reportCacheMap.get(reportCacheMapKey)!=null){
            System.out.println(" entry already exists in the report");
            return; // if the linked hash map contains the key / is not null, do nothing and return
        }
        else{
            reportCacheMap.put(reportCacheMapKey, reportCacheMapKey);
            csvLogger.printRecord(LocalDateTime.now(ZoneId.systemDefault()),transaction.MARKET,transaction.TRANSACTION_TYPE,transaction.ERROR,transaction.CTN,transaction.TRANSACTION_NUMBER,transaction.RETRIES,transaction.DIGIT, ecp, ecpDone);
            csvLogger.flush();
            System.out.println(" added the entry in the report");
            // else continue with the flow of control and add the entry in the cache map
        }
    }

    public void closeLogger() throws IOException{
        csvLogger.flush();
        csvLogger.close();
    }

    public enum Action{
        RESEND, CLEAN_RESEND, HANDLE, RSA_HANDLE
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
            if(List.of(Transaction.TransactionType.RSA, Transaction.TransactionType.CCN).contains(transaction.TRANSACTION_TYPE)){
                System.out.println(transaction);
                System.out.println("ecp blocked by preconditions set | RSA | CCN | trx type");
            }
            else if(action!=null){
                segragatedMap.get(action).add(transaction);
                if(action == Action.RESEND || action.equals(Action.CLEAN_RESEND) || action.equals(Action.HANDLE)){
                    logTrx(transaction,action.name(),action.name());
                }
            }
            else{
                System.out.println(transaction);
                System.out.println("no ecp for this error");
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

