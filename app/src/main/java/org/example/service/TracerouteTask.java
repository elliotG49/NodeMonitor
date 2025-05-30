package org.example.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.concurrent.Task;

public class TracerouteTask extends Task<List<String>> {
    private final String target;
    private final List<String> hopIpAddresses = new ArrayList<>();
    private final int MAX_HOPS = 15; // Increased to 15
    private boolean targetFound = false;
    private boolean completed = false;
    
    public TracerouteTask(String target) {
        this.target = target;
    }
    
    @Override
    protected List<String> call() throws Exception {
        ProcessBuilder processBuilder;
        
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows - limit to MAX_HOPS, use -w timeout of 1000ms per hop (shortened from 3000ms)
            processBuilder = new ProcessBuilder("tracert", "-d", "-w", "1000", "-h", String.valueOf(MAX_HOPS), target);
        } else {
            // Linux/Mac - limit to MAX_HOPS, use -w timeout of 1 seconds per hop (shortened from 3)
            processBuilder = new ProcessBuilder("traceroute", "-n", "-w", "1", "-m", String.valueOf(MAX_HOPS), target);
        }
        
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        // Regex patterns to match IP addresses and hop number
        Pattern ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
        Pattern hopNumberPattern = Pattern.compile("^\\s*(\\d+)");
        Pattern timeoutPattern = Pattern.compile("Request timed out|\\*\\s+\\*\\s+\\*");
        
        String line;
        int lastHopNumber = 0;
        
        while ((line = reader.readLine()) != null) {
            if (isCancelled()) {
                process.destroy();
                return hopIpAddresses;
            }
            
            // Skip header lines (first couple of lines in Windows tracert)
            if (line.contains("Tracing route") || line.contains("over a maximum") || 
                line.trim().isEmpty()) {
                continue;
            }
            
            // Check for hop number to identify valid traceroute lines
            Matcher hopMatcher = hopNumberPattern.matcher(line);
            if (hopMatcher.find()) {
                int hopNumber = Integer.parseInt(hopMatcher.group(1));
                lastHopNumber = hopNumber;
                
                // Check for timeout pattern
                if (timeoutPattern.matcher(line).find()) {
                    // Add "Timeout" as the IP for this hop
                    hopIpAddresses.add("Timeout");
                    updateValue(new ArrayList<>(hopIpAddresses));
                    continue;
                }
                
                // Look for an IP address in the line
                Matcher ipMatcher = ipPattern.matcher(line);
                if (ipMatcher.find()) {
                    String ip = ipMatcher.group();
                    hopIpAddresses.add(ip);
                    updateValue(new ArrayList<>(hopIpAddresses));
                    
                    // If this is the target and we have a match, we can stop
                    if (ip.equals(target) || 
                        containsIp(ip, target) || 
                        hopIpAddresses.size() >= MAX_HOPS) {
                        if (ip.equals(target) || containsIp(ip, target)) {
                            targetFound = true;
                        }
                        break;
                    }
                }
            }
        }
        
        // If the target wasn't in our list and we have space, add it as the final hop
        // Only add target if it wasn't already added in the traceroute output
        if (!targetFound && !hopIpAddresses.contains(target) && 
            !containsIp(hopIpAddresses, target) && hopIpAddresses.size() < MAX_HOPS) {
            // Try to resolve the target address
            try {
                String ip = InetAddress.getByName(target).getHostAddress();
                hopIpAddresses.add(ip);
                targetFound = true;
                updateValue(new ArrayList<>(hopIpAddresses));
            } catch (Exception e) {
                // If we can't resolve it, just add the target string
                hopIpAddresses.add(target);
                targetFound = true;
                updateValue(new ArrayList<>(hopIpAddresses));
            }
        }
        
        completed = true;
        process.waitFor();
        return hopIpAddresses;
    }
    
    public List<String> getHopIpAddresses() {
        return hopIpAddresses;
    }
    
    public boolean isTargetFound() {
        return targetFound;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    private boolean containsIp(String ip1, String ip2) {
        try {
            return InetAddress.getByName(ip1).getHostAddress().equals(
                   InetAddress.getByName(ip2).getHostAddress());
        } catch (Exception e) {
            return false;
        }
    }
    
    // Add helper method to the TracerouteTask class to check if an IP is in the list
    private boolean containsIp(List<String> ipList, String targetIp) {
        try {
            String resolvedTarget = InetAddress.getByName(targetIp).getHostAddress();
            for (String ip : ipList) {
                try {
                    if (ip.equals(resolvedTarget) || InetAddress.getByName(ip).getHostAddress().equals(resolvedTarget)) {
                        return true;
                    }
                } catch (Exception e) {
                    // Skip comparison if we can't resolve
                }
            }
        } catch (Exception e) {
            // If we can't resolve the target, fall back to string comparison
            return ipList.contains(targetIp);
        }
        
        return false;
    }
}