package org.example.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.model.DeviceField;
import org.example.model.DeviceType;

/**
 * Configuration class that defines which fields should be shown for each device type
 */
public class DeviceFormConfig {
    
    // Common fields that appear for all devices (modified to remove NETWORK_TYPE)
    private static final List<DeviceField> COMMON_REQUIRED_FIELDS = Arrays.asList(
        DeviceField.NETWORK_LOCATION,
        DeviceField.DISPLAY_NAME,
        DeviceField.CONNECTION_TYPE
    );

    // Modified constructor to add NODE_ROUTING as required for specific devices
    private static final Map<DeviceType, List<DeviceField>> REQUIRED_FIELDS = new HashMap<>() {{
        // Computer required fields
        put(DeviceType.COMPUTER, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));
        
        // Router required fields
        put(DeviceType.ROUTER, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.DHCP_ENABLED,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));
        
        // Switch required fields
        put(DeviceType.UNMANAGED_SWITCH, Arrays.asList(
            DeviceField.NODE_ROUTING // Add Node Route here
        ));
        
        // Managed Switch required fields
        put(DeviceType.MANAGED_SWITCH, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));

        // Server required fields
        put(DeviceType.SERVER, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));

                put(DeviceType.LAPTOP, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));
        
        // Security Camera required fields
        put(DeviceType.SECURITY_CAMERA, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));
        
        // Phone required fields
        put(DeviceType.PHONE, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NODE_ROUTING // Add Node Route here
        ));
        
        // VM required fields (exclude Node Route)
        put(DeviceType.VIRTUAL_MACHINE, Arrays.asList(
            DeviceField.IP_HOSTNAME,
            DeviceField.NETWORK_ADAPTER_TYPE

        ));
    }};

    // Maps device types to their optional fields
    private static final Map<DeviceType, List<DeviceField>> OPTIONAL_FIELDS = new HashMap<>() {{
        // Computer optional fields
        put(DeviceType.COMPUTER, Arrays.asList(
            DeviceField.DOMAIN_NAME,
            DeviceField.MAC_ADDRESS
        ));
        
        // Router optional fields
        put(DeviceType.ROUTER, Arrays.asList(
            DeviceField.PORTS,
            DeviceField.MAC_ADDRESS,
            DeviceField.WAN_IP
        ));
        
        // Switch optional fields
        put(DeviceType.UNMANAGED_SWITCH, Arrays.asList(

        ));
        
        // Add Managed Switch optional fields
        put(DeviceType.MANAGED_SWITCH, Arrays.asList(
            DeviceField.MAC_ADDRESS,
            DeviceField.VLANS,
            DeviceField.POE_SUPPORTED,
            DeviceField.UPLINK_DEVICE
        ));

        // Server optional fields
        put(DeviceType.SERVER, Arrays.asList(
            DeviceField.HOSTING_PROVIDER,
            DeviceField.REGION,
            DeviceField.MAC_ADDRESS
        ));
        
        // Security Camera optional fields
        put(DeviceType.SECURITY_CAMERA, Arrays.asList(
            DeviceField.MAC_ADDRESS
        ));
        
        // Phone optional fields
        put(DeviceType.PHONE, Arrays.asList(
            DeviceField.MAC_ADDRESS
        ));
        
        // VM optional fields
        put(DeviceType.VIRTUAL_MACHINE, Arrays.asList(
            DeviceField.HOST_NODE,
            DeviceField.MAC_ADDRESS,
            DeviceField.OPERATING_SYSTEM
        ));
    }};

    /**
     * Gets all fields that should be shown for a device type
     */
    public static List<DeviceField> getFieldsForDevice(DeviceType deviceType) {
        List<DeviceField> fields = new ArrayList<>(COMMON_REQUIRED_FIELDS);
        fields.addAll(REQUIRED_FIELDS.getOrDefault(deviceType, Collections.emptyList()));
        fields.addAll(OPTIONAL_FIELDS.getOrDefault(deviceType, Collections.emptyList()));
        return fields;
    }

    /**
     * Checks if a field is required for a device type
     */
    public static boolean isFieldRequired(DeviceType deviceType, DeviceField field) {
        return COMMON_REQUIRED_FIELDS.contains(field) ||
               REQUIRED_FIELDS.getOrDefault(deviceType, Collections.emptyList()).contains(field);
    }

    /**
     * Gets fields that should be disabled based on other field values
     */
    public static List<DeviceField> getConditionalFields(DeviceType deviceType, Map<DeviceField, String> currentValues) {
        List<DeviceField> disabledFields = new ArrayList<>();
        
        switch (deviceType) {
            case ROUTER:
                if (!"Yes".equals(currentValues.get(DeviceField.MANAGED))) {
                    disabledFields.add(DeviceField.ROUTING_TABLE);
                    disabledFields.add(DeviceField.FIRMWARE_VERSION);
                }
                break;
                
            case UNMANAGED_SWITCH:
                String managedValue = currentValues.get(DeviceField.MANAGED);
                if (!"Yes".equals(managedValue)) {
                    disabledFields.add(DeviceField.VLANS);
                    disabledFields.add(DeviceField.FIRMWARE_VERSION);
                    disabledFields.add(DeviceField.IP_HOSTNAME);
                }
                break;
                
            case VIRTUAL_MACHINE:
                if (!"Yes".equals(currentValues.get(DeviceField.HYPERVISOR_MANAGED))) {
                    disabledFields.add(DeviceField.HYPERVISOR_IP);
                }
                break;
        }
        
        return disabledFields;
    }

    // Add new method to check if a field should be required based on current values
    public static boolean isFieldRequired(DeviceType deviceType, DeviceField field, Map<DeviceField, String> currentValues) {
        // For switches, IP_HOSTNAME is conditionally required based on MANAGED status
        if (deviceType == DeviceType.UNMANAGED_SWITCH && field == DeviceField.IP_HOSTNAME) {
            String managedValue = currentValues.get(DeviceField.MANAGED);
            return "Yes".equals(managedValue);
        }
        
        // Otherwise use the standard required field check
        return isFieldRequired(deviceType, field);
    }
}