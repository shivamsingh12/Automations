package com.company;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.edge.EdgeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.time.LocalDate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static List<String> trxNo = new ArrayList<>();
    private static List<String> ctns = new ArrayList<>();
    private static List<String> results = new ArrayList<>();
    private static List<Transaction> trxForRsa = new ArrayList<>();
    private static AtomicBoolean wantToContinue = new AtomicBoolean(true);
    private static String userHomeDataDir;
    public static Thread mainThread;

    public static void main(String[] args){
        boolean isHeadless = false;
        boolean useErrorHandlerRegistry = true;
        if(args.length>0 && (args[0].equals("-h") || args[0].equals("-H"))){
            isHeadless = true;
        }
        if(args.length>0 && (args[0].equals("-f") || args[0].equals("-F"))){
            useErrorHandlerRegistry = false;
        }
//        ErrorHandler errorHandler = ErrorHandler.getErrorHandler();
//        Thread cliThread = new Thread(CLI.getCLI(errorHandler,isHeadless));
//        cliThread.start();
//        while(errorHandler.getState()!=ErrorHandler.State.STOPPED){ continue;}
        ErrorHandler errorHandler = ErrorHandler.getErrorHandler();
        Thread mainThread = Thread.currentThread();
        CLI cli = CLI.getCLI(errorHandler,isHeadless,useErrorHandlerRegistry,mainThread);
        new Thread(cli).start();
        while(true){
            try {
                if (errorHandler.getState() == ErrorHandler.State.STARTED || errorHandler.getState() == ErrorHandler.State.RESUMED) {
                    consoleWrite(" Started ");
                    errorHandler.run();
                    break;
                }
                if (errorHandler.getState() == ErrorHandler.State.STOPPED) {
                    System.out.println("Stopped in Main");
                    break;
                } else {
                    Thread.sleep(500);
                    continue;
                }
            }catch (InterruptedException e){
                System.out.println("interruted in Main");
            }
        }
    }

    private static void consoleWrite(String string){
        System.console().writer().write(string+"\n");
        System.console().flush();
    }

