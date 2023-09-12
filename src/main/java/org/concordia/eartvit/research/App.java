package org.concordia.eartvit.research;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import org.json.JSONObject;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.Gauge;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;


public class App 
{
    public static boolean TRACE = false;
    public static void main( String[] args )
    {

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        DefaultExports.initialize();

        HTTPServer prometheusServer = null;

        Gauge systemLoadAverage = Gauge.build()
                .name("load_generator_system_load_average")
                .help("Load Generator System Load Average")
                .register();

        Gauge totalMessagesGauge = Gauge.build()
                .name("load_generator_total_msg")
                .help("Total number of messages received back by the Load Generator")
                .register();    
        Gauge total1XXMessagesGauge = Gauge.build()
                .name("load_generator_1xx_msg")
                .help("Total number of 1XX messages received back by the Load Generator")
                .register();
        Gauge total2XXMessagesGauge = Gauge.build()
                .name("load_generator_2xx_msg")
                .help("Total number of 2XX messages received back by the Load Generator")
                .register();
        Gauge total3XXMessagesGauge = Gauge.build()
                .name("load_generator_3xx_msg")
                .help("Total number of 3XX messages received back by the Load Generator")
                .register();
        Gauge total4XXMessagesGauge = Gauge.build()
                .name("load_generator_4xx_msg")
                .help("Total number of 4XX messages received back by the Load Generator")
                .register();
        Gauge total5XXMessagesGauge = Gauge.build()
                .name("load_generator_5xx_msg")
                .help("Total number of 5XX messages received back by the Load Generator")
                .register();
        Gauge totalOtherMessagesGauge = Gauge.build()
                .name("load_generator_other_msg")
                .help("Total number of other messages received back by the Load Generator")
                .register();
        Gauge averageLatencyGauge = Gauge.build()
                .name("load_generator_avg_latency_ms")
                .help("Average latency for the total messages received back by the Load Generator")
                .register();

        Gauge totalSpikeMessagesGauge = Gauge.build()
                .name("load_generator_spike_total_msg")
                .help("Total number of spike messages received back by the Load Generator")
                .register();    
        Gauge totalSpike1XXMessagesGauge = Gauge.build()
                .name("load_generator_spike_1xx_msg")
                .help("Total number of 1XX spike messages received back by the Load Generator")
                .register();
        Gauge totalSpike2XXMessagesGauge = Gauge.build()
                .name("load_generator_spike_2xx_msg")
                .help("Total number of 2XX messages received back by the Load Generator")
                .register();
        Gauge totalSpike3XXMessagesGauge = Gauge.build()
                .name("load_generator_spike_3xx_msg")
                .help("Total number of 3XX spike messages received back by the Load Generator")
                .register();
        Gauge totalSpike4XXMessagesGauge = Gauge.build()
                .name("load_generator_spike_4xx_msg")
                .help("Total number of 4XX spike messages received back by the Load Generator")
                .register();
        Gauge totalSpike5XXMessagesGauge = Gauge.build()
                .name("load_generator_spike_5xx_msg")
                .help("Total number of 5XX spike messages received back by the Load Generator")
                .register();
        Gauge totalSpikeOtherMessagesGauge = Gauge.build()
                .name("load_generator_spike_other_msg")
                .help("Total number of other spike messages received back by the Load Generator")
                .register();
        Gauge averageSpikeLatencyGauge = Gauge.build()
                .name("load_generator_spike_avg_latency_ms")
                .help("Average spike latency for the total spike messages received back by the Load Generator")
                .register();


        String trace = System.getenv().getOrDefault("TRACEACTIVE", "True");
        if (trace.equalsIgnoreCase("True")){
            App.TRACE = true;
            System.out.println("Hello world, tracing is active!");
        }
            
        int prometheusPort = Integer.valueOf(System.getenv().getOrDefault("PROMETHEUSPORT", "9090"));

        Map<String, String> environment = new HashMap<String, String>();

        environment.put("CONNECTIONS", System.getenv().getOrDefault("CONNECTIONS", "2"));
        environment.put("TIMEOUTSECONDS", System.getenv().getOrDefault("TIMEOUTSECONDS", "2"));
        environment.put("DURATION", System.getenv().getOrDefault("DURATION", "10"));
        environment.put("ENDPOINT", System.getenv().getOrDefault("ENDPOINT", "http://localhost:8080/mock"));
        environment.put("THREADSLEEPMS", System.getenv().getOrDefault("THREADSLEEPMS", "50"));
        environment.put("HEADERS", System.getenv().getOrDefault("HEADERS", "{\"Authorization\":\"Bearer YourAccessToken\"}"));

        environment.put("CREATESPIKES", System.getenv().getOrDefault("CREATESPIKES", "False"));
        environment.put("SPIKECONNECTIONS", System.getenv().getOrDefault("SPIKECONNECTIONS", "5"));
        environment.put("SPIKEDURATIONLOWERBOUND", System.getenv().getOrDefault("SPIKEDURATIONLOWERBOUND", "2"));
        environment.put("SPIKEDURATIONUPPERBOUND", System.getenv().getOrDefault("SPIKEDURATIONUPPERBOUND", "5"));
        environment.put("RANDOMSPIKEDURATION", System.getenv().getOrDefault("RANDOMSPIKEDURATION", "False"));
        environment.put("SPIKEREPETITIONINTLOBOUND", System.getenv().getOrDefault("SPIKEREPETITIONINTLOBOUND", "2"));
        environment.put("SPIKEREPETITIONINTHIBOUND", System.getenv().getOrDefault("SPIKEREPETITIONINTHIBOUND", "4"));
        environment.put("RANDOMSPIKEREPEAT", System.getenv().getOrDefault("RANDOMSPIKEREPEAT", "False"));

        environment.put("OUTPUT", System.getenv().getOrDefault("OUTPUT", "json"));
        environment.put("DETAILEDOUTPUT", System.getenv().getOrDefault("DETAILEDOUTPUT", "False"));
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

        try {
            for (int i=0; i<connections; i++){
                String threadName="Thread-"+String.valueOf(i+1);
                LoadGeneratorThread ldThread = new LoadGeneratorThread(threadName, environment);
                ldThread.start();
                threadList.add(ldThread);
            }            
        } catch (Exception e) {
            if (App.TRACE){
                System.out.println("Exception caught starting thread");
                e.printStackTrace();
            }            
        }

        if (App.TRACE){
            System.out.println("Created " + threadList.size() + " threads out of " + connections + " requested.");
        }

        boolean detailedOutput = false;
        if (environment.get("DETAILEDOUTPUT").equalsIgnoreCase("True")){
            detailedOutput = true;
        }

        boolean createSpikes = false;
        boolean randomSpikeDuration = false;
        boolean randomSpikeStart = false;
        List<LoadGeneratorThread> spikeThreadList = new ArrayList<>();
        int spikeDuration = 0;
        int spikeRepetitionLoBound = 0;
        int spikeRepetitionHiBound = 0;
        int minSpikeDuration = 0;
        int maxSpikeDuration = 0;
        long spikeRepetition = 0;
        if (environment.get("CREATESPIKES").equalsIgnoreCase("True")){
            createSpikes = true;

            randomSpikeDuration = Boolean.valueOf(environment.get("RANDOMSPIKEDURATION"));
            minSpikeDuration = Integer.valueOf(environment.get("SPIKEDURATIONLOWERBOUND")); //in seconds
            maxSpikeDuration = Integer.valueOf(environment.get("SPIKEDURATIONUPPERBOUND")); //in seconds
            spikeDuration = 1000* (int) ((Math.random() * (maxSpikeDuration - minSpikeDuration)) + minSpikeDuration); //transform to ms

            randomSpikeStart = Boolean.valueOf(environment.get("RANDOMSPIKEREPEAT"));
            spikeRepetitionLoBound = Integer.valueOf(environment.get("SPIKEREPETITIONINTLOBOUND"));
            spikeRepetitionHiBound = Integer.valueOf(environment.get("SPIKEREPETITIONINTHIBOUND"));
            spikeRepetition = 1000* (int) ((Math.random() * (spikeRepetitionHiBound - spikeRepetitionLoBound)) + spikeRepetitionLoBound); //transform to ms

            try {
                for (int i=0; i<Integer.valueOf(environment.get("SPIKECONNECTIONS")); i++){
                    String threadName="SpikeThread-"+String.valueOf(i+1);
                    LoadGeneratorThread ldThread = new LoadGeneratorThread(threadName, environment,true, false);
                    ldThread.start();
                    spikeThreadList.add(ldThread);
                }            
            } catch (Exception e) {
                if (App.TRACE){
                    System.out.println("Exception caught starting thread");
                    e.printStackTrace();
                }            
            }

            if (App.TRACE){
                System.out.println("Created " + spikeThreadList.size() + " spiking threads out of " + environment.get("SPIKECONNECTIONS") + " requested.");
            }
        }

        boolean allThreadsCompleted = false;
        long now = System.currentTimeMillis();
        int duration = Integer.valueOf(environment.get("DURATION"));
        long end = now + 2000 * duration; //wait twice the duration then force exit

        spikeRepetition = 1000 * (long) ((Math.random() * (spikeRepetitionHiBound - spikeRepetitionLoBound)) + spikeRepetitionLoBound);
        long nextSpikeStartAt = now + spikeRepetition;

        try{

            prometheusServer = new HTTPServer(prometheusPort);

            boolean spikesAreActive = false;

            while (!allThreadsCompleted ){
                allThreadsCompleted = true;
                for (LoadGeneratorThread ldThread: threadList){
                    allThreadsCompleted = allThreadsCompleted && ldThread.isCompleted();
                }

                if (createSpikes){
                    long crtNow = System.currentTimeMillis();
                    if ((crtNow > nextSpikeStartAt) && (crtNow < nextSpikeStartAt + spikeDuration)){
                        if (spikesAreActive == false){
                            if(App.TRACE){
                                System.out.println("Main thread is activating spike threads...");
                            }
                            for (LoadGeneratorThread ldThread: spikeThreadList){
                                ldThread.setSpikeActive(true); // we tell the spikeThreads to execute the main loop
                            }
                            spikesAreActive = true;
                        } else {
                            if(App.TRACE){
                                System.out.println("Main thread is looping over active spike threads...");
                            }
                        }
                    } else if (crtNow >= nextSpikeStartAt + spikeDuration){
                        //this is executed once the next cycle
                        if(App.TRACE){
                            System.out.println("Main thread is deactivating spike threads...");
                        }
                        for (LoadGeneratorThread ldThread: spikeThreadList){
                            ldThread.setSpikeActive(false); // we tell the spikeThreads to skip over the main loop
                        }
                        spikesAreActive = false;

                        if (randomSpikeStart){
                            spikeRepetition = 1000 * (long) ((Math.random() * (spikeRepetitionHiBound - spikeRepetitionLoBound)) + spikeRepetitionLoBound);
                        }
                        nextSpikeStartAt = crtNow + spikeRepetition;

                        if (randomSpikeDuration){
                            spikeDuration = 1000* (int) ((Math.random() * (maxSpikeDuration - minSpikeDuration)) + minSpikeDuration); //transform to ms
                        }
                        if (App.TRACE){
                            System.out.println("Main thread: Next spike will be in " + spikeRepetition/1000 + 
                                            " seconds and will last for " + spikeDuration/1000 + " seconds.");
                        }
                    } else {
                        if(App.TRACE){
                            System.out.println("Main thread is looping over inactive spikes until next spike start...");
                        }
                    }
                }

                if (System.currentTimeMillis() > end){
                    if (App.TRACE){
                        System.out.println("Forcing stop as time limit exceeds 2xDURATION.");
                    }
                    for (LoadGeneratorThread ldThread: threadList){
                        ldThread.setCompleted(true);
                    }
                    if (createSpikes) {
                        for (LoadGeneratorThread ldThread: spikeThreadList){
                            ldThread.setCompleted(true);
                        }
                    }
                }
                try {
                    // Update Prometheus counters
                    double crtSystemLoadAverage = osBean.getSystemLoadAverage();
                    systemLoadAverage.set(crtSystemLoadAverage);

                    // Update regular load message gauges
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
                    }
                    if (totalMessages == 0)
                        avgLatency = (minLatency + maxLatency)/2;
                    else
                        avgLatency = cumulativeLatency / totalMessages;
                    
                    totalMessagesGauge.set(totalMessages);
                    total1XXMessagesGauge.set(total1xxMessages);
                    total2XXMessagesGauge.set(total2xxMessages);
                    total3XXMessagesGauge.set(total3xxMessages);
                    total4XXMessagesGauge.set(total4xxMessages);
                    total5XXMessagesGauge.set(total5xxMessages);
                    totalOtherMessagesGauge.set(totalOtherMessages);
                    averageLatencyGauge.set(avgLatency);

                    if (createSpikes){
                        // Update spike message gauges
                        long totalSpikeMessages = 0;
                        long totalSpike1xxMessages = 0;
                        long totalSpike2xxMessages = 0;
                        long totalSpike3xxMessages = 0;
                        long totalSpike4xxMessages = 0;
                        long totalSpike5xxMessages = 0;
                        long totalSpikeOtherMessages = 0;
                        long minSpikeLatency = Long.MAX_VALUE;
                        long maxSpikeLatency = 0;
                        long cumulativeSpikeLatency = 0;
                        long avgSpikeLatency = 0;

                        for (LoadGeneratorThread ldThread: spikeThreadList){
                            totalSpikeMessages += ldThread.getNumberOfMessages();
                            totalSpike1xxMessages += ldThread.getNumber1xxMessages();
                            totalSpike2xxMessages += ldThread.getNumber2xxMessages();
                            totalSpike3xxMessages += ldThread.getNumber3xxMessages();
                            totalSpike4xxMessages += ldThread.getNumber4xxMessages();
                            totalSpike5xxMessages += ldThread.getNumber5xxMessages();
                            totalSpikeOtherMessages += ldThread.getNumberOtherMessages();
                            cumulativeSpikeLatency += ldThread.getCumulativeLatency();
                        }
                        if (totalSpikeMessages == 0)
                            avgSpikeLatency = (minSpikeLatency + maxSpikeLatency)/2;
                        else
                            avgSpikeLatency = cumulativeSpikeLatency / totalSpikeMessages;
                        
                        totalSpikeMessagesGauge.set(totalSpikeMessages);
                        totalSpike1XXMessagesGauge.set(totalSpike1xxMessages);
                        totalSpike2XXMessagesGauge.set(totalSpike2xxMessages);
                        totalSpike3XXMessagesGauge.set(totalSpike3xxMessages);
                        totalSpike4XXMessagesGauge.set(totalSpike4xxMessages);
                        totalSpike5XXMessagesGauge.set(totalSpike5xxMessages);
                        totalSpikeOtherMessagesGauge.set(totalSpikeOtherMessages);
                        averageSpikeLatencyGauge.set(avgSpikeLatency);
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException e){
                    if (App.TRACE){
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e){
            if (App.TRACE){
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
            System.out.println("RequestedConnections:" + String.valueOf(environment.get("CONNECTIONS")));
            System.out.println("Connections:" + String.valueOf(threadList.size()));
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

            System.out.println("CreateSpikes:" + String.valueOf(environment.get("CREATESPIKES")));  
            if (createSpikes){
                // Update spike message gauges
                long totalSpikeMessages = 0;
                long totalSpike1xxMessages = 0;
                long totalSpike2xxMessages = 0;
                long totalSpike3xxMessages = 0;
                long totalSpike4xxMessages = 0;
                long totalSpike5xxMessages = 0;
                long totalSpikeOtherMessages = 0;
                long minSpikeLatency = Long.MAX_VALUE;
                long maxSpikeLatency = 0;
                long cumulativeSpikeLatency = 0;
                long avgSpikeLatency = 0;

                for (LoadGeneratorThread ldThread: spikeThreadList){
                    totalSpikeMessages += ldThread.getNumberOfMessages();
                    totalSpike1xxMessages += ldThread.getNumber1xxMessages();
                    totalSpike2xxMessages += ldThread.getNumber2xxMessages();
                    totalSpike3xxMessages += ldThread.getNumber3xxMessages();
                    totalSpike4xxMessages += ldThread.getNumber4xxMessages();
                    totalSpike5xxMessages += ldThread.getNumber5xxMessages();
                    totalSpikeOtherMessages += ldThread.getNumberOtherMessages();
                    cumulativeSpikeLatency += ldThread.getCumulativeLatency();
                }
                if (totalSpikeMessages == 0)
                    avgSpikeLatency = (minSpikeLatency + maxSpikeLatency)/2;
                else
                    avgSpikeLatency = cumulativeSpikeLatency / totalSpikeMessages;  

                System.out.println("Total number of spike messages sent: " + totalSpikeMessages);
                System.out.println("Total number of spike 1xx responses: " + totalSpike1xxMessages);
                System.out.println("Total number of spike 2xx responses: " + totalSpike2xxMessages);
                System.out.println("Total number of spike 3xx responses: " + totalSpike3xxMessages);
                System.out.println("Total number of spike 4xx responses: " + totalSpike4xxMessages);
                System.out.println("Total number of spike 5xx responses: " + totalSpike5xxMessages);
                System.out.println("Total number of spike other responses: " + totalSpikeOtherMessages);
                System.out.println("Spike average latency MS (rounded): " + avgSpikeLatency);

                System.out.println("SpikeConnections: " + environment.get("SPIKECONNECTIONS"));
                System.out.println("SpikeDurationLowerBound: " + environment.get("SPIKEDURATIONLOWERBOUND"));
                System.out.println("SpikeDurationUpperBound: " + environment.get("SPIKEDURATIONUPPERBOUND"));
                System.out.println("RandomSpikeDuration: " + String.valueOf(randomSpikeDuration));
                System.out.println("RandomSpikeRepeat: " + String.valueOf(randomSpikeStart));
                System.out.println("SpikeRepetitionLowerBound: " + environment.get("SPIKEREPETITIONINTLOBOUND"));
                System.out.println("SpikeRepetitionUpperBound: " + environment.get("SPIKEREPETITIONINTHIBOUND"));
            }          

            if (detailedOutput) {
                for (LoadGeneratorThread ldThread: threadList){
                    System.out.println("Thread " + ldThread.getThreadName() + ": Total number of messages sent " + ldThread.getNumberOfMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Total number of 1xx responses: " + ldThread.getNumber1xxMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Total number of 2xx responses: " + ldThread.getNumber2xxMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Total number of 3xx responses: " + ldThread.getNumber3xxMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Total number of 4xx responses: " + ldThread.getNumber4xxMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Total number of 5xx responses: " + ldThread.getNumber5xxMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Total number of other responses: " + ldThread.getNumberOtherMessages());
                    System.out.println("Thread " + ldThread.getThreadName() + " Minimum latency MS (rounded): " + ldThread.getMinLatencyMS());
                    System.out.println("Thread " + ldThread.getThreadName() + " Maximum latency MS (rounded): " + ldThread.getMaxLatencyMS());
                    System.out.println("Thread " + ldThread.getThreadName() + " Average latency MS (rounded): " + ldThread.getAvgLatencyMS());
                }
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
            loadTestParams.put("RequestedConnections", String.valueOf(environment.get("CONNECTIONS")));
            loadTestParams.put("Connections", String.valueOf(threadList.size()));
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
            loadTestParams.put("CreateSpikes", String.valueOf(environment.get("CREATESPIKES")));
            report.put("LoadTestParams", loadTestParams);

            if (createSpikes){
                // Update spike message gauges
                long totalSpikeMessages = 0;
                long totalSpike1xxMessages = 0;
                long totalSpike2xxMessages = 0;
                long totalSpike3xxMessages = 0;
                long totalSpike4xxMessages = 0;
                long totalSpike5xxMessages = 0;
                long totalSpikeOtherMessages = 0;
                long minSpikeLatency = Long.MAX_VALUE;
                long maxSpikeLatency = 0;
                long cumulativeSpikeLatency = 0;
                long avgSpikeLatency = 0;

                for (LoadGeneratorThread ldThread: spikeThreadList){
                    totalSpikeMessages += ldThread.getNumberOfMessages();
                    totalSpike1xxMessages += ldThread.getNumber1xxMessages();
                    totalSpike2xxMessages += ldThread.getNumber2xxMessages();
                    totalSpike3xxMessages += ldThread.getNumber3xxMessages();
                    totalSpike4xxMessages += ldThread.getNumber4xxMessages();
                    totalSpike5xxMessages += ldThread.getNumber5xxMessages();
                    totalSpikeOtherMessages += ldThread.getNumberOtherMessages();
                    cumulativeSpikeLatency += ldThread.getCumulativeLatency();
                }
                if (totalSpikeMessages == 0)
                    avgSpikeLatency = (minSpikeLatency + maxSpikeLatency)/2;
                else
                    avgSpikeLatency = cumulativeSpikeLatency / totalSpikeMessages;  

                Map<String, String> spikeMessageStats = new HashMap<String, String>();
                spikeMessageStats.put("TotalSpikeMessages", String.valueOf(totalSpikeMessages));
                spikeMessageStats.put("TotalSpike1xxResponses", String.valueOf(totalSpike1xxMessages));
                spikeMessageStats.put("TotalSpike2xxResponses", String.valueOf(totalSpike2xxMessages));
                spikeMessageStats.put("TotalSpike3xxResponses", String.valueOf(totalSpike3xxMessages));
                spikeMessageStats.put("TotalSpike4xxResponses", String.valueOf(totalSpike4xxMessages));
                spikeMessageStats.put("TotalSpike5xxResponses", String.valueOf(totalSpike5xxMessages));
                spikeMessageStats.put("TotalSpikeOtherResponses", String.valueOf(totalSpikeOtherMessages));
                spikeMessageStats.put("AvgSpikeLatencyMSRounded", String.valueOf(avgSpikeLatency));

                spikeMessageStats.put("SpikeConnections", String.valueOf(environment.get("SPIKECONNECTIONS")));
                spikeMessageStats.put("SpikeDurationLowerBound", String.valueOf(environment.get("SPIKEDURATIONLOWERBOUND")));
                spikeMessageStats.put("SpikeDurationUpperBound", String.valueOf(environment.get("SPIKEDURATIONUPPERBOUND")));
                spikeMessageStats.put("RandomSpikeDuration", String.valueOf(randomSpikeDuration));
                spikeMessageStats.put("RandomSpikeRepeat", String.valueOf(randomSpikeStart));
                spikeMessageStats.put("SpikeRepetitionLowerBound", String.valueOf(environment.get("SPIKEREPETITIONINTLOBOUND")));
                spikeMessageStats.put("SpikeRepetitionUpperBound", String.valueOf(environment.get("SPIKEREPETITIONINTHIBOUND")));

                report.put("SpikeMessageStats", spikeMessageStats);
            }            

            if (detailedOutput) {
                Map<String, Map<String, String>> threadMessages = new HashMap<String, Map<String, String>>();
                long crtThreadNumber = 1; //iterate until connections
                for (LoadGeneratorThread ldThread: threadList){
                    Map<String, String> crtThread = new HashMap<String, String>();

                    crtThread.put(ldThread.getThreadName() + "-TotalMessages" , String.valueOf(ldThread.getNumberOfMessages()));
                    crtThread.put(ldThread.getThreadName() + "-Total1xxResponses" , String.valueOf(ldThread.getNumber1xxMessages()));
                    crtThread.put(ldThread.getThreadName() + "-Total2xxResponses" , String.valueOf(ldThread.getNumber2xxMessages()));
                    crtThread.put(ldThread.getThreadName() + "-Total3xxResponses" , String.valueOf(ldThread.getNumber3xxMessages()));
                    crtThread.put(ldThread.getThreadName() + "-Total4xxResponses" , String.valueOf(ldThread.getNumber4xxMessages()));
                    crtThread.put(ldThread.getThreadName() + "-Total5xxResponses" , String.valueOf(ldThread.getNumber5xxMessages()));
                    crtThread.put(ldThread.getThreadName() + "-TotalOtherResponses" , String.valueOf(ldThread.getNumberOtherMessages()));
                    crtThread.put(ldThread.getThreadName() + "-MinLatencyMSRounded" , String.valueOf(ldThread.getMinLatencyMS()));
                    crtThread.put(ldThread.getThreadName() + "-MaxLatencyMSRounded" , String.valueOf(ldThread.getMaxLatencyMS()));
                    crtThread.put(ldThread.getThreadName() + "-AvgLatencyMSRounded" , String.valueOf(ldThread.getAvgLatencyMS()));

                    threadMessages.put("Thread-" + crtThreadNumber, crtThread);
                    crtThreadNumber += 1;
                }
                report.put("ThreadResultDetails", threadMessages);
            }
            System.out.println(report);
        }
        try{
            Thread.sleep(10*1000); //sleep 10 more seconds to allow Prometheus scrapes
        } catch (InterruptedException e){
            //do nothing since we don;t want anything printed after the report
        }
        prometheusServer.close();
    }
}
