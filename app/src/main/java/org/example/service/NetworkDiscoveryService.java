package org.example.service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.example.app.NetworkMonitorApp;
import org.example.config.DiscoveredNode;
import org.example.model.NetworkNode;

public class NetworkDiscoveryService {
    private final ConcurrentHashMap<String, List<DiscoveredNode>> discoveredNodes = new ConcurrentHashMap<>();
    private volatile boolean isCancelled = false;
    
    public CompletableFuture<Void> startDiscovery(
            Consumer<Integer> interfaceCountCallback,
            Consumer<String> newInterfaceCallback,
            Consumer<DiscoveredNode> nodeDiscoveredCallback) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Get existing nodes' IPs for filtering
                Set<String> existingIPs = new HashSet<>();
                for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
                    if (node.getResolvedIp() != null && !node.getResolvedIp().isEmpty()) {
                        existingIPs.add(node.getResolvedIp());
                    }
                    existingIPs.add(node.getIpOrHostname());
                }
                
                // Get ARP table
                Process process = Runtime.getRuntime().exec("arp -a");
                process.waitFor();
                String output = new String(process.getInputStream().readAllBytes());
                
                // Debug print raw ARP output
                System.out.println("=== Raw ARP Output ===");
                System.out.println(output);
                System.out.println("=====================");
                
                String[] lines = output.split("\n");
                String currentInterface = null;
                List<NetworkInterface> activeInterfaces = new ArrayList<>();

                // First pass - count interfaces
                int interfaceCount = (int) Arrays.stream(lines)
                    .filter(line -> line.trim().startsWith("Interface:"))
                    .count();
                
                // Update interface count immediately
                interfaceCountCallback.accept(interfaceCount);
                System.out.println("Found " + interfaceCount + " interfaces");

                // Rest of the discovery process
                for (String line : lines) {
                    if (isCancelled) break;
                    
                    line = line.trim();
                    
                    // More precise interface detection
                    if (line.startsWith("Interface:")) {
                        currentInterface = line.split(":")[1].trim().split("---")[0].trim();
                        System.out.println("\nFound Interface: " + currentInterface);
                        
                        if (!discoveredNodes.containsKey(currentInterface)) {
                            discoveredNodes.put(currentInterface, new ArrayList<>());
                            activeInterfaces.add(NetworkInterface.getByInetAddress(
                                InetAddress.getByName(currentInterface)));
                            newInterfaceCallback.accept(currentInterface);
                            System.out.println("Added new interface to tracking");
                        }
                        continue;
                    }

                    // Skip headers and empty lines
                    if (line.isEmpty() || line.contains("Internet Address") || currentInterface == null) {
                        continue;
                    }

                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        String ip = parts[0];
                        String mac = parts[1];
                        String type = parts[2];
                        
                        System.out.println("Checking entry: IP=" + ip + ", MAC=" + mac + ", Type=" + type);
                        
                        // Check if IP already exists in application
                        if (existingIPs.contains(ip)) {
                            System.out.println("Skipped entry - IP already exists in application");
                            continue;
                        }
                        
                        // Only process dynamic entries with valid MAC addresses
                        // Exclude broadcast and multicast addresses
                        if (type.equalsIgnoreCase("dynamic") && 
                            !mac.equals("---") && 
                            !ip.endsWith(".255") &&
                            !ip.startsWith("224.") &&
                            !ip.startsWith("239.") &&
                            mac.matches("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")) {
                        
                            try {
                                InetAddress address = InetAddress.getByName(ip);
                                String hostname = address.getCanonicalHostName();
                                
                                DiscoveredNode node = new DiscoveredNode(
                                    ip,
                                    mac,
                                    hostname.equals(ip) ? "" : hostname,
                                    currentInterface
                                );
                                
                                discoveredNodes.get(currentInterface).add(node);
                                nodeDiscoveredCallback.accept(node);
                                System.out.println("Added node: " + ip + " to interface " + currentInterface);
                            } catch (Exception e) {
                                System.out.println("Failed to process node: " + ip + " - " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Skipped entry - doesn't meet criteria");
                        }
                    }
                }

                // Print final discovery results
                System.out.println("\n=== Discovery Results ===");
                System.out.println("Active Interfaces: " + activeInterfaces.size());
                discoveredNodes.forEach((iface, nodes) -> {
                    System.out.println("\nInterface: " + iface);
                    System.out.println("Nodes found: " + nodes.size());
                    nodes.forEach(node -> 
                        System.out.println("  - " + node.ip + " (" + node.mac + ")" + 
                            (node.hostname.isEmpty() ? "" : " hostname: " + node.hostname))
                    );
                });
                System.out.println("=====================");

            } catch (Exception e) {
                System.out.println("Discovery process failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void cancelDiscovery() {
        isCancelled = true;
    }

    public List<DiscoveredNode> getNodesForInterface(String interfaceName) {
        return discoveredNodes.getOrDefault(interfaceName, new ArrayList<>());
    }

    public List<String> getDiscoveredInterfaces() {
        return new ArrayList<>(discoveredNodes.keySet());
    }
}