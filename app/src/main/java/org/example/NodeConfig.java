package org.example;

public class NodeConfig {
    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private NetworkType networkType;
    private double layoutX;
    private double layoutY;
    private double relativeX; // New field: value between 0.0 and 1.0
    private double relativeY; // New field: value between 0.0 and 1.0
    private boolean mainNode;
    private String nodeColour; // outline color
    private ConnectionType connectionType; // connection type

    public NodeConfig() {
        // Default constructor for Gson
    }

    public NodeConfig(String ipOrHostname, String displayName, DeviceType deviceType, NetworkType networkType,
                      double layoutX, double layoutY, double relativeX, double relativeY,
                      boolean mainNode, String nodeColour, ConnectionType connectionType) {
        this.ipOrHostname = ipOrHostname;
        this.displayName = displayName;
        this.deviceType = deviceType;
        this.networkType = networkType;
        this.layoutX = layoutX;
        this.layoutY = layoutY;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.mainNode = mainNode;
        this.nodeColour = nodeColour;
        this.connectionType = connectionType;
    }

    // Getters and setters
    public String getIpOrHostname() {
        return ipOrHostname;
    }

    public void setIpOrHostname(String ipOrHostname) {
        this.ipOrHostname = ipOrHostname;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }

    public double getLayoutX() {
        return layoutX;
    }

    public void setLayoutX(double layoutX) {
        this.layoutX = layoutX;
    }

    public double getLayoutY() {
        return layoutY;
    }

    public void setLayoutY(double layoutY) {
        this.layoutY = layoutY;
    }

    public double getRelativeX() {
        return relativeX;
    }

    public void setRelativeX(double relativeX) {
        this.relativeX = relativeX;
    }

    public double getRelativeY() {
        return relativeY;
    }

    public void setRelativeY(double relativeY) {
        this.relativeY = relativeY;
    }

    public boolean isMainNode() {
        return mainNode;
    }

    public void setMainNode(boolean mainNode) {
        this.mainNode = mainNode;
    }

    public String getNodeColour() {
        return nodeColour;
    }

    public void setNodeColour(String nodeColour) {
        this.nodeColour = nodeColour;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }
}
