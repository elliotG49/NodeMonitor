package org.example.model;

import java.net.InetAddress;

import org.example.app.NetworkMonitorApp;
import org.example.ui.components.ConnectionLine;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
/**
 * Represents a network node on the spider map.
 * Displays as a square icon with a glow and label below.
 */
public class NetworkNode extends StackPane {
    private static long nextId = 1; // Static counter for generating IDs
    private final long nodeId;      // Unique ID for this node

    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private NetworkLocation networkLocation;
    private boolean mainNode = false;
    private Long routeSwitchId; // Change from String to Long
    private ConnectionType connectionType = ConnectionType.ETHERNET;
    private String resolvedIp = null;
    private boolean connected = false;
    private long startTime;

    // Visual components
    private static final double SQUARE_SIZE = 65; // Size of the square
    private final Rectangle square;
    private final ImageView iconView;
    private final Label nameLabel;

    // Dragging support
    private double dragDeltaX;
    private double dragDeltaY;

    private Long routeViaId; // Change from String to Long
    private Long hostNodeId;    // Change from String to Long

    // Update setters/getters to use IDs
    public Long getRouteSwitchId() { return routeSwitchId; }
    public void setRouteSwitchId(Long id) { this.routeSwitchId = id; }
    
    public Long getHostNodeId() { return hostNodeId; }
    public void setHostNodeId(Long id) { this.hostNodeId = id; }

    // Keep string versions for UI display only
    private String routeSwitch = "";
    private String hostNode = "";
    
    public String getRouteSwitch() {
        return routeSwitch == null ? "" : routeSwitch;
    }

    
    public String getHostNode() {
        return hostNode == null ? "" : hostNode;
    }


