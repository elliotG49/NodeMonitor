package org.example.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.model.DeviceField;
import org.example.model.DeviceType;
import org.example.model.FieldSection;

/**
 * Configuration for the node detail panel, defining which fields 
 * and sections are available for different device types
 */
public class NodeDetailPanelConfig {
    // Map device types to their field configurations
    private static final Map<DeviceType, List<NodeDetailFieldConfig>> FIELD_CONFIGS = new HashMap<>();
    
    // Map device types to their available sections
    private static final Map<DeviceType, List<FieldSection>> DEVICE_SECTIONS = new HashMap<>();
    
    // Initialize the configurations
    static {
        // Configure for each device type
        configureComputerFields();
        configureRouterFields();
        // Add more configure methods for other device types
    }
    
    private static void configureComputerFields() {
        List<NodeDetailFieldConfig> fields = new ArrayList<>();
        
        // Basic Info section
        addField(fields, DeviceField.DEVICE_TYPE, false, "Device Type", FieldSection.NODE_BASIC_INFORMATION);
        addField(fields, DeviceField.DISPLAY_NAME, true, "Name", FieldSection.NODE_BASIC_INFORMATION);
        addField(fields, DeviceField.NETWORK_LOCATION, true, "Network Location", FieldSection.NODE_BASIC_INFORMATION);
        addField(fields, DeviceField.CONNECTION_TYPE, true, "Connection Type", FieldSection.NODE_BASIC_INFORMATION);
        addField(fields, DeviceField.IP_HOSTNAME, true, "IP/Hostname", FieldSection.NODE_BASIC_INFORMATION);
        addField(fields, DeviceField.NODE_ROUTING, true, "Node Routing", FieldSection.NODE_BASIC_INFORMATION);
        addField(fields, DeviceField.MAC_ADDRESS, false, "MAC Address", FieldSection.NODE_BASIC_INFORMATION);
        
        // Connection Info section
        addField(fields, DeviceField.TOTAL_CONNECTIONS, false, "Total Connections", FieldSection.CONNECTION_INFORMATION);
        addField(fields, DeviceField.ONLINE_CONNECTIONS, false, "Online Connections", FieldSection.CONNECTION_INFORMATION);
        
        FIELD_CONFIGS.put(DeviceType.COMPUTER, fields);
        
        // Define which sections this device type has
        DEVICE_SECTIONS.put(DeviceType.COMPUTER, Arrays.asList(
            FieldSection.NODE_BASIC_INFORMATION,
            FieldSection.CONNECTION_INFORMATION
        ));
    }
    
    // Add configuration methods for other device types...
    
    private static void configureRouterFields() {
        // Similar implementation as configureComputerFields...
    }
    
    private static void addField(List<NodeDetailFieldConfig> list, DeviceField field, 
                            boolean editable, String label, FieldSection section) {
        list.add(new NodeDetailFieldConfig(field, editable, label, section));
    }
    
    /**
     * Get all fields for a device type
     */
    public static List<NodeDetailFieldConfig> getFieldsForDevice(DeviceType deviceType) {
        return FIELD_CONFIGS.getOrDefault(deviceType, Collections.emptyList());
    }
    
    /**
     * Get fields for a specific section of a device type
     */
    public static List<NodeDetailFieldConfig> getFieldsForSection(DeviceType deviceType, FieldSection section) {
        return getFieldsForDevice(deviceType).stream()
            .filter(field -> field.getSection() == section)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all sections available for a device type
     */
    public static List<FieldSection> getSectionsForDevice(DeviceType deviceType) {
        return DEVICE_SECTIONS.getOrDefault(deviceType, Collections.emptyList());
    }
}