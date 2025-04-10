package org.example;

import java.net.InetAddress;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class NetworkNode extends Pane {
    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private NetworkType networkType;
    private boolean mainNode = false;
    private ObjectProperty<Color> outlineColorProperty = new SimpleObjectProperty<>(Color.web("#3B3B3B"));

    // Field for connection type; default to Ethernet.
    private ConnectionType connectionType = ConnectionType.ETHERNET;
    
    // Field to store the resolved IP address (if applicable)
    private String resolvedIp = null;
    
    // Visual components.
    private Rectangle background;
    private ImageView iconView;
    private HBox nameContainer;  // Container for the display name label (for centering)
    private Label nameLabel;
    private Label ipLabel; // still created but not added
    // Connection icon has been removed.
    // Resize icon (visible handle) for resizing the node.
    private ImageView resizeIcon;
    
    // For dragging.
    private double dragDeltaX;
    private double dragDeltaY;
    
    // For resizing.
    private double resizeStartWidth;
    private double resizeStartHeight;
    private double resizeStartX;
    private double resizeStartY;
    
    // Base dimensions.
    private final double BASE_SIZE = 100;
    private final double MIN_SIZE = BASE_SIZE / 2;  // 50
    private final double MAX_SIZE = BASE_SIZE * 2;    // 200
    
    // For uptime.
    private long startTime;
    
    public NetworkNode(String ipOrHostname, String displayName, DeviceType deviceType, NetworkType networkType) {
        this.ipOrHostname = ipOrHostname;
        this.displayName = displayName;
        this.deviceType = deviceType;
        this.networkType = networkType;
        this.startTime = System.currentTimeMillis();
        initialize();
    }
    
    private void initialize() {
        setPrefSize(BASE_SIZE, BASE_SIZE);
        // Apply the container style via CSS.
        getStyleClass().add("node-container");
        
        // Background rectangle.
        background = new Rectangle(BASE_SIZE, BASE_SIZE);
        background.getStyleClass().add("node-background");
        outlineColorProperty.addListener((obs, oldVal, newVal) -> background.setStroke(newVal));
        background.setStroke(outlineColorProperty.get());
        
        // Device icon: Increase the size from 48 to 56.
        iconView = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + getIconFileName())));
        iconView.setFitWidth(60);
        iconView.setFitHeight(60);
        iconView.setLayoutX((BASE_SIZE - iconView.getFitWidth()) / 2);
        iconView.setLayoutY(10);
        
        // Display name label, with increased text size.
        nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: 600;");
        // Instead of manually setting layoutX, we wrap the label in an HBox.
        nameContainer = new HBox(nameLabel);
        nameContainer.setAlignment(Pos.CENTER);
        nameContainer.setPrefWidth(BASE_SIZE);
        // Position the name container below the icon.
        // For instance, set its Y so that it appears below the icon:
        nameContainer.setLayoutY(10 + iconView.getFitHeight() + 5);
        // No need to set layoutX (0 means it will start at the left but the HBox centers its content).
        
        // IP label is still used by the node details but not added to the node.
        ipLabel = new Label(ipOrHostname);
        ipLabel.getStyleClass().add("node-ip-label");
        ipLabel.setLayoutX(5);
        ipLabel.setLayoutY(80);
        
        // Add background, the device icon, and the name container.
        getChildren().add(background);
        getChildren().addAll(iconView, nameContainer);
        // The ipLabel and connection icon are not added.
        
        // --- Resize Hit Area and Icon Setup ---
        resizeIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/resize.png")));
        resizeIcon.setFitWidth(8);
        resizeIcon.setFitHeight(8);
        resizeIcon.setMouseTransparent(true);
        resizeIcon.layoutXProperty().bind(widthProperty().subtract(12));
        resizeIcon.layoutYProperty().bind(heightProperty().subtract(12));
        Region resizeHitArea = new Region();
        resizeHitArea.setPrefSize(30, 30);
        resizeHitArea.setStyle("-fx-background-color: transparent;");
        resizeHitArea.setCursor(Cursor.SE_RESIZE);
        resizeHitArea.layoutXProperty().bind(widthProperty().subtract(32));
        resizeHitArea.layoutYProperty().bind(heightProperty().subtract(32));
        getChildren().addAll(resizeHitArea, resizeIcon);
        resizeHitArea.setOnMousePressed((MouseEvent e) -> {
            e.consume();
            resizeStartWidth = getPrefWidth();
            resizeStartHeight = getPrefHeight();
            resizeStartX = e.getSceneX();
            resizeStartY = e.getSceneY();
        });
        resizeHitArea.setOnMouseDragged((MouseEvent e) -> {
            e.consume();
            double dx = e.getSceneX() - resizeStartX;
            double newSize = resizeStartWidth + dx;
            newSize = Math.round(newSize / 10) * 10;
            newSize = Math.max(MIN_SIZE, Math.min(MAX_SIZE, newSize));
            setPrefSize(newSize, newSize);
            setMinSize(newSize, newSize);
            setMaxSize(newSize, newSize);
            background.setWidth(newSize);
            background.setHeight(newSize);
            double scale = newSize / BASE_SIZE;
            iconView.setFitWidth(56 * scale);
            iconView.setFitHeight(56 * scale);
            iconView.setLayoutX((newSize - iconView.getFitWidth()) / 2);
            iconView.setLayoutY(10 * scale);
            // Re-center the name container.
            nameContainer.setPrefWidth(newSize);
            nameContainer.setLayoutY(10 * scale + iconView.getFitHeight() + 5 * scale);
            // Update font size based on scale.
            nameLabel.setStyle("-fx-font-size: " + (16 * scale) + "px; -fx-text-fill: white;");
        });
        // --- End Resize Hit Area and Icon Setup ---
        
        // Dragging node.
        setOnMousePressed(e -> {
            if (!e.getTarget().equals(resizeIcon)) {
                dragDeltaX = getLayoutX() - e.getSceneX();
                dragDeltaY = getLayoutY() - e.getSceneY();
            }
        });
        setOnMouseDragged(e -> {
            if (!e.getTarget().equals(resizeIcon)) {
                setLayoutX(e.getSceneX() + dragDeltaX);
                setLayoutY(e.getSceneY() + dragDeltaY);
            }
        });
        setOnMouseReleased(e -> {
            setLayoutX(Math.round(getLayoutX() / 10) * 10);
            setLayoutY(Math.round(getLayoutY() / 10) * 10);
        });
        
        // Hover effects.
        setOnMouseEntered(e -> background.setFill(Color.web("#2c384a")));
        setOnMouseExited(e -> background.setFill(Color.web("#182030")));
        
        // Resolve hostname if necessary.
        if (!ipOrHostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            new Thread(() -> {
                try {
                    InetAddress addr = InetAddress.getByName(ipOrHostname);
                    String resolved = addr.getHostAddress();
                    resolvedIp = resolved;
                    Platform.runLater(() -> ipLabel.setText(resolved));
                } catch (Exception e) {
                    // If resolution fails, leave the original text.
                }
            }).start();
        }
    }
    
    private String getIconFileName() {
        switch (deviceType) {
            case COMPUTER: return "host.png";
            case LAPTOP: return "laptop.png";
            case SERVER: return "server.png";
            case ROUTER: return "internet.png";
            case GATEWAY: return "gateway.png";
            case PHONE: return "phone.png";
            case TV: return "tv.png";
            case SECURITY_CAMERA: return "security_camera.png";
            case VIRTUAL_MACHINE: return "virtual_machine.png";
            default: return "host.png";
        }
    }

    public long getStartTime() {
        return startTime;
    }
    
    public void setIpOrHostname(String ip) {
        this.ipOrHostname = ip;
        // Not updating ipLabel since it isn’t shown on the node.
    }
    
    public void setDisplayName(String name) {
        this.displayName = name;
        nameLabel.setText(name);
        // The nameContainer will center the label automatically.
    }
    
    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        iconView.setImage(new Image(getClass().getResourceAsStream("/icons/" + getIconFileName())));
    }
    
    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
        // No connection icon is used.
    }
    
    public String getIpOrHostname() {
        return ipOrHostname;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public DeviceType getDeviceType() {
        return deviceType;
    }
    
    public NetworkType getNetworkType() {
        return networkType;
    }
    
    public boolean isMainNode() {
        return mainNode;
    }
    
    public void setMainNode(boolean mainNode) {
        this.mainNode = mainNode;
        // If main node, nothing additional to do (no connection icon).
    }
    
    public String getOutlineColor() {
        Color c = outlineColorProperty.get();
        return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
    
    public void setOutlineColor(String colorHex) {
        outlineColorProperty.set(Color.web(colorHex));
    }
    
    public ConnectionType getConnectionType() {
        return connectionType;
    }
    
    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
        // No connection icon updated.
    }
    
    public String getResolvedIp() {
        return resolvedIp;
    }
    
    /**
     * Updates this node's properties from another instance.
     * For main nodes, the color and connection type remain unchanged.
     */
    public void updateFrom(NetworkNode updated) {
        this.ipOrHostname = updated.getIpOrHostname();
        this.displayName = updated.getDisplayName();
        this.deviceType = updated.getDeviceType();
        this.networkType = updated.getNetworkType();
        if (!this.isMainNode()) {
            setOutlineColor(updated.getOutlineColor());
            this.connectionType = updated.getConnectionType();
        }
        iconView.setImage(new Image(getClass().getResourceAsStream("/icons/" + getIconFileName())));
        nameLabel.setText(displayName);
        // The name container keeps the label centered.
    }
}
