package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NodeDetailPanel extends BorderPane {
    private final double FIELD_WIDTH = 200;
    // Updated panel width is now 350.
    private final double PANEL_WIDTH = 350;
    private TextField ipField;
    private TextField displayNameField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ColorPicker nodeColorPicker;
    // Replace the editable MAC text field with a label.
    private Label macValueLabel;
    private Label uptimeLabel;
    private Button updateButton;

    // Top part: contains header and grid.
    private VBox topContent;
    // Bottom container using BorderPane.
    private BorderPane bottomContainer;

    private NetworkNode node;

    public NodeDetailPanel(NetworkNode node) {
        this.node = node;
        createUI();
        populateFields();
        setupListeners();
        // Set fixed dimensions – updated width to 350.
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setPrefHeight(250);
        setMaxHeight(250);
        setPadding(new Insets(10));
        // Apply panel style.
        getStyleClass().add("nodedetail-panel");
        setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0.5, 0, 0);");
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hidePanel();
            }
        });
    }


    private void createUI() {
        // --- Top Content ---
        topContent = new VBox(10);
        topContent.setAlignment(Pos.TOP_CENTER);

        // Header section using a bottom border via style.
        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(node.getDisplayName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        // Apply a 0.5px bottom border with color #b8d4f1 and padding.
        headerBox.setStyle("-fx-border-color: transparent transparent #b8d4f1 transparent; " +
                           "-fx-border-width: 0 0 0.5px 0; -fx-padding: 0 0 10px 0;");
        headerBox.getChildren().add(titleLabel);

        // Create a grid for node fields.
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        Label ipLabel = new Label("IP/Hostname:");
        ipLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                         "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                         "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");

        Label displayNameLabel = new Label("Display Name:");
        displayNameLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                  "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                  "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");

        Label deviceTypeLabel = new Label("Device Type:");
        deviceTypeLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.setValue(DeviceType.COMPUTER);
        deviceTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                               "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                               "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");

        Label networkTypeLabel = new Label("Network Type:");
        networkTypeLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);
        networkTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");

        Label connectionTypeLabel = new Label("Connection Type:");
        connectionTypeLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);
        connectionTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                   "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                   "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");

        Label nodeColorLabel = new Label("Node Colour:");
        nodeColorLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        nodeColorPicker = new ColorPicker();
        nodeColorPicker.setStyle("-fx-background-color: #1b2433; -fx-text-fill: white;");

        // Replace MAC text field with a label.
        Label macLabel = new Label("MAC Address:");
        macLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        macValueLabel = new Label(); // New read-only label for MAC
        macValueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label uptimeStaticLabel = new Label("Uptime:");
        uptimeStaticLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        uptimeLabel = new Label();
        uptimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // Add all fields to the grid.
        grid.add(ipLabel, 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(displayNameLabel, 0, 1);
        grid.add(displayNameField, 1, 1);
        grid.add(deviceTypeLabel, 0, 2);
        grid.add(deviceTypeBox, 1, 2);
        grid.add(networkTypeLabel, 0, 3);
        grid.add(networkTypeBox, 1, 3);
        grid.add(connectionTypeLabel, 0, 4);
        grid.add(connectionTypeBox, 1, 4);
        grid.add(nodeColorLabel, 0, 5);
        grid.add(nodeColorPicker, 1, 5);
        grid.add(macLabel, 0, 6);
        grid.add(macValueLabel, 1, 6);
        grid.add(uptimeStaticLabel, 0, 7);
        grid.add(uptimeLabel, 1, 7);

        topContent.getChildren().clear();
        topContent.getChildren().addAll(headerBox, grid);
        setCenter(topContent);

        // --- Bottom Container using BorderPane ---
        bottomContainer = new BorderPane();
        bottomContainer.setPrefHeight(50);
        bottomContainer.setPadding(new Insets(10));

        updateButton = new Button("Update");
        updateButton.setDisable(true);
        updateButton.setMinWidth(Region.USE_PREF_SIZE);
        updateButton.setMinWidth(100);
        updateButton.setMinHeight(35);
        updateButton.setStyle("-fx-background-color: #2E8B57; -fx-text-fill: white; -fx-font-size: 16px;");
        BorderPane.setAlignment(updateButton, Pos.CENTER);
        bottomContainer.setCenter(updateButton);

        Button deleteButton = new Button();
        ImageView binIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/bin.png")));
        binIcon.setFitWidth(25);
        binIcon.setFitHeight(25);
        deleteButton.setGraphic(binIcon);
        deleteButton.setStyle("-fx-background-color: red; -fx-cursor: hand;");
        deleteButton.setPrefSize(35, 35);
        BorderPane.setAlignment(deleteButton, Pos.CENTER_RIGHT);
        bottomContainer.setRight(deleteButton);

        setBottom(bottomContainer);

        deleteButton.setOnAction(e -> {
            NetworkMonitorApp.removeNode(node);
            hidePanel();
        });
    }

    private void populateFields() {
        if (node.getResolvedIp() != null && !node.getResolvedIp().isEmpty()) {
            ipField.setText(node.getResolvedIp() + "/" + node.getIpOrHostname());
        } else {
            ipField.setText(node.getIpOrHostname());
        }
        displayNameField.setText(node.getDisplayName());
        deviceTypeBox.setValue(node.getDeviceType());
        networkTypeBox.setValue(node.getNetworkType());
        connectionTypeBox.setValue(node.getConnectionType());
        nodeColorPicker.setValue(Color.web(node.getOutlineColor()));
        // Update MAC Address asynchronously.
        updateMacAddress();
        long uptimeSeconds = (System.currentTimeMillis() - node.getStartTime()) / 1000;
        uptimeLabel.setText(uptimeSeconds + " s");
    }

    private void setupListeners() {
        ipField.textProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Optionally update the header title if you want it to change as well.
            enableUpdate();
        });
        deviceTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        networkTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        connectionTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        nodeColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());

        updateButton.setOnAction(e -> {
            String ipInput = ipField.getText();
            if (ipInput.contains("/")) {
                // Use the text after the slash as the ipOrHostname.
                String[] parts = ipInput.split("/", 2);
                node.setIpOrHostname(parts[1].trim());
            } else {
                node.setIpOrHostname(ipInput);
            }
            node.setDisplayName(displayNameField.getText());
            node.setDeviceType(deviceTypeBox.getValue());
            node.setNetworkType(networkTypeBox.getValue());
            node.setConnectionType(connectionTypeBox.getValue());
            node.setOutlineColor(String.format("#%02X%02X%02X",
                    (int)(nodeColorPicker.getValue().getRed()*255),
                    (int)(nodeColorPicker.getValue().getGreen()*255),
                    (int)(nodeColorPicker.getValue().getBlue()*255)));
            updateButton.setDisable(true);
            // Refresh the connection line to reflect the new network type.
            NetworkMonitorApp.updateConnectionLineForNode(node);
        });
    }

    private void enableUpdate() {
        updateButton.setDisable(false);
    }

    // Run arp -a command to look up MAC for internal nodes.
    private void updateMacAddress() {
        new Thread(() -> {
            String mac = "N/A";
            if (node.getNetworkType() == NetworkType.INTERNAL) {
                String ip = (node.getResolvedIp() != null) ? node.getResolvedIp() : node.getIpOrHostname();
                mac = getMacAddressForIP(ip);
            }
            final String macFinal = mac;
            Platform.runLater(() -> {
                macValueLabel.setText(macFinal);
            });
        }).start();
    }

    // Helper method: runs arp -a and parses output for the given IP.
    private String getMacAddressForIP(String ip) {
        try {
            Process process = Runtime.getRuntime().exec("arp -a");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(ip)) {
                    // Split line into tokens and assume the MAC is the second token.
                    String[] tokens = line.trim().split("\\s+");
                    if (tokens.length >= 2) {
                        return tokens[1].replace('-', ':'); 
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    public void hidePanel() {
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), this);
        ft.setFromValue(getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (getParent() instanceof Pane) {
                ((Pane)getParent()).getChildren().remove(this);
            }
        });
        ft.play();
    }

    public void showPanel() {
        setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), this);
        ft.setFromValue(0);
        ft.setToValue(0.9);
        ft.play();
    }
}
