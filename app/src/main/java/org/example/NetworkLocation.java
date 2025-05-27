package org.example;

public enum NetworkLocation {
    LOCAL("Local Network"),
    PUBLIC("Public Network"),
    REMOTE_PRIVATE("Remote Private Network");

    private final String label;

    NetworkLocation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
    // Helper method to determine if direct connections are possible
    public boolean isDirectlyAccessible() {
        return this != REMOTE_PRIVATE; // Both LOCAL and PUBLIC can be directly accessed
    }
    
    // Helper method to determine if ping is likely to work
    public boolean isPingable() {
        return this == LOCAL; // Only LOCAL devices are guaranteed pingable
    }
}