package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
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
    private final double WIDTH = 50;
    private final double MIN_HEIGHT = 50;
    // Expanded height remains the same.
    private final double EXPANDED_HEIGHT = 575;
    // Expanded width updated to match NodeDetailPanel (350).
    private final double EXPANDED_WIDTH = 250;
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
    // Removed nodeColorBox.
    private ComboBox<String> routeSwitchBox;

    private final Button createButton;


    public NewNodeBox() {
        setPrefWidth(WIDTH);
        setPrefHeight(MIN_HEIGHT);
        getStyleClass().add("newnodebox-panel");

        // Minimized label.
        minimizedLabel = new Label("+");
        minimizedLabel.getStyleClass().add("newnodebox-minimized-label");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // Build expanded view.
        contentBox = new VBox(NODE_BOX_GAP);
        contentBox.getStyleClass().add("newnodebox-content-box");
        contentBox.setAlignment(Pos.TOP_CENTER);

        // Header section.
        // Header section.
        VBox headerBox = new VBox();
        headerBox.getStyleClass().add("newnodebox-header-box");
        Label titleLabel = new Label("Add Node");
        VBox.setMargin(headerBox, new Insets(0, 20, 0, 20));  // 25px from far left edge
        titleLabel.getStyleClass().add("newnodebox-title-label");
        titleLabel.setAlignment(Pos.CENTER);
        headerBox.getChildren().add(titleLabel);

                // DEVICE TYPE Section.
        VBox deviceSection = new VBox(10);
        deviceSection.setAlignment(Pos.TOP_LEFT);  // Let the VBox align its children to the left by default.
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.getStyleClass().add("newnodebox-label");
        VBox.setMargin(deviceLabel, new Insets(0, 0, 0, 25));  // 25px from far left edge
        // Create an HBox to center the control.
        HBox deviceControlContainer = new HBox();
        deviceControlContainer.setAlignment(Pos.CENTER);
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("newnodebox-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceControlContainer.getChildren().add(deviceTypeBox);
        deviceSection.getChildren().addAll(deviceLabel, deviceControlContainer);

        // DISPLAY NAME Section.
        VBox nameSection = new VBox(10);
        nameSection.setAlignment(Pos.TOP_LEFT);
        Label nameLabel = new Label("Display Name:");
        nameLabel.getStyleClass().add("newnodebox-label");
        VBox.setMargin(nameLabel, new Insets(0, 0, 0, 25));
        HBox nameControlContainer = new HBox();
        nameControlContainer.setAlignment(Pos.CENTER);
        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.setMinWidth(FIELD_WIDTH);
        displayNameField.setMaxWidth(FIELD_WIDTH);
        displayNameField.getStyleClass().add("newnodebox-textfield");
        nameControlContainer.getChildren().add(displayNameField);
        nameSection.getChildren().addAll(nameLabel, nameControlContainer);

        // HOSTNAME/IP Section.
        VBox ipSection = new VBox(10);
        ipSection.setAlignment(Pos.TOP_LEFT);
        Label ipLabel = new Label("Hostname/IP:");
        ipLabel.getStyleClass().add("newnodebox-label");
        VBox.setMargin(ipLabel, new Insets(0, 0, 0, 25));
        HBox ipControlContainer = new HBox();
        ipControlContainer.setAlignment(Pos.CENTER);
        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setMinWidth(FIELD_WIDTH);
        ipField.setMaxWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.getStyleClass().add("newnodebox-textfield");
        ipControlContainer.getChildren().add(ipField);
        ipSection.getChildren().addAll(ipLabel, ipControlContainer);

        // NETWORK TYPE Section.
        VBox networkSection = new VBox(10);
        networkSection.setAlignment(Pos.TOP_LEFT);
        Label networkLabel = new Label("Network Type:");
        networkLabel.getStyleClass().add("newnodebox-label");
        VBox.setMargin(networkLabel, new Insets(0, 0, 0, 25));
        HBox networkControlContainer = new HBox();
        networkControlContainer.setAlignment(Pos.CENTER);
        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("newnodebox-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkControlContainer.getChildren().add(networkTypeBox);
        networkSection.getChildren().addAll(networkLabel, networkControlContainer);

        // ROUTE VIA SWITCH Section.
        VBox routeSection = new VBox(10);
        routeSection.setAlignment(Pos.TOP_LEFT);
        Label routeLabel = new Label("Network Route:");
        routeLabel.getStyleClass().add("newnodebox-label");
        VBox.setMargin(routeLabel, new Insets(0, 0, 0, 25));
        HBox routeControlContainer = new HBox();
        routeControlContainer.setAlignment(Pos.CENTER);
        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("newnodebox-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        updateRouteSwitchList();
        routeSwitchBox.setValue("None");
        routeControlContainer.getChildren().add(routeSwitchBox);
        routeSection.getChildren().addAll(routeLabel, routeControlContainer);

        // CONNECTION TYPE Section.
        VBox connectionSection = new VBox(10);
        connectionSection.setAlignment(Pos.TOP_LEFT);
        Label connectionLabel = new Label("Connection Type:");
        connectionLabel.getStyleClass().add("newnodebox-label");
        VBox.setMargin(connectionLabel, new Insets(0, 0, 0, 25));
        HBox connectionControlContainer = new HBox();
        connectionControlContainer.setAlignment(Pos.CENTER);
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("newnodebox-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionControlContainer.getChildren().add(connectionTypeBox);
        connectionSection.getChildren().addAll(connectionLabel, connectionControlContainer);



        // Button container.
        VBox buttonSection = new VBox(10);
        buttonSection.setAlignment(Pos.CENTER);
        createButton = new Button("Create");
        VBox.setMargin(createButton, new Insets(20, 0, 0, 0));
        createButton.getStyleClass().add("newnodebox-create-button");
        createButton.setPrefWidth(150);
        createButton.setDisable(true);
        buttonSection.getChildren().addAll(createButton);

        // Assemble all sections into the content box.
        contentBox.getChildren().addAll(headerBox, deviceSection, nameSection, ipSection, networkSection, routeSection, connectionSection, buttonSection);

        contentBox.setAlignment(Pos.TOP_CENTER);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() != null) {
                layoutYProperty().bind(((Region) getParent()).heightProperty()
                        .subtract(prefHeightProperty())
                        .subtract(15));
            }
        });

        createButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String displayName = displayNameField.getText().trim();
            DeviceType deviceType = deviceTypeBox.getValue();
            NetworkType networkType = networkTypeBox.getValue();
            ConnectionType connectionType = connectionTypeBox.getValue();
            String routeSwitch = routeSwitchBox.getValue();
            if ("None".equals(routeSwitch)) {
                routeSwitch = "";
            }
            NetworkNode newNode = new NetworkNode(ip, displayName, deviceType, networkType);
            newNode.setConnectionType(connectionType);
            newNode.setRouteSwitch(routeSwitch);
            NetworkMonitorApp.addNewNode(newNode);
            collapse();

            
        });

        ipField.textProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        deviceTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        networkTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        connectionTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());
        routeSwitchBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCreateButtonState());

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
        // ――― Hover + Press effects ―――
        this.setOnMouseEntered(e -> {
            if (!expanded) {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
                st.setToX(1.05);
                st.setToY(1.05);
                st.play();
            }
        });
        this.setOnMouseExited(e -> {
            if (!expanded) {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            }
        });
        this.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
        });
        this.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
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
                && routeSwitchBox.getValue() != null;
        createButton.setDisable(!enable);
    }
    
    private void expand() {
    // Delay the expand animation by 100ms
    PauseTransition delay = new PauseTransition(Duration.millis(100));
    delay.setOnFinished(evt -> {
        updateRouteSwitchList();
        expanded = true;
        getStyleClass().remove("collapsed");
        if (!getStyleClass().contains("expanded")) {
            getStyleClass().add("expanded");
        }
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
    });
    delay.play();
}

    
    
    
    public void collapse() {
        expanded = false;
        if (!getStyleClass().contains("collapsed")) {
            getStyleClass().add("collapsed");
        }
        // Remove the "expanded" style class
        getStyleClass().remove("expanded");
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
