package org.example;

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
    private static final double FIELD_WIDTH = 200;
    private static final double GAP = 10;

    private final Label minimizedLabel = new Label("+");
    private final VBox contentBox = new VBox(GAP);
    private final Button createButton;
    private final TextField ipField;
    private final TextField displayNameField;
    private final ComboBox<DeviceType> deviceTypeBox;
    private final ComboBox<NetworkType> networkTypeBox;
    private final ComboBox<ConnectionType> connectionTypeBox;
    private final ComboBox<String> routeSwitchBox;

    private boolean expanded = false;

    public NewNodeBox() {
        // Allow dynamic sizing
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setMinHeight(Region.USE_COMPUTED_SIZE);
        setMaxHeight(Region.USE_COMPUTED_SIZE);
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        getStyleClass().add("newnodebox-panel");

        // Collapsed “+”
        minimizedLabel.getStyleClass().add("newnodebox-minimized-label");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // Expanded content
        contentBox.getStyleClass().add("newnodebox-content-box");
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setVisible(false);          // start hidden
        contentBox.managedProperty().bind(contentBox.visibleProperty());

        // Start in collapsed state
        getStyleClass().add("collapsed");

        // Build the form…

        // Header
        Label titleLabel = new Label("Add Node");
        titleLabel.getStyleClass().add("newnodebox-title-label");
        VBox headerBox = new VBox(titleLabel);
        headerBox.getStyleClass().add("newnodebox-header-box");

        // Device Type
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.getStyleClass().add("newnodebox-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("newnodebox-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        HBox deviceContainer = new HBox(deviceTypeBox);
        deviceContainer.setAlignment(Pos.CENTER);
        VBox deviceSection = new VBox(5, deviceLabel, deviceContainer);
        deviceSection.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(deviceLabel, new Insets(0,0,0,25));

        // Display Name
        Label nameLabel = new Label("Display Name:");
        nameLabel.getStyleClass().add("newnodebox-label");
        displayNameField = new TextField();
        displayNameField.setPromptText("eg PC1");
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.getStyleClass().add("newnodebox-textfield");
        HBox nameContainer = new HBox(displayNameField);
        nameContainer.setAlignment(Pos.CENTER);
        VBox nameSection = new VBox(5, nameLabel, nameContainer);
        nameSection.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(nameLabel, new Insets(0,0,0,25));

        // Hostname/IP
        Label ipLabel = new Label("Hostname/IP:");
        ipLabel.getStyleClass().add("newnodebox-label");
        ipField = new TextField();
        ipField.setPromptText("eg 192.168.1.1");
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.getStyleClass().add("newnodebox-textfield");
        HBox ipContainer = new HBox(ipField);
        ipContainer.setAlignment(Pos.CENTER);
        VBox ipSection = new VBox(5, ipLabel, ipContainer);
        ipSection.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(ipLabel, new Insets(0,0,0,25));

        // Network Type
        Label netLabel = new Label("Network Type:");
        netLabel.getStyleClass().add("newnodebox-label");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("newnodebox-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        HBox netContainer = new HBox(networkTypeBox);
        netContainer.setAlignment(Pos.CENTER);
        VBox netSection = new VBox(5, netLabel, netContainer);
        netSection.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(netLabel, new Insets(0,0,0,25));

        // Route via Switch
        Label routeLabel = new Label("Network Route:");
        routeLabel.getStyleClass().add("newnodebox-label");
        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("newnodebox-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        updateRouteSwitchList();
        routeSwitchBox.setValue("None");
        HBox routeContainer = new HBox(routeSwitchBox);
        routeContainer.setAlignment(Pos.CENTER);
        VBox routeSection = new VBox(5, routeLabel, routeContainer);
        routeSection.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(routeLabel, new Insets(0,0,0,25));

        // Connection Type
        Label connLabel = new Label("Connection Type:");
        connLabel.getStyleClass().add("newnodebox-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("newnodebox-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        HBox connContainer = new HBox(connectionTypeBox);
        connContainer.setAlignment(Pos.CENTER);
        VBox connSection = new VBox(5, connLabel, connContainer);
        connSection.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(connLabel, new Insets(0,0,0,25));

        // Create button
        createButton = new Button("Create");
        createButton.getStyleClass().add("newnodebox-create-button");
        createButton.setPrefWidth(150);
        createButton.setDisable(true);
        VBox buttonSection = new VBox(10, createButton);
        buttonSection.setAlignment(Pos.CENTER);
        VBox.setMargin(createButton, new Insets(20,0,0,0));

        // Assemble
        contentBox.getChildren().addAll(
            headerBox,
            deviceSection,
            nameSection,
            ipSection,
            netSection,
            routeSection,
            connSection,
            buttonSection
        );
        getChildren().add(contentBox);

        // Enable Create
        ipField.textProperty().addListener((o,a,b)-> updateCreateButtonState());
        displayNameField.textProperty().addListener((o,a,b)-> updateCreateButtonState());
        deviceTypeBox.valueProperty().addListener((o,a,b)-> updateCreateButtonState());
        networkTypeBox.valueProperty().addListener((o,a,b)-> updateCreateButtonState());
        connectionTypeBox.valueProperty().addListener((o,a,b)-> updateCreateButtonState());
        routeSwitchBox.valueProperty().addListener((o,a,b)-> updateCreateButtonState());

        // Create action
        createButton.setOnAction(e -> {
            String ip    = ipField.getText().trim();
            String name  = displayNameField.getText().trim();
            DeviceType dt= deviceTypeBox.getValue();
            NetworkType nt= networkTypeBox.getValue();
            ConnectionType ct= connectionTypeBox.getValue();
            String route = "None".equals(routeSwitchBox.getValue()) ? "" : routeSwitchBox.getValue();

            NetworkNode newNode = new NetworkNode(ip, name, dt, nt);
            newNode.setConnectionType(ct);
            newNode.setRouteSwitch(route);
            NetworkMonitorApp.addNewNode(newNode);
            collapse();
        });

        // Toggle expand/collapse
        setOnMouseClicked(e -> toggle());
        setOnKeyPressed(e -> {
            if (expanded && e.getCode() == KeyCode.ESCAPE) collapse();
        });

        // Init size for “+”
        minimizedLabel.applyCss();
        minimizedLabel.layout();
        setPrefWidth(minimizedLabel.prefWidth(-1));
        setPrefHeight(minimizedLabel.prefHeight(-1));

        // Pin to bottom and enforce initial collapse
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() instanceof Region) {
                // 1) Collapse immediately so the CSS classes & sizing are correct
                collapse();
                // 2) Bind vertical position to actual height
                layoutYProperty().bind(
                    ((Region)getParent()).heightProperty()
                        .subtract(heightProperty())
                        .subtract(15)
                );
            }
        });
    }

    public void expand() {
        if (expanded) return;
        expanded = true;

        getStyleClass().remove("collapsed");
        if (!getStyleClass().contains("expanded")) {
            getStyleClass().add("expanded");
        }

        updateRouteSwitchList();
        getChildren().remove(minimizedLabel);
        if (!getChildren().contains(contentBox)) {
            getChildren().add(contentBox);
        }
        contentBox.setVisible(true);

        contentBox.applyCss();
        contentBox.layout();

        double targetH = contentBox.prefHeight(-1);
        double targetW = contentBox.prefWidth(-1);

        new Timeline(
          new KeyFrame(Duration.ZERO,
            new KeyValue(prefHeightProperty(), getHeight()),
            new KeyValue(prefWidthProperty(),  getWidth())
          ),
          new KeyFrame(Duration.millis(200),
            new KeyValue(prefHeightProperty(), targetH),
            new KeyValue(prefWidthProperty(),  targetW)
          )
        ).play();
    }

    public void collapse() {
        if (!expanded) {
            // Ensure collapsed size if never expanded
            minimizedLabel.applyCss();
            minimizedLabel.layout();
            setPrefWidth(minimizedLabel.prefWidth(-1));
            setPrefHeight(minimizedLabel.prefHeight(-1));
            contentBox.setVisible(false);
            return;
        }
        expanded = false;

        getStyleClass().remove("expanded");
        if (!getStyleClass().contains("collapsed")) {
            getStyleClass().add("collapsed");
        }

        minimizedLabel.applyCss();
        minimizedLabel.layout();
        double minH = minimizedLabel.prefHeight(-1);
        double minW = minimizedLabel.prefWidth(-1);

        Timeline tl = new Timeline(
          new KeyFrame(Duration.ZERO,
            new KeyValue(prefHeightProperty(), getHeight()),
            new KeyValue(prefWidthProperty(),  getWidth())
          ),
          new KeyFrame(Duration.millis(200),
            new KeyValue(prefHeightProperty(), minH),
            new KeyValue(prefWidthProperty(), minW)
          )
        );
        tl.setOnFinished(e -> {
            getChildren().remove(contentBox);
            contentBox.setVisible(false);
            if (!getChildren().contains(minimizedLabel)) {
                getChildren().add(minimizedLabel);
            }
        });
        tl.play();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void toggle() {
        if (expanded) collapse();
        else expand();
    }

    private void updateRouteSwitchList() {
        routeSwitchBox.getItems().setAll("None");
        for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
            if (n.getDeviceType() == DeviceType.SWITCH) {
                routeSwitchBox.getItems().add(n.getDisplayName());
            }
        }
    }

    private void updateCreateButtonState() {
        boolean ok =
             !ipField.getText().trim().isEmpty()
          && !displayNameField.getText().trim().isEmpty()
          && deviceTypeBox.getValue()     != null
          && networkTypeBox.getValue()    != null
          && connectionTypeBox.getValue() != null
          && routeSwitchBox.getValue()    != null;
        createButton.setDisable(!ok);
    }
}
