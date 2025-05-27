package org.example;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing all possible device fields that can appear in device forms.
 * Each field has a display label and can be required or optional based on device type.
 */
public enum DeviceField {
    // Common fields for all devices
    DISPLAY_NAME("Display Name"),
    IP_HOSTNAME("IP/Hostname"),
    NETWORK_LOCATION("Network Location"),
    CONNECTION_TYPE("Connection Type"),
    NODE_ROUTING("Node Route"),
    MAC_ADDRESS("MAC Address"),
    
    // Computer specific fields
    OPERATING_SYSTEM("Operating System"),
    GROUP("Group"),
    ANTIVIRUS_STATUS("Antivirus Status"),
    UPTIME("Uptime/Last Reboot"),
    DOMAIN_NAME("Domain Name"),
    
    // Router specific fields
    FIRMWARE_VERSION("Firmware Version"),
    PORTS("Ports"),
    UPLINK_DEVICE("Uplink Device"),
    MANAGED("Managed"),
    ROUTING_TABLE("Routing Table/VLANs"),
    SSID("SSID Broadcasted"),
    WAN_IP("WAN IP"),
    DHCP_ENABLED("DHCP Enabled"),
    FIREWALL_ENABLED("Firewall Enabled"),
    
    // Switch specific fields
    SWITCH_PORTS("Number of Ports"),
    VLANS("VLANs Configured"),
    POE_SUPPORTED("PoE Supported"),
    
    // Server specific fields
    DNS_NAME("DNS Name/IP"),
    HOSTING_PROVIDER("Hosting Provider"),
    CLOUD_PLATFORM("Cloud Platform"),
    REGION("Region/Zone"),
    SSH_ENABLED("SSH Enabled"),
    
    // Security Camera specific fields
    FIELD_OF_VIEW("Field of View"),
    RECORDING("Recording Enabled"),
    LOCAL_STORAGE("Local Storage Size"),
    MOTION_DETECTION("Motion Detection"),
    STREAM_URL("Stream URL"),
    NIGHT_VISION("Night Vision Enabled"),
    
    // Phone specific fields
    OS_VERSION("OS Type & Version"),
    USER_ASSIGNED("User Assigned"),
    DEVICE_ID("IMEI/Device ID"),
    
    // VM specific fields
    HOST_MACHINE("Host Machine Name"),
    VIRTUALIZATION_PLATFORM("Virtualization Platform"),
    HYPERVISOR_MANAGED("Managed by Hypervisor"),
    HYPERVISOR_IP("Hypervisor IP"),
    NETWORK_ADAPTER_TYPE("Network Adapter Type"),
    HOST_NODE("Host Node");

    private final String label;
    
    DeviceField(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    /**
     * Returns true if this field should be rendered as a Yes/No choice
     */
    public boolean isYesNoField() {
        return this == MANAGED 
            || this == DHCP_ENABLED
            || this == FIREWALL_ENABLED
            || this == POE_SUPPORTED
            || this == SSH_ENABLED
            || this == RECORDING
            || this == MOTION_DETECTION
            || this == NIGHT_VISION
            || this == HYPERVISOR_MANAGED;
    }
    
    /**
     * Returns true if this field should be rendered as a text area
     * (for longer text input)
     */
    public boolean isTextArea() {
        return this == ROUTING_TABLE 
            || this == VLANS;
    }
    
    /**
     * Returns a list of predefined options for fields that require them.
     * Returns null for fields without predefined options.
     */
    public List<String> getOptions() {
        if (this == NETWORK_ADAPTER_TYPE) {
            return Arrays.asList("NAT", "Bridged", "Host-Only", "Internal");
        }
        return null; // No options for other fields
    }
}