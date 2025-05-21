package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class that defines which fields should be shown for each device type
 */
public class DeviceFormConfig {
    
    // Common fields that appear for all devices
    private static final List<DeviceField> COMMON_REQUIRED_FIELDS = Arrays.asList(
        DeviceField.DISPLAY_NAME,
        DeviceField.IP_HOSTNAME,
        DeviceField.NETWORK_TYPE,
        DeviceField.CONNECTION_TYPE,
        DeviceField.NODE_ROUTING
    );

    // Maps device types to their required fields (beyond common fields)
    private static final Map<DeviceType, List<DeviceField>> REQUIRED_FIELDS = new HashMap<>() {{
        // Computer required fields
        put(DeviceType.COMPUTER, Arrays.asList(
        ));
        
        // Router required fields
        put(DeviceType.ROUTER, Arrays.asList(
            DeviceField.MANAGED,
            DeviceField.DHCP_ENABLED
        ));
        
        // Switch required fields
        put(DeviceType.SWITCH, Arrays.asList(
            DeviceField.MANAGED
        ));
        
        // Server required fields
        put(DeviceType.SERVER, Arrays.asList(
        ));
        
        // Security Camera required fields
        put(DeviceType.SECURITY_CAMERA, Arrays.asList(

        ));
        
        // Phone required fields
        put(DeviceType.PHONE, Arrays.asList(
        ));
        
        // VM required fields
        put(DeviceType.VIRTUAL_MACHINE, Arrays.asList(

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
            DeviceField.WAN_IP
        ));
        
        // Switch optional fields
        put(DeviceType.SWITCH, Arrays.asList(
            DeviceField.MAC_ADDRESS
        ));
        
        // Server optional fields
        put(DeviceType.SERVER, Arrays.asList(
            DeviceField.HOSTING_PROVIDER,
            DeviceField.REGION
        ));
        
        // Security Camera optional fields
        put(DeviceType.SECURITY_CAMERA, Arrays.asList(

        ));
        
        // Phone optional fields
        put(DeviceType.PHONE, Arrays.asList(
            DeviceField.MAC_ADDRESS
        ));
        
        // VM optional fields
        put(DeviceType.VIRTUAL_MACHINE, Arrays.asList(
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
                
            case SWITCH:
                if (!"Yes".equals(currentValues.get(DeviceField.MANAGED))) {
                    disabledFields.add(DeviceField.VLANS);
                    disabledFields.add(DeviceField.FIRMWARE_VERSION);
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
}