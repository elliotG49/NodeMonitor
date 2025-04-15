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
    private final double EXPANDED_HEIGHT = 650;
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

        // HOSTNAME/IP Section.
        VBox ipSection = new VBox(NODE_BOX_GAP);
        Label ipLabel = new Label("HOSTNAME/IP");
        ipLabel.getStyleClass().add("newnodebox-label");
        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.getStyleClass().add("newnodebox-textfield");
        ipSection.getChildren().addAll(ipLabel, ipField);

        // DISPLAY NAME Section.
        VBox nameSection = new VBox(NODE_BOX_GAP);
        Label nameLabel = new Label("DISPLAY NAME");
        nameLabel.getStyleClass().add("newnodebox-label");
        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.getStyleClass().add("newnodebox-textfield");
        nameSection.getChildren().addAll(nameLabel, displayNameField);

        // DEVICE TYPE Section.
        VBox deviceSection = new VBox(NODE_BOX_GAP);
        Label deviceLabel = new Label("DEVICE TYPE");
        deviceLabel.getStyleClass().add("newnodebox-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("newnodebox-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setValue(DeviceType.COMPUTER);
        deviceSection.getChildren().addAll(deviceLabel, deviceTypeBox);

        // NETWORK TYPE Section.
        VBox networkSection = new VBox(NODE_BOX_GAP);
        Label networkLabel = new Label("NETWORK TYPE");
        networkLabel.getStyleClass().add("newnodebox-label");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("newnodebox-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);
        networkSection.getChildren().addAll(networkLabel, networkTypeBox);

        // CONNECTION TYPE Section.
        VBox connectionSection = new VBox(NODE_BOX_GAP);
        Label connectionLabel = new Label("CONNECTION TYPE");
        connectionLabel.getStyleClass().add("newnodebox-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("newnodebox-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);
        connectionSection.getChildren().addAll(connectionLabel, connectionTypeBox);

        // ROUTE VIA SWITCH Section.
        VBox routeSection = new VBox(NODE_BOX_GAP);
        Label routeLabel = new Label("Network Route");
        routeLabel.getStyleClass().add("newnodebox-label");
        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("newnodebox-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        routeSwitchBox.getItems().add("None");
        routeSwitchBox.setValue("None");
        routeSection.getChildren().addAll(routeLabel, routeSwitchBox);

        // NODE COLOUR Section.
        VBox colorSection = new VBox(NODE_BOX_GAP);
        Label colorLabel = new Label("NODE COLOUR");
        colorLabel.getStyleClass().add("newnodebox-label");
        nodeColorBox = new ComboBox<>();
        nodeColorBox.getStyleClass().add("newnodebox-combobox");
        nodeColorBox.setPrefWidth(FIELD_WIDTH);
        // Assume rainbowColors is already populated with keys
        nodeColorBox.getItems().addAll(rainbowColors.keySet());
        nodeColorBox.setValue("White");
        colorSection.getChildren().addAll(colorLabel, nodeColorBox);

        // Button container.
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        createButton = new Button("Create");
        createButton.getStyleClass().add("newnodebox-create-button");
        createButton.setDisable(true);
        cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("newnodebox-cancel-button");
        cancelButton.setOnAction(e -> collapse()); // Added collapse action on cancel button
        buttonBox.getChildren().addAll(createButton, cancelButton);
        

        // Assemble all sections.
        contentBox.getChildren().addAll(headerBox, deviceSection, nameSection, ipSection, networkSection, routeSection, connectionSection, colorSection, buttonBox);
        // Add the expanded view as needed later.
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
        
        // NEW: Bind layoutY to position the box at the bottom.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() != null) {
                layoutYProperty().bind(((Region) getParent()).heightProperty()
                        .subtract(prefHeightProperty())
                        .subtract(15));
            }
        });
    }

    private void expand() {
        updateRouteSwitchList();
        expanded = true;
        // Remove the "collapsed" style class when expanded.
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

    private void updateRouteSwitchList() {
        routeSwitchBox.getItems().clear();
        routeSwitchBox.getItems().add("None");
        for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
            if (node.getDeviceType() == DeviceType.SWITCH) {
                routeSwitchBox.getItems().add(node.getDisplayName());
            }
        }
        if (!routeSwitchBox.getItems().contains(routeSwitchBox.getValue())) {
            routeSwitchBox.setValue("None");
        }
    }
}