//    public static void not_main(String[] args) throws IOException {
//        // write your code here
//        try {
//            userHomeDataDir = System.getProperty("user.home") + "\\ErrorHandlerFiles";
//            createDirectories();
//
//            Reader file = Files.newBufferedReader(Paths.get(userHomeDataDir + "\\ErrorHandler.csv"));
//
//            Iterable<CSVRecord> records = CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).build().parse(file);
//            HashMap<String, ECP.Action> ecpMap = new HashMap<>();
//
//            for (CSVRecord record : records) {
//                String error = record.get("message");
//                String trxtype = record.get("trxtype");
//                String retries = record.get("retries");
//                String is10digit = record.get("is10digit");
//                String market = record.get("market");
//                ECP.Action action = ECP.Action.valueOf(record.get("ecp"));
//                ecpMap.put(error + market + trxtype + is10digit + retries, action);
//            }
//
//            file.close();
//
//            Console console = System.console();
//            consoleWrite("Enter the server");
//            String server = console.readLine();
//            consoleWrite("Enter the instance");
//            String instance = console.readLine();
//            consoleWrite("Enter the id");
//            String id = console.readLine();
//            consoleWrite("Enter the password");
//            char[] password = console.readPassword();
//
//            String fileName = userHomeDataDir + "\\ErrorHandlerReport\\" + LocalDate.now() + "_" + instance + "_" + "_ErrorHandler.log.csv";
//            CSVPrinter csvPrinter;
//            try {
//                System.out.println(" creating new log and inserting header");
//                csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(fileName), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND), CSVFormat.EXCEL);
//                csvPrinter.printRecord("datetime", "market", "trxtype", "message", "ctn", "trxno", "retries", "is10digit", "ecp", "ecpdone");
//                csvPrinter.flush();
//            } catch (Exception e) {
//                System.out.println(" log already exists" + e.toString());
//                csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(fileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND), CSVFormat.EXCEL);
//            }
//
//            WebDriverManager.edgedriver().proxy("http://genproxy.amdocs.com:8080").setup();
//            EdgeOptions edgeOptions = new EdgeOptions();
////        edgeOptions.setHeadless(true);
////        edgeOptions.addArguments("disable-gpu");
//            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
//            desiredCapabilities.setAcceptInsecureCerts(true);
//            //desiredCapabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
//            edgeOptions = edgeOptions.merge(desiredCapabilities);
//            WebDriver driver = new EdgeDriver(edgeOptions);
//
////        WebDriver driver = new EdgeDriver(edgeOptions);
//
//            OnlineTool onlineTool = OnlineTool.getOnlineTool(driver);
//            ECP ecp = ECPBasicErrors.getEcpBasicErrors(ecpMap, onlineTool, csvPrinter);
//
//            // theres also two ecp sheets, one with just the test ecps and copy one with all ecps
//            // for generating csv, use excel {CSV UTF-8 COMMA DELIMITED} format then use notepad to remove BOM
//            // remove BOM from excel csv by opening it in notepad, save as all files, encoding UTF-8, not UTS-8 with BOM then save
//
//            //currenty its hardcoded in the ECPBasicErrors that this script should not act on CCN or RSA type
//            //or more than 1 retires or more than 1 device error, so we can use REST for group of trx types who have
//            //similar ecp in ecp sheet, REST also includes CCN/RSA but ECP object itself prevents from acting on it
//            //even if ecp is available. so we can safely use it in ecp sheet, without worrying about it acting on RSA,CCN types
//            //we can safely remove this hardcoded restriction as soon as
//            //ancestor resend capability is added in the onlinetool class, so that we can safely handle CCN types
//            //and imsi doesnot exist types or whatever ecp requires ancestor resend
//
//            Thread promptUser = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while (true) {
//                        consoleWrite("stop Errorhanlder? [yes][no]");
//                        String promptInput = System.console().readLine();
//                        promptInput = promptInput == null ? "" : promptInput;
//                        if (promptInput.equalsIgnoreCase("yes")) {
//                            wantToContinue.set(false);
//                            break;
//                        } else if (Thread.interrupted()) break;
//                        else continue;
//                    }
//                }
//            });
//
//            String logFileName = userHomeDataDir + "\\Logs\\Debug\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-h-mm")) + instance + "_ErrorHandler.debug.log";
//            PrintStream logFile = null;
//            try {
//                logFile = new PrintStream(String.valueOf(Paths.get(logFileName)));
//            } catch (Exception e) {
//                e.printStackTrace();
//                consoleWrite("unable to create debug file"+e.getMessage());
//            }
//            System.setOut(logFile);
//
//            String errorFileName = userHomeDataDir + "\\Logs\\Error\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-h-mm")) + instance + "_ErrorHandler.error.log";
//            PrintStream errorLogFile = null;
//            try {
//                errorLogFile = new PrintStream(String.valueOf(Paths.get(errorFileName)));
//            } catch (Exception e) {
//                e.printStackTrace();
//                consoleWrite("unable to create error file"+e.getMessage());
//            }
//            System.setErr(errorLogFile);
//
//            try {
////                onlineTool.start("https://zlpv%s.vci.att.com:8443/SWC%s/", id, new String(password), server, instance);
//                //onlineTool.testStart("https://zltv9345.vci.att.com:8443/SWCOnlineCRFQC/home.html", id, new String(password),server, instance);
//                Thread.sleep(1000 * 5);
//                consoleWrite("press enter to start errorHandler");
//                console.readLine();
//                promptUser.start();
////
//                int i = 0; // used to loop through RSA list
//                boolean wasActionPerformed = false;
//                int noOfActionsNotPerformed = 0;
//
////            getCtnsFromFile();
////            consoleWrite(" RSA for "+trxForRsa.size()+ "ctns : do you want to continue%n");
//
//                while (wantToContinue.get()) {
//                    noOfActionsNotPerformed = 0;
//                    try {
//                        List<Transaction> transactions = onlineTool.getListOfTransactions();
//                        System.out.println(transactions.size());
//                        ecp.getAllInHandling(transactions);
//                        for (Transaction transaction : transactions) {
//                            if(!wantToContinue.get()) break;
//                            System.out.println(transaction);
//                            wasActionPerformed = ecp.applyECP(transaction);
//                            if (!wasActionPerformed) {
//                                noOfActionsNotPerformed += 1;
//                            }
//                            if (noOfActionsNotPerformed >= 10) {
//                                noOfActionsNotPerformed = 0;
//                                break;
//                            }
//                            Thread.sleep(500);
//                        }
//
//                        Thread.sleep(1000*5);
//                        onlineTool.refresh();
//                        System.out.println("refreshed");
//
//
////                    if (i >= trxForRsa.size()) break;
////                    System.out.println(i+". "+trxForRsa.get(i).CTN);
////                    if (onlineTool.rsa(trxForRsa.get(i))) {
////                        results.add(i+". "+trxForRsa.get(i).CTN + " : DONE");
////                    } else {
////                        results.add(i+". "+trxForRsa.get(i).CTN + " : NOT_DONE");
////                    }
////                    i += 1;
//
//                    } catch (TimeoutException e) {
//                        System.out.println("Significant Exception occured : TimeoutException switch might not be working or unresponsive " + e.getMessage());
////                        consoleWrite("timeoutException occured, exit errorhandler[yes] if occurs multiple times");
//                        e.printStackTrace(System.err);
//                    } catch (UnhandledAlertException e) {
////                    onlineTool.reload();
//                        driver.switchTo().alert().accept();
//                        System.out.println("Significant Exception occured : Alert is present, switch might not be working or unresponsive " + e.getMessage());
//                        e.printStackTrace(System.err);
////                        consoleWrite("Alert is present, switch might be slow");
//                    } catch (StaleElementReferenceException | ElementNotInteractableException e) {
//                        System.out.format("some insignificant exception occured %s, ignoring...%n", e.getMessage());
//                        e.printStackTrace(System.err);
//                    }
//                    catch (SocketException | SocketTimeoutException e) {
//                        System.out.println(" SocketException " + e.getMessage());
//                        e.printStackTrace(System.err);
////                        consoleWrite("Socket Exception : exit if occurs multiple times");
//                    }
//                    catch (InterruptedException e){
//                        System.out.println("interrupted");
//                        break;
//                    }
//                    catch (NoSuchWindowException e){
//                        System.out.println("no such window"+e.getMessage());
//                        Actions action = new Actions(driver);
//                        Action enter = action.keyDown(Keys.ENTER).build();
//                        try{
//                            enter.perform();
//                        }catch (Exception f){
//                            System.out.println("unable to perform enter"+f.getMessage());
//                        }
//                    }
//                    catch(Exception e){
//                        System.out.println(" UnknownException " + e.getMessage());
//                        e.printStackTrace(System.err);
//                        consoleWrite("UnknownException : exit if occurs multiple times" + e.getMessage());
//                    }
//                }
//            } catch (InterruptedException e) {
//                System.out.println(" thread interrupted ");
//            }
////            catch (SocketException | SocketTimeoutException | Error e){
////                System.out.println(" socket timeout, switch might be slow");
////                e.printStackTrace(System.err);
////            }
//            finally {
//                if (promptUser.isAlive()) promptUser.interrupt();
//                results.stream().distinct().forEach(System.out::println);
//                console = null;
//                ecp.closeLogger();
//                onlineTool.close();
//            }
//        }catch (Exception e){
//            consoleWrite(e.getMessage());
//            e.printStackTrace(System.err);
//            new Scanner(System.in).next();
//        }
//    }
//
//
//    private static void getCtnsFromFile() throws IOException{
//        Reader file = Files.newBufferedReader(Paths.get(System.getProperty("user.home")+"\\Downloads\\rsa.csv"));
//
//        Iterable<CSVRecord> records = CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).build().parse(file);
//
//        for (CSVRecord record : records) {
//            String trx = record.get("trxno");
//            String ctn = record.get("ctn");
//            trxNo.add(trx);
//            ctns.add(ctn);
//        }
//        file.close();
//        for(int i = 0; i< ctns.size();i++){
//            trxForRsa.add(new Transaction(trxNo.get(i),null, ctns.get(i),"GPL","CCD","1",null,"1",null,""));
//        }
//    }
//
//    public static boolean createDirectories() throws IOException{
//        String userHome = System.getProperty("user.home");
//        try {
//            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFiles"))){
//                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFiles"));
//            }
//            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFiles\\Logs"))){
//                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFiles\\Logs"));
//            }
//            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFiles\\Logs\\Error"))){
//                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFiles\\Logs\\Error"));
//            }
//            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFiles\\Logs\\Debug"))){
//                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFiles\\Logs\\Debug"));
//            }
//            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFiles\\ErrorHandlerReport"))){
//                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFiles\\ErrorHandlerReport"));
//            }
//            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFiles\\ErrorHandler.csv"))){
//                Files.copy(Paths.get(userHome+"\\ErrorHandler.csv"),Paths.get(userHomeDataDir+"\\ErrorHandler.csv"), StandardCopyOption.REPLACE_EXISTING);
//            }
//            return true;
//        }
//        catch (Exception e){
//            e.printStackTrace(System.err);
//            return false;
//        }
//    }
}
