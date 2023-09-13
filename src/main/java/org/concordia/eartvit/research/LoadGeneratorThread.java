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

    /* being lazy and not creating another class for spike threads */
    private boolean isSpikingThread = false;
    private long duration = 0;
    private long threadSleepMS = 50;
    private boolean spikeActive = false;
    private long spikeCycleNumberOfMessages = 0;
    private long spikeCycleNumber1xxMessages = 0;
    private long spikeCycleNumber2xxMessages = 0;
    private long spikeCycleNumber3xxMessages = 0;
    private long spikeCycleNumber4xxMessages = 0;
    private long spikeCycleNumber5xxMessages = 0;
    private long spikeCycleNumberOtherMessages = 0;
    private long spikeCycleCumulativeLatency = 0;

    HttpClient client = null; 


    public LoadGeneratorThread(String threadName, Map<String, String> envMap) {
        this.threadName = threadName;
        this.envMap = envMap;
        this.isSpikingThread = false;
    }

    public LoadGeneratorThread(String threadName, Map<String, String> envMap, boolean isSpikingThread, boolean spikeActive) {
        this.threadName = threadName;
        this.envMap = envMap;
        this.isSpikingThread = isSpikingThread;
        this.spikeActive = spikeActive;
    }

    public void start() {
        if (null == t) {
            t = new Thread(this, threadName);
        }
        t.start();
    }

    public void run() {

        duration = Long.valueOf(envMap.get("DURATION"));
        threadSleepMS = Long.valueOf(envMap.get("THREADSLEEPMS"));
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

        try{
            client = HttpClient.newHttpClient();
        } catch (Exception e){
            if (App.TRACE){
                System.out.println(threadName + ": Could not create HttpClient object. Bailing out!");
                e.printStackTrace();
            }
            isCompleted = true;
        }
        
        while (System.currentTimeMillis() < end && !isCompleted()) {
            if (isSpikingThread() && isSpikeActive() == false){
                if (App.TRACE){
                    System.out.println(this.threadName + " is inactive. Checking for next active interval after 1000ms...");
                }
                try {
                    resetCycleMessages(); //we reset the cycle here after the previous cycle completed and updated the counters
                    Thread.sleep(1000);
                    continue;//we check again spikeActive which is updated by the main thread
                } catch (Exception e) {
                    if (e instanceof InterruptedException){
                        if (App.TRACE){
                            System.out.println("Thread: " + threadName + "caught an InterruptedException at iteration "
                                + Long.toString(i));
                        }
                        if (stopOnError){
                            setCompleted(true);
                            break;
                        }
                    } else {
                        if (App.TRACE){
                            e.printStackTrace();
                        }
                        setCompleted(true);
                        break;
                    }
                }
            }
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

                incCumulativeLatency(reqLatency);


                if (isSpikingThread())
                    incSpykeCumulativeLatency(reqLatency);

                if (reqLatency < getMinLatencyMS())
                    setMinLatencyMS(reqLatency);

                if (reqLatency > getMaxLatencyMS())
                    setMaxLatencyMS(reqLatency);

                setAvgLatencyMS(getCumulativeLatency() / i);

                if (App.TRACE) {
                    System.out.println(this.threadName + " Got response code: " + response.statusCode());
                    System.out.println(this.threadName + " Got response body: " + response.body());
                }

                if (response.statusCode() >= 100 && response.statusCode() <= 199) {
                    incNumber1xxMessages();
                    //1xx information messages, don't need to break
                    if (isSpikingThread())
                        incSpikeCycleNumber1xxMessages();
                } else if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                    incNumber2xxMessages();
                    //2XX messages are HTTP OK type
                    if (isSpikingThread())
                        incSpikeCycleNumber2xxMessages();
                } else if (response.statusCode() >= 300 && response.statusCode() <= 399) {
                    incNumber3xxMessages();
                    //3XX messages are redirect message types, don't need to break
                    if (isSpikingThread())
                        incSpikeCycleNumber3xxMessages();
                } else if (response.statusCode() >= 400 && response.statusCode() <= 499) {
                    incNumber4xxMessages();
                    if (isSpikingThread())
                        incSpikeCycleNumber4xxMessages();
                    if (stopOnError) {
                        if (App.TRACE){
                            System.out.println("StopOnError is active. Thread: " + threadName + "was interrupted by statusCode "
                                + response.statusCode() + " at iteration " + Long.toString(i));
                        }
                        break;
                    }
                } else if (response.statusCode() >= 500 && response.statusCode() <= 599) {
                    incNumber5xxMessages();
                    if (isSpikingThread())
                        incSpikeCycleNumber5xxMessages();
                    if (stopOnError) {
                        if (App.TRACE){
                            System.out.println("StopOnError is active. Thread: " + threadName + "was interrupted by statusCode "
                                + response.statusCode() + " at iteration " + Long.toString(i));
                        }
                        break;
                    }
                } else {
                    incNumberOtherMessages();
                    if (isSpikingThread())
                        incSpikeCycleNumberOtherMessages();
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
                    setCompleted(true);
                    break; //we always stop on malformed URI
                } else if (e instanceof HttpTimeoutException) {
                    incNumberOtherMessages(); //probably request timed out
                    if (isSpikingThread())
                        incSpikeCycleNumberOtherMessages();
                    if (App.TRACE){
                        System.out.println(threadName + " Request timed out at iteration "+ Long.toString(i));
                        //e.printStackTrace();
                    }
                    if (stopOnError)
                        break;
                } else {
                    if (App.TRACE){
                        e.printStackTrace();
                    }
                    setCompleted(true);
                    break;
                }                
            }
            try {
                Thread.sleep(threadSleepMS);
            } catch (Exception e) {
                if (e instanceof InterruptedException){
                    if (App.TRACE){
                        System.out.println(threadName + "caught an InterruptedException at iteration "
                            + Long.toString(i));
                    }
                    if (stopOnError){
                        break;
                    }
                } else {
                    if (App.TRACE){
                        e.printStackTrace();
                    }
                    setCompleted(true);
                    break;
                }
            }
        }
        
        if (App.TRACE)
            System.out.println("Thread " + threadName + " sent " + getNumberOfMessages() + " messages.");
    }

    public synchronized boolean isCompleted() {
        return isCompleted;
    }

    public synchronized void setCompleted(boolean flag){
        isCompleted = flag;
    }

    public String getThreadName() {
        return threadName;
    }

    public synchronized long getNumberOfMessages() {
        numberOfMessages = number1xxMessages + number2xxMessages + number3xxMessages + number4xxMessages + number5xxMessages + numberOtherMessages;
        return numberOfMessages;
    }

    public synchronized long getNumber1xxMessages() {
        return number1xxMessages;
    }

    public synchronized long getNumber2xxMessages() {
        return number2xxMessages;
    }

    public synchronized long getNumber3xxMessages() {
        return number3xxMessages;
    }

    public synchronized long getNumber4xxMessages() {
        return number4xxMessages;
    }

    public synchronized long getNumber5xxMessages() {
        return number5xxMessages;
    }

    public synchronized long getNumberOtherMessages() {
        return numberOtherMessages;
    }

    public synchronized long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public synchronized long getMinLatencyMS() {
        return minLatencyMS;
    }

    public synchronized void setMinLatencyMS(long latency){
        this.minLatencyMS = latency;
    }

    public synchronized long getMaxLatencyMS() {
        return maxLatencyMS;
    }

    public synchronized void setMaxLatencyMS(long latency){
        this.maxLatencyMS = latency;
    }

    public synchronized long getAvgLatencyMS() {
        return avgLatencyMS;
    }

    public synchronized void setAvgLatencyMS(long latency){
        this.avgLatencyMS = latency;
    }

    public synchronized long getCumulativeLatency() {
        return cumulativeLatency;
    }

    public synchronized void setCumulativeLatency(long latency){
        this.cumulativeLatency = latency;
    }

    public synchronized void incCumulativeLatency(long latency){
        this.cumulativeLatency += latency;
    }

    public synchronized void incCumulativeLatency(){
        this.cumulativeLatency += 1;
    }

    public synchronized void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public synchronized boolean isSpikingThread() {
        return isSpikingThread;
    }

    public synchronized void setSpikingThread(boolean isSpikingThread) {
        this.isSpikingThread = isSpikingThread;
    }

    public synchronized long getDuration() {
        return duration;
    }

    public synchronized void setDuration(long duration) {
        this.duration = duration;
    }

    public synchronized boolean isSpikeActive() {
        return spikeActive;
    }

    public synchronized void setSpikeActive(boolean spikeActive) {
        this.spikeActive = spikeActive;
    }

    public synchronized long getThreadSleepMS() {
        return threadSleepMS;
    }

    public synchronized void setThreadSleepMS(long threadSleepMS) {
        this.threadSleepMS = threadSleepMS;
    }

    public synchronized long getSpikeCycleNumberOfMessages() {
        spikeCycleNumberOfMessages = spikeCycleNumber1xxMessages + spikeCycleNumber2xxMessages + spikeCycleNumber3xxMessages +
                                    spikeCycleNumber4xxMessages + spikeCycleNumber5xxMessages + spikeCycleNumberOtherMessages;
        return spikeCycleNumberOfMessages;
    }

    public synchronized void setSpikeCycleNumberOfMessages(long spikeCycleNumberOfMessages) {
        this.spikeCycleNumberOfMessages = spikeCycleNumberOfMessages;
    }

    public synchronized long getSpikeCycleNumber1xxMessages() {
        return spikeCycleNumber1xxMessages;
    }

    public synchronized void setSpikeCycleNumber1xxMessages(long spikeCycleNumber1xxMessages) {
        this.spikeCycleNumber1xxMessages = spikeCycleNumber1xxMessages;
    }

    public synchronized long getSpikeCycleNumber2xxMessages() {
        return spikeCycleNumber2xxMessages;
    }

    public synchronized void setSpikeCycleNumber2xxMessages(long spikeCycleNumber2xxMessages) {
        this.spikeCycleNumber2xxMessages = spikeCycleNumber2xxMessages;
    }

    public synchronized long getSpikeCycleNumber3xxMessages() {
        return spikeCycleNumber3xxMessages;
    }

    public synchronized void setSpikeCycleNumber3xxMessages(long spikeCycleNumber3xxMessages) {
        this.spikeCycleNumber3xxMessages = spikeCycleNumber3xxMessages;
    }

    public synchronized long getSpikeCycleNumber4xxMessages() {
        return spikeCycleNumber4xxMessages;
    }

    public synchronized void setSpikeCycleNumber4xxMessages(long spikeCycleNumber4xxMessages) {
        this.spikeCycleNumber4xxMessages = spikeCycleNumber4xxMessages;
    }

    public synchronized long getSpikeCycleNumber5xxMessages() {
        return spikeCycleNumber5xxMessages;
    }

    public synchronized void setSpikeCycleNumber5xxMessages(long spikeCycleNumber5xxMessages) {
        this.spikeCycleNumber5xxMessages = spikeCycleNumber5xxMessages;
    }

    public synchronized long getSpikeCycleNumberOtherMessages() {
        return spikeCycleNumberOtherMessages;
    }

    public synchronized void setSpikeCycleNumberOtherMessages(long spikeCycleNumberOtherMessages) {
        this.spikeCycleNumberOtherMessages = spikeCycleNumberOtherMessages;
    }

    public synchronized long getSpikeCycleCumulativeLatency(){
        return this.spikeCycleCumulativeLatency;
    }

    public synchronized void setSpykeCumulativeLatency(long latency){
        this.spikeCycleCumulativeLatency = latency;
    }

    public synchronized void incSpykeCumulativeLatency(long latency){
        this.spikeCycleCumulativeLatency += latency;
    }

    public synchronized void incSpykeCumulativeLatency(){
        this.spikeCycleCumulativeLatency += 1;
    }

    public synchronized void resetCycleMessages(){
        spikeCycleNumber1xxMessages = 0;
        spikeCycleNumber2xxMessages = 0;
        spikeCycleNumber3xxMessages = 0;
        spikeCycleNumber4xxMessages = 0;
        spikeCycleNumber5xxMessages = 0;
        spikeCycleNumberOtherMessages = 0;
        spikeCycleNumberOfMessages = 0;
        spikeCycleCumulativeLatency = 0;
    }

    public synchronized void incSpikeCycleNumber1xxMessages(){
        this.spikeCycleNumber1xxMessages += 1;
    }

    public synchronized void incSpikeCycleNumber2xxMessages(){
        this.spikeCycleNumber2xxMessages += 1;
    }

    public synchronized void incSpikeCycleNumber3xxMessages(){
        this.spikeCycleNumber3xxMessages += 1;
    }

    public synchronized void incSpikeCycleNumber4xxMessages(){
        this.spikeCycleNumber4xxMessages += 1;
    }

    public synchronized void incSpikeCycleNumber5xxMessages(){
        this.spikeCycleNumber5xxMessages += 1;
    }
    
    public synchronized void incSpikeCycleNumberOtherMessages(){
        this.spikeCycleNumberOtherMessages += 1;
    }

    public synchronized void incNumber1xxMessages(){
        this.number1xxMessages += 1;
    }

    public synchronized void incNumber2xxMessages(){
        this.number2xxMessages += 1;
    }

    public synchronized void incNumber3xxMessages(){
        this.number3xxMessages += 1;
    }

    public synchronized void incNumber4xxMessages(){
        this.number4xxMessages += 1;
    }

    public synchronized void incNumber5xxMessages(){
        this.number5xxMessages += 1;
    }

    public synchronized void incNumberOtherMessages(){
        this.numberOtherMessages += 1;
    }

    public synchronized void printOut(){
        System.out.println(threadName + "-Printout: Total number of messages: " + getNumberOfMessages());
        System.out.println(threadName + "-Printout: Total number of 1xx responses: " + number1xxMessages);
        System.out.println(threadName + "-Printout: Total number of 2xx responses: " + number2xxMessages);
        System.out.println(threadName + "-Printout: Total number of 3xx responses: " + number3xxMessages);
        System.out.println(threadName + "-Printout: Total number of 4xx responses: " + number4xxMessages);
        System.out.println(threadName + "-Printout: Total number of 5xx responses: " + number5xxMessages);
        System.out.println(threadName + "-Printout: Total number of other responses: " + numberOtherMessages);
        System.out.println(threadName + "-Printout: Minimum latency MS (rounded): " + minLatencyMS);
        System.out.println(threadName + "-Printout: Maximum latency MS (rounded): " + maxLatencyMS);
        System.out.println(threadName + "-Printout: Average latency MS (rounded): " + avgLatencyMS);

        if (isSpikingThread){
            long spikeMessages = getSpikeCycleNumberOfMessages();
            System.out.println(threadName + "-Printout: Cycle number of messages sent: " + spikeMessages);
            System.out.println(threadName + "-Printout: Cycle number of 1xx responses: " + spikeCycleNumber1xxMessages);
            System.out.println(threadName + "-Printout: Cycle number of 2xx responses: " + spikeCycleNumber2xxMessages);
            System.out.println(threadName + "-Printout: Cycle number of 3xx responses: " + spikeCycleNumber3xxMessages);
            System.out.println(threadName + "-Printout: Cycle number of 4xx responses: " + spikeCycleNumber4xxMessages);
            System.out.println(threadName + "-Printout: Cycle number of 5xx responses: " + spikeCycleNumber5xxMessages);
            System.out.println(threadName + "-Printout: Cycle number of other responses: " + spikeCycleNumberOtherMessages);
            if (spikeMessages >0)
                System.out.println(threadName + "-Printout: Cycle average latency MS (rounded): " + spikeCycleCumulativeLatency/spikeMessages);
        }
    }

}