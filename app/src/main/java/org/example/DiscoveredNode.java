package org.example;

public class DiscoveredNode {
    public final String ip, mac, hostname, iface;

    public DiscoveredNode(String ip, String mac, String hostname, String iface) {
        this.ip       = ip;
        this.mac      = mac;
        this.hostname = hostname;
        this.iface    = iface;
    }
}
