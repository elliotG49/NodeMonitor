package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NodeDetailPanel extends BorderPane {
    private final double FIELD_WIDTH = 200;
    private final double PANEL_WIDTH = 350;
    private final double PANEL_HEIGHT = 475;
    private TextField ipField;
    private TextField displayNameField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    // Removed nodeColorBox.
    private ComboBox<String> routeSwitchBox;
    private Label macValueLabel;
    private Label uptimeLabel;
    private Button updateButton;

    private VBox topContent;
    private BorderPane bottomContainer;

    private NetworkNode node;
    
    public NodeDetailPanel(NetworkNode node) {
        this.node = node;
        createUI();
        populateFields();
        setupListeners();
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setPrefHeight(PANEL_HEIGHT);
        setMaxHeight(PANEL_HEIGHT);
        setPadding(new Insets(10));
        getStyleClass().add("nodedetail-panel");
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hidePanel();
            }
        });
    }

    private void createUI() {
        topContent = new VBox(10);
        topContent.setAlignment(Pos.TOP_CENTER);

        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(node.getDisplayName());
        titleLabel.getStyleClass().add("nodedetail-title-label");
        headerBox.getStyleClass().add("nodedetail-header-box");
        headerBox.getChildren().add(titleLabel);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        Label ipLabel = new Label("IP/Hostname:");
        ipLabel.getStyleClass().add("nodedetail-label");

        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.getStyleClass().add("nodedetail-textfield");

        Label displayNameLabel = new Label("Display Name:");
        displayNameLabel.getStyleClass().add("nodedetail-label");

        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.getStyleClass().add("nodedetail-textfield");

        Label deviceTypeLabel = new Label("Device Type:");
        deviceTypeLabel.getStyleClass().add("nodedetail-label");

        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("nodedetail-combobox");
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.setValue(DeviceType.COMPUTER);

        Label networkTypeLabel = new Label("Network Type:");
        networkTypeLabel.getStyleClass().add("nodedetail-label");

        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("nodedetail-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);

        Label connectionTypeLabel = new Label("Connection Type:");
        connectionTypeLabel.getStyleClass().add("nodedetail-label");

        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("nodedetail-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);

        Label routeSwitchLabel = new Label("Route via Switch:");
        routeSwitchLabel.getStyleClass().add("nodedetail-label");

        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("nodedetail-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        updateRouteSwitchList();
        if (node.getRouteSwitch() == null || node.getRouteSwitch().isEmpty()) {
            routeSwitchBox.setValue("None");
        } else {
            routeSwitchBox.setValue(node.getRouteSwitch());
        }

        Label macLabel = new Label("MAC Address:");
        macLabel.getStyleClass().add("nodedetail-label");
        macValueLabel = new Label();
        macValueLabel.getStyleClass().add("nodedetail-value-label");

        Label uptimeStaticLabel = new Label("Uptime:");
        uptimeStaticLabel.getStyleClass().add("nodedetail-label");
        uptimeLabel = new Label();
        uptimeLabel.getStyleClass().add("nodedetail-value-label");

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

        grid.add(routeSwitchLabel, 0, 5);
        grid.add(routeSwitchBox, 1, 5);

        grid.add(macLabel, 0, 6);
        grid.add(macValueLabel, 1, 6);

        grid.add(uptimeStaticLabel, 0, 7);
        grid.add(uptimeLabel, 1, 7);

        topContent.getChildren().clear();
        topContent.getChildren().addAll(headerBox, grid);
        setCenter(topContent);

        bottomContainer = new BorderPane();
        bottomContainer.setPrefHeight(20);
        bottomContainer.setPadding(new Insets(10));

        updateButton = new Button("Update");
        updateButton.setDisable(true);
        updateButton.setMinWidth(125);
        updateButton.setMinHeight(35);
        updateButton.getStyleClass().add("nodedetail-updatebutton");
        BorderPane.setAlignment(updateButton, Pos.CENTER_LEFT);
        bottomContainer.setCenter(updateButton);

        Button deleteButton = new Button("Delete");
        deleteButton.setMinWidth(125);
        deleteButton.setMinHeight(35);
        deleteButton.getStyleClass().add("nodedetail-binbutton");
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
        if (node.getRouteSwitch() == null || node.getRouteSwitch().isEmpty()) {
            routeSwitchBox.setValue("None");
        } else {
            routeSwitchBox.setValue(node.getRouteSwitch());
        }
        updateMacAddress();
        long uptimeSeconds = (System.currentTimeMillis() - node.getStartTime()) / 1000;
        uptimeLabel.setText(uptimeSeconds + " s");
    }

    private void setupListeners() {
        ipField.textProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        deviceTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        networkTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        connectionTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        routeSwitchBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());

        updateButton.setOnAction(e -> {
            String ipInput = ipField.getText();
            if (ipInput.contains("/")) {
                String[] parts = ipInput.split("/", 2);
                node.setIpOrHostname(parts[1].trim());
            } else {
                node.setIpOrHostname(ipInput);
            }
            node.setDisplayName(displayNameField.getText());
            node.setDeviceType(deviceTypeBox.getValue());
            node.setNetworkType(networkTypeBox.getValue());
            node.setConnectionType(connectionTypeBox.getValue());
            node.setRouteSwitch(routeSwitchBox.getValue().equals("None") ? "" : routeSwitchBox.getValue());
            updateButton.setDisable(true);
            NetworkMonitorApp.updateConnectionLineForNode(node);
        });
    }

    private void updateRouteSwitchList() {
        routeSwitchBox.getItems().clear();
        routeSwitchBox.getItems().add("None");
        for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
            if (n.getDeviceType() == DeviceType.SWITCH) {
                routeSwitchBox.getItems().add(n.getDisplayName());
            }
        }
    }
    
    private void enableUpdate() {
        updateButton.setDisable(false);
    }

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

    private String getMacAddressForIP(String ip) {
        try {
            Process process = Runtime.getRuntime().exec("arp -a");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(ip)) {
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
