package org.example;

import java.util.HashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class FilterBox extends StackPane {

    private final double FIELD_WIDTH = 200;
    private final double WIDTH = 150;
    private final double MIN_HEIGHT = 50;
    // Expanded height increased to 550px.
    private final double EXPANDED_HEIGHT = 500;
    private final double EXPANDED_WIDTH = 250;
    private boolean expanded = false;
    
    // Define a final variable for padding so it's easily changeable.
    private final double FILTER_BOX_PADDING = 10;

    // Styles to match NewNodeBox.
    private final String normalStyle = "-fx-background-color: #182030; " +
                                         "-fx-border-color: #3B3B3B; " +
                                         "-fx-border-width: 1px; " +
                                         "-fx-border-radius: 10px; " +
                                         "-fx-background-radius: 10px; " +
                                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0.5, 0, 0);";

    private final String hoverStyle = "-fx-background-color: #2c384a; " +
                                        "-fx-border-color: #3B3B3B; " +
                                        "-fx-border-width: 1px; " +
                                        "-fx-border-radius: 10px; " +
                                        "-fx-background-radius: 10px; " +
                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0.5, 0, 0);";

    // Collapsed state label.
    private Label minimizedLabel;

    // Expanded state content.
    private VBox contentBox;

    // Filtering controls.
    // Subnet dropdown instead of text box.
    private ComboBox<String> subnetComboBox;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ComboBox<String> connectionStatusBox;
    private ColorPicker nodeColorPicker;
    private Button applyFilterButton;
    private Button resetFilterButton;

    public FilterBox() {
        setPrefWidth(WIDTH);
        setPrefHeight(MIN_HEIGHT);
        setMinWidth(WIDTH);
        setStyle(normalStyle);

        // Hover behavior.
        setOnMouseEntered(e -> {
            if (!expanded) {
                setStyle(hoverStyle);
            }
        });
        setOnMouseExited(e -> {
            if (!expanded) {
                setStyle(normalStyle);
            }
        });

        // Collapsed view.
        minimizedLabel = new Label("Filter Node");
        minimizedLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // Build expanded view.
        contentBox = new VBox(15);
        // Use the final variable for padding.
        contentBox.setPadding(new Insets(FILTER_BOX_PADDING));
        contentBox.setAlignment(Pos.TOP_CENTER);
        
        // Header section.
        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Filter Node");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        // Apply a bottom border of 0.5px with color #b8d4f1 and padding.
        headerBox.setStyle("-fx-border-color: transparent transparent #b8d4f1 transparent; -fx-border-width: 0 0 0.5px 0; -fx-padding: 0 0 10px 0;");
        headerBox.getChildren().add(titleLabel);
        
        // Subnet Section.
        VBox subnetSection = new VBox(10);
        Label subnetLabel = new Label("Subnet:");
        subnetLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        subnetComboBox = new ComboBox<>();
        subnetComboBox.setPrefWidth(FIELD_WIDTH);
        subnetComboBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        subnetSection.getChildren().addAll(subnetLabel, subnetComboBox);
        
        // Device Type Section.
        VBox deviceSection = new VBox(10);
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setValue(null); // 'Any'
        deviceTypeBox.setPromptText("Any");
        deviceTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                               "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                               "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        deviceSection.getChildren().addAll(deviceLabel, deviceTypeBox);
        
        // Connection Type Section.
        VBox connectionSection = new VBox(10);
        Label connectionLabel = new Label("Connection Type:");
        connectionLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(null);
        connectionTypeBox.setPromptText("Any");
        connectionTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                   "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                   "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        connectionSection.getChildren().addAll(connectionLabel, connectionTypeBox);
        
        // Connection Status Section.
        VBox connectionStatusSection = new VBox(10);
        Label connectionStatusLabel = new Label("Connection Status:");
        connectionStatusLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        connectionStatusBox = new ComboBox<>();
        connectionStatusBox.setPrefWidth(FIELD_WIDTH);
        connectionStatusBox.getItems().addAll("Any", "Connected", "Disconnected");
        connectionStatusBox.setValue("Any");
        connectionStatusBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                      "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                      "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        connectionStatusSection.getChildren().addAll(connectionStatusLabel, connectionStatusBox);
        
        // Node Colour Section.
        VBox colorSection = new VBox(10);
        Label colorLabel = new Label("Node Colour:");
        colorLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        nodeColorPicker = new ColorPicker();
        nodeColorPicker.setPrefWidth(FIELD_WIDTH);
        colorSection.getChildren().addAll(colorLabel, nodeColorPicker);
        
        // Buttons Section using an HBox (stacked horizontally).
        HBox buttonSection = new HBox(10);
        buttonSection.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonSection, new Insets(15, 0, 0, 0));
        applyFilterButton = new Button("Apply Filter");
        applyFilterButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        resetFilterButton = new Button("Reset Filter");
        resetFilterButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 14px;");
        applyFilterButton.setOnAction(e -> {
            applyFilters();
            NetworkMonitorApp.updateConnectionLinesVisibility();
        });
        resetFilterButton.setOnAction(e -> {
            resetFilters();
            applyFilters();
            NetworkMonitorApp.updateConnectionLinesVisibility();
        });
        buttonSection.getChildren().addAll(applyFilterButton, resetFilterButton);
        
        // Assemble all controls into contentBox.
        contentBox.getChildren().addAll(headerBox, subnetSection, deviceSection, connectionSection, connectionStatusSection, colorSection, buttonSection);
        
        // Anchor the bottom.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() != null) {
                layoutYProperty().bind(((Region)getParent()).heightProperty()
                        .subtract(prefHeightProperty())
                        .subtract(15));
            }
        });
        
        // Automatically update subnet options.
        updateSubnetOptions();
    }
    
    // Automatically gather unique /24 subnets from all nodes (ignoring 127.0.0.1).
    private void updateSubnetOptions() {
        Set<String> subnets = new HashSet<>();
        for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
            String ip = (node.getResolvedIp() != null) ? node.getResolvedIp() : node.getIpOrHostname();
            if (ip.equals("127.0.0.1")) continue;
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                String subnet = parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
                subnets.add(subnet);
            }
        }
        subnetComboBox.getItems().clear();
        subnetComboBox.getItems().add("Any");
        subnetComboBox.getItems().addAll(subnets);
        subnetComboBox.setValue("Any");
    }
    
    private void applyFilters() {
        String subnetFilter = subnetComboBox.getValue();
        DeviceType deviceFilter = deviceTypeBox.getValue();
        ConnectionType connectionFilter = connectionTypeBox.getValue();
        String connectionStatusFilter = connectionStatusBox.getValue();
        Color selectedColor = nodeColorPicker.getValue();
        String colorFilter = colorToHex(selectedColor);
        
        for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
            if (node.isMainNode()) {
                node.setVisible(true);
                continue;
            }
            boolean match = true;
            
            if (!subnetFilter.equalsIgnoreCase("Any")) {
                String ipToCheck = (node.getResolvedIp() != null) ? node.getResolvedIp() : node.getIpOrHostname();
                if (!isIPInSubnet(ipToCheck, subnetFilter)) {
                    match = false;
                }
            }
            if (deviceFilter != null && !node.getDeviceType().equals(deviceFilter)) {
                match = false;
            }
            if (connectionFilter != null && !node.getConnectionType().equals(connectionFilter)) {
                match = false;
            }
            if (!colorFilter.equalsIgnoreCase("#FFFFFF")) {
                if (!node.getOutlineColor().equalsIgnoreCase(colorFilter)) {
                    match = false;
                }
            }
            if (!connectionStatusFilter.equalsIgnoreCase("Any")) {
                if (connectionStatusFilter.equalsIgnoreCase("Connected")) {
                    if (!node.isConnected()) match = false;
                } else if (connectionStatusFilter.equalsIgnoreCase("Disconnected")) {
                    if (node.isConnected()) match = false;
                }
            }
            node.setVisible(match);
        }
    }
    
    private void resetFilters() {
        subnetComboBox.setValue("Any");
        deviceTypeBox.setValue(null);
        connectionTypeBox.setValue(null);
        connectionStatusBox.setValue("Any");
        nodeColorPicker.setValue(Color.web("#FFFFFF"));
        System.out.println("Filters reset.");
    }
    
    private int ipToInt(String ip) throws NumberFormatException {
        String[] parts = ip.split("\\.");
        int result = 0;
        for (String part : parts) {
            result = (result << 8) + Integer.parseInt(part);
        }
        return result;
    }
    
    private boolean isIPInSubnet(String ip, String subnetFilter) {
        try {
            String[] parts = subnetFilter.split("/");
            if (parts.length != 2) return false;
            String subnetAddress = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            int ipInt = ipToInt(ip);
            int subnetInt = ipToInt(subnetAddress);
            int mask = prefixLength == 0 ? 0 : 0xFFFFFFFF << (32 - prefixLength);
            return (ipInt & mask) == (subnetInt & mask);
        } catch (Exception e) {
            return false;
        }
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }
    
    private void expand() {
        expanded = true;
        setStyle(normalStyle);
        getChildren().remove(minimizedLabel);
        Timeline expandTimeline = new Timeline();
        KeyValue kvHeight = new KeyValue(prefHeightProperty(), EXPANDED_HEIGHT);
        KeyValue kvWidth = new KeyValue(prefWidthProperty(), EXPANDED_WIDTH);
        KeyFrame kf = new KeyFrame(Duration.millis(200), kvHeight, kvWidth);
        expandTimeline.getKeyFrames().add(kf);
        expandTimeline.setOnFinished(e -> {
            if (!getChildren().contains(contentBox)) {
                getChildren().add(contentBox);
            }
            this.requestFocus();
        });
        expandTimeline.play();
    }
    
    public void collapse() {
        expanded = false;
        getChildren().remove(contentBox);
        Timeline collapseTimeline = new Timeline();
        KeyValue kvHeight = new KeyValue(prefHeightProperty(), MIN_HEIGHT);
        KeyValue kvWidth = new KeyValue(prefWidthProperty(), WIDTH);
        KeyFrame kf = new KeyFrame(Duration.millis(200), kvHeight, kvWidth);
        collapseTimeline.getKeyFrames().add(kf);
        collapseTimeline.setOnFinished(e -> {
            if (!getChildren().contains(minimizedLabel)) {
                getChildren().add(minimizedLabel);
            }
        });
        collapseTimeline.play();
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }
    
    {
        setOnMouseClicked(e -> {
            if (!expanded || e.getTarget().equals(this)) {
                toggle();
            }
        });
        setOnKeyPressed(e -> {
            if (expanded && e.getCode() == KeyCode.ESCAPE) {
                collapse();
            }
        });
    }
}
