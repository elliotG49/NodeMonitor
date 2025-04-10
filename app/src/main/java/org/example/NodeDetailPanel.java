package org.example;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NodeDetailPanel extends BorderPane {
    private final double FIELD_WIDTH = 200; // Or any value you want
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

    // Top part: contains header and grid.
    private VBox topContent;
    // Bottom container using BorderPane.
    private BorderPane bottomContainer;

    public NodeDetailPanel(NetworkNode node) {
        this.node = node;
        createUI();
        populateFields();
        setupListeners();
        // Set fixed dimensions.
        setPrefWidth(250);
        setMaxWidth(250);
        setPrefHeight(250);
        setMaxHeight(250);
        setPadding(new Insets(10));
        // Apply style.
        getStyleClass().add("nodedetail-panel");
        setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0.5, 0, 0);");
    }

    private void createUI() {
        // --- Top Content ---
        topContent = new VBox(10);
        topContent.setAlignment(Pos.TOP_CENTER);

        // Header with large node name.
        headerLabel = new Label(node.getDisplayName());
        headerLabel.getStyleClass().add("nodedetail-header");

        // Create a container for the header aligned to the left.
        HBox headerContainer = new HBox(headerLabel);
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setPadding(new Insets(0, 0, 5, 0));

        // Create a grid for node fields.
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.getStyleClass().add("nodedetail-grid");

        Label ipLabel = new Label("IP/Hostname:");
        ipLabel.getStyleClass().add("nodedetail-label");
        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.setStyle("-fx-font-size: 14px; " +
                         "-fx-background-color: #1b2433; " +
                         "-fx-border-color: #3B3B3B; " +
                         "-fx-border-width: 1px; " +
                         "-fx-background-radius: 5; " +
                         "-fx-border-radius: 5; " +
                         "-fx-text-fill: white;");

        Label displayNameLabel = new Label("Display Name:");
        displayNameLabel.getStyleClass().add("nodedetail-label");
        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.setStyle("-fx-font-size: 14px; " +
                                  "-fx-background-color: #1b2433; " +
                                  "-fx-border-color: #3B3B3B; " +
                                  "-fx-border-width: 1px; " +
                                  "-fx-background-radius: 5; " +
                                  "-fx-border-radius: 5; " +
                                  "-fx-text-fill: white;");

        Label deviceTypeLabel = new Label("Device Type:");
        deviceTypeLabel.getStyleClass().add("nodedetail-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.setValue(DeviceType.COMPUTER);
        deviceTypeBox.setStyle("-fx-font-size: 14px; " +
                               "-fx-background-color: #1b2433; " +
                               "-fx-border-color: #3B3B3B; " +
                               "-fx-border-width: 1px; " +
                               "-fx-background-radius: 5; " +
                               "-fx-border-radius: 5; " +
                               "-fx-text-fill: white;");

        Label networkTypeLabel = new Label("Network Type:");
        networkTypeLabel.getStyleClass().add("nodedetail-label");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);
        networkTypeBox.setStyle("-fx-font-size: 14px; " +
                                "-fx-background-color: #1b2433; " +
                                "-fx-border-color: #3B3B3B; " +
                                "-fx-border-width: 1px; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-radius: 5; " +
                                "-fx-text-fill: white;");

        Label connectionTypeLabel = new Label("Connection Type:");
        connectionTypeLabel.getStyleClass().add("nodedetail-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);
        connectionTypeBox.setStyle("-fx-font-size: 14px; " +
                                   "-fx-background-color: #1b2433; " +
                                   "-fx-border-color: #3B3B3B; " +
                                   "-fx-border-width: 1px; " +
                                   "-fx-background-radius: 5; " +
                                   "-fx-border-radius: 5; " +
                                   "-fx-text-fill: white;");

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

        topContent.getChildren().clear();
        topContent.getChildren().addAll(headerContainer, grid);
        setCenter(topContent);

        // --- Bottom Container using BorderPane ---
        bottomContainer = new BorderPane();
        bottomContainer.setPrefHeight(50);
        bottomContainer.setPadding(new Insets(10));

        // Create the update button.
        updateButton = new Button("Update");
        updateButton.getStyleClass().add("nodedetail-update-button");
        updateButton.setDisable(true);
        updateButton.setMaxWidth(Region.USE_PREF_SIZE);
        updateButton.setMinWidth(100);
        updateButton.setMinHeight(35);
        updateButton.setStyle("-fx-background-color: #2E8B57; -fx-text-fill: white; -fx-font-size: 16px;");
        BorderPane.setAlignment(updateButton, Pos.CENTER);
        bottomContainer.setCenter(updateButton);

        // Create the delete button.
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

        // Delete button action.
        deleteButton.setOnAction(e -> {
            NetworkMonitorApp.removeNode(node);
            hidePanel();
        });
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
        });
    }

    private void enableUpdate() {
        updateButton.setDisable(false);
    }

    public void hidePanel() {
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), this);
        ft.setFromValue(getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (getParent() != null && getParent() instanceof Pane) {
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
