package org.example;

import java.net.InetAddress;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class NetworkNode extends Pane {
    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private NetworkType networkType;
    private boolean mainNode = false;
    private ObjectProperty<Color> outlineColorProperty = new SimpleObjectProperty<>(Color.web("#3B3B3B"));
    private String routeSwitch = ""; // default value

    // Field for connection type; default to Ethernet.
    private ConnectionType connectionType = ConnectionType.ETHERNET;
    
    // Field to store the resolved IP address (if applicable)
    private String resolvedIp = null;
    
    // New field: connection status. It will be updated periodically.
    private boolean connected = false;
    
    // Visual components.
    private Rectangle background;
    private ImageView iconView;
    private HBox nameContainer;  // Container for the display name label (for centering)
    private Label nameLabel;
    private Label ipLabel; // still created but not added
    // Resize icon for resizing the node.
    private ImageView resizeIcon;
    
    // For dragging.
    private double dragDeltaX;
    private double dragDeltaY;
    
    // For resizing.
    private double resizeStartWidth;
    private double resizeStartHeight;
    private double resizeStartX;
    private double resizeStartY;
    // Save the node’s original layoutX and layoutY at the start of resizing.
    private double resizeStartLayoutX;
    private double resizeStartLayoutY;
    
    // Base dimensions.
    private final double BASE_SIZE = 100; // base height remains 100
    private final double MIN_SIZE = BASE_SIZE / 2;  // 50
    private final double MAX_SIZE = BASE_SIZE * 2;    // 200
    // Extra padding added to the measured text width.
    private final double NAME_PADDING = 20;
    
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

    public String getRouteSwitch() {
        return routeSwitch;
    }
    
    public void setRouteSwitch(String routeSwitch) {
        this.routeSwitch = routeSwitch;
    }
    
    private void initialize() {
    // Measure the display name's width.
    Text textMeasure = new Text(displayName);
    textMeasure.setFont(Font.font(14));
    double textWidth = textMeasure.getLayoutBounds().getWidth();
    double newWidth = Math.max(BASE_SIZE, textWidth + NAME_PADDING);
    
    // Set preferred size using newWidth and BASE_SIZE for height.
    setPrefSize(newWidth, BASE_SIZE);
    
    // Background rectangle.
    background = new Rectangle(newWidth, BASE_SIZE);
    background.getStyleClass().add("node-background");
    outlineColorProperty.addListener((obs, oldVal, newVal) -> background.setStroke(newVal));
    background.setStroke(outlineColorProperty.get());
    
    // Device icon.
    iconView = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + getIconFileName())));
    iconView.setFitWidth(60);
    iconView.setFitHeight(60);
    iconView.setLayoutX((newWidth - iconView.getFitWidth()) / 2);
    iconView.setLayoutY(10);
    
    // Display name label.
    nameLabel = new Label(displayName);
    nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: 600;");
    nameContainer = new HBox(nameLabel);
    nameContainer.setAlignment(Pos.CENTER);
    nameContainer.setPrefWidth(newWidth);
    nameContainer.setLayoutY(10 + iconView.getFitHeight() + 5);
    
    // IP label (not added to visible layout)
    ipLabel = new Label(ipOrHostname);
    ipLabel.getStyleClass().add("node-ip-label");
    ipLabel.setLayoutX(5);
    ipLabel.setLayoutY(80);
    
    // Add components.
    getChildren().add(background);
    getChildren().addAll(iconView, nameContainer);
    
    // --- Resize Hit Area and Icon Setup (Top-Right Corner) --- 
    resizeIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/resize.png")));
    resizeIcon.setFitWidth(8);
    resizeIcon.setFitHeight(8);
    resizeIcon.setMouseTransparent(true);
    resizeIcon.layoutXProperty().bind(widthProperty().subtract(12));
    resizeIcon.setLayoutY(6);
    
    Region resizeHitArea = new Region();
    resizeHitArea.setPrefSize(30, 30);
    resizeHitArea.setStyle("-fx-background-color: transparent;");
    resizeHitArea.setCursor(Cursor.NE_RESIZE);
    resizeHitArea.layoutXProperty().bind(widthProperty().subtract(32));
    resizeHitArea.setLayoutY(6);
    
    getChildren().addAll(resizeHitArea, resizeIcon);
    
    // Attach resize event handlers.
    resizeHitArea.setOnMousePressed((MouseEvent e) -> {
        e.consume();
        resizeStartWidth = getPrefWidth();
        resizeStartHeight = getPrefHeight();
        resizeStartX = e.getSceneX();
        resizeStartY = e.getSceneY();
        resizeStartLayoutX = getLayoutX();
        resizeStartLayoutY = getLayoutY();
    });
    
    resizeHitArea.setOnMouseDragged((MouseEvent e) -> {
        e.consume();
        double dx = e.getSceneX() - resizeStartX;
        double newSize = resizeStartWidth + dx;
        newSize = Math.round(newSize / 10) * 10;
        newSize = Math.max(MIN_SIZE, Math.min(MAX_SIZE, newSize));
        setLayoutY(resizeStartLayoutY + resizeStartHeight - newSize);
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
        nameContainer.setPrefWidth(newSize);
        nameContainer.setLayoutY(10 * scale + iconView.getFitHeight() + 5 * scale);
        nameLabel.setStyle("-fx-font-size: " + (16 * scale) + "px; -fx-text-fill: white;");
    });
    
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
    
    // --- New: Context Menu for Right-Click ---
    setOnContextMenuRequested(e -> {
        ContextMenu cm = new ContextMenu();
        MenuItem tracerouteItem = new MenuItem("Traceroute");
        MenuItem portscanItem = new MenuItem("Portscan");
        
        // Apply custom styling so it matches our application's theme.
        String menuItemStyle = "-fx-text-fill: white; ";
        tracerouteItem.setStyle(menuItemStyle);
        portscanItem.setStyle(menuItemStyle);
        
        // Set placeholder actions.
        tracerouteItem.setOnAction(event -> {
            NetworkMonitorApp.performTraceroute(NetworkNode.this);
        });
        
        portscanItem.setOnAction(event -> {
            StackPane rootStack = (StackPane) ((BorderPane) getScene().getRoot()).getCenter();
            rootStack.getChildren().removeIf(n -> n instanceof PortscanConfigPanel);
            // Use NetworkNode.this to ensure the correct instance is passed.
            PortscanConfigPanel configPanel = new PortscanConfigPanel(NetworkNode.this);
            // Set alignment and margin to ensure 15px spacing from the parent's edges.
            StackPane.setAlignment(configPanel, Pos.TOP_LEFT);
            StackPane.setMargin(configPanel, new Insets(10));
            rootStack.getChildren().add(configPanel);
        });
        

        
        cm.getItems().addAll(tracerouteItem, portscanItem);
        // Set the context menu's overall style.
        cm.setStyle("-fx-background-color:rgb(39, 51, 77); -fx-text-fill: white; -fx-background-radius: 5px;");
        
        cm.show(this, e.getScreenX(), e.getScreenY());
        e.consume(); 
    });
}

    
    private String getIconFileName() {
        switch (deviceType) {
            case COMPUTER: return "host.png";
            case SWITCH: return "switch.png";
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
    }
    
    public void setDisplayName(String name) {
        this.displayName = name;
        nameLabel.setText(name);
    }
    
    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        iconView.setImage(new Image(getClass().getResourceAsStream("/icons/" + getIconFileName())));
    }
    
    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
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
    }
    
    public String getResolvedIp() {
        return resolvedIp;
    }
    
    // New connection status getters/setters.
    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
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
    }
    
    /**
     * Recalculates and updates the layout of internal components (background, icon, and name label)
     * based on the current preferred size.
     */
    private void updateLayoutComponents() {
        double newSize = getPrefWidth(); // Assuming the node is square.
        background.setWidth(newSize);
        background.setHeight(newSize);
        double scale = newSize / BASE_SIZE;
        iconView.setFitWidth(56 * scale);
        iconView.setFitHeight(56 * scale);
        iconView.setLayoutX((newSize - iconView.getFitWidth()) / 2);
        iconView.setLayoutY(10 * scale);
        nameContainer.setPrefWidth(newSize);
        nameContainer.setLayoutY(10 * scale + iconView.getFitHeight() + 5 * scale);
        nameLabel.setStyle("-fx-font-size: " + (16 * scale) + "px; -fx-text-fill: white;");
    }
    
    /**
     * Public method to trigger an update of the internal layout components.
     * This should be called after setting a new preferred size (for example, when reloading from file).
     */
    public void updateLayoutForSavedSize() {
        updateLayoutComponents();
    }
}
