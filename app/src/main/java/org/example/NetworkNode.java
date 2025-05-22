package org.example;

import java.net.InetAddress;

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
    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private NetworkType networkType;
    private boolean mainNode = false;
    private String routeSwitch = "";
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

    public NetworkNode(String ipOrHostname, String displayName,
                       DeviceType deviceType, NetworkType networkType) {
        this.ipOrHostname = ipOrHostname;
        this.displayName  = displayName;
        this.deviceType   = deviceType;
        this.networkType  = networkType;
        this.startTime    = System.currentTimeMillis();

        // --- Square background ---
        square = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
        square.setArcWidth(30);  // Border radius
        square.setArcHeight(30);
        square.getStyleClass().add("node-square");
        square.setFill(Color.web("#222")); // Or use your preferred color
        square.setStroke(Color.web("#444")); // Optional: border color
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
        nameLabel.setFont(Font.font(16));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-font-weight: 600;");

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
            square.setStyle(
                "-fx-effect: dropshadow(gaussian, -node-glow-hovered-color, 9, 0.5, 0, 0);"
            );

            setCursor(Cursor.HAND);

            // Increase brightness of connected lines
            NetworkMonitorApp.getInstance().spiderMapPane.getChildren().forEach(child -> {
                if (child instanceof ConnectionLine) {
                    ConnectionLine line = (ConnectionLine) child;
                    if (line.getFrom() == this || line.getTo() == this) {
                        ColorAdjust brighten = new ColorAdjust();
                        brighten.setBrightness(0.75); // Adjust brightness level
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
            square.setStyle("");

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
            tracerouteItem.setOnAction(a -> NetworkMonitorApp.performTraceroute(NetworkNode.this));
            portscanItem.setOnAction(a -> {/*...*/});
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
            case ROUTER:              return "internet.png";
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

    public NetworkType getNetworkType()         { return networkType; }
    public void setNetworkType(NetworkType nt)  { this.networkType = nt; }

    public boolean isMainNode()                 { return mainNode; }
    public void setMainNode(boolean m)          { this.mainNode = m; }

    public String getRouteSwitch()              { return routeSwitch; }
    public void setRouteSwitch(String rs)       { this.routeSwitch = rs; }

    public ConnectionType getConnectionType()   { return connectionType; }
    public void setConnectionType(ConnectionType ct) { this.connectionType = ct; }

    public String getResolvedIp()               { return resolvedIp; }

    public boolean isConnected()                { return connected; }
    public void setConnected(boolean c)         { this.connected = c; }

    public long getStartTime()                  { return startTime; }

        private String macAddress;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    /** Update all properties from another node */
    public void updateFrom(NetworkNode updated) {
        setIpOrHostname(updated.getIpOrHostname());
        setDisplayName(updated.getDisplayName());
        setDeviceType(updated.getDeviceType());
        setNetworkType(updated.getNetworkType());
        if (!isMainNode()) setConnectionType(updated.getConnectionType());
        setRouteSwitch(updated.getRouteSwitch());
    }

    /** No-op: fixed size square
     *  but retained for interface consistency */
    public void updateLayoutForSavedSize() {}
}
