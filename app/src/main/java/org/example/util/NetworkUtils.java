package org.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NetworkUtils {
    /** 
     * Returns the system’s default gateway on Windows, 
     * or null if it can’t be parsed.
     */
    public static String getDefaultGateway() {
        try {
            Process p = new ProcessBuilder("cmd", "/c", "route PRINT 0.0.0.0").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    // Look for a line starting “0.0.0.0” — columns are:
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
}
