package org.example;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NewNodeBox extends StackPane {
    private final double FIELD_WIDTH = 200;
    private final double WIDTH = 150;
    private final double MIN_HEIGHT = 50;
    // Expanded height remains the same.
    private final double EXPANDED_HEIGHT = 425;
    // Expanded width updated to match NodeDetailPanel (350).
    private final double EXPANDED_WIDTH = 350;
    private final double NODE_BOX_GAP = 10;
    private boolean expanded = false;

    // Minimized state content.
    private Label minimizedLabel;
    // Expanded state content.
    private VBox contentBox;

    private TextField ipField;
    private TextField displayNameField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ComboBox<String> nodeColorBox;
    private ComboBox<String> routeSwitchBox;

    private Button createButton;
    private Button cancelButton;

    // Local mapping of rainbow colour names to Color objects.
    private Map<String, javafx.scene.paint.Color> rainbowColors = new HashMap<>();

    public NewNodeBox() {
        // Setup rainbowColors mapping.
        rainbowColors.put("White", javafx.scene.paint.Color.WHITE);
        rainbowColors.put("Red", javafx.scene.paint.Color.RED);
        rainbowColors.put("Orange", javafx.scene.paint.Color.ORANGE);
        rainbowColors.put("Yellow", javafx.scene.paint.Color.YELLOW);
        rainbowColors.put("Green", javafx.scene.paint.Color.GREEN);
        rainbowColors.put("Blue", javafx.scene.paint.Color.BLUE);
        rainbowColors.put("Indigo", javafx.scene.paint.Color.INDIGO);
        rainbowColors.put("Violet", javafx.scene.paint.Color.VIOLET);

        // Remove inline styles and instead add style classes.
        setPrefWidth(WIDTH);
        setPrefHeight(MIN_HEIGHT);
        getStyleClass().add("newnodebox-panel");

        // Minimized label.
        minimizedLabel = new Label("Add Node");
        minimizedLabel.getStyleClass().add("newnodebox-minimized-label");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // Build expanded view.
        contentBox = new VBox(NODE_BOX_GAP);
        contentBox.setPadding(new Insets(NODE_BOX_GAP));
        contentBox.getStyleClass().add("newnodebox-content-box");
        contentBox.setAlignment(Pos.TOP_CENTER);

        // Header section.
        VBox headerBox = new VBox();
        headerBox.getStyleClass().add("newnodebox-header-box");
        Label titleLabel = new Label("Add Node");
        titleLabel.getStyleClass().add("newnodebox-title-label");
        headerBox.getChildren().add(titleLabel);

        // Create horizontal sections for each field.
        // Define a standard label width.
        double labelWidth = 120;
        
        // DEVICE TYPE Section.
        HBox deviceSection = new HBox(10);
        deviceSection.setAlignment(Pos.CENTER_LEFT);
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.setPrefWidth(labelWidth);
        deviceLabel.getStyleClass().add("newnodebox-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("newnodebox-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceSection.getChildren().addAll(deviceLabel, deviceTypeBox);

        // DISPLAY NAME Section.
        HBox nameSection = new HBox(10);
        nameSection.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Display Name:");
        nameLabel.setPrefWidth(labelWidth);
        nameLabel.getStyleClass().add("newnodebox-label");
        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.getStyleClass().add("newnodebox-textfield");
        nameSection.getChildren().addAll(nameLabel, displayNameField);

        // HOSTNAME/IP Section.
        HBox ipSection = new HBox(10);
        ipSection.setAlignment(Pos.CENTER_LEFT);
        Label ipLabel = new Label("Hostname/IP:");
        ipLabel.setPrefWidth(labelWidth);
        ipLabel.getStyleClass().add("newnodebox-label");
        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.getStyleClass().add("newnodebox-textfield");
        ipSection.getChildren().addAll(ipLabel, ipField);

        // NETWORK TYPE Section.
        HBox networkSection = new HBox(10);
        networkSection.setAlignment(Pos.CENTER_LEFT);
        Label networkLabel = new Label("Network Type:");
        networkLabel.setPrefWidth(labelWidth);
        networkLabel.getStyleClass().add("newnodebox-label");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("newnodebox-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkSection.getChildren().addAll(networkLabel, networkTypeBox);

        // ROUTE VIA SWITCH Section.
        HBox routeSection = new HBox(10);
        routeSection.setAlignment(Pos.CENTER_LEFT);
        Label routeLabel = new Label("Network Route:");
        routeLabel.setPrefWidth(labelWidth);
        routeLabel.getStyleClass().add("newnodebox-label");
        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("newnodebox-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        updateRouteSwitchList();
        routeSwitchBox.setValue("None");
        routeSection.getChildren().addAll(routeLabel, routeSwitchBox);

        // CONNECTION TYPE Section.
        HBox connectionSection = new HBox(10);
        connectionSection.setAlignment(Pos.CENTER_LEFT);
        Label connectionLabel = new Label("Connection Type:");
        connectionLabel.setPrefWidth(labelWidth);
        connectionLabel.getStyleClass().add("newnodebox-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("newnodebox-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionSection.getChildren().addAll(connectionLabel, connectionTypeBox);

        // NODE COLOUR Section.
        HBox colorSection = new HBox(10);
        colorSection.setAlignment(Pos.CENTER_LEFT);
        Label colorLabel = new Label("Node Colour:");
        colorLabel.setPrefWidth(labelWidth);
        colorLabel.getStyleClass().add("newnodebox-label");
        nodeColorBox = new ComboBox<>();
        nodeColorBox.getStyleClass().add("newnodebox-combobox");
        nodeColorBox.setPrefWidth(FIELD_WIDTH);
        nodeColorBox.getItems().addAll(rainbowColors.keySet());
        nodeColorBox.setValue("White");
        colorSection.getChildren().addAll(colorLabel, nodeColorBox);

        // Button container.
        HBox buttonBox = new HBox(10);
        VBox.setMargin(buttonBox, new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER);
        createButton = new Button("Create");
        createButton.getStyleClass().add("newnodebox-create-button");
        createButton.setDisable(true);
        cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("newnodebox-cancel-button");
        cancelButton.setOnAction(e -> collapse());
        buttonBox.getChildren().addAll(createButton, cancelButton);

        // Assemble all sections into the content box.
        contentBox.getChildren().addAll(headerBox, deviceSection, nameSection, ipSection, networkSection, routeSection, connectionSection, colorSection, buttonBox);

        // Attach listeners to update the create button state.
        ipField.textProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        deviceTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        networkTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        connectionTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        nodeColorBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        routeSwitchBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());

        // Set up expand/collapse logic.
        setOnMouseClicked(e -> {
            if (!expanded) {
                expand();
            }
        });
        setOnKeyPressed(e -> {
            if (expanded && e.getCode() == KeyCode.ESCAPE) {
                collapse();
            }
        });
        
        // Bind layoutY to position the box at the bottom.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() != null) {
                layoutYProperty().bind(((Region) getParent()).heightProperty()
                        .subtract(prefHeightProperty())
                        .subtract(15));
            }
        });

        // Set action for create button to create a new node.
        createButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String displayName = displayNameField.getText().trim();
            DeviceType deviceType = deviceTypeBox.getValue();
            NetworkType networkType = networkTypeBox.getValue();
            ConnectionType connectionType = connectionTypeBox.getValue();
            String colorName = nodeColorBox.getValue();
            String routeSwitch = routeSwitchBox.getValue();
            if ("None".equals(routeSwitch)) {
                routeSwitch = "";
            }
            // Convert color name to hex.
            javafx.scene.paint.Color color = rainbowColors.get(colorName);
            String hexColor = String.format("#%02X%02X%02X",
                    (int)(color.getRed() * 255),
                    (int)(color.getGreen() * 255),
                    (int)(color.getBlue() * 255));
            
            // Create the new node.
            NetworkNode newNode = new NetworkNode(ip, displayName, deviceType, networkType);
            newNode.setConnectionType(connectionType);
            newNode.setOutlineColor(hexColor);
            newNode.setRouteSwitch(routeSwitch);
            NetworkMonitorApp.addNewNode(newNode);
            // Optionally, collapse the box after creation.
            collapse();
        });
    }
    
    private void updateRouteSwitchList() {
        routeSwitchBox.getItems().clear();
        routeSwitchBox.getItems().add("None");
        for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
            if (node.getDeviceType() == DeviceType.SWITCH) {
                routeSwitchBox.getItems().add(node.getDisplayName());
            }
        }
    }
    
    private void updateCreateButtonState() {
        boolean enable = !ipField.getText().trim().isEmpty()
                && !displayNameField.getText().trim().isEmpty()
                && deviceTypeBox.getValue() != null
                && networkTypeBox.getValue() != null
                && connectionTypeBox.getValue() != null
                && nodeColorBox.getValue() != null
                && routeSwitchBox.getValue() != null;
        createButton.setDisable(!enable);
    }
    
    private void expand() {
        updateRouteSwitchList();
        expanded = true;
        // Remove any collapsed style if present.
        getStyleClass().remove("collapsed");
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
        // When collapsing, add the "collapsed" style class.
        if (!getStyleClass().contains("collapsed")) {
            getStyleClass().add("collapsed");
        }
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
    
    public void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }
}
