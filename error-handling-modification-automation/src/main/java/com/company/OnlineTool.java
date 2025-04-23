package com.company;

import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.poi.ss.usermodel.ExcelStyleDateFormatter;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.net.SocketException;
import java.sql.Time;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class OnlineTool {
    private final WebDriver driver;
    private Map<Window,String> windows;
    private Wait<WebDriver> wait;
    private Wait<WebDriver> waitLong;
    private Wait<WebDriver> waitShort;

    private static int noOfOnlineToolInstances = 0;
    private static final int MAX_NO_OF_INSTANCES = 1;

    private OnlineTool(WebDriver driver){this.driver = driver;}

    // there can only be given no of OnlineTool instances, defined by MAX_NO_OF_INSTANCES, otherwise throw an exception
    // will also avoid memory leaks if the instantiation is done in a loop and also inconsistencies between
    // different onlineTool instances as they will different state until refreshed/reloaded
    public static OnlineTool getOnlineTool(WebDriver driver) throws Exception{
        if(noOfOnlineToolInstances<MAX_NO_OF_INSTANCES){
            noOfOnlineToolInstances+=1;
        }
        else {
            throw new UnsupportedOperationException(String.format("Cannot instantiate more than %d instances", MAX_NO_OF_INSTANCES));
        }
        OnlineTool onlineTool = new OnlineTool(driver);
        onlineTool.waitLong = new FluentWait<>(driver).withTimeout(Duration.ofMinutes(2)).pollingEvery(Duration.ofMillis(300)).ignoreAll(List.of(WebDriverException.class));
        onlineTool.wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(15)).pollingEvery(Duration.ofMillis(300)).ignoreAll(List.of(NoSuchElementException.class, ElementNotInteractableException.class, StaleElementReferenceException.class));
        onlineTool.waitShort = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(10)).pollingEvery(Duration.ofMillis(300)).ignoreAll(List.of(NoSuchElementException.class, ElementNotInteractableException.class, StaleElementReferenceException.class));
        return onlineTool;
    }

    public void start(Map<String,String> link, String id, String password) throws Exception{
        driver.get(String.format(link.get("home")));
        logOn(id,password);
        Thread.sleep(1000*5);
        waitLong.until(ExpectedConditions.presenceOfElementLocated(By.id("loggedInDateTime")));
        initializeWindows(link);
//        windows = new HashMap<>();
//        ((JavascriptExecutor)driver).executeScript(String.format("window.open(\"https://zlpv%s.vci.att.com:8443/SWC%s/main.html?datacenter=SWC%s&tabIdx=0|home|1\")",server,instance,instance));
//        ((JavascriptExecutor)driver).executeScript(String.format("window.open(\"https://zlpv%s.vci.att.com:8443/SWC%s/main.html?datacenter=SWC%s&tabIdx=0|home|4\")",server,instance,instance));
//        ((JavascriptExecutor)driver).executeScript(String.format("window.open(\"https://zlpv%s.vci.att.com:8443/SWC%s/main.html?datacenter=SWC%s&tabIdx=0|home|5\")",server,instance,instance));
//        List<String> windowList = driver.getWindowHandles().stream().toList();
//        windows.put(Window.DEVICE_Q,windowList.get(3));
//        windows.put(Window.ERROR_HANDLER,windowList.get(2));
//        windows.put(Window.SUB_SERVICE,windowList.get(1));
    }

    public void initializeWindows(Map<String,String> link){
        if(windows == null) windows = new HashMap<>();
        if(!windows.isEmpty()){
            for(Window window : Window.values()){
                try {
                    switchTo(window);
                    driver.close();
                }catch (Exception e){
                    System.out.println(" error intializing windows");
                }
            }
        }
//        ((JavascriptExecutor)driver).executeScript(String.format("window.open('%s')",link.get("deviceq")));
        ((JavascriptExecutor)driver).executeScript(String.format("window.open('%s')",link.get("errorhandler")));
        ((JavascriptExecutor)driver).executeScript(String.format("window.open('%s')",link.get("subservice")));
        ((JavascriptExecutor)driver).executeScript(String.format("window.open('%s')",link.get("errorhandler")));
        List<String> windowList = driver.getWindowHandles().stream().toList();
//        windows.put(Window.DEVICE_Q,windowList.get(3));
        windows.put(Window.ERROR_HANDLER,windowList.get(3));
        windows.put(Window.SUB_SERVICE,windowList.get(2));
        windows.put(Window.ERROR_HANDLER_HANDLE, windowList.get(1));
    }

    public boolean resend(Transaction transaction) throws Exception {
        System.out.println("currently in resend");
        switchTo(Window.ERROR_HANDLER);
        WebElement resendButton;
        boolean isTransactionSelected = false;
        boolean isTransactionHandling = false;
        boolean areAllDeselected = false;
        boolean wasResend = false;
        areAllDeselected = deselectAll();
        isTransactionHandling = getInHandling(transaction);
        isTransactionSelected = tryClick(transaction.WEB_ELEMENT);
        isTransactionSelected = isTransactionSelected || transaction.WEB_ELEMENT.getAttribute("class").contains("selected");
//        resendButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("resend")));
        resendButton = getElement("resend",IDENTIFIER.ID,2, ELEMENT_STATE.CLICKABLE);
        if(areAllDeselected && isTransactionHandling && isTransactionSelected){
            wasResend = tryClick(resendButton);
            for(int i = 0; i < 3 && !wasResend; i++ )
            {
//                resendButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("resend")));
                resendButton = getElement("resend",IDENTIFIER.ID,2, ELEMENT_STATE.CLICKABLE);
                wasResend = tryClick(resendButton);
            }
        }
        return wasResend;
    }

    public boolean cleanResend(Transaction transaction) throws Exception {
        switchTo(Window.ERROR_HANDLER);
        WebElement cleanResendButton;
        boolean isTransactionSelected = false;
        boolean isTransactionHandling = false;
        boolean areAllDeselected = false;
        boolean wasCleanResend = false;
        areAllDeselected = deselectAll();
        isTransactionHandling = getInHandling(transaction);
        isTransactionSelected = tryClick(transaction.WEB_ELEMENT);
        isTransactionSelected = isTransactionSelected || transaction.WEB_ELEMENT.getAttribute("class").contains("selected");
//        cleanResendButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("cleanResend")));
        cleanResendButton = getElement("cleanResend",IDENTIFIER.ID,2, ELEMENT_STATE.CLICKABLE);
        if(areAllDeselected && isTransactionHandling && isTransactionSelected){
            wasCleanResend = tryClick(cleanResendButton);
            for(int i = 0; i < 3 && !wasCleanResend; i++ )
            {
//                cleanResendButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("cleanResend")));
                cleanResendButton = getElement("cleanResend",IDENTIFIER.ID,2, ELEMENT_STATE.CLICKABLE);
                wasCleanResend = tryClick(cleanResendButton);
            }
        }
        return wasCleanResend;
    }

    public boolean handle(Transaction transaction) throws Exception {
        switchTo(Window.ERROR_HANDLER);
        WebElement handleButton;
        boolean isTransactionSelected = false;
        boolean isTransactionHandling = false;
        boolean areAllDeselected = false;
        boolean washandled = false;
        areAllDeselected = deselectAll();
        isTransactionHandling = getInHandling(transaction);
        isTransactionSelected = tryClick(transaction.WEB_ELEMENT);
        isTransactionSelected = isTransactionSelected || transaction.WEB_ELEMENT.getAttribute("class").contains("selected");
//        handleButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("handled")));
        handleButton = getElement("handled",IDENTIFIER.ID,2, ELEMENT_STATE.CLICKABLE);
        if(areAllDeselected && isTransactionHandling && isTransactionSelected){
            washandled = tryClick(handleButton);
            for(int i = 0; i < 3 && !washandled; i++ )
            {
                handleButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("handled")));
//                handleButton = getElement("handled",IDENTIFIER.ID,2, ELEMENT_STATE.CLICKABLE);
                washandled = tryClick(handleButton);
            }
        }
        return  washandled;
    }

    public boolean rsa(Transaction transaction) throws Exception {
        System.out.println("currenlty in rsa");
        switchTo(Window.SUB_SERVICE);
        boolean rsaDone = false;
        //check conditions when RSA is not needed i.e. RSA, SUS or DAC exists
//        WebElement searchField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchField")));
        WebElement searchField = getElement("searchField",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);
        searchField.clear();
        searchField.sendKeys(transaction.CTN);
        System.out.println("searching for all trx");
//        WebElement searchFieldSubmit = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btnsubServiceSubmit")));
        WebElement searchFieldSubmit = getElement("btnsubServiceSubmit",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);
        if(!tryClick(searchFieldSubmit)){
            //if unable to search for transaction in subservice return false
            System.out.println("unable to search");
            return false;
        }
        rsaDone = verifyRsa(transaction);
        if(rsaDone){
            System.out.println("rsa already exists");
            return rsaDone;
        }
        else{
            System.out.println("rsa doesnt exist, doing rsa");
            if(doRsa(transaction)){
                rsaDone = verifyRsa(transaction);
                System.out.println("rsa done, verifying if its done" + rsaDone);
            }
        }
        return rsaDone;
    }

    public boolean verifyRsa(Transaction transaction) throws Exception {
        System.out.println("currently in verfiy rsa");
        switchTo(Window.SUB_SERVICE);
        boolean rsaNeededAndDone = false;
        boolean isTrxPresent = false;
        // if trx type is RSA do not RSa again, return false
        if(transaction.TRANSACTION_TYPE.equals(Transaction.TransactionType.RSA)) return false;
        //check conditions when RSA is not needed i.e. RSA, SUS or DAC exists
        List<WebElement> webElements;
        List<String> RSAorSUSorDACTrxs = new ArrayList<>();
        List<String> trxsType = List.of(Transaction.TransactionType.RSA.name(), Transaction.TransactionType.DAC.name(), Transaction.TransactionType.SUS.name());
        WebElement refresh ;
        String firstElementText;
        for(int i = 0; i < 2; i++){
            refresh = wait.until(ExpectedConditions.elementToBeClickable(By.id("refresh")));
//            refresh = getElement("refresh",IDENTIFIER.ID,4,ELEMENT_STATE.CLICKABLE);
            tryClick(refresh);
            System.out.println("refreshing "+ i);
            Thread.sleep(700);
        }
        webElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[@id='subServiceTable']/tbody/tr/td[1]")));
        firstElementText = webElements.get(0).getText();
        if(webElements.size() == 1 && firstElementText.equalsIgnoreCase("No matching transactions found")){
            // not transactions are present for this sub in this instance
            return false;
        }
        for(int i = 1; i <= webElements.size() ;i++) {
            String trxXpath = String.format("//*[@id='subServiceTable']/tbody/tr[%d]", i);
            WebElement trxNoElement = getElement(trxXpath + "/td[1]",IDENTIFIER.X_PATH,2,ELEMENT_STATE.PRESENT);
            if(transaction.TRANSACTION_NUMBER.equals(trxNoElement.getText())){
                isTrxPresent = true;
                System.out.println("trx present, search was successfull");
            }
            if (trxsType.contains(getElement(trxXpath + "/td[6]",IDENTIFIER.X_PATH,2,ELEMENT_STATE.PRESENT).getText())) {
                RSAorSUSorDACTrxs.add(trxNoElement.getText());
            }
        }
        System.out.println(transaction.TRANSACTION_NUMBER);
        RSAorSUSorDACTrxs.forEach(System.out::println);
        for(String trxNoRsaDacSus : RSAorSUSorDACTrxs){
            long trxNoLong = Long.parseLong(transaction.TRANSACTION_NUMBER);
            long rsaDacSusLong = Long.parseLong(trxNoRsaDacSus);
            System.out.println(trxNoLong+">"+rsaDacSusLong+"?");
            if(rsaDacSusLong>=trxNoLong){
                rsaNeededAndDone = true;
                System.out.println(rsaDacSusLong+">"+trxNoLong+"!!!!");
                System.out.println("rsa is present");
                break;
            }
        }
        return rsaNeededAndDone && isTrxPresent;
    }
            // for trxno in 2 and 3rd row and 1 colums
            //*[@id="subServiceTable"]/tbody/tr[2]/td[1]
            //*[@id="subServiceTable"]/tbody/tr[3]/td[1]

            // for trx type in 2 and 3 row in 6 column
            //*[@id="subServiceTable"]/tbody/tr[2]/td[6]
            //*[@id="subServiceTable"]/tbody/tr[3]/td[6]



    public boolean doRsa(Transaction transaction) throws Exception {
        System.out.println("curretnly in do rsa");
        switchTo(Window.SUB_SERVICE);
        List<WebElement> webElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[@id='subServiceTable']/tbody/tr/td[1]")));
        boolean rsaClickSuccessful = false;
        for (int i = 1; i <= webElements.size(); i++) {
            String trxNo = String.format("//*[@id='subServiceTable']/tbody/tr[%d]/td[1]", i);
            if (getElement(trxNo,IDENTIFIER.X_PATH,4,ELEMENT_STATE.PRESENT).getText().equals(transaction.TRANSACTION_NUMBER)) {
                System.out.println("trx found");
                String trx = String.format("//*[@id='subServiceTable']/tbody/tr[%d]", i);
                if (tryClick(getElement(trx,IDENTIFIER.X_PATH,4,ELEMENT_STATE.CLICKABLE))) {
                    System.out.println("doing rsa");
                    rsaClickSuccessful = tryClick(getElement("rsa",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE));
                }
                break;
            }
        }
        Thread.sleep(1000); //run verify after 1.5 seconds, its called after this,
        // calling it quick results in lots of false negatives
        System.out.println("rsa result in rsa done "+rsaClickSuccessful);
        return rsaClickSuccessful;
    }
        //*[@id='subServiceTable']/tbody/tr[4]"
        //*[@id='subServiceTable']/tbody/tr[4]/td[1]"

    private boolean doAncestorResend(Transaction transaction) throws Exception{
        switchTo(Window.SUB_SERVICE);
        System.out.println("inside do ancestor resend");
        boolean zoomSuccessful = getSubscriberServiceTable(transaction);
        System.out.println("get default content");
        // table xPath '//*[@id="deviceTable"]/tbody/tr/td[1]'
        // errorCode xPath '//*[@id="deviceTable"]/tbody/tr[i]/td[7]'
        //"Ancestor Failure" STring to search for
        // resend button id "resend-Detail"
        System.out.println("get all device trx elememnt");
        List<WebElement> deviceTansactionElement = null;
        deviceTansactionElement = waitShort.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[@id='deviceTable']/tbody/tr")));
        deviceTansactionElement.forEach(System.out::println);
        System.out.println(deviceTansactionElement.size());
        boolean ancestorsPresent = false;
        WebElement ancestorElement = null;
        for(WebElement element : deviceTansactionElement){
//            System.out.println(element.getText());
            if(element.getText().contains("Ancestor Failure")){
                ancestorsPresent = true;
                ancestorElement = element;
                break;
            }
        }
        System.out.println("clicking on trx element");
        if(ancestorsPresent) tryClick(ancestorElement,3);
        if(ancestorElement.getAttribute("class").contains("selected")){
            System.out.println(" clicking on resend detail");
            tryClick(getElement("resend-Detail",IDENTIFIER.ID,3,ELEMENT_STATE.PRESENT),3);
            waitTillOverlay();
        }
        return ancestorsPresent;
    }

    public boolean ancestorResend(Transaction transaction) throws Exception{
        System.out.println("inside ancestor resend");
        boolean wasAncestorResend = false;
        while(true){
            System.out.println("resending ancestor");
            wasAncestorResend  = doAncestorResend(transaction); // returns whether ancestors are present, false meaning no ancestor
            System.out.println("resent");
            if(!wasAncestorResend) break;                       // invert it to true and break;
        }
        System.out.println("ancestor resend" + wasAncestorResend);
        return !wasAncestorResend;                              // return true
    }

    public boolean getDevScript(Transaction transaction) throws Exception{
        switchTo(Window.SUB_SERVICE);
        if(getSubscriberServiceTable(transaction)){
            System.out.println(" sub table called");
            getConvDeviceScriptWindow(transaction);
            System.out.println("conv dev table called");
            //TODO
            Boolean isResend = (Boolean) ((JavascriptExecutor)driver).executeScript("return arguments[0].innerText.includes('soapenv:Envelope');",driver.findElements(By.id("dvcCvrsDisplay")).get(0));
            if(isResend)tryClick(wait.until(ExpectedConditions.presenceOfElementLocated(By.id("resend"))),3);
            tryClick(wait.until(ExpectedConditions.presenceOfElementLocated(By.id("refresh"))),3);
        }

        //TODO
        return false;
    }

    public boolean getConvScript(Transaction transaction) throws Exception{
        switchTo(Window.SUB_SERVICE);
        if(getSubscriberServiceTable(transaction)){
            System.out.println(" sub table called");
            getConvDeviceScriptWindow(transaction);
            System.out.println("conv dev table called");
            //TODO

        }

        //TODO
        return false;
    }

    private boolean getConvDeviceScriptWindow(Transaction transaction) throws Exception{
        System.out.println("inside getConvDeviceScriptWindow");
        boolean zoomSuccessful = getSubscriberServiceTable(transaction);
        System.out.println("get default content");
        // table xPath '//*[@id="deviceTable"]/tbody/tr/td[1]'
        // errorCode xPath '//*[@id="deviceTable"]/tbody/tr[i]/td[7]'
        //"Ancestor Failure" STring to search for
        // resend button id "resend-Detail"
        System.out.println("get all device trx elememnt");
        List<WebElement> deviceTansactionElement = null;
        deviceTansactionElement = waitShort.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[@id='deviceTable']/tbody/tr")));
        deviceTansactionElement.forEach(System.out::println);
        System.out.println(deviceTansactionElement.size());
        boolean devicePresent = false;
        WebElement deviceElement = null;
        for(WebElement element : deviceTansactionElement){
//            System.out.println(element.getText());
            if(element.getText().contains(transaction.ERROR)){
                devicePresent = true;
                deviceElement = element;
                break;
            }
        }
        System.out.println("clicking on trx element");
        if(devicePresent) tryClick(deviceElement,3);
        if(deviceElement.getAttribute("class").contains("selected")){
            System.out.println(" clicking on zoom detail");
            tryClick(getElement("zoom-Detail",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE),3);
            waitTillOverlay();
        }
        return true;
    }

    private boolean getSubscriberServiceTable(Transaction transaction) throws Exception{
        List<WebElement> webElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[@id='subServiceTable']/tbody/tr/td[1]")));
        boolean zoomClickSuccessful = false;
        WebElement searchField = getElement("searchField",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);
        WebElement searchFieldSubmit = getElement("btnsubServiceSubmit",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);
        tryClick(searchFieldSubmit,3);
        waitTillOverlay();
        searchField.clear();
        searchField.sendKeys(transaction.CTN);
        System.out.println("searching for all trx");
//        WebElement searchFieldSubmit = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btnsubServiceSubmit")));
        tryClick(searchFieldSubmit,3);
        waitTillOverlay();
        for (int i = 1; i <= webElements.size(); i++) {
            String trxNo = String.format("//*[@id='subServiceTable']/tbody/tr[%d]/td[1]", i);
            if (getElement(trxNo,IDENTIFIER.X_PATH,4,ELEMENT_STATE.PRESENT).getText().equals(transaction.TRANSACTION_NUMBER)) {
                System.out.println("trx found");
                String trx = String.format("//*[@id='subServiceTable']/tbody/tr[%d]", i);
                if (tryClick(getElement(trx,IDENTIFIER.X_PATH,4,ELEMENT_STATE.CLICKABLE),3)) {
                    System.out.println("doing zoom");
                    zoomClickSuccessful = tryClick(getElement("zoom",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE),3);
                }
                break;
            }
        }
        waitTillOverlay();
        System.out.println("zoom success");
        return zoomClickSuccessful  /* || driver.findElements(By.className("trx-detail-tab-zoom")).size() > 0*/;
    }

    private boolean getInHandling(Transaction transaction) throws Exception {
        System.out.println("currently in handling");
        switchTo(Window.ERROR_HANDLER);
        boolean isHandling = false;
        boolean areAllDeselected = false;
        boolean isTransactionSelected = false;
        WebElement handling;
        areAllDeselected = deselectAll();
        String isActiveHandling = transaction.WEB_ELEMENT.getAttribute("class");
        System.out.println(isActiveHandling+" class name ");
        if(isActiveHandling.contains("activeHandling")){
            System.out.println(" element already in Handling ");
            return areAllDeselected && true;
        }
        isTransactionSelected = tryClick(transaction.WEB_ELEMENT);
        isTransactionSelected = isTransactionSelected || transaction.WEB_ELEMENT.getAttribute("class").contains("selected");
        handling = getElement("handling",IDENTIFIER.ID,4,ELEMENT_STATE.CLICKABLE);
        if(areAllDeselected && isTransactionSelected){
            isHandling = tryClick(handling);
            isHandling = isHandling || transaction.WEB_ELEMENT.getAttribute("class").contains("activeHandling");
        }
        areAllDeselected = deselectAll();
        List<WebElement> transactions = driver.findElements(By.className("activeHandling"));
        if(transactions.size() == 1 && transactions.get(0).equals(transaction.WEB_ELEMENT)) isHandling = true;
        return areAllDeselected && isHandling;
    }

    public void getAllInHandling(List<Transaction> transactions) throws Exception {
        deselectAll();
        System.out.println("currently in getAllInHandling");
        WebElement handling = getElement("handling",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);;
        Actions action = new Actions(driver);
        Action controlKeydown = action.keyDown(Keys.CONTROL).build();
        Action controlKeyUp= action.keyUp(Keys.CONTROL).build();

        try{
            controlKeydown.perform();
        }catch (Exception e){
            e.printStackTrace();
        }
        for(Transaction transaction : transactions){
            WebElement transactionElement = transaction.WEB_ELEMENT;
            String isAlreadyHandling = transactionElement.getAttribute("class");
            if(isAlreadyHandling.contains("activeHandling")) continue;
            else{
                try{
                    tryClick(transactionElement);
                }catch (Exception e){
                    System.out.println(" unable to click element");
                }
            }
        }
        try {
            controlKeyUp.perform();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(driver.findElements(By.className("selected")).size()>=1){
            tryClick(handling);
        }else{
            System.out.println(" no elements were selected");
        }
        deselectAll();
    }

    public void performActionOnAll(List<Transaction> transactions, ECP.Action actionEnum) throws Exception {
        if(!deselectAll()) return;
        if(transactions.isEmpty()) return;
//        System.out.println("currently in getAllInHandling");
        WebElement actionElement = null;
        switch(actionEnum){
            case RESEND :
                actionElement = getElement("resend",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);;
                break;

            case CLEAN_RESEND :
                actionElement = getElement("cleanResend",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);;
                break;

            case HANDLE :
                actionElement = getElement("handled",IDENTIFIER.ID,2,ELEMENT_STATE.CLICKABLE);;
                break;
        }

        Actions action = new Actions(driver);
        Action controlKeydown = action.keyDown(Keys.CONTROL).build();
        Action controlKeyUp= action.keyUp(Keys.CONTROL).build();

        try{
            controlKeydown.perform();
        }catch (Exception e){
            e.printStackTrace();
        }
        for(Transaction transaction : transactions){
            WebElement transactionElement = transaction.WEB_ELEMENT;
            String isAlreadyHandling = transactionElement.getAttribute("class");
            if(isAlreadyHandling.contains("activeHandling")){
                try{
                    tryClick(transactionElement);
                }catch (Exception e){
                    System.out.println(" unable to click element");
                }
            }
        }
        try {
            controlKeyUp.perform();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(driver.findElements(By.className("selected")).size()>=1){
            tryClick(actionElement);
        }else{
            System.out.println(" no elements were selected");
        }
        deselectAll();
    }

    private boolean deselectAll() throws Exception {
        System.out.println("currently in deselect all");
        switchTo(Window.ERROR_HANDLER);
        List<WebElement> transactions = driver.findElements(By.className("selected"));
        List<String> xPaths = new ArrayList<>();

        for(int i = 0; i < 3; i++){
//            for(WebElement element : transactions){
//                tryClick(element);
//            }
//            transactions = driver.findElements(By.className("selected"));
            ((JavascriptExecutor)driver).executeScript("[...document.getElementsByClassName(\"selected\")].map(a=>a.classList.remove(\"selected\"))");
        }
        transactions = driver.findElements(By.className("selected"));

        if(transactions.size()>0) return false;
        else return true;
    }

    public void waitTillOverlay() throws Exception{
        List<WebElement> overlays = null;
        while (true){
            try {
                overlays = driver.findElements(By.id("overlay"));
                if(overlays.size()>0){
                    Thread.sleep(500);
                }
                else break;
            }catch (Exception e){}
        }
    }

    private boolean tryClick(WebElement webElement, int retries) throws Exception{
        boolean isClickSuccessfull = false;
        for(int i = 0; i < 3; i++){
            try {
                driver.switchTo().window(driver.getWindowHandle());
                isClickSuccessfull = tryClick(webElement);
                break;
            }
            catch (ElementNotInteractableException e){
                System.out.println("exception in tryClick overloaded : not interactable");
                driver.switchTo().defaultContent();
            }
            catch(Exception e){
                System.out.println("exception in tryClick overloaded : Exception "+e.toString());
                continue;
            }
        }
        return isClickSuccessfull;
    }

    private boolean tryClick(WebElement webElement) throws Exception{
        System.out.println("currently in tryClick");
        List<WebElement> dialogs;
        List<WebElement> overlays;
        List<WebElement> closeButtons;            //device trx element button, clicking returns to sub trx service table
        List<WebElement> closeDetailButtons;      // subservice table close Detail button, clicking returns to sub service
        List<WebElement> convDevWindow;
        List<WebElement> yesAndOKButtons;
        boolean isClickSuccessfull = false;
        for(int i = 0; i < 3; i++){
            try {
                webElement.click();
                isClickSuccessfull = true;
                break;
            }
            catch (ElementNotInteractableException e){
                System.out.println("click intercepted : element might be behind a overlay");
                waitTillOverlay();
                driver.switchTo().defaultContent();
                dialogs = driver.findElements(By.xpath("/html/body/div[5]/div[3]/div/button[1]")); // get all ok/yes button
                overlays = driver.findElements(By.id("overlay"));// check if loading overlay is present
                closeButtons = driver.findElements(By.xpath("//*[@title='Close']"));   // get all 'x' close button
                closeDetailButtons = driver.findElements(By.className("close-Detail"));            // get all close detail 'close' button
                convDevWindow = driver.findElements(By.id("device-Detail-desc"));
                yesAndOKButtons = driver.findElements(By.tagName("Button")).stream().filter(a-> a.isDisplayed() && (a.getText().contains("Yes")||a.getText().contains("Ok"))).toList();

                if(dialogs.size()>0){
                    dialogs.get(0).click();
                    dialogs.clear();
//                    break;
                }
                else if(yesAndOKButtons.size()>0){
                    for(WebElement yesOrOK : yesAndOKButtons){
                        yesOrOK.click();
                    }
                    yesAndOKButtons.clear();
//                    break;
                }
                else if(closeButtons.stream().filter(WebElement::isDisplayed).toList().size()>0 && convDevWindow.size() > 0 && convDevWindow.get(0).isDisplayed()) {
                    System.out.println("x present "+closeButtons.size());
                    for(WebElement close : closeButtons){
                        if(convDevWindow.size() > 0 && convDevWindow.get(0).isDisplayed() && close.isDisplayed()){
                            close.click();
//                            break;
                        }
                    }
                    closeButtons.clear();
//                    break;
                }
                else if(closeDetailButtons.stream().filter(WebElement::isDisplayed).toList().size()>0){
                    if(closeDetailButtons.get(0).isDisplayed()){
                        closeDetailButtons.get(0).click();
//                        break;
                    }
                    closeDetailButtons.clear();
                }
                else if(overlays.size()>0){
                    waitTillOverlay();
                    Thread.sleep(1000);
                }
                else {
                    break;
                }
            }
            catch (StaleElementReferenceException e){
                System.out.println("stale element : will retry");
//                webElement = getElement(generateXpath(webElement,""),IDENTIFIER.X_PATH,4,ELEMENT_STATE.CLICKABLE);
            }
//            webElement = wait.until(ExpectedConditions.(webElement));
        }
        return isClickSuccessfull;
    }

    private WebElement getElement(String value, IDENTIFIER identifier, int retries, ELEMENT_STATE state) throws Exception{
        WebElement webElement = null;
        //onlineTool.wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(30)).pollingEvery(Duration.ofMillis(300)).ignoreAll(List.of(NoSuchElementException.class, ElementNotInteractableException.class, StaleElementReferenceException.class));
        for(int i = 0; i < retries; i++){
            try {
                switch(identifier){
                    case X_PATH:
                    {
                        switch(state){
                            case CLICKABLE:
                                webElement = waitShort.until(ExpectedConditions.elementToBeClickable(By.xpath(value)));
                                break;
                            case PRESENT:
                                webElement = waitShort.until(ExpectedConditions.presenceOfElementLocated(By.xpath(value)));
                                break;
                        }
                    }
                        break;
                    case ID:
                    {
                        switch(state){
                            case CLICKABLE:
                                webElement = waitShort.until(ExpectedConditions.elementToBeClickable(By.id(value)));
                                break;
                            case PRESENT:
                                webElement = waitShort.until(ExpectedConditions.presenceOfElementLocated(By.id(value)));
                                break;
                        }
                    }
                        break;
                }
                break;
            }catch (StaleElementReferenceException | NoSuchElementException e){
                continue;
            }
        }
        return webElement;
    }

    //for refreshing online tool called everytime while getting errors
    public boolean refresh() throws Exception {
        System.out.println(" currently in refresh");
        switchTo(Window.ERROR_HANDLER);
        WebElement refreshButton;
        boolean isRefreshSuccessfull = false;
        WebElement refreshorNext = refreshOrNext();
        // xpath of the cross on "transaction record has changed" dialog
        String xpathOfX = "/html/body/div[7]/div[1]/button";
        for(int i = 0; i < 3; i++){
            switchTo(Window.ERROR_HANDLER); // switch to the error handler window
            refreshButton = wait.until(ExpectedConditions.elementToBeClickable(refreshorNext));
            isRefreshSuccessfull = tryClick(refreshButton);
            if(isRefreshSuccessfull) break;
            else{
                driver.switchTo().defaultContent(); // switch to a dilaog/overlay if present
                if(driver.findElements(By.xpath(xpathOfX)).size()>0){
                   tryClick(wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathOfX))));
                }
            }
        }
        return isRefreshSuccessfull;
    }

    //for reloading window
    public void reload(){

    }

    private void logOn(String attid, String password) throws Exception{
        Wait<WebDriver> waitLogOn = new FluentWait<>(driver).withTimeout(Duration.ofMinutes(5)).pollingEvery(Duration.ofMillis(500)).ignoreAll(List.of(NoSuchElementException.class,ElementNotInteractableException.class,StaleElementReferenceException.class));
        WebElement id = waitLogOn.until(ExpectedConditions.presenceOfElementLocated(By.id("GloATTUID")));
        id.sendKeys(attid);

        id = waitLogOn.until(ExpectedConditions.presenceOfElementLocated(By.id("GloPassword")));
        id.sendKeys(password);

        id = waitLogOn.until(ExpectedConditions.elementToBeClickable(By.id("GloPasswordSubmit")));
        id.click();

        id = waitLogOn.until(ExpectedConditions.elementToBeClickable(By.id("successButtonId")));
        id.click();
    }

    private void switchTo(Window window) throws Exception{
        System.out.println("switching to "+window);
        if(!driver.getWindowHandle().equals(windows.get(window))){
            driver.switchTo().window(windows.get(window));
        }
    }

    public void close() throws Exception{
        try{
            driver.close();
        }
        catch (UnhandledAlertException e){
            driver.switchTo().alert().accept();
        }
        catch (Exception e){
            System.out.println("while closing" + e.toString());
        }
        driver.quit();
    }

    private enum Window{
        SUB_SERVICE, ERROR_HANDLER, DEVICE_Q, ERROR_HANDLER_HANDLE
    }

    private enum IDENTIFIER{
        ID, X_PATH
    }

    private enum ELEMENT_STATE{
        CLICKABLE, PRESENT
    }

    public List<Transaction> getListOfTransactions() throws Exception{
        switchTo(Window.ERROR_HANDLER);
        List<WebElement> webElements = getListOfErrorElements();
        List<Transaction> transactions = new ArrayList<>();
        int noOfTransactions = webElements.size();
        String firstElementText = waitLong.until(ExpectedConditions.elementToBeClickable(webElements.get(0))).getText();
        if(noOfTransactions == 1 && firstElementText.equalsIgnoreCase("No matching transactions found")){
            return transactions;
        }

        for(int i = 1; i<=noOfTransactions; i++){
            String xPath = String.format("//*[@id='errorHandlerTable']/tbody/tr[%d]",i);
            WebElement errorRowElement = waitLong.until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
            String dateTime = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[1]"))).getText();
            String transactionNo = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[2]"))).getText();
            String ctn = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[3]"))).getText();
            String market = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[5]"))).getText();
            String transactionType = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[6]"))).getText();
            String error = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[8]"))).getText();
            String retries = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[9]"))).getText();
            String noOfDeviceErrors = waitLong.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath+"/td[10]"))).getText();
            if(market.isEmpty() || transactionType.isEmpty() ) continue;
            Transaction transaction = new Transaction(transactionNo,error,ctn,market,transactionType,retries,dateTime,noOfDeviceErrors,errorRowElement, xPath);
            transactions.add(transaction);
        }
        return transactions;
    }

    private List<WebElement> getListOfErrorElements() throws Exception{
        switchTo(Window.ERROR_HANDLER);
        //refresh();
        List<WebElement> webElements = waitLong.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[@id='errorHandlerTable']/tbody/tr/td[1]")));
        return webElements;
    }

    //xpath of the error and all the fields
    //*[@id="errorHandlerTable"]/tbody/tr[1]       ->errorXpath
    //*[@id="errorHandlerTable"]/tbody/tr[1]/td[2] ->trxNo
    //*[@id="errorHandlerTable"]/tbody/tr[1]/td[3] ->ctn
    //*[@id="errorHandlerTable"]/tbody/tr[1]/td[5] ->market
    //*[@id="errorHandlerTable"]/tbody/tr[1]/td[6] ->trxType
    //*[@id="errorHandlerTable"]/tbody/tr[1]/td[8] ->error
    //*[@id="errorHandlerTable"]/tbody/tr[1]/td[9] ->retries
    //handling, handled, resend, cleanResend, refresh


    //private constructor for this and should be a singleton.
    // do all driver and element loading stuff, switch between Error handler window
    // and sub service in this class, HashMap for storing both window handles
    // log everything and print in console too
    // add functions like doHandle, doRSA, doResend, doCleanResend
    // pass to these functions trx no, ctn, action to be taken(action enum)
    // these functions do their action and verify whether the action is done and update in log
    // verify by checking RSA exists before and after for a subscriber and status has changed to HA or not
    // this class will fetch all web elements from the table and give it to main calss
    // main class will iterate, create transaction objects and call Ecp for all elements which will subsequently call this class

    private WebElement refreshOrNext() throws Exception{
        WebElement next = null;
        WebElement refresh = null;
        System.out.println("waiting for next button");
        next = getElement("errorHandlerTable_next",IDENTIFIER.ID,1,ELEMENT_STATE.PRESENT);
        System.out.println("waiting for refresh button");
        refresh = getElement("refresh",IDENTIFIER.ID,1,ELEMENT_STATE.PRESENT);
        System.out.println("is next disabled?");
        if(next.getAttribute("class").contains("disabled")){
            System.out.println("yes!");
            System.out.println("refresh will be used");
            return refresh;
        }
        else{
            System.out.println("no");
            System.out.println("next will be used");
            return next;
        }
    }

    private static String generateXpath(WebElement childElement, String current) {
        String childTag = childElement.getTagName();
        if(childTag.equals("html")) {
            return "/html[1]"+current;
        }
        WebElement parentElement = childElement.findElement(By.xpath(".."));
        List<WebElement> childrenElements = parentElement.findElements(By.xpath("*"));
        int count = 0;
        for(int i=0;i<childrenElements.size(); i++) {
            WebElement childrenElement = childrenElements.get(i);
            String childrenElementTag = childrenElement.getTagName();
            if(childTag.equals(childrenElementTag)) {
                count++;
            }
            if(childElement.equals(childrenElement)) {
                return generateXpath(parentElement, "/" + childTag + "[" + count + "]"+current);
            }
        }
        return null;
    }

    // test functions
    public void testStart(Map<String, String> link, String id, String password) throws Exception{
//        driver.get(String.format(URL,server,instance));
        driver.get(link.get("home"));
        System.out.println("logging in");
        logOn(id,password);
        System.out.println("logged in");
//        new Scanner(System.in).next();
        System.out.println("waiting for submitBttn to be clickable");
//        new Scanner(Scanner)
//        driver.switchTo().defaultContent();
//        driver.switchTo().activeElement();

        waitLong.until(ExpectedConditions.elementToBeClickable(By.id("submitBttn"))).click();
        System.out.println("is the button clicked");
//        new Scanner(System.in).next();
        System.out.println("button clicked");
        Thread.sleep(1000*5);
        waitLong.until(ExpectedConditions.presenceOfElementLocated(By.id("loggedInDateTime")));
        initializeWindows(link);
    }

    //
}