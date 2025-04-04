package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Enumeration;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // no longer used in advanced overlay
import javafx.scene.image.Image;      // no longer used in advanced overlay
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class NetworkNode extends Pane {
    private String ipOrHostname;
    private String displayName;
    private DeviceType deviceType;
    private NetworkType networkType;
    private boolean mainNode = false;
    private ObjectProperty<Color> outlineColorProperty = new SimpleObjectProperty<>(Color.web("#555"));
    
    // Field for connection type; default to Ethernet.
    private ConnectionType connectionType = ConnectionType.ETHERNET;
    
    // Field to store the resolved IP address (if applicable)
    private String resolvedIp = null;
    
    // Visual components.
    private Rectangle background;
    private ImageView iconView;
    private Label nameLabel;
    private Label ipLabel;
    // Connection type icon – will only be added for internal, non‑main nodes.
    private ImageView connectionIcon;
    // Resize icon (visible handle) for resizing the node.
    private ImageView resizeIcon;
    // Advanced options overlay icon container.
    private StackPane advButtonContainer;
    
    // Advanced overlay.
    private AdvancedOptionsPane advancedOptionsPane;
    
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
        getStyleClass().add("node-container");
        
        // Background rectangle.
        background = new Rectangle(BASE_SIZE, BASE_SIZE);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setFill(Color.web("#13213F"));
        background.setStroke(outlineColorProperty.get());
        outlineColorProperty.addListener((obs, oldVal, newVal) -> background.setStroke(newVal));
        background.setStrokeWidth(2);
        
        // Device icon.
        iconView = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + getIconFileName())));
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        iconView.setLayoutX((BASE_SIZE - iconView.getFitWidth()) / 2);
        iconView.setLayoutY(10);
        
        // Labels.
        nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("node-label");
        nameLabel.setLayoutX(5);
        nameLabel.setLayoutY(65);
        
        ipLabel = new Label(ipOrHostname);
        ipLabel.getStyleClass().add("node-ip-label");
        ipLabel.setLayoutX(5);
        ipLabel.setLayoutY(80);
        
        // Connection icon.
        connectionIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + getConnectionIconFileName())));
        connectionIcon.setFitWidth(14);
        connectionIcon.setFitHeight(14);
        connectionIcon.setLayoutX(BASE_SIZE - connectionIcon.getFitWidth() - 5);
        connectionIcon.setLayoutY(5);
        
        // Add background and components.
        getChildren().add(background);
        getChildren().addAll(iconView, nameLabel, ipLabel);
        if (networkType == NetworkType.INTERNAL && !mainNode) {
            getChildren().add(connectionIcon);
        }
        
        // --- Advanced Options Icon Container ---
        advButtonContainer = new StackPane();
        advButtonContainer.setPrefSize(16, 16);
        advButtonContainer.setMaxSize(16, 16);
        advButtonContainer.setStyle("-fx-background-color: #080E1B; -fx-background-radius: 8; -fx-border-color: white; -fx-border-width: 1;");
        Label infoLabel = new Label("i");
        infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        advButtonContainer.getChildren().add(infoLabel);
        // Position so that its center is at the very top left (half outside).
        advButtonContainer.setLayoutX(-advButtonContainer.getPrefWidth() / 2);
        advButtonContainer.setLayoutY(-advButtonContainer.getPrefHeight() / 2);
        advButtonContainer.setCursor(Cursor.HAND);
        advButtonContainer.setOnMouseClicked(e -> {
            if (advancedOptionsPane == null) {
                advancedOptionsPane = new AdvancedOptionsPane();
                getChildren().add(advancedOptionsPane);
            }
            advancedOptionsPane.setVisible(!advancedOptionsPane.isVisible());
        });
        getChildren().add(advButtonContainer);
        // --- End Advanced Options Icon Container ---
        
        // --- Resize Hit Area and Icon Setup ---
        resizeIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/resize.png")));
        resizeIcon.setFitWidth(10);
        resizeIcon.setFitHeight(10);
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
            iconView.setFitWidth(48 * scale);
            iconView.setFitHeight(48 * scale);
            iconView.setLayoutX((newSize - iconView.getFitWidth()) / 2);
            iconView.setLayoutY(10 * scale);
            nameLabel.setLayoutX(5 * scale);
            nameLabel.setLayoutY(65 * scale);
            nameLabel.setStyle("-fx-font-size: " + (12 * scale) + "px;");
            ipLabel.setLayoutX(5 * scale);
            ipLabel.setLayoutY(80 * scale);
            ipLabel.setStyle("-fx-font-size: " + (10 * scale) + "px;");
            if (networkType == NetworkType.INTERNAL && !mainNode && getChildren().contains(connectionIcon)) {
                connectionIcon.setFitWidth(14 * scale);
                connectionIcon.setFitHeight(14 * scale);
                connectionIcon.setLayoutX(newSize - connectionIcon.getFitWidth() - 5 * scale);
                connectionIcon.setLayoutY(5 * scale);
            }
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
        setOnMouseEntered(e -> background.setFill(Color.web("#1A2B57")));
        setOnMouseExited(e -> background.setFill(Color.web("#13213F")));
        
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
    
    private String getConnectionIconFileName() {
        switch (connectionType) {
            case ETHERNET: return "ethernet.png";
            case WIRELESS: return "wireless.png";
            default: return "ethernet.png";
        }
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
        if (mainNode) {
            getChildren().remove(connectionIcon);
        } else if (networkType == NetworkType.INTERNAL) {
            if (!getChildren().contains(connectionIcon)) {
                getChildren().add(connectionIcon);
                double scale = getPrefWidth() / BASE_SIZE;
                connectionIcon.setFitWidth(14 * scale);
                connectionIcon.setFitHeight(14 * scale);
                connectionIcon.setLayoutX(getPrefWidth() - connectionIcon.getFitWidth() - 5 * scale);
                connectionIcon.setLayoutY(5 * scale);
            }
        }
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
        if (networkType == NetworkType.INTERNAL && !mainNode) {
            if (connectionIcon != null) {
                connectionIcon.setImage(new Image(getClass().getResourceAsStream("/icons/" + getConnectionIconFileName())));
                double scale = getPrefWidth() / BASE_SIZE;
                connectionIcon.setFitWidth(14 * scale);
                connectionIcon.setFitHeight(14 * scale);
                connectionIcon.setLayoutX(getPrefWidth() - connectionIcon.getFitWidth() - 5 * scale);
                connectionIcon.setLayoutY(5 * scale);
                if (!getChildren().contains(connectionIcon)) {
                    getChildren().add(connectionIcon);
                }
            }
        } else {
            getChildren().remove(connectionIcon);
        }
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
        ipLabel.setText(ipOrHostname);
        if (!ipOrHostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            new Thread(() -> {
                try {
                    InetAddress addr = InetAddress.getByName(ipOrHostname);
                    String resolved = addr.getHostAddress();
                    resolvedIp = resolved;
                    Platform.runLater(() -> ipLabel.setText(resolved));
                } catch (Exception e) {
                    // If resolution fails, do nothing.
                }
            }).start();
        }
        if (networkType == NetworkType.INTERNAL && !isMainNode()) {
            if (!getChildren().contains(connectionIcon)) {
                getChildren().add(connectionIcon);
            }
            connectionIcon.setImage(new Image(getClass().getResourceAsStream("/icons/" + getConnectionIconFileName())));
            double scale = getPrefWidth() / BASE_SIZE;
            connectionIcon.setFitWidth(14 * scale);
            connectionIcon.setFitHeight(14 * scale);
            connectionIcon.setLayoutX(getPrefWidth() - connectionIcon.getFitWidth() - 5 * scale);
            connectionIcon.setLayoutY(5 * scale);
        } else {
            getChildren().remove(connectionIcon);
        }
    }
    
    // --- Advanced Options Overlay ---
    private class AdvancedOptionsPane extends Pane {
        // For non-host nodes.
        private Label uptimeLabel;
        private Label macLabel;
        private Label hopsLabel;
        // For host nodes.
        private Label hostInfoLabel;
        private Button closeButton;
        private Timeline updateTimeline;
        
        public AdvancedOptionsPane() {
            boolean isHost = ipOrHostname.equals("127.0.0.1") || (resolvedIp != null && resolvedIp.equals("127.0.0.1"));
            if (isHost) {
                double advWidth = getPrefWidth() * 0.6;
                double advHeight = getPrefHeight() * 0.6;
                setPrefSize(advWidth, advHeight);
                setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 5;");
                // Position above the node (aligned to left).
                setLayoutX(0);
                setLayoutY(-advHeight - 5);
                
                hostInfoLabel = new Label("Loading host info...");
                hostInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                VBox vbox = new VBox(5, hostInfoLabel, createCloseButton());
                vbox.setAlignment(Pos.CENTER);
                vbox.setPadding(new Insets(5));
                getChildren().add(vbox);
                
                updateHostInfo();
            } else {
                double advWidth = getPrefWidth() * 0.6;
                double advHeight = getPrefHeight() * 0.6;
                setPrefSize(advWidth, advHeight);
                setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 5;");
                // Position above the node.
                setLayoutX(0);
                setLayoutY(-advHeight - 5);
                
                uptimeLabel = new Label("Uptime: N/A");
                uptimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                VBox vbox;
                if (networkType == NetworkType.INTERNAL) {
                    macLabel = new Label("MAC: N/A");
                    macLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                    hopsLabel = new Label("Hops: N/A");
                    hopsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                    vbox = new VBox(5, uptimeLabel, macLabel, hopsLabel, createCloseButton());
                } else { // External node.
                    hopsLabel = new Label("Hops: N/A");
                    hopsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                    vbox = new VBox(5, uptimeLabel, hopsLabel, createCloseButton());
                }
                vbox.setAlignment(Pos.CENTER);
                vbox.setPadding(new Insets(5));
                getChildren().add(vbox);
                
                updateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateMetrics()));
                updateTimeline.setCycleCount(Timeline.INDEFINITE);
                updateTimeline.play();
                
                updateHops();
                if (networkType == NetworkType.INTERNAL) {
                    updateMacAddress();
                }
            }
        }
        
        private Button createCloseButton() {
            closeButton = new Button("Close");
            closeButton.setOnAction(e -> setVisible(false));
            return closeButton;
        }
        
        private void updateMetrics() {
            long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
            Platform.runLater(() -> uptimeLabel.setText("Uptime: " + uptimeSeconds + " s"));
        }
        
        private void updateMacAddress() {
            new Thread(() -> {
                try {
                    String targetIP = ipOrHostname;
                    if (!ipOrHostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && resolvedIp != null) {
                        targetIP = resolvedIp;
                    }
                    ProcessBuilder pb = new ProcessBuilder("arp", "-a");
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    String foundMac = "N/A";
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(targetIP)) {
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 2) {
                                foundMac = parts[1];
                            }
                            break;
                        }
                    }
                    reader.close();
                    final String mac = foundMac;
                    Platform.runLater(() -> macLabel.setText("MAC: " + mac));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> macLabel.setText("MAC: Error"));
                }
            }).start();
        }
        
        private void updateHops() {
            new Thread(() -> {
                try {
                    String targetIP = ipOrHostname;
                    if (!ipOrHostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && resolvedIp != null) {
                        targetIP = resolvedIp;
                    }
                    ProcessBuilder pb = new ProcessBuilder("tracert", "-d", targetIP);
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    int hops = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.matches("^\\d+\\s+.*")) {
                            hops++;
                        }
                    }
                    reader.close();
                    final int finalHops = hops;
                    Platform.runLater(() -> hopsLabel.setText("Hops: " + finalHops));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> hopsLabel.setText("Hops: Error"));
                }
            }).start();
        }
        
        private void updateHostInfo() {
            new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                try {
                    for (Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                        java.net.NetworkInterface intf = en.nextElement();
                        sb.append("Adapter: ").append(intf.getDisplayName()).append("\n");
                        byte[] mac = intf.getHardwareAddress();
                        if (mac != null) {
                            for (int i = 0; i < mac.length; i++) {
                                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                            }
                            sb.append("\n");
                        }
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            sb.append("IP: ").append(inetAddress.getHostAddress()).append("\n");
                        }
                        sb.append("\n");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sb.append("Error retrieving host info");
                }
                final String info = sb.toString();
                Platform.runLater(() -> hostInfoLabel.setText(info));
            }).start();
        }
    }
    // --- End Advanced Options Overlay ---
}
