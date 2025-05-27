package org.example;

public class NodeConfig {
    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private double layoutX;
    private double layoutY;
    private double relativeX;
    private double relativeY;
    private boolean mainNode;
    // Removed nodeColour field.
    private ConnectionType connectionType;
    private double width;
    private double height;
    private Long routeSwitchId;  // Add these fields
    private Long hostNodeId;
    private String routeSwitch;  // Keep these for display purposes
    private String hostNode;  // Add this field
    private Long nodeId;  // Add this field
    private NetworkLocation networkLocation; // Add new field

    public NodeConfig() { }

    public NodeConfig(String ipOrHostname, String displayName, DeviceType deviceType,
                    NetworkLocation networkLocation, double layoutX, double layoutY, double relativeX, double relativeY, boolean mainNode,
                    ConnectionType connectionType, double width, double height) {
        this.ipOrHostname = ipOrHostname;
        this.displayName = displayName;
        this.deviceType = deviceType;
        this.layoutX = layoutX;
        this.layoutY = layoutY;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.mainNode = mainNode;
        this.connectionType = connectionType;
        this.width = width;
        this.height = height;
        this.networkLocation = networkLocation;
    }

    public String getRouteSwitch() {
        return routeSwitch;
    }

    public void setRouteSwitch(String routeSwitch) {
        this.routeSwitch = routeSwitch;
    }

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

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long id) {
        this.nodeId = id;
    }

    public Long getRouteSwitchId() { return routeSwitchId; }
    public void setRouteSwitchId(Long id) { this.routeSwitchId = id; }
    
    public Long getHostNodeId() { return hostNodeId; }
    public void setHostNodeId(Long id) { this.hostNodeId = id; }

    public String getHostNode() {
        return hostNode;
    }

    public void setHostNode(String hostNode) {
        this.hostNode = hostNode;
    }

    public NetworkLocation getNetworkLocation() {
        return networkLocation;
    }

    public void setNetworkLocation(NetworkLocation networkLocation) {
        this.networkLocation = networkLocation;
    }
}
