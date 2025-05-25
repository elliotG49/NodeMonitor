package org.example;

public enum NetworkLocation {
    REMOTE_PRIVATE("Remote Private Network"),
    LOCAL("Local Network"),
    PUBLIC("Public Network");

    private final String label;

    NetworkLocation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}