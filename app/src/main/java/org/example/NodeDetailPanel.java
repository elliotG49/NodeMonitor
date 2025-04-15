package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
    private final double PANEL_WIDTH = 350;
    private final double PANEL_HEIGHT = 500;
    private TextField ipField;
    private TextField displayNameField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    // Replace the colour selector with a ComboBox of colour names.
    private ComboBox<String> nodeColorBox;
    private ComboBox<String> routeSwitchBox;
    // Replace the editable MAC text field with a label.
    private Label macValueLabel;
    private Label uptimeLabel;
    private Button updateButton;

    // Top part: contains header and grid.
    private VBox topContent;
    // Bottom container using BorderPane.
    private BorderPane bottomContainer;

    private NetworkNode node;
    
    // Local mapping of rainbow colour names to Color objects.
    private Map<String, Color> rainbowColors = new HashMap<>();

    public NodeDetailPanel(NetworkNode node) {
        this.node = node;
        // Initialize rainbowColors.
        rainbowColors.put("White", Color.WHITE);
        rainbowColors.put("Red", Color.RED);
        rainbowColors.put("Orange", Color.ORANGE);
        rainbowColors.put("Yellow", Color.YELLOW);
        rainbowColors.put("Green", Color.GREEN);
        rainbowColors.put("Blue", Color.BLUE);
        rainbowColors.put("Indigo", Color.INDIGO);
        rainbowColors.put("Violet", Color.VIOLET);

        
        createUI();
        populateFields();
        setupListeners();
        // Set fixed dimensions – updated width to 350.
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setPrefHeight(PANEL_HEIGHT);
        setMaxHeight(PANEL_HEIGHT);
        setPadding(new Insets(10));
        // Apply panel style.
        getStyleClass().add("nodedetail-panel");
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
        titleLabel.getStyleClass().add("nodedetail-title-label");
        headerBox.getStyleClass().add("nodedetail-header-box");
        headerBox.getChildren().add(titleLabel);

        // Create a grid for node fields.
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        // (Optional: set row constraints if needed)

        // IP/Hostname label and field:
        Label ipLabel = new Label("IP/Hostname:");
        ipLabel.getStyleClass().add("nodedetail-label");

        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.getStyleClass().add("nodedetail-textfield");

        // Display Name label and field:
        Label displayNameLabel = new Label("Display Name:");
        displayNameLabel.getStyleClass().add("nodedetail-label");

        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        // Use the corrected class name here:
        displayNameField.getStyleClass().add("nodedetail-textfield");

        // Device Type label and ComboBox:
        Label deviceTypeLabel = new Label("Device Type:");
        deviceTypeLabel.getStyleClass().add("nodedetail-label");

        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("nodedetail-combobox");
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.setValue(DeviceType.COMPUTER);

        // Network Type label and ComboBox:
        Label networkTypeLabel = new Label("Network Type:");
        networkTypeLabel.getStyleClass().add("nodedetail-label");

        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("nodedetail-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);

        // Connection Type label and ComboBox:
        Label connectionTypeLabel = new Label("Connection Type:");
        connectionTypeLabel.getStyleClass().add("nodedetail-label");

        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("nodedetail-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);

        // Route via Switch label and ComboBox:
        Label routeSwitchLabel = new Label("Route via Switch:");
        routeSwitchLabel.getStyleClass().add("nodedetail-label");

        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("nodedetail-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        updateRouteSwitchList(); // Populate with current switches
        // Set the default value (if the node's route switch is not set, "None" will be used)
        if (node.getRouteSwitch() == null || node.getRouteSwitch().isEmpty()) {
            routeSwitchBox.setValue("None");
        } else {
            routeSwitchBox.setValue(node.getRouteSwitch());
        }

        // Node Colour label and ComboBox (using a slightly different style for radius):
        Label nodeColorLabel = new Label("Node Colour:");
        nodeColorLabel.getStyleClass().add("nodedetail-label");
        nodeColorBox = new ComboBox<>();
        // Here we use a separate style class to achieve different rounded corners.
        nodeColorBox.getStyleClass().add("nodedetail-combobox");
        nodeColorBox.setPrefWidth(FIELD_WIDTH);
        nodeColorBox.getItems().addAll(rainbowColors.keySet());


        // Replace MAC text field with a label.
        Label macLabel = new Label("MAC Address:");
        macLabel.getStyleClass().add("nodedetail-label");
        macValueLabel = new Label();
        macValueLabel.getStyleClass().add("nodedetail-value-label"); // Dynamic value styling

        Label uptimeStaticLabel = new Label("Uptime:");
        uptimeStaticLabel.getStyleClass().add("nodedetail-label");
        Label uptimeLabel = new Label();
        macValueLabel.getStyleClass().add("nodedetail-value-label"); // Dynamic value styling

        // Add all fields to the grid with updated row indices.
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
        grid.add(nodeColorBox, 1, 5);

        grid.add(routeSwitchLabel, 0, 6);
        grid.add(routeSwitchBox, 1, 6);

        grid.add(macLabel, 0, 7);
        grid.add(macValueLabel, 1, 7);

        grid.add(uptimeStaticLabel, 0, 8);
        grid.add(uptimeLabel, 1, 8);

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
        updateButton.getStyleClass().add("nodedetail-updatebutton");
        BorderPane.setAlignment(updateButton, Pos.CENTER);
        bottomContainer.setCenter(updateButton);

        Button deleteButton = new Button();
        ImageView binIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/bin.png")));
        binIcon.setFitWidth(25);
        binIcon.setFitHeight(25);
        deleteButton.setGraphic(binIcon);
        deleteButton.getStyleClass().add("nodedetail-binbutton");
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
        // Set the node colour based on its outlineColor.
        // Look up the colour name from the rainbowColors map by comparing hex values.
        String selectedColorName = "Red";
        for (Map.Entry<String, Color> entry : rainbowColors.entrySet()) {
            if (node.getOutlineColor().equalsIgnoreCase(
                    String.format("#%02X%02X%02X",
                        (int)(entry.getValue().getRed()*255),
                        (int)(entry.getValue().getGreen()*255),
                        (int)(entry.getValue().getBlue()*255)
                    ))) {
                selectedColorName = entry.getKey();
                break;
            }
        }
        nodeColorBox.setValue(selectedColorName);
        // Update route switch
        if (node.getRouteSwitch() == null || node.getRouteSwitch().isEmpty()) {
            routeSwitchBox.setValue("None");
        } else {
            routeSwitchBox.setValue(node.getRouteSwitch());
        }
        updateMacAddress();
        long uptimeSeconds = (System.currentTimeMillis() - node.getStartTime()) / 1000;
        // You might want to update uptime periodically.
        // For now, set it once.
        // (Note: Here we use the local variable uptimeLabel from createUI; ensure consistency.)
    }

    private void setupListeners() {
        ipField.textProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        deviceTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        networkTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        connectionTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        routeSwitchBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());
        nodeColorBox.valueProperty().addListener((obs, oldVal, newVal) -> enableUpdate());

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
            // Look up the selected colour from the rainbow map.
            Color color = rainbowColors.get(nodeColorBox.getValue());
            node.setOutlineColor(String.format("#%02X%02X%02X",
                    (int)(color.getRed()*255),
                    (int)(color.getGreen()*255),
                    (int)(color.getBlue()*255)));
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
