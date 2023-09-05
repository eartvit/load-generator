package org.concordia.eartvit.research;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import org.json.JSONObject;

public class App 
{
    public static boolean TRACE = false;
    public static void main( String[] args )
    {

        String trace = System.getenv().getOrDefault("TRACEACTIVE", "True");
        if (trace.equalsIgnoreCase("True")){
            App.TRACE = true;
            System.out.println("Hello world, tracing is active!");
        }
            
        Map<String, String> environment = new HashMap<String, String>();

        environment.put("CONNECTIONS", System.getenv().getOrDefault("CONNECTIONS", "2"));
        environment.put("TIMEOUTSECONDS", System.getenv().getOrDefault("TIMEOUTSECONDS", "2"));
        environment.put("DURATION", System.getenv().getOrDefault("DURATION", "5"));
        environment.put("ENDPOINT", System.getenv().getOrDefault("ENDPOINT", "http://localhost:8080/mock"));
        environment.put("THREADSLEEPMS", System.getenv().getOrDefault("THREADSLEEPMS", "50"));
        environment.put("HEADERS", System.getenv().getOrDefault("HEADERS", "{\"Authorization\":\"Bearer YourAccessToken\"}"));

        environment.put("OUTPUT", System.getenv().getOrDefault("OUTPUT", "json"));
        environment.put("STOPONERROR", System.getenv().getOrDefault("STOPONERROR", "False"));

        // Random sent payload generation 
        environment.put("RANDPAYLOAD", System.getenv().getOrDefault("RANDPAYLOAD", "True"));
        // Random selection of payloads in the sent requests. Cyclic mode otherwise.
        environment.put("RANDREQMODE", System.getenv().getOrDefault("RANDREQMODE", "False")); 
        //Payload sizing factor controls the size of the sent data vs expected to receive back data
        environment.put("LTREQPAYLOADSIZEFACTOR", System.getenv().getOrDefault("LTREQPAYLOADSIZEFACTOR", "10"));

        if (environment.get("RANDPAYLOAD").equalsIgnoreCase("True")) {
            // Branch for sending randomly generated payload. 

            // This control variable is for sending only ONE payload (same size) and expecting back random different payloads (sizes)                        
            environment.put("LTREQFIRSTSIZEONLY", System.getenv().getOrDefault("LTREQFIRSTSIZEONLY", "False"));

            // The list of EXPECTED back payload sizes. Sent payload is controlled with the LTREQPAYLOADSIZEFACTOR by dividing the value from the array with the factor.
            String payloadSizes = System.getenv().getOrDefault("PAYLOADSIZES", "50,150,250");            
            String[] payloadSizesArray = payloadSizes.split(",");
            environment.put("REQPAYLOADS", String.valueOf(payloadSizesArray.length));
            for (int i =0; i< Integer.parseInt(environment.get("REQPAYLOADS")); i++ ){
                environment.put("PAYLOADSIZE"+String.valueOf(i+1), payloadSizesArray[i]);
            }
        }
        else {
            // Branch for manually defined payloads to send
            // LTREQPAYLOADSIZEFACTOR and LTREQFIRSTSIZEONLY are not used in this branch
            environment.put("REQPAYLOADS", System.getenv().getOrDefault("REQPAYLOADS", "3"));
            for (int i =0; i< Integer.parseInt(environment.get("REQPAYLOADS")); i++ ){
                environment.put("PAYLOAD"+String.valueOf(i+1), System.getenv().getOrDefault("PAYLOAD"+String.valueOf(i+1), "{\"content\":\"default\"}"));
            }
        }

        if (App.TRACE){
            System.out.println("Using the following ENV values:");
            for(String key:environment.keySet()){
                System.out.println("\t" + key + ": " + environment.get(key));
            }
        }

        int connections = Integer.valueOf(environment.get("CONNECTIONS"));

        List<LoadGeneratorThread> threadList = new ArrayList<>();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateStart = new Date(System.currentTimeMillis()); 

        String startDateStr = dateFormatter.format(dateStart);

        for (int i=0; i<connections; i++){
            String threadName="Thread-"+String.valueOf(i+1);
            LoadGeneratorThread ldThread = new LoadGeneratorThread(threadName, environment);
            threadList.add(ldThread);
            ldThread.start();
            /*
            try{
                ldThread.join();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            */
        }
        boolean allThreadsCompleted = false;
        while (!allThreadsCompleted){
            allThreadsCompleted = true;
            for (LoadGeneratorThread ldThread: threadList){
                allThreadsCompleted = allThreadsCompleted && ldThread.isCompleted();
            }
            try{
                Thread.sleep(100);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        Date dateStop = new Date(System.currentTimeMillis()); 
        String stopDateStr = dateFormatter.format(dateStop);

        String reportFormat = environment.get("OUTPUT");

        if (reportFormat.equalsIgnoreCase("silent")){
            //we report nothing back
        }

        long totalMessages = 0;
        long total1xxMessages = 0;
        long total2xxMessages = 0;
        long total3xxMessages = 0;
        long total4xxMessages = 0;
        long total5xxMessages = 0;
        long totalOtherMessages = 0;
        long minLatency = Long.MAX_VALUE;
        long maxLatency = 0;
        long cumulativeLatency = 0;
        long avgLatency = 0;

        for (LoadGeneratorThread ldThread: threadList){
            totalMessages += ldThread.getNumberOfMessages();
            total1xxMessages += ldThread.getNumber1xxMessages();
            total2xxMessages += ldThread.getNumber2xxMessages();
            total3xxMessages += ldThread.getNumber3xxMessages();
            total4xxMessages += ldThread.getNumber4xxMessages();
            total5xxMessages += ldThread.getNumber5xxMessages();
            totalOtherMessages += ldThread.getNumberOtherMessages();
            cumulativeLatency += ldThread.getCumulativeLatency();

            if (ldThread.getMinLatencyMS() <= minLatency)
                minLatency = ldThread.getMinLatencyMS();

            if (ldThread.getMaxLatencyMS() >= maxLatency)
                maxLatency = ldThread.getMaxLatencyMS();
        }
        if (totalMessages == 0)
            avgLatency = (minLatency + maxLatency)/2;
        else
            avgLatency = cumulativeLatency / totalMessages;

        if (reportFormat.equalsIgnoreCase("text")){
            System.out.println("Load test start: " + startDateStr);
            System.out.println("Load test end: " + stopDateStr);

            System.out.println("Total number of messages sent: " + totalMessages);
            System.out.println("Total number of 1xx responses: " + total1xxMessages);
            System.out.println("Total number of 2xx responses: " + total2xxMessages);
            System.out.println("Total number of 3xx responses: " + total3xxMessages);
            System.out.println("Total number of 4xx responses: " + total4xxMessages);
            System.out.println("Total number of 5xx responses: " + total5xxMessages);
            System.out.println("Total number of other responses: " + totalOtherMessages);
            System.out.println("Minimum latency MS (rounded): " + minLatency);
            System.out.println("Maximum latency MS (rounded): " + maxLatency);
            System.out.println("Average latency MS (rounded): " + avgLatency);

            System.out.println("Timeout seconds:" + String.valueOf(environment.get("TIMEOUTSECONDS")));
            System.out.println("Connections:" + String.valueOf(environment.get("CONNECTIONS")));
            System.out.println("Duration (seconds):" + String.valueOf(environment.get("DURATION")));
            System.out.println("ThreadSleepMS:" + String.valueOf(environment.get("THREADSLEEPMS")));
            System.out.println("RandomPayload:" + String.valueOf(environment.get("RANDPAYLOAD")));
            System.out.println("RequestPayloads:" + String.valueOf(environment.get("REQPAYLOADS")));
            System.out.println("RandomRequestMode:" + String.valueOf(environment.get("RANDREQMODE")));            

            if (environment.get("RANDPAYLOAD").equalsIgnoreCase("True")) {
                System.out.println("LTRequestPayloadSizeFactor:" + String.valueOf(environment.get("LTREQPAYLOADSIZEFACTOR")));
                System.out.println("LTRequestFirstSizeOnly:" + String.valueOf(environment.get("LTREQFIRSTSIZEONLY")));
                for (int i =0; i< Integer.parseInt(environment.get("REQPAYLOADS")); i++ ){
                    System.out.println("PayloadSize" + String.valueOf(i+1) + ": " + environment.get("PAYLOADSIZE"+String.valueOf(i+1)));
                }
            }
            else {
                //We have predefined payloads, however we only care about the length
                for (int i =0; i< Integer.parseInt(environment.get("REQPAYLOADS")); i++ ){
                    System.out.println("PayloadSize" + String.valueOf(i+1) + ": " + environment.get("PAYLOAD"+String.valueOf(i+1)).length());
                }
            }

            for (LoadGeneratorThread ldThread: threadList){
                System.out.println("Thread " + ldThread.getName() + ": Total number of messages sent " + ldThread.getNumberOfMessages());
                System.out.println("Thread " + ldThread.getName() + " Total number of 1xx responses: " + ldThread.getNumber1xxMessages());
                System.out.println("Thread " + ldThread.getName() + " Total number of 2xx responses: " + ldThread.getNumber2xxMessages());
                System.out.println("Thread " + ldThread.getName() + " Total number of 3xx responses: " + ldThread.getNumber3xxMessages());
                System.out.println("Thread " + ldThread.getName() + " Total number of 4xx responses: " + ldThread.getNumber4xxMessages());
                System.out.println("Thread " + ldThread.getName() + " Total number of 5xx responses: " + ldThread.getNumber5xxMessages());
                System.out.println("Thread " + ldThread.getName() + " Total number of other responses: " + ldThread.getNumberOtherMessages());
                System.out.println("Thread " + ldThread.getName() + " Minimum latency MS (rounded): " + ldThread.getMinLatencyMS());
                System.out.println("Thread " + ldThread.getName() + " Maximum latency MS (rounded): " + ldThread.getMaxLatencyMS());
                System.out.println("Thread " + ldThread.getName() + " Average latency MS (rounded): " + ldThread.getAvgLatencyMS());
            }
        }

        if (reportFormat.equalsIgnoreCase("json")){
            JSONObject report = new JSONObject();

            report.put("LoadTestStart", startDateStr);
            report.put("LoadTestStop", stopDateStr);


            Map<String, String> messageStats = new HashMap<String, String>();
            messageStats.put("TotalMessages", String.valueOf(totalMessages));
            messageStats.put("Total1xxResponses", String.valueOf(total1xxMessages));
            messageStats.put("Total2xxResponses", String.valueOf(total2xxMessages));
            messageStats.put("Total3xxResponses", String.valueOf(total3xxMessages));
            messageStats.put("Total4xxResponses", String.valueOf(total4xxMessages));
            messageStats.put("Total5xxResponses", String.valueOf(total5xxMessages));
            messageStats.put("TotalOtherResponses", String.valueOf(totalOtherMessages));
            messageStats.put("MinLatencyMSRounded", String.valueOf(minLatency));
            messageStats.put("MaxLatencyMSRounded", String.valueOf(maxLatency));
            messageStats.put("AvgLatencyMSRounded", String.valueOf(avgLatency));
            report.put("MessageStats", messageStats);

            Map<String, String> loadTestParams = new HashMap<String, String>();
            loadTestParams.put("ReqTimeoutSeconds", String.valueOf(environment.get("TIMEOUTSECONDS")));
            loadTestParams.put("Connections", String.valueOf(environment.get("CONNECTIONS")));
            loadTestParams.put("LoadDurationSeconds", String.valueOf(environment.get("DURATION")));
            loadTestParams.put("ThreadSleepMS", String.valueOf(environment.get("THREADSLEEPMS")));
            loadTestParams.put("RandomPayload", String.valueOf(environment.get("RANDPAYLOAD")));
            loadTestParams.put("RequestPayloads", String.valueOf(environment.get("REQPAYLOADS")));
            loadTestParams.put("RandomRequestMode", String.valueOf(environment.get("RANDREQMODE")));
            if (environment.get("RANDPAYLOAD").equalsIgnoreCase("True")) {
                loadTestParams.put("LTRequestPayloadSizeFactor", String.valueOf(environment.get("LTREQPAYLOADSIZEFACTOR")));
                loadTestParams.put("LTRequestFirstSizeOnly", String.valueOf(environment.get("LTREQFIRSTSIZEONLY")));
                for (int i =0; i< Integer.parseInt(environment.get("REQPAYLOADS")); i++ ){
                    String keyString = "PayloadSize" + String.valueOf(i+1);
                    loadTestParams.put(keyString, environment.get("PAYLOADSIZE"+String.valueOf((i+1))));
                }
            }
            else {
                //We have predefined payloads, however we only care about the length
                for (int i =0; i< Integer.parseInt(environment.get("REQPAYLOADS")); i++ ){
                    String keyString = "PayloadSize" + String.valueOf(i+1);
                    loadTestParams.put(keyString, String.valueOf(environment.get("PAYLOAD"+String.valueOf((i+1))).length()));
                }
            }
            report.put("LoadTestParams", loadTestParams);

            Map<String, Map<String, String>> threadMessages = new HashMap<String, Map<String, String>>();
            long crtThreadNumber = 0; //iterate until connections
            for (LoadGeneratorThread ldThread: threadList){
                Map<String, String> crtThread = new HashMap<String, String>();

                crtThread.put(ldThread.getName() + "-TotalMessages" , String.valueOf(ldThread.getNumberOfMessages()));
                crtThread.put(ldThread.getName() + "-Total1xxResponses" , String.valueOf(ldThread.getNumber1xxMessages()));
                crtThread.put(ldThread.getName() + "-Total2xxResponses" , String.valueOf(ldThread.getNumber2xxMessages()));
                crtThread.put(ldThread.getName() + "-Total3xxResponses" , String.valueOf(ldThread.getNumber3xxMessages()));
                crtThread.put(ldThread.getName() + "-Total4xxResponses" , String.valueOf(ldThread.getNumber4xxMessages()));
                crtThread.put(ldThread.getName() + "-Total5xxResponses" , String.valueOf(ldThread.getNumber5xxMessages()));
                crtThread.put(ldThread.getName() + "-TotalOtherResponses" , String.valueOf(ldThread.getNumberOtherMessages()));
                crtThread.put(ldThread.getName() + "-MinLatencyMSRounded" , String.valueOf(ldThread.getMinLatencyMS()));
                crtThread.put(ldThread.getName() + "-MaxLatencyMSRounded" , String.valueOf(ldThread.getMaxLatencyMS()));
                crtThread.put(ldThread.getName() + "-AvgLatencyMSRounded" , String.valueOf(ldThread.getAvgLatencyMS()));

                threadMessages.put("Thread-" + crtThreadNumber, crtThread);
                crtThreadNumber += 1;
            }
            report.put("ThreadResultDetails", threadMessages);

            System.out.println(report);
        }
    }
}
