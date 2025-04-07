package org.example;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NodeDetailPanel extends VBox {
    private Label headerLabel;
    private TextField ipField;
    private TextField displayNameField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ColorPicker nodeColorPicker;
    private TextField macField;
    private Label uptimeLabel;
    private Button updateButton;

    private NetworkNode node;

    public NodeDetailPanel(NetworkNode node) {
        this.node = node;
        createUI();
        populateFields();
        setupListeners();
        // Apply the style class for the detail panel.
        getStyleClass().add("nodedetail-panel");
        // Set preferred dimensions.
        setPrefWidth(250);
        setMaxWidth(250);
        setPrefHeight(250);
        setMaxHeight(250);
        setPadding(new Insets(10));
        setSpacing(10);
    }

    private void createUI() {
        // Header with large node name.
        headerLabel = new Label(node.getDisplayName());
        headerLabel.getStyleClass().add("nodedetail-header");

        // Create a grid for the fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("nodedetail-grid");

        Label ipLabel = new Label("IP/Hostname:");
        ipLabel.getStyleClass().add("nodedetail-label");
        ipField = new TextField();
        ipField.getStyleClass().add("nodedetail-textfield");

        Label displayNameLabel = new Label("Display Name:");
        displayNameLabel.getStyleClass().add("nodedetail-label");
        displayNameField = new TextField();
        displayNameField.getStyleClass().add("nodedetail-textfield");

        Label deviceTypeLabel = new Label("Device Type:");
        deviceTypeLabel.getStyleClass().add("nodedetail-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.getStyleClass().add("nodedetail-combobox");

        Label networkTypeLabel = new Label("Network Type:");
        networkTypeLabel.getStyleClass().add("nodedetail-label");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.getStyleClass().add("nodedetail-combobox");

        Label connectionTypeLabel = new Label("Connection Type:");
        connectionTypeLabel.getStyleClass().add("nodedetail-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.getStyleClass().add("nodedetail-combobox");

        Label nodeColorLabel = new Label("Node Colour:");
        nodeColorLabel.getStyleClass().add("nodedetail-label");
        nodeColorPicker = new ColorPicker();
        nodeColorPicker.getStyleClass().add("nodedetail-colorpicker");

        Label macLabel = new Label("MAC Address:");
        macLabel.getStyleClass().add("nodedetail-label");
        macField = new TextField();
        macField.getStyleClass().add("nodedetail-textfield");
        macField.setEditable(false);

        Label uptimeStaticLabel = new Label("Uptime:");
        uptimeStaticLabel.getStyleClass().add("nodedetail-label");
        uptimeLabel = new Label();
        uptimeLabel.getStyleClass().add("nodedetail-uptime");

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
        grid.add(macField, 1, 6);
        grid.add(uptimeStaticLabel, 0, 7);
        grid.add(uptimeLabel, 1, 7);

        updateButton = new Button("Update");
        updateButton.getStyleClass().add("nodedetail-update-button");
        updateButton.setDisable(true);

        setAlignment(Pos.TOP_CENTER);
        getChildren().addAll(headerLabel, grid, updateButton);
    }

    

    private void populateFields() {
        ipField.setText(node.getIpOrHostname());
        displayNameField.setText(node.getDisplayName());
        deviceTypeBox.setValue(node.getDeviceType());
        networkTypeBox.setValue(node.getNetworkType());
        connectionTypeBox.setValue(node.getConnectionType());
        nodeColorPicker.setValue(Color.web(node.getOutlineColor()));
        macField.setText("00:11:22:33:44:55"); // Replace with actual lookup if available.
        long uptimeSeconds = (System.currentTimeMillis() - node.getStartTime()) / 1000;
        uptimeLabel.setText(uptimeSeconds + " s");
    }

    private void setupListeners() {
        ipField.textProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            headerLabel.setText(newVal);
            enableUpdate();
        });
        deviceTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        networkTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        connectionTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        nodeColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());

        updateButton.setOnAction(e -> {
            node.setIpOrHostname(ipField.getText());
            node.setDisplayName(displayNameField.getText());
            node.setDeviceType(deviceTypeBox.getValue());
            node.setNetworkType(networkTypeBox.getValue());
            node.setConnectionType(connectionTypeBox.getValue());
            node.setOutlineColor(String.format("#%02X%02X%02X",
                    (int)(nodeColorPicker.getValue().getRed()*255),
                    (int)(nodeColorPicker.getValue().getGreen()*255),
                    (int)(nodeColorPicker.getValue().getBlue()*255)));
            updateButton.setDisable(true);
            // The update button style will be set in the stylesheet.
        });
    }

    private void enableUpdate() {
        updateButton.setDisable(false);
        // The enabled style is controlled by CSS.
    }

    public void hidePanel() {
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), this);
        ft.setFromValue(getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (getParent() != null) {
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
