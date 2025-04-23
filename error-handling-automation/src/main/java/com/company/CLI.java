package com.company;

import java.io.Console;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

import static java.time.temporal.ChronoUnit.SECONDS;

public final class CLI implements Runnable{
    private ErrorHandler errorHandler;
    private static CLI cli;
    private String id;
    private String instanceString;
    private String errorHandlerRegistryValue;
    private boolean useErrorHandlerRegistry = true;
    private char[] password;
    private Map<String, String> link;
    private Console console;
    private volatile boolean isLiveStatusShowing = false;
    private volatile boolean isLiveQueueShowing = false;
    private final boolean isHeadless;
    private boolean isInitialized = false;
    private Thread errorHandlerThread = null;

    private CLI(ErrorHandler errorHandler, boolean isHeadless, boolean useErrorHandlerRegistry, Thread mainThread){
        this.errorHandler = errorHandler;
        this.isHeadless = isHeadless;
        errorHandlerThread = mainThread;
        this.useErrorHandlerRegistry = useErrorHandlerRegistry;
        console = System.console();
    }

    public static CLI getCLI(ErrorHandler errorHandler, boolean isHeadless, boolean useErrorHandlerRegistry, Thread mainThread){
        if(cli == null){
            cli = new CLI(errorHandler, isHeadless, useErrorHandlerRegistry, mainThread);
        }
        return cli;
    }

    public void run(){
        // id/pwd
        // instance
        // showLiveQueue, showLiveStatus, reloadEcp, pause, resume (for later four cases, first interrupt), repair window, exit

        try{
            try{
                loadPrerequisites();
                promptCredentials();
                promptInstance();

                for(String host : List.of("RAGHVENKMV2","SHIVAMS1MV2"))
                {
                    if(useErrorHandlerRegistry){
                        errorHandlerRegistryValue = "RAN-"+id.toUpperCase();
                        Map<String,String> response = getInstanceState(host, instanceString);
                        String instanceValueNotRan = String.format("{\"name\":\"%s\",\"status\":\"NOT_RAN   \"}",instanceString);
                        String instanceValueRan = String.format("{\"name\":\"%s\",\"status\":\"%s\"}",instanceString,errorHandlerRegistryValue);
                        if(response.get("statusCode").equals("200")){
                            System.out.println(response.toString());
                            if(response.get("response").contains(instanceValueNotRan)){
                                System.out.println(response.toString());
                                response = setInstanceState(host, instanceString,instanceValueRan);
                                System.out.println(response.toString());
                            }
                            else{consoleWrite("Tool already running on instance\npress ENTER to Exit");
                                System.out.println(response.toString());
                                new Scanner(System.in).nextLine();
                                return;
                            }
                            if(getInstanceState(host, instanceString).get("response").contains(instanceValueRan)){
                                System.out.println(response.toString());
                                System.out.println("errorHandlingRegistry successfully set");
                                break;

                            }else{
                                System.out.println(response.toString());
                                consoleWrite("Unable to set errorHandlingRegistry value, check logs for detail \npress ENTER to Exit");
                                new Scanner(System.in).nextLine();
                                return;
                            }
                        }
                        else{
                            consoleWrite(response.get("reason")+" \n Press ENTER to Exit");
                            new Scanner(System.in).nextLine();
                            return;
                        }
                    }
                }

                errorHandler.createLogs();
                initializeErrorHandler();
                isInitialized = true;
                errorHandler.messageQueue.stream().forEach(System.out::println);
            }catch (Exception e){
                errorHandler.messageQueue.stream().forEach(System.out::println);
                e.printStackTrace(System.err);
            }

            outer:
            while(true){
                Thread.sleep(300);
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                consoleWrite("\nShowLiveStatus [1] | ShowLiveQueue [2] | Pause [3] | Resume [4] | RepairWindow [5] | LoadEcp [6] | Exit [7]\n");
                String inputString = new Scanner(System.in).next();
                int input = 0;
                try {
                    input = Integer.parseInt(inputString);
                }catch (Exception e){}
                switch (input){
                    case 1:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (isLiveStatusShowing){
                                    try{
                                        Thread.sleep(500);
                                        consoleWrite("\r"+errorHandler.getState()+" last updated "+errorHandler.getLastUpdatedTime()+"s ago");
                                    }catch (Exception e){}
                                }
                            }
                        }).start();
                        showLiveErrorHandlerStatus();
                        break;

                    case 2:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (isLiveQueueShowing){
                                    if(errorHandler.messageQueue.size()==0){
                                        isLiveQueueShowing = false;
                                        break;
                                    }
                                    try{
                                        Thread.sleep(500);
                                        consoleWrite("\n"+errorHandler.messageQueue.take());
                                    }catch (Exception e){}
                                }
                            }
                        }).start();
                        showLiveMessageQueue();
                        break;

                    case 3:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        errorHandler.pauseErrorHandler();
                        errorHandlerThread.interrupt();
                        break;

                    case 4:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        errorHandler.resumeErrorHandler();
                        errorHandlerThread.interrupt();
                        break;

                    case 5:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        errorHandler.pauseErrorHandler();
                        errorHandler.repairWindow(link);
                        errorHandler.resumeErrorHandler();
                        break;

                    case 6:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        errorHandler.loadECP();
                        break;

                    case 7:
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        try{
                            errorHandler.stopErrorHandler();
                        }catch (Exception e){}
                        errorHandlerThread.interrupt();
