package com.company;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ErrorHandler {
    private static ErrorHandler errorHandler;
    private ECP ecp;
    private OnlineTool onlineTool;
    private WebDriver driver;

    private Map<String, String> configStrings;
    private Map<Integer, Map<String, String>> links;
    private Map<String, ECP.Action> ecpMap;
    CSVPrinter csvPrinter;
    public final BlockingQueue<String> messageQueue;
    private State state = State.UNSET_STATE;
    private long lastSetTime;
    private volatile boolean pause = false;
    private volatile boolean stop = false;

    private String id;
    private char[] password;

    // all below keys and their values should be loaded into cofigStrings and retrieved from there
    //


    private ErrorHandler() {
        configStrings = new HashMap<>();
        ecpMap = new HashMap<>();
        messageQueue = new ArrayBlockingQueue<String>(1000);
    }

    public static ErrorHandler getErrorHandler() {
        if (errorHandler == null) {
            errorHandler = new ErrorHandler();
        }
        return errorHandler;
    }

    public boolean loadECP() throws Exception {
        if(ecpMap == null) ecpMap = new HashMap<>();
        else ecpMap.clear();
        try {
            File input = new File(configStrings.get("userHomeDataDir")+"\\ErrorHandlerModification.xlsx");
            String password = "3u7uC5el";
            Workbook wb = WorkbookFactory.create(input, password);
            Sheet sheet = wb.getSheetAt(0);
            File temp = File.createTempFile("file_",".csv",new File(configStrings.get("userHomeDataDir")));
            writeWorkbookAsCSVToOutputStream(wb, new FileOutputStream(temp));
            setState(State.LOAD_ECP);
            Reader file = Files.newBufferedReader(Paths.get(configStrings.get("userHomeDataDir") + "\\"+temp.getName()));

            Iterable<CSVRecord> records = CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).build().parse(file);

            for (CSVRecord record : records) {
                String error = record.get("message");
                String trxtype = record.get("trxtype");
                String retries = record.get("retries");
                String is10digit = record.get("is10digit");
                String market = record.get("market");
                ECP.Action action = ECP.Action.valueOf(record.get("ecp"));
                String isEnabled = record.get("isEnabled");
                if(!isEnabled.equalsIgnoreCase("N")) ecpMap.put(error + market + trxtype + is10digit + retries, action);
            }

            messageQueue.put("Operation : LoadEcp \n Status : Success \n"+ecpMap.size());
            temp.delete();
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            messageQueue.put("Operation : LoadEcp \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }
    }

    public boolean createLogs() throws Exception {
        setState(State.CREATE_LOG);
        try {
            String fileName = configStrings.get("userHomeDataDir") + "\\ErrorHandlerReport\\" + LocalDate.now() + "_" + configStrings.get("instance") + "_" + "_ErrorHandler.log.csv";
            try {
                System.out.println(" creating new log and inserting header");
                csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(fileName), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND), CSVFormat.EXCEL);
                csvPrinter.printRecord("datetime", "market", "trxtype", "message", "ctn", "trxno", "retries", "is10digit", "ecp", "ecpdone");
                csvPrinter.flush();            } catch (FileAlreadyExistsException e) {
                System.out.println(" log already exists" + e.toString());
                csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(fileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND), CSVFormat.EXCEL);
            }
            messageQueue.put("Operation : CreateLogs(csv report) \n Status : Success \n");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            messageQueue.put("Operation : CreateLogs(csv report) \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }

        String logFileName = configStrings.get("userHomeDataDir") + "\\Logs\\Debug\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-h-mm")) + configStrings.get("instance") + "_ErrorHandler.debug.log";
        PrintStream logFile = null;
        try {
            logFile = new PrintStream(String.valueOf(Paths.get(logFileName)));
            System.setOut(logFile);
            messageQueue.put("Operation : CreateLogs(debug log) \n Status : Success \n");

        } catch (Exception e) {
            e.printStackTrace();
            messageQueue.put("Operation : CreateLogs(debug log) \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }

        String errorFileName = configStrings.get("userHomeDataDir") + "\\Logs\\Error\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-h-mm")) + configStrings.get("instance") + "_ErrorHandler.error.log";
        PrintStream errorLogFile = null;
        try {
            errorLogFile = new PrintStream(String.valueOf(Paths.get(errorFileName)));
            System.setErr(errorLogFile);
            messageQueue.put("Operation : CreateLogs(error log) \n Status : Success \n");

        } catch (Exception e) {
            e.printStackTrace(System.err);
            messageQueue.put("Operation : CreateLogs(error log) \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }
        return true;
    }

    public boolean loadConfig() throws Exception {
        setState(State.LOAD_CONFIG);
        //todo
        //all the hardcoded string must be loaded from here
        configStrings.put("userHomeDataDir",System.getProperty("user.home") + "\\ErrorHandlerFilesModification");
        configStrings.put("links",configStrings.get("userHomeDataDir")+"\\links.xml");
        return true;
    }

    public boolean loadLinks() throws Exception {
        setState(State.LOAD_LINKS);
        try {
            links = new HashMap<>();
            // solve staleelement
            // add in links.xml
            // add also config.xml which is called at the start of the applications
            // add loading config from remote afterwords as it will require REST call
            File stocks = new File(configStrings.get("links"));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stocks);
            doc.getDocumentElement().normalize();

            System.out.println("root of xml file" + doc.getDocumentElement().getNodeName());
            NodeList nodes = doc.getElementsByTagName("link");
            System.out.println("==========================");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int id = Integer.parseInt(getValue("id", element));
                    String name = getValue("name", element);
                    String home =  getValue("home", element);
                    String deviceQ =  getValue("deviceq", element);
                    String errorHandler = getValue("errorhandler", element);
                    String subService =  getValue("subservice", element);
                    Map<String, String> link = new HashMap<>();
                    link.put("id",Integer.toString(id));
                    link.put("name",name);
                    link.put("home",home);
                    link.put("deviceq",deviceQ);
                    link.put("errorhandler",errorHandler);
                    link.put("subservice",subService);
                    links.put(id,link);
                    }
                }
        } catch (Exception ex) {
            ex.printStackTrace();
            messageQueue.put("Operation : loadLinks \n Status : Fail \n Reason : "+ ex.toString()+"\n");
            return false;
        }
        messageQueue.put("Operation : loadLinks \n Status : Success \n");
        return true;
    }

    private static String getValue(String tag, Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }

    public Map<Integer, Map<String, String>> getLinks(){
        return links;
    }

    public void setConfigStrings(String key, String value){
        configStrings.put(key,value);
    }

    public String getConfigStrings(String key){
        return configStrings.get(key);
    }

    public void setState(State state){
        this.state = state;
        this.lastSetTime = System.currentTimeMillis();
    }

    public State getState(){
        return state;
    }

    public synchronized float getLastUpdatedTime(){
        return ((float)(System.currentTimeMillis() - lastSetTime))/1000;
    }

    public synchronized boolean startErrorHandler(Map<String,String> link, String id, char[] password, boolean isHeadless) throws Exception{
        setState(State.STARTING);
        pause = stop = false;
        try {
            WebDriverManager.edgedriver().proxy("http://genproxy.amdocs.com:8080").setup();
            EdgeOptions edgeOptions = new EdgeOptions();
            edgeOptions.setHeadless(isHeadless);
//        edgeOptions.addArguments("disable-gpu");
            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            desiredCapabilities.setAcceptInsecureCerts(true);
            //desiredCapabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
            edgeOptions = edgeOptions.merge(desiredCapabilities);
            driver = new EdgeDriver(edgeOptions);
            driver.manage().window().maximize();

            onlineTool = OnlineTool.getOnlineTool(driver);
            ecp = ECPBasicErrors.getEcpBasicErrors(ecpMap, onlineTool, csvPrinter);
            messageQueue.put("Operation : StartErrorHandler(initialize) \n Status : Success \n");
        }catch (Exception e){
            e.printStackTrace(System.err);
            messageQueue.put("Operation : StartErrorHandler(initialize) \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }

        try{
            onlineTool.testStart(link, id, new String(password));
//            onlineTool.start(link, id, new String(password));
            messageQueue.put("Operation : StartErrorHandler(logOn) \n Status : Success\n ");
            setState(State.STARTED);
        }catch (Exception e){
            e.printStackTrace(System.err);
            messageQueue.put("Operation : StartErrorHandler(logOn) \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }
        return true;
    }

    public boolean repairWindow(Map<String,String> link) throws Exception{
        try {
            onlineTool.initializeWindows(link);
            messageQueue.put("Operation : InitializeWindow \n Status : Success\n ");
            return true;
        }catch (Exception e){
            e.printStackTrace(System.err);
            messageQueue.put("Operation : InitializeWindow( \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }
    }

    public synchronized boolean stopErrorHandler() throws Exception{
        onlineTool.close();
        ecp.closeLogger();
        setState(State.STOPPED);
        return stop;
    }

    public synchronized void pauseErrorHandler() throws Exception{
        setState(State.PAUSED);
        pause = true;
    }

    public synchronized void resumeErrorHandler() throws Exception{
        setState(State.RESUMED);
        pause = false;
    }

    public boolean createDirectories() throws Exception{
        setState(State.CREATE_DIRECTORIES);
        String userHome = System.getProperty("user.home");
        try {
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification"))){
                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFilesModification"));
            }
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification\\Logs"))){
                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFilesModification\\Logs"));
            }
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification\\Logs\\Error"))){
                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFilesModification\\Logs\\Error"));
            }
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification\\Logs\\Debug"))){
                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFilesModification\\Logs\\Debug"));
            }
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification\\ErrorHandlerReport"))){
                Files.createDirectory(Paths.get(System.getProperty("user.home")+"\\ErrorHandlerFilesModification\\ErrorHandlerReport"));
            }
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification\\ErrorHandlerModification.xlsx"))){
                Files.copy(Paths.get(userHome+"\\ErrorHandlerModification.xlsx"),Paths.get(configStrings.get("userHomeDataDir")+"\\ErrorHandlerModification.xlsx"), StandardCopyOption.REPLACE_EXISTING);
            }
            if(!Files.exists(Paths.get(userHome+"\\ErrorHandlerFilesModification\\links.xml"))){
                Files.copy(Paths.get(userHome+"\\links.xml"),Paths.get(configStrings.get("userHomeDataDir")+"\\links.xml"), StandardCopyOption.REPLACE_EXISTING);
            }
            messageQueue.put("Operation : CreateDirectories \n Status : Success \n");
            return true;
        }
        catch (Exception e){
            e.printStackTrace(System.err);
            messageQueue.put("Operation : CreateDirectories \n Status : Fail \n Reason : " + e.toString() + "\n");
            return false;
        }
        finally {

        }
    }

    public void run(){
        // todo
        // call all the above methods which needs to be called before running, here before entering while loop
        // all the exception handlings here, if exception occur, pass it into the messageQueue
        // check against breakfromstate, pause, resume, stop here
        // in break from state, break from the for loop inside while loop
        // in pause just wait without performing anything
        // in stop break away from the while loop
        // reinitialize on staleElement and retry, just retry for notFoundException, notInteractable
        // put wait time at minimum for above cases, not for notFoundException be around 30 second
        // for StaleElement, around 2 second and then reinitialize

        try {
            while (!stop) {
                if(state == State.PAUSED || pause) continue;
                if(state == State.STOPPED || stop) break;
                if(Thread.interrupted()){
                    if(pause){
                        setState(State.PAUSED);
                        continue;
                    }
                    if(stop) {
                        setState(State.STOPPED);
                        break;
                    }
                }
                try {
                    onlineTool.waitTillOverlay();
                    List<Transaction> transactions = onlineTool.getListOfTransactions();
                    setState(State.RUNNING);
                    System.out.println(transactions.size());
//                    ecp.getAllInHandling(transactions);
//                    ecp.applyAll(transactions);
                    for (Transaction transaction : transactions) {
                        setState(State.RUNNING);
                        if(Thread.interrupted() && (state == State.PAUSED || state == State.STOPPED || stop || pause || stop) ) break;
//                        System.out.println(transaction.WEB_ELEMENT.getText());
                        System.out.println(transaction);
                        ecp.applyECP(transaction);
                        Thread.sleep(700);
                        onlineTool.waitTillOverlay();
                    }
                    setState(State.RUNNING);
                    Thread.sleep(1000*3);
                    onlineTool.refresh();
                    Thread.sleep(1000*3);
                    System.out.println("refreshed");

                } catch (TimeoutException e) {
                    System.out.println("Significant Exception occured : TimeoutException switch might not be working or unresponsive " + e.getMessage());
                    e.printStackTrace(System.err);
                    setState(State.TIMEOUT);
                } catch (UnhandledAlertException e) {
                    driver.switchTo().alert().accept();
                    System.out.println("Significant Exception occured : Alert is present, switch might not be working or unresponsive " + e.getMessage());
                    e.printStackTrace(System.err);
                    setState(State.UNHANDLED_ALERT);
                } catch (StaleElementReferenceException | ElementNotInteractableException e) {
                    System.out.format("some insignificant exception occured %s, ignoring...%n", e.getMessage());
                    e.printStackTrace(System.err);
                    setState(State.STALE_ELEMENT);
                }
                catch (SocketException | SocketTimeoutException e) {
                    System.out.println(" SocketException " + e.getMessage());
                    e.printStackTrace(System.err);
                }
                catch (InterruptedException e){
                    System.out.println("interrupted");
                    setState(State.INTERRUPTED);
                    if(pause) setState(State.PAUSED);
                    if(stop) setState(State.STOPPED);
                }
                catch (NoSuchWindowException e){
                    setState(State.NO_SUCH_WINDOW);
                    System.out.println("no such window"+e.getMessage());
                    Actions action = new Actions(driver);
                    Action enter = action.keyDown(Keys.ENTER).build();
                    try{
                        enter.perform();
                    }catch (Exception f){
                        System.out.println("unable to perform enter"+f.getMessage());
                    }
                }
                catch (NullPointerException e){
                    System.out.println("null pointer" + e.getMessage());
                    e.printStackTrace(System.err);
                    break;
                }
                catch(Exception e){
                    setState(State.UNKNOWN_EXCEPTION);
                    System.out.println(" UnknownException " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }catch (Exception e){
            e.printStackTrace(System.err);
        }finally {
            errorHandler.setState(State.STOPPED);
        }
    }
    public enum State{
        LOAD_CONFIG, CREATE_DIRECTORIES, LOAD_ECP, CREATE_LOG, STARTING, STARTED, STOPPED, PAUSED, RESUMED, RUNNING, LOAD_LINKS,
        TIMEOUT, STALE_ELEMENT, UNHANDLED_ALERT, NO_SUCH_WINDOW, INTERRUPTED, UNSET_STATE,
        UNKNOWN_EXCEPTION
    }
    private void writeWorkbookAsCSVToOutputStream(Workbook workbook, OutputStream out) {
        CSVPrinter csvPrinter = null;
        try {
            csvPrinter = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT);
//            Iterable<CSVRecord> records = CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).build().parse(file);
            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0); // Sheet #0 in this example
                Iterator<Row> rowIterator = sheet.rowIterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        switch (cell.getCellType())
                        {
                            case NUMERIC:
                                csvPrinter.print((int)(cell.getNumericCellValue()));
                                break;
                            case STRING:
                                csvPrinter.print(cell.getStringCellValue());
                                break;
                        }
                    }
                    csvPrinter.println(); // Newline after each row
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (csvPrinter != null) {
                    csvPrinter.flush(); // Flush and close CSVPrinter
                    csvPrinter.close();
                }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