    public NetworkNode(String ipOrHostname, String displayName,
                       DeviceType deviceType, NetworkLocation networkLocation) {
        this.nodeId = nextId++;
        this.ipOrHostname = ipOrHostname;
        this.displayName  = displayName;
        this.deviceType   = deviceType;
        this.networkLocation = networkLocation; // Use networkLocation directly
        this.startTime    = System.currentTimeMillis();

        // --- Square background ---
        square = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
        square.setArcWidth(30);  // Border radius
        square.setArcHeight(30);
        square.getStyleClass().add("node-square");
        square.setFill(Color.web("#222E3C")); // Or use your preferred color
        square.setStroke(Color.web("#008b97")); // Optional: border color
        square.setStyle(
                "-fx-effect: dropshadow(gaussian, -node-glow-color, 9, 0.5, 0, 0);"
            );
        square.setStrokeWidth(2);           // Optional: border width

        // --- Icon inside square ---
        iconView = new ImageView(new Image(
            getClass().getResourceAsStream("/icons/" + getIconFileName())
        ));
        iconView.setFitWidth(40);
        iconView.setFitHeight(40);

        // Create inner container for square and icon
        StackPane nodeContainer = new StackPane();
        nodeContainer.getChildren().addAll(square, iconView);
        nodeContainer.setAlignment(Pos.CENTER);

        // Create label
        nameLabel = new Label(displayName);
        nameLabel.setFont(Font.font(12));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-font-weight: 700; -fx-background-color: rgba(81, 81, 81, 0.4); -fx-padding: 2 6; -fx-background-radius: 10;");
        nameLabel.setViewOrder(-1); // Ensures label stays on top of other elements

        // Use VBox to maintain consistent vertical spacing
        VBox layout = new VBox(10); // 15px spacing between elements
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(nodeContainer, nameLabel);
        
        // Add VBox to the StackPane (this)
        getChildren().add(layout);

        // Remove previous padding since we're using VBox spacing
        setPadding(new Insets(0));

        // --- Hover effect ---
        ScaleTransition hoverScale = new ScaleTransition(Duration.millis(200), nodeContainer);
        setOnMouseEntered(e -> {
            hoverScale.stop();
            hoverScale.setToX(1.05);
            hoverScale.setToY(1.05);
            hoverScale.playFromStart();
            
            square.setStroke(Color.web("#C2C2C2"));
            square.setStyle(
                "-fx-effect: dropshadow(gaussian, #C2C2C2, 9, 0.5, 0, 0);"
            );

            setCursor(Cursor.HAND);

            // Increase brightness of connected lines
            NetworkMonitorApp.getInstance().spiderMapPane.getChildren().forEach(child -> {
                if (child instanceof ConnectionLine) {
                    ConnectionLine line = (ConnectionLine) child;
                    if (line.getFrom() == this || line.getTo() == this) {
                        ColorAdjust brighten = new ColorAdjust();
                        brighten.setBrightness(0.6); // Adjust brightness level
                        line.setEffect(brighten);
                    }
                }
            });
        });
        setOnMouseExited(e -> {
            hoverScale.stop();
            hoverScale.setToX(1.0);
            hoverScale.setToY(1.0);
            hoverScale.playFromStart();
            
            // Return to original border color and remove glow
            square.setFill(Color.web("#222E3C")); // Or use your preferred color
            square.setStroke(Color.web("#008b97")); // Optional: border color
            square.setStyle(
                    "-fx-effect: dropshadow(gaussian, -node-glow-color, 9, 0.5, 0, 0);"
                );

            setCursor(Cursor.DEFAULT);

            // Reset brightness of connected lines
            NetworkMonitorApp.getInstance().spiderMapPane.getChildren().forEach(child -> {
                if (child instanceof ConnectionLine) {
                    ConnectionLine line = (ConnectionLine) child;
                    if (line.getFrom() == this || line.getTo() == this) {
                        line.setEffect(null); // Remove the effect
                    }
                }
            });
        });

        // --- Drag handling ---
        setOnMousePressed(e -> {
            dragDeltaX = getLayoutX() - e.getSceneX();
            dragDeltaY = getLayoutY() - e.getSceneY();
            toFront();
        });
        setOnMouseDragged(e -> {
            setLayoutX(e.getSceneX() + dragDeltaX);
            setLayoutY(e.getSceneY() + dragDeltaY);
        });

        // --- Context menu on right-click ---
        setOnContextMenuRequested(e -> {
            ContextMenu cm = new ContextMenu();
            MenuItem tracerouteItem = new MenuItem("Traceroute");
            MenuItem portscanItem  = new MenuItem("Portscan");
            cm.getItems().addAll(tracerouteItem, portscanItem);
            cm.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // --- Resolve hostname asynchronously ---
        if (!ipOrHostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            new Thread(() -> {
                try {
                    InetAddress addr = InetAddress.getByName(ipOrHostname);
                    resolvedIp = addr.getHostAddress();
                } catch (Exception ignored) {}
            }).start();
        }
    }

    /** Returns the appropriate icon filename */
    private String getIconFileName() {
        switch (deviceType) {
            case COMPUTER:            return "host.png";
            case UNMANAGED_SWITCH:    return "switch.png";
            case MANAGED_SWITCH:      return "switch.png";  // Add this line
            case LAPTOP:              return "laptop.png";
            case SERVER:              return "server.png";
            case ROUTER:              return "gateway.png";
            case GATEWAY:             return "gateway.png";
            case PHONE:               return "phone.png";
            case TV:                  return "tv.png";
            case SECURITY_CAMERA:     return "security_camera.png";
            case VIRTUAL_MACHINE:     return "virtual_machine.png";
            case WIRELESS_ACCESS_POINT: return "wap.png";
            default:                  return "host.png";
        }
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────
    public String getIpOrHostname()            { return ipOrHostname; }
    public void setIpOrHostname(String ip)      { this.ipOrHostname = ip; }

    public String getDisplayName()              { return displayName; }
    public void setDisplayName(String name) {
        this.displayName = name;
        nameLabel.setText(name);
    }

    public DeviceType getDeviceType()           { return deviceType; }
    public void setDeviceType(DeviceType dt) {
        this.deviceType = dt;
        iconView.setImage(new Image(
            getClass().getResourceAsStream("/icons/"+getIconFileName())
        ));
    }

    public boolean isMainNode()                 { return mainNode; }
    public void setMainNode(boolean m)          { this.mainNode = m; }

    public ConnectionType getConnectionType()   { return connectionType; }
    public void setConnectionType(ConnectionType ct) { this.connectionType = ct; }

    public String getResolvedIp()               { return resolvedIp; }

    public boolean isConnected()                { return this.connected; }
    public void setConnected(boolean c)         { this.connected = c; }

    public long getStartTime()                  { return startTime; }

        private String macAddress;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setRouteVia(NetworkNode node) {
        this.routeViaId = node != null ? node.getNodeId() : null;
    }

    public Long getRouteViaId() {
        return routeViaId;
    }

    /** Update all properties from another node */
    public void updateFrom(NetworkNode updated) {
        setIpOrHostname(updated.getIpOrHostname());
        setDisplayName(updated.getDisplayName());
        setDeviceType(updated.getDeviceType());
        if (!isMainNode()) setConnectionType(updated.getConnectionType());
        setRouteSwitch(updated.getRouteSwitch());
    }

    /** No-op: fixed size square
     *  but retained for interface consistency */
    public void updateLayoutForSavedSize() {}

    // Add getter (no setter - ID should be immutable)
    public long getNodeId() {
        return nodeId;
    }

    public double getRelativeX() {
        return getLayoutX() / getScene().getWidth();
    }

    public double getRelativeY() {
        return getLayoutY() / getScene().getHeight();
    }

    public void setRelativeX(double x) {
        setLayoutX(x * getScene().getWidth());
    }

    public void setRelativeY(double y) {
        setLayoutY(y * getScene().getHeight());
    }

    public void setRouteSwitch(String routeSwitch) {
        this.routeSwitch = routeSwitch;
        
        // Always look up the node by display name and update the ID
        // This ensures the ID is always in sync with the name
        if (routeSwitch != null && !routeSwitch.isEmpty()) {
            NetworkNode routeNode = NetworkMonitorApp.getInstance().getNodeByDisplayName(routeSwitch);
            if (routeNode != null) {
                this.routeSwitchId = routeNode.getNodeId();
            }
        } else {
            this.routeSwitchId = null;
        }
    }

    public void setHostNode(String hostNode) {
        this.hostNode = hostNode;
        // When setting the host node name, also set the ID
        if (hostNode != null && !hostNode.isEmpty()) {
            NetworkNode hostNodeObj = NetworkMonitorApp.getInstance().getNodeByDisplayName(hostNode);
            if (hostNodeObj != null) {
                this.hostNodeId = hostNodeObj.getNodeId();
            }
        } else {
            this.hostNodeId = null;
        }
    }

    // Add this method to set the ID directly (used during loading)
    public void setNodeIdDirectly(Long id) {
        // Access the normally-private static counter
        NetworkNode.nextId = Math.max(NetworkNode.nextId, id + 1);
        // Use reflection to modify the final field for this instance
        try {
            java.lang.reflect.Field field = NetworkNode.class.getDeclaredField("nodeId");
            field.setAccessible(true);
            field.set(this, id);
        } catch (Exception e) {
            System.err.println("Failed to set node ID directly: " + e.getMessage());
        }
    }

    // Methods to set names without changing IDs
    public void setRouteSwitchWithoutIdUpdate(String name) {
        this.routeSwitch = name;
        // Don't update routeSwitchId
    }

    public void setHostNodeWithoutIdUpdate(String name) {
        this.hostNode = name;
        // Don't update hostNodeId
    }

    // Add getter and setter
    public NetworkLocation getNetworkLocation() {
        return networkLocation;
    }

    public void setNetworkLocation(NetworkLocation networkLocation) {
        this.networkLocation = networkLocation;
    }

    private boolean isHighlighted = false; // Add this field to track highlighted state

    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted; // Store the highlighted state
        
        // Create a timeline for smooth color transition
        javafx.animation.Timeline colorTimeline = new javafx.animation.Timeline();
        
        if (highlighted) {
            // Transition to white glow
            javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(300),
                new javafx.animation.KeyValue(square.strokeProperty(), javafx.scene.paint.Color.WHITE)
            );
            colorTimeline.getKeyFrames().add(keyFrame);
            
            // Also add a glow effect
            javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow(0.5);
            javafx.animation.KeyFrame glowFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(300),
                new javafx.animation.KeyValue(square.effectProperty(), glow)
            );
            colorTimeline.getKeyFrames().add(glowFrame);
        } else {
            // Return to original color
            javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(300),
                new javafx.animation.KeyValue(square.strokeProperty(), javafx.scene.paint.Color.web("#444"))
            );
            colorTimeline.getKeyFrames().add(keyFrame);
            
            // Remove glow effect
            javafx.animation.KeyFrame glowFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(300),
                new javafx.animation.KeyValue(square.effectProperty(), null)
            );
            colorTimeline.getKeyFrames().add(glowFrame);
        }
        
        colorTimeline.play();
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }
}
