package org.example.config;

import org.example.model.DeviceField;
import org.example.model.FieldSection;

/**
 * Configuration for a specific field in the node detail panel
 */
public class NodeDetailFieldConfig {
    private DeviceField field;
    private boolean editable;
    private String label;
    private FieldSection section;
    
    public NodeDetailFieldConfig(DeviceField field, boolean editable, String label, FieldSection section) {
        this.field = field;
        this.editable = editable;
        this.label = label;
        this.section = section;
    }
    
    public DeviceField getField() {
        return field;
    }
    
    public boolean isEditable() {
        return editable;
    }
    
    public String getLabel() {
        return label;
    }
    
    public FieldSection getSection() {
        return section;
    }
}