//                        errorHandler = null;
//                        cli = null;

                        for(String host : List.of("RAGHVENKMV2","SHIVAMS1MV2"))
                        {
                            if(useErrorHandlerRegistry){
                                    errorHandlerRegistryValue = "RAN-"+id.toUpperCase();
                                    String instanceValueNotRan = String.format("{\"name\":\"%s\",\"status\":\"NOT_RAN   \"}",instanceString);
                                    Map<String,String> response = setInstanceState(host, instanceString, instanceValueNotRan);
                                    if(response.get("statusCode").equals("200")){
                                        System.out.println(response.toString());
                                        if(getInstanceState(host, instanceString).get("response").contains(instanceValueNotRan)){
                                            System.out.println(response.toString());
                                            System.out.println("errorHandlingRegistry successfully set");
                                            break outer;
                                        }
                                        else{
                                            System.out.println(response.toString());
                                            consoleWrite("Unable to set errorHandlingRegistry value, check logs for detail");
                                            break outer;
                                        }
                                    }
                                    else{
                                        consoleWrite(response.get("reason"));
                                        break outer;
                                    }
                                }
                        }

                        break outer;

                    default:
                        consoleWrite(input + " invalid input\n");
                }
            }
        }catch (Exception e){
            System.out.println("cli "+e.toString());
        }
    }

    private void promptCredentials(){
        if(id != null || password != null) return;
        consoleWrite("Enter the id\n");
        id = console.readLine().toUpperCase();
        consoleWrite("Enter the password\n");
        password = console.readPassword();
//        consoleWrite("id : " + id + " password : " + new String(password) + " \n");
    }

    private void promptInstance(){
        if(link != null) return;
        String instancesAvailable = "";
        Map<Integer, Map<String, String >> links = errorHandler.getLinks();
        for(Map<String , String > link : links.values()){
            instancesAvailable = instancesAvailable +"\n ["+link.get("id")+"]" + link.get("name");
        }
        consoleWrite("instances available "+instancesAvailable+"\n");
        int instanceSelected = Integer.parseInt(console.readLine());
        link = links.get(instanceSelected);
        String instance = link.get("name");
        String instanceProperty;
        if(instance.contains("SCB")){
            instanceProperty = instanceString = "SCB";
        }else if(instance.contains("SCE")){
            instanceProperty = instanceString = "SCE";
        }else if(instance.contains("SCC")){
            instanceProperty = instanceString = "SCC";
        }else{
            instanceProperty = "TEST_ENV";
        }
        errorHandler.setConfigStrings("instance",instanceProperty);
    }

    private void showLiveErrorHandlerStatus(){
        consoleWrite("Live ErrorHandler Status, press ENTER to return\n");
        isLiveStatusShowing = true;
        new Scanner(System.in).nextLine();
        isLiveStatusShowing = false;
    }

    private void showLiveMessageQueue(){
        consoleWrite("Live Queue Status, press ENTER to return\n");
        isLiveQueueShowing = true;
        new Scanner(System.in).nextLine();
        isLiveQueueShowing = false;
    }

    private boolean loadPrerequisites() throws Exception{
        errorHandler.loadConfig();
        errorHandler.createDirectories();
        errorHandler.loadLinks();
        errorHandler.loadECP();
        return true;
    }

    private boolean initializeErrorHandler() throws Exception {
        errorHandler.startErrorHandler(link,id,password,isHeadless);
        return true;
    }

    public boolean cliInitialized(){
        return isInitialized;
    }

    private void consoleWrite(String string){
        System.console().writer().print(string);
        System.console().flush();
    }

    // call all errorHandler methods upto StartErrorHandler, the start the error handler thread
    // many things can go wrong, so ask user for reloadingEcp, pause(using interrupt), loadEcp, resume
    // ErrorHandler, interrupt the thread before calling any method

    public static Map<String,String> getInstanceState (String host, String instance) throws URISyntaxException {
        Map<String,String> responseMap = new HashMap<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(String.format("http://%s:8080/instance/%s", host, instance)))
                .timeout(Duration.of(10, SECONDS))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = null;

        try{
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch(IOException | InterruptedException e){
            responseMap.put("reason",""+e.toString());
        }
        responseMap.put("statusCode",""+(response!=null?response.statusCode():"null"));
        responseMap.put("response",""+(response!=null?response.body():"null"));

        return responseMap;
    }

    public static Map<String,String> setInstanceState(String host, String instance, String state) throws Exception{
        Map<String,String> responseMap = new HashMap<>();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(String.format("http://%s:8080/instance/%s", host, instance)))
                .timeout(Duration.of(10, SECONDS))
                .headers("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(state))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = null;

        try{
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch(IOException | InterruptedException e){
            responseMap.put("reason",""+e.toString());
        }
        responseMap.put("statusCode",""+(response!=null?response.statusCode():"null"));

        return responseMap;
    }
}
