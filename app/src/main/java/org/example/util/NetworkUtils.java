package org.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {
    /** 
     * Returns the system's default gateway IP on Windows,
     * or null if it can't be parsed.
     */
    public static String getDefaultGateway() {
        try {
            Process p = new ProcessBuilder("cmd", "/c", "route PRINT 0.0.0.0").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    // Look for a line starting "0.0.0.0" — columns are:
                    // Network Destination | Netmask | Gateway | Interface | Metric
                    if (line.startsWith("0.0.0.0")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            return parts[2];
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns hostname information for the default gateway.
     * @return String[] where [0] is IP address and [1] is hostname or "Default Gateway" if hostname not resolvable
     */
    public static String[] getDefaultGatewayInfo() {
        String gatewayIp = getDefaultGateway();
        if (gatewayIp == null) {
            return null;
        }
        
        String hostname = "Default Gateway";
        try {
            // First try nslookup
            Process process = new ProcessBuilder("cmd", "/c", "nslookup " + gatewayIp).start();
            boolean hostnameFound = false;
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean passedAddress = false;
                
                while ((line = reader.readLine()) != null) {
                    System.out.println("[DEBUG] nslookup output: " + line);
                    
                    // Look for a line containing "Name:"
                    if (line.contains("Name:")) {
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            String possibleHostname = parts[1].trim();
                            // Ensure it's not just the IP address again or empty
                            if (!possibleHostname.equals(gatewayIp) && !possibleHostname.isEmpty()) {
                                hostname = possibleHostname;
                                hostnameFound = true;
                                System.out.println("[DEBUG] Found hostname via nslookup: " + hostname);
                                break;
                            }
                        }
                    }
                }
            }
            
            // If nslookup didn't work, try InetAddress as backup
            if (!hostnameFound) {
                System.out.println("[DEBUG] nslookup failed, trying InetAddress");
                InetAddress address = InetAddress.getByName(gatewayIp);
                String resolvedName = address.getHostName();
                // Only use if it returned something different than the IP
                if (!resolvedName.equals(gatewayIp)) {
                    hostname = resolvedName;
                    System.out.println("[DEBUG] Found hostname via InetAddress: " + hostname);
                    hostnameFound = true;
                }
            }
            
            // If still no hostname, try reverse DNS lookup via 'ping -a'
            if (!hostnameFound) {
                System.out.println("[DEBUG] Trying ping -a for reverse DNS lookup");
                Process pingProcess = new ProcessBuilder("cmd", "/c", "ping -a -n 1 " + gatewayIp).start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pingProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[DEBUG] ping -a output: " + line);
                        // First line should contain "Pinging [hostname] ([ip])"
                        if (line.startsWith("Pinging")) {
                            int openBracket = line.indexOf('[');
                            int closeBracket = line.indexOf(']');
                            if (openBracket > 8 && closeBracket > openBracket) { // "Pinging " is 8 chars
                                String possibleHostname = line.substring(8, openBracket).trim();
                                if (!possibleHostname.isEmpty() && !possibleHostname.equals(gatewayIp)) {
                                    hostname = possibleHostname;
                                    System.out.println("[DEBUG] Found hostname via ping -a: " + hostname);
                                    hostnameFound = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            if (!hostnameFound) {
                System.out.println("[DEBUG] Could not resolve hostname, using default: " + hostname);
            }
        } catch (Exception e) {
            // If any errors occur during resolution, we'll use the default name
            System.out.println("[DEBUG] Could not resolve gateway hostname: " + e.getMessage());
        }
        
        return new String[]{gatewayIp, hostname};
    }

    /**
     * Gets the local IP address and subnet mask of the primary network interface
     * @return String[] where [0] is IP address and [1] is subnet mask
     */
    public static String[] getLocalNetworkInfo() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Skip IPv6 addresses
                    if (addr.getHostAddress().contains(":")) {
                        continue;
                    }
                    
                    // Get subnet mask
                    short prefixLength = iface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
                    String subnetMask = prefixLengthToSubnetMask(prefixLength);
                    
                    return new String[]{addr.getHostAddress(), subnetMask};
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts CIDR prefix length to subnet mask
     */
    private static String prefixLengthToSubnetMask(short prefixLength) {
        int mask = 0xffffffff << (32 - prefixLength);
        return String.format("%d.%d.%d.%d",
            (mask >> 24) & 0xFF,
            (mask >> 16) & 0xFF,
            (mask >> 8) & 0xFF,
            mask & 0xFF);
    }

    /**
     * Calculates the network address from IP and subnet mask
     */
    private static String calculateNetworkAddress(String ip, String subnetMask) {
        String[] ipParts = ip.split("\\.");
        String[] maskParts = subnetMask.split("\\.");
        int[] network = new int[4];
        
        for (int i = 0; i < 4; i++) {
            network[i] = Integer.parseInt(ipParts[i]) & Integer.parseInt(maskParts[i]);
        }
        
        return String.format("%d.%d.%d.%d", network[0], network[1], network[2], network[3]);
    }

    /**
     * Gets all local IP addresses and subnet masks
     * @return List of String[] where [0] is IP address and [1] is subnet mask
     */
    public static List<String[]> getAllLocalNetworkInfo() {
        List<String[]> networkInfoList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                // Print interface name for debugging
                System.out.println("\n[DEBUG] Checking interface: " + iface.getDisplayName());

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Skip IPv6 addresses
                    if (addr.getHostAddress().contains(":")) {
                        continue;
                    }
                    
                    // Get subnet mask
                    short prefixLength = iface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
                    String subnetMask = prefixLengthToSubnetMask(prefixLength);
                    
                    String[] networkInfo = new String[]{addr.getHostAddress(), subnetMask};
                    networkInfoList.add(networkInfo);
                    
                    System.out.println("[DEBUG] Found network: IP=" + networkInfo[0] + ", Mask=" + networkInfo[1]);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return networkInfoList;
    }

    /**
     * Performs a ping sweep of the local subnet
     * @return List of responsive IP addresses
     */
    public static List<String> pingSweep() {
        List<String> activeHosts = new ArrayList<>();
        List<String[]> allNetworkInfo = getAllLocalNetworkInfo();
        
        if (allNetworkInfo.isEmpty()) {
            System.err.println("[DEBUG] Could not determine network information");
            return activeHosts;
        }

        // Create thread pool for parallel execution
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Future<String>> futures = new ArrayList<>();
        int attemptedPings = 0;

        // Scan each network interface
        for (String[] networkInfo : allNetworkInfo) {
            String localIp = networkInfo[0];
            String subnetMask = networkInfo[1];
            String networkAddress = calculateNetworkAddress(localIp, subnetMask);
            
            System.out.println("\n[DEBUG] Network Sweep Details:");
            System.out.println("Local IP: " + localIp);
            System.out.println("Subnet Mask: " + subnetMask);
            System.out.println("Network Address: " + networkAddress);

            // Calculate hosts for this subnet
            String[] networkParts = networkAddress.split("\\.");
            String[] maskParts = subnetMask.split("\\.");
            int[] network = new int[4];
            int[] mask = new int[4];
            
            for (int i = 0; i < 4; i++) {
                network[i] = Integer.parseInt(networkParts[i]);
                mask[i] = Integer.parseInt(maskParts[i]);
            }

            // Calculate total hosts to scan based on subnet mask
            int hostBits = 32 - Integer.bitCount((mask[0] << 24) | (mask[1] << 16) | (mask[2] << 8) | mask[3]);
            int totalHosts = (1 << hostBits) - 2;
            
            System.out.println("Total hosts to scan: " + totalHosts);
            System.out.println("Starting ping sweep for this subnet...\n");

            // Iterate through possible addresses for this subnet
            for (int i = 1; i < 255; i++) {
                final String ipToTest = String.format("%d.%d.%d.%d",
                    network[0],
                    network[1],
                    network[2],
                    i);

                // Skip if it's our own IP
                if (ipToTest.equals(localIp)) {
                    System.out.println("[DEBUG] Skipping local IP: " + ipToTest);
                    continue;
                }

                attemptedPings++;
                System.out.println("[DEBUG] Queuing ping to: " + ipToTest);

                // Submit ping task
                futures.add(executor.submit(() -> {
                    try {
                        Process ping = new ProcessBuilder("ping", "-n", "1", "-w", "500", ipToTest).start();
                        if (ping.waitFor() == 0) {
                            System.out.println("[DEBUG] ✓ Host responding: " + ipToTest);
                            return ipToTest;
                        } else {
                            System.out.println("[DEBUG] ✗ No response from: " + ipToTest);
                        }
                    } catch (Exception e) {
                        System.err.println("[DEBUG] Error pinging " + ipToTest + ": " + e.getMessage());
                    }
                    return null;
                }));
            }
        }

        // Collect results
        for (Future<String> future : futures) {
            try {
                String result = future.get(750, TimeUnit.MILLISECONDS);
                if (result != null) {
                    activeHosts.add(result);
                }
            } catch (Exception e) {
                // Timeout or execution exception - ignore
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n[DEBUG] Ping sweep summary:");
        System.out.println("Attempted pings: " + attemptedPings);
        System.out.println("Responsive hosts: " + activeHosts.size());
        System.out.println("Response rate: " + String.format("%.1f%%", (activeHosts.size() / (double)attemptedPings) * 100));
        System.out.println("Active hosts:");
        activeHosts.forEach(host -> System.out.println("- " + host));
        System.out.println();

        return activeHosts;
    }
}
