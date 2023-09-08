package org.concordia.eartvit.research;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.*;

public class LoadGeneratorThread extends Thread {

    private Thread t = null;
    private String threadName = "";
    private boolean isCompleted = false;
    private Map<String, String> envMap;
    private long numberOfMessages = 0;
    private long number1xxMessages = 0;
    private long number2xxMessages = 0;
    private long number3xxMessages = 0;
    private long number4xxMessages = 0;
    private long number5xxMessages = 0;
    private long numberOtherMessages = 0;
    private long timeoutSeconds = 2;
    private long minLatencyMS = Long.MAX_VALUE;
    private long maxLatencyMS = 0;
    private long avgLatencyMS = 0;
    private long cumulativeLatency = 0;


    public LoadGeneratorThread(String threadName, Map<String, String> envMap) {
        this.threadName = threadName;
        this.envMap = envMap;
    }

    public void start() {
        if (null == t) {
            t = new Thread(this, threadName);
        }
        t.start();
    }

    public void run() {

        int duration = Integer.valueOf(envMap.get("DURATION"));
        int threadSleepMS = Integer.valueOf(envMap.get("THREADSLEEPMS"));
        timeoutSeconds = Long.valueOf(envMap.get("TIMEOUTSECONDS"));

        long now = System.currentTimeMillis();
        long end = now + 1000 * duration;

        boolean stopOnError = Boolean.valueOf(envMap.get("STOPONERROR"));

        JSONObject headerObject = new JSONObject(envMap.get("HEADERS"));

        long i = 0;
        int j = 0;

        int reqPayloads = Integer.parseInt(envMap.get("REQPAYLOADS"));
        String[] messageBody = new String[reqPayloads];
        int[] returnPayloadSizes = new int[reqPayloads];
        int[] requestPayloadSizes = new int[reqPayloads];

        if (envMap.get("RANDPAYLOAD").equalsIgnoreCase("True")) {

            int factor = Integer.parseInt(envMap.get("LTREQPAYLOADSIZEFACTOR"));
            if (factor <= 0)
                factor = 1;

            for (int idx = 0; idx < reqPayloads; idx++) {
                Map<String, String> map = new HashMap<>();
                
                String payloadString = "";
                
                int crtPayloadSize = Integer.parseInt(envMap.get("PAYLOADSIZE" + (idx + 1)));
                returnPayloadSizes[idx] = crtPayloadSize;
                requestPayloadSizes[idx] = crtPayloadSize/factor;

                if (envMap.get("LTREQFIRSTSIZEONLY").equalsIgnoreCase("True")){
                    // We generate different strings of the same length
                    payloadString = RandomStringBuilder.getInstance().generateRandomString(requestPayloadSizes[0]);
                }
                else{
                    payloadString = RandomStringBuilder.getInstance().generateRandomString(requestPayloadSizes[idx]);
                }
                map.put("content", payloadString);
                messageBody[idx] = new JSONObject(map).toString();
            }
        } else {
            for (int idx = 0; idx < reqPayloads; idx++) {
                messageBody[idx] = new JSONObject(envMap.get("PAYLOAD" + (idx + 1))).toString();
                requestPayloadSizes[idx] = messageBody[idx].length();
                returnPayloadSizes[idx] = messageBody[idx].length();
            }
        }

        boolean randReqMode = Boolean.valueOf(envMap.get("RANDREQMODE")).booleanValue();
        Random rand = new Random();

        /*
         * Declare the HttpClient outside of the main loop as we only want one instance of it.
         * Opening multiple clients may lead to running out of stack memory and thus the app will behave as if crashed and the pod will not finish
         * with the right return value which can lead to unexpected problems in the ecosystem.
         */
        HttpClient client = null; 
        try{
            client = HttpClient.newHttpClient();
        } catch (Exception e){
            if (App.TRACE){
                System.out.println(threadName + ": Could not create HttpClient object. Bailing out!");
                e.printStackTrace();
            }
            isCompleted = true;
        }
        
        while (System.currentTimeMillis() < end && !isCompleted) {
            try {
                i++; //we increase the general counter here to compensate for STOPONERROR==True
                if (randReqMode == true) {

                    j = rand.nextInt(reqPayloads);
                }
                String uri = envMap.get("ENDPOINT") + "?id=" + String.valueOf(returnPayloadSizes[j]);
                if (App.TRACE){
                    System.out.println(this.threadName + " Next payload id is: " + j);
                    if (envMap.get("LTREQFIRSTSIZEONLY").equalsIgnoreCase("True")){
                        System.out.println(this.threadName + " Next request payload size is: " + requestPayloadSizes[0]);
                    } else {
                        System.out.println(this.threadName + " Next request payload size is: " + requestPayloadSizes[j]);
                    }
                    System.out.println(this.threadName + " The URI is: " + uri);
                }
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(uri))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .headers("Content-Type", "application/json")
                        .header("Custom-Headers", headerObject.toString())
                        .POST(HttpRequest.BodyPublishers.ofString(messageBody[j]))
                        .build();

                if (App.TRACE) {
                    System.out.println(this.threadName + " The request is: " + request.toString());                    
                    System.out.println(this.threadName + " Having headers: " + request.headers().toString());
                    System.out.println(this.threadName + " And body as string: " + messageBody[j]);
                }

                j += 1;
                if (j >= reqPayloads)
                    j = 0;

                
                long reqStart = System.currentTimeMillis();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                long reqEnd = System.currentTimeMillis();
                long reqLatency = reqEnd - reqStart;
                cumulativeLatency += reqLatency;
                if (reqLatency < minLatencyMS)
                    minLatencyMS = reqLatency;

                if (reqLatency > maxLatencyMS)
                    maxLatencyMS = reqLatency;

                avgLatencyMS = cumulativeLatency / i;

                if (App.TRACE) {
                    System.out.println(this.threadName + " Got response code: " + response.statusCode());
                    System.out.println(this.threadName + " Got response body: " + response.body());
                }

                if (response.statusCode() >= 100 && response.statusCode() <= 199) {
                    number1xxMessages += 1;
                    //1xx information messages, don't need to break
                } else if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                    number2xxMessages += 1;
                    //2XX messages are HTTP OK type
                } else if (response.statusCode() >= 300 && response.statusCode() <= 399) {
                    number3xxMessages += 1;
                    //3XX messages are redirect message types, don't need to break
                } else if (response.statusCode() >= 400 && response.statusCode() <= 499) {
                    number4xxMessages += 1;
                    if (stopOnError) {
                        if (App.TRACE){
                            System.out.println("StopOnError is active. Thread: " + threadName + "was interrupted by statusCode "
                                + response.statusCode() + " at iteration " + Long.toString(i));
                        }
                        break;
                    }
                } else if (response.statusCode() >= 500 && response.statusCode() <= 599) {
                    number5xxMessages += 1;
                    if (stopOnError) {
                        if (App.TRACE){
                            System.out.println("StopOnError is active. Thread: " + threadName + "was interrupted by statusCode "
                                + response.statusCode() + " at iteration " + Long.toString(i));
                        }
                        break;
                    }
                } else {
                    numberOtherMessages += 1;
                    if (stopOnError) {
                        if (App.TRACE) {                      
                            System.out.println("StopOnError is active. Thread: " + threadName + "was interrupted by statusCode "
                                + response.statusCode() + " at iteration " + Long.toString(i));
                        }
                        break;
                    }                    
                }
            } catch (Exception e) {
                if (e instanceof URISyntaxException) {
                    if (App.TRACE){
                        System.out.println("Thread: " + threadName + "was interrupted by URISyntaxException at iteration "
                            + Long.toString(i));
                    }
                    break; //we always stop on malformed URI
                } else if (e instanceof HttpTimeoutException) {
                    numberOtherMessages += 1; //probably request timed out
                    if (App.TRACE){
                        e.printStackTrace();
                    }
                    if (stopOnError)
                        break;
                } else {
                    if (App.TRACE){
                        e.printStackTrace();
                    }
                    isCompleted = true;
                    break;
                }                
            }
            try {
                Thread.sleep(threadSleepMS);
            } catch (Exception e) {
                if (e instanceof InterruptedException){
                    if (stopOnError){
                        break;
                    }
                    if (App.TRACE){
                        System.out.println("Thread: " + threadName + "was interrupted by InterruptedException at iteration "
                            + Long.toString(i));
                    }
                } else {
                    if (App.TRACE){
                        e.printStackTrace();
                    }
                    isCompleted = true;
                    break;
                }
            }
        }
        isCompleted = true;
        numberOfMessages = number1xxMessages + number2xxMessages + number3xxMessages + number4xxMessages + number5xxMessages + numberOtherMessages;
        if (App.TRACE)
            System.out.println("Thread " + threadName + " sent " + numberOfMessages + " messages.");
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean flag){
        isCompleted = flag;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getNumberOfMessages() {
        return numberOfMessages;
    }

    public long getNumber1xxMessages() {
        return number1xxMessages;
    }

    public long getNumber2xxMessages() {
        return number2xxMessages;
    }

    public long getNumber3xxMessages() {
        return number3xxMessages;
    }

    public long getNumber4xxMessages() {
        return number4xxMessages;
    }

    public long getNumber5xxMessages() {
        return number5xxMessages;
    }

    public long getNumberOtherMessages() {
        return numberOtherMessages;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public long getMinLatencyMS() {
        return minLatencyMS;
    }

    public long getMaxLatencyMS() {
        return maxLatencyMS;
    }

    public long getAvgLatencyMS() {
        return avgLatencyMS;
    }

    public long getCumulativeLatency() {
        return cumulativeLatency;
    }
}
