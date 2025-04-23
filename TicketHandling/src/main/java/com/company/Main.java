package com.company;


import java.io.*;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;


import java.util.concurrent.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static java.time.temporal.ChronoUnit.SECONDS;

public class Main {
    private static volatile boolean stop = false;
    private static Thread mainThread = null;
    public static Map<Integer, BigInteger> steps = new HashMap<>();

//"{\"name\":\"SCE\",\"status\":\"NOT_RAN   \"}"
//"{\"name\":\"SCE\",\"status\":\"RAN-sx1813\"}"

    public static void main(String[] args) {
//        Scanner scan = new Scanner(System.in);
        System.out.println(largestNumberOfStepsUnderMillion());
//        System.out.println(numberOfSteps(2));
    }

    public static int largestNumberOfStepsUnderMillion(){
        int largestStep = 0;
        int numberWithLargestStep = 0;
        for(int i = 1; i < 100; i++){
            steps.put(i,numberOfSteps(i));
        }
        for(Map.Entry<Integer, Integer> entry : steps.entrySet()){
            if(entry.getValue()>largestStep){
                largestStep = entry.getValue();
                numberWithLargestStep = entry.getKey();
            }
        }
        steps.forEach((a,b)-> System.out.println(a+" number of Steps : "+b));
        return numberWithLargestStep;
    }

    public static int numberOfSteps(int number){
        BigInteger numberofSteps = BigInteger.ONE;
        number = new BigInteger(Integer.toString(number)));
        while(number !=1 ){
            Integer numberofStepsInCache = steps.get(number);
            if(numberofStepsInCache!=null) {
                System.out.println(numberofStepsInCache + "presend in cache for " + number);
                numberofSteps = numberofSteps + numberofStepsInCache;
                return numberofSteps;
            }
            if(number%2==0) number = number/2;
            else number = (number*3) + 1;
            numberofSteps+=1;
            System.out.println(number  + " not present in cache, calculation required");
        }
        return numberofSteps;
    }
    //1000000=
//    i am given a number
//if it is even,i will half it
//if it is odd,i will triple it and add 1
//    i will continue this till i reach 1
//    howmany steps does this take?


    public static void not_main(String[] args) throws Exception{
        boolean useErrorHandlerRegistry = true;
        String instance = "SCB";
        String errorHandlerRegistryValue = "RAN-SX1813";

        if(useErrorHandlerRegistry){
            Map<String,String> response = getInstanceState(instance);
            String instanceValueNotRan = String.format("{\"name\":\"%s\",\"status\":\"NOT_RAN   \"}",instance);
            String instanceValueRan = String.format("{\"name\":\"%s\",\"status\":\"%s\"}",instance,errorHandlerRegistryValue);
            if(response.get("statusCode").equals("200")){
                System.out.println(response.toString());
                if(response.get("response").contains(instanceValueNotRan)){
                    response = setInstanceState(instance,instanceValueRan);
                    System.out.println(response.toString());
                }
                else{consoleWrite("Tool already running on instance\npress ENTER to Exit");
                    System.out.println(response.toString());
                    new Scanner(System.in).nextLine();
                    return;
                }
                if(getInstanceState(instance).get("response").contains(instanceValueRan)){
                    System.out.println(response.toString());
                    System.out.println("errorHandlingRegistry successfully set");
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

        System.out.println("tool is running");
        Thread.sleep(5000);

        if(useErrorHandlerRegistry){
            String instanceValueNotRan = String.format("{\"name\":\"%s\",\"status\":\"NOT_RAN   \"}",instance);
            Map<String,String> response = setInstanceState(instance, instanceValueNotRan);
            if(response.get("statusCode").equals("200")){
                System.out.println(response.toString());
                if(getInstanceState(instance).get("response").contains(instanceValueNotRan)){
                    System.out.println(response.toString());
                    System.out.println("errorHandlingRegistry successfully set");
                }
                else{
                    System.out.println(response.toString());
                    consoleWrite("Unable to set errorHandlingRegistry value, check logs for detail");
                }
            }
            else{
                consoleWrite(response.get("reason"));
            }
        }

    }

    public static Map<String,String> getInstanceState (String instance) throws URISyntaxException {
        Map<String,String> responseMap = new HashMap<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(String.format("http://RAGHVENKMV2:8080/instance/%s",instance)))
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

    public static Map<String,String> setInstanceState(String instance, String state) throws Exception{
        Map<String,String> responseMap = new HashMap<>();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(String.format("http://RAGHVENKMV2:8080/instance/%s",instance)))
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
    public static void consoleWrite(String s){
        System.out.println(s);
    }
}
