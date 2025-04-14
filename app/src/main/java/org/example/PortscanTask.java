package org.example;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.Task;

public class PortscanTask extends Task<String> {
    private final String targetIp;
    private final List<Integer> ports;
    private final boolean bannerDetection;

    /**
     * Constructs a PortscanTask.
     * @param targetIp The target IP address.
     * @param ports A list of ports to scan.
     * @param bannerDetection Whether to perform banner detection.
     */
    public PortscanTask(String targetIp, List<Integer> ports, boolean bannerDetection) {
        this.targetIp = targetIp;
        this.ports = ports;
        this.bannerDetection = bannerDetection;
    }

    @Override
    protected String call() throws Exception {
        StringBuilder resultBuilder = new StringBuilder();
        int totalPorts = ports.size();
        int scannedPorts = 0;
        
        // Loop through each port.
        for (Integer port : ports) {
            if (isCancelled()) break;
            updateMessage("Scanning port " + port + "...");
            try (Socket socket = new Socket()) {
                // Attempt to connect with a short timeout.
                socket.connect(new InetSocketAddress(targetIp, port), 200);
                resultBuilder.append("Port ").append(port).append(" is OPEN");
                if (bannerDetection) {
                    try {
                        socket.setSoTimeout(200);
                        byte[] buffer = new byte[128];
                        int bytesRead = socket.getInputStream().read(buffer);
                        if (bytesRead > 0) {
                            String banner = new String(buffer, 0, bytesRead).trim();
                            if (!banner.isEmpty()) {
                                resultBuilder.append(" (Banner: ").append(banner).append(")");
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore banner read exceptions.
                    }
                }
                resultBuilder.append("\n");
            } catch (Exception ex) {
            }
            scannedPorts++;
            updateProgress(scannedPorts, totalPorts);
        }
        updateMessage("Port scan complete");
        return resultBuilder.toString();
    }
    
    /**
     * Utility method to parse a ports string into a list of integers.
     * Accepts comma-separated values and ranges (e.g., "22,80,443" or "1-1024").
     */
    public static List<Integer> parsePorts(String portsInput) {
        List<Integer> portList = new ArrayList<>();
        if (portsInput == null || portsInput.isEmpty()) {
            return portList;
        }
        String[] parts = portsInput.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int p = start; p <= end; p++) {
                            portList.add(p);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers.
                    }
                }
            } else {
                try {
                    portList.add(Integer.parseInt(part));
                } catch (NumberFormatException e) {
                    // Skip invalid entry.
                }
            }
        }
        return portList;
    }
}
