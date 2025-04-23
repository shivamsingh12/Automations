package com.company;

import java.io.Console;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public final class CLI implements Runnable{
    private ErrorHandler errorHandler;
    private static CLI cli;
    private String id;
    private char[] password;
    private Map<String, String> link;
    private Console console;
    private volatile boolean isLiveStatusShowing = false;
    private volatile boolean isLiveQueueShowing = false;
    private final boolean isHeadless;
    private boolean isInitialized = false;
    private Thread errorHandlerThread = null;

    private CLI(ErrorHandler errorHandler, boolean isHeadless, Thread mainThread){
        this.errorHandler = errorHandler;
        this.isHeadless = isHeadless;
        errorHandlerThread = mainThread;
        console = System.console();
    }

    public static CLI getCLI(ErrorHandler errorHandler, boolean isHeadless, Thread mainThread){
        if(cli == null){
            cli = new CLI(errorHandler, isHeadless, mainThread);
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
                        errorHandler = null;
                        cli = null;
                        break outer;

                    default:
                        consoleWrite(input + " invalid input\n");
                }
            }
        }catch (Exception e){}
    }

    private void promptCredentials(){
        if(id != null || password != null) return;
        consoleWrite("Enter the id\n");
        id = console.readLine();
        consoleWrite("Enter the password\n");
        password = console.readPassword();
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
            instanceProperty = "SCB";
        }else if(instance.contains("SCE")){
            instanceProperty = "SCE";
        }else if(instance.contains("SCC")){
            instanceProperty = "SCC";
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

}
