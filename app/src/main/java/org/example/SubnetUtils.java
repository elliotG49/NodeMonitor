// src/main/java/org/example/SubnetUtils.java
package org.example;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class SubnetUtils {
    /**
     * Given an IPv4 address and prefix length, computes the network in CIDR form.
     * @param ipString     e.g. "192.168.1.42"
     * @param prefixLength 0–32
     * @return              e.g. "192.168.1.0/24"
     */
    public static String getSubnet(String ipString, int prefixLength) throws UnknownHostException {
        byte[] ipBytes = InetAddress.getByName(ipString).getAddress();
        // Build the mask as a 32‑bit int then split into bytes
        int mask = (prefixLength == 0)
            ? 0
            : 0xFFFFFFFF << (32 - prefixLength);
        byte[] maskBytes = ByteBuffer.allocate(4).putInt(mask).array();

        // AND the two arrays to get the network address
        byte[] networkBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            networkBytes[i] = (byte)(ipBytes[i] & maskBytes[i]);
        }

        String networkAddress = InetAddress.getByAddress(networkBytes).getHostAddress();
        return networkAddress + "/" + prefixLength;
    }
}
