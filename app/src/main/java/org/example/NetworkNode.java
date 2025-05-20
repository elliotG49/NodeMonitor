package org.example;

import java.net.InetAddress;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;
/**
 * Represents a network node on the spider map.
 * Displays as a circular icon with a glow and label below.
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
    private final Circle circle;
    private final ImageView iconView;
    private final Label nameLabel;

    // Dragging support
    private double dragDeltaX;
    private double dragDeltaY;

    // Circle radius
    private static final double RADIUS = 40;

    public NetworkNode(String ipOrHostname, String displayName,
                       DeviceType deviceType, NetworkType networkType) {
        this.ipOrHostname = ipOrHostname;
        this.displayName  = displayName;
        this.deviceType   = deviceType;
        this.networkType  = networkType;
        this.startTime    = System.currentTimeMillis();

        // --- Circle background ---
        circle = new Circle(RADIUS);
        circle.getStyleClass().add("node-circle");
        getChildren().add(circle);

        // --- Icon inside circle ---
        iconView = new ImageView(new Image(
            getClass().getResourceAsStream("/icons/" + getIconFileName())
        ));
        iconView.setFitWidth(50);
        iconView.setFitHeight(50);
        getChildren().add(iconView);
        setAlignment(iconView, Pos.CENTER);

                ScaleTransition hoverScale = new ScaleTransition(Duration.millis(200), this);
        setOnMouseEntered(e -> {
            hoverScale.stop();
            hoverScale.setToX(1.05);
            hoverScale.setToY(1.05);
            hoverScale.playFromStart();
            // override CSS effect to use hovered glow variable
            circle.setStyle(
                "-fx-effect: dropshadow(gaussian, -node-glow-hovered-color, 9, 0.5, 0, 0);"
            );
        });
        setOnMouseExited(e -> {
            hoverScale.stop();
            hoverScale.setToX(1.0);
            hoverScale.setToY(1.0);
            hoverScale.playFromStart();
            // clear inline style to revert to default glow
            circle.setStyle("");
        });

        // --- Label beneath ---
        nameLabel = new Label(displayName);
        nameLabel.setFont(Font.font(16));
        nameLabel.setTextFill(Color.WHITE);
        getChildren().add(nameLabel);
        setAlignment(nameLabel, Pos.BOTTOM_CENTER);
        nameLabel.setTranslateY(20);

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
            case SWITCH:              return "switch.png";
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

    /** Update all properties from another node */
    public void updateFrom(NetworkNode updated) {
        setIpOrHostname(updated.getIpOrHostname());
        setDisplayName(updated.getDisplayName());
        setDeviceType(updated.getDeviceType());
        setNetworkType(updated.getNetworkType());
        if (!isMainNode()) setConnectionType(updated.getConnectionType());
        setRouteSwitch(updated.getRouteSwitch());
    }

    /** No-op: fixed size circle
     *  but retained for interface consistency */
    public void updateLayoutForSavedSize() {}
}
