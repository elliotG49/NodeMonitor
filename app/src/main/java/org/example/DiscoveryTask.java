package org.example;

import javafx.concurrent.Task;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class DiscoveryTask extends Task<List<DiscoveredNode>> {
    @Override
    protected List<DiscoveredNode> call() throws Exception {
        // 1) Determine a single IPv4 subnet from your main interface
    NetworkInterface foundNi = null;
    InterfaceAddress foundIa = null;
    for (NetworkInterface cand : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (!cand.isUp() || cand.isLoopback()) continue;
        for (InterfaceAddress addr : cand.getInterfaceAddresses()) {
            if (addr.getAddress() instanceof Inet4Address) {
                foundNi = cand;
                foundIa = addr;
                break;
            }
        }
        if (foundNi != null) break;
    }
    if (foundNi == null || foundIa == null) {
        throw new IOException("No suitable interface with IPv4 address");
    }

    // 2) Now assign to final locals so they can be captured by lambdas
    final NetworkInterface ni = foundNi;
    final InterfaceAddress ia = foundIa;

        byte[] base = ia.getAddress().getAddress();
        int prefix = ia.getNetworkPrefixLength();
        int baseInt = toInt(base) & (prefix==0 ? 0 : 0xFFFFFFFF << (32 - prefix));
        int hostBits = 32 - prefix;
        int maxHosts = (1 << hostBits) - 1;

        // 2) Scan all hosts in parallel
        ExecutorService exec = Executors.newFixedThreadPool(50);
        List<Future<DiscoveredNode>> futures = new ArrayList<>();
        for (int i = 1; i < maxHosts; i++) {
            final int host = i;
            futures.add(exec.submit(() -> {
                int addrInt = baseInt | host;
                byte[] addrBytes = new byte[] {
                    (byte)(addrInt>>24), (byte)(addrInt>>16),
                    (byte)(addrInt>>8),  (byte)(addrInt)
                };
                InetAddress target = InetAddress.getByAddress(addrBytes);
                if (!target.isReachable(200)) return null;

                // MAC via ARP cache
                String mac = "?";
                try {
                    Process p = Runtime.getRuntime().exec("arp -a " + target.getHostAddress());
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.contains(target.getHostAddress())) {
                            String[] parts = line.trim().split("\\s+");
                            mac = parts[1];
                            break;
                        }
                    }
                } catch (Exception ignored){}

                // Reverse DNS
                String hostn = target.getHostName();

                // Interface name (same logic as ConnectionLine)
                String iface = ni.getName();

                return new DiscoveredNode(target.getHostAddress(), mac, hostn, iface);
            }));
        }
        exec.shutdown();

        List<DiscoveredNode> results = new ArrayList<>();
        int done = 0, total = futures.size();
        for (Future<DiscoveredNode> f : futures) {
            DiscoveredNode dn = f.get();
            if (dn != null) results.add(dn);
            updateProgress(++done, total);
        }
        return results;
    }

    private int toInt(byte[] b) {
        return ((b[0]&0xFF)<<24)|((b[1]&0xFF)<<16)|((b[2]&0xFF)<<8)|(b[3]&0xFF);
    }
}
