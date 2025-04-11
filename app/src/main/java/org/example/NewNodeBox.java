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
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NewNodeBox extends StackPane {
    private final double FIELD_WIDTH = 200;
    private final double WIDTH = 150;
    private final double MIN_HEIGHT = 50;
    private final double EXPANDED_HEIGHT = 550;
    private final double EXPANDED_WIDTH = 250;
    private final double NODE_BOX_GAP = 10;

    private boolean expanded = false;
    
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
    
    // Minimized state content.
    private Label minimizedLabel;
    
    // Expanded state content.
    private VBox contentBox;
    
    private TextField ipField;
    private TextField displayNameField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ComboBox<Color> nodeColorBox;
    
    private Button createButton;
    private Button cancelButton;
    
    public NewNodeBox() {
        setPrefWidth(WIDTH);
        setPrefHeight(MIN_HEIGHT);
        setMinWidth(WIDTH);
        setStyle(normalStyle);
        
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
        
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() != null) {
                layoutYProperty().bind(((Region)getParent()).heightProperty().subtract(prefHeightProperty()).subtract(15));
            }
        });
        
        // Updated minimized label.
        minimizedLabel = new Label("Add Node");
        minimizedLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);
        
        // Build expanded view.
        contentBox = new VBox(NODE_BOX_GAP);
        contentBox.setPadding(new Insets(NODE_BOX_GAP));
        contentBox.setAlignment(Pos.TOP_CENTER);
        
        // Header section.
        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Add Node");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        // Bottom border with color #b8d4f1 and padding.
        headerBox.setStyle("-fx-border-color: transparent transparent #b8d4f1 transparent; " +
                           "-fx-border-width: 0 0 0.5px 0; -fx-padding: 0 0 10px 0;");
        headerBox.getChildren().add(titleLabel);
        
        // HOSTNAME/IP Section.
        VBox ipSection = new VBox(NODE_BOX_GAP);
        Label ipLabel = new Label("HOSTNAME/IP");
        ipLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        ipField = new TextField();
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.setPromptText("");
        ipField.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                         "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                         "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        ipSection.getChildren().addAll(ipLabel, ipField);
        
        // DISPLAY NAME Section.
        VBox nameSection = new VBox(NODE_BOX_GAP);
        Label nameLabel = new Label("DISPLAY NAME");
        nameLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        displayNameField = new TextField();
        displayNameField.setPrefWidth(FIELD_WIDTH);
        displayNameField.setPromptText("");
        displayNameField.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                  "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                  "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        nameSection.getChildren().addAll(nameLabel, displayNameField);
        
        // DEVICE TYPE Section.
        VBox deviceSection = new VBox(NODE_BOX_GAP);
        Label deviceLabel = new Label("DEVICE TYPE");
        deviceLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setValue(DeviceType.COMPUTER);
        deviceTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                               "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                               "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        deviceSection.getChildren().addAll(deviceLabel, deviceTypeBox);
        
        // NETWORK TYPE Section.
        VBox networkSection = new VBox(NODE_BOX_GAP);
        Label networkLabel = new Label("NETWORK TYPE");
        networkLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);
        networkTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        networkSection.getChildren().addAll(networkLabel, networkTypeBox);
        
        // CONNECTION TYPE Section.
        VBox connectionSection = new VBox(NODE_BOX_GAP);
        Label connectionLabel = new Label("CONNECTION TYPE");
        connectionLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);
        connectionTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                   "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                   "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        connectionSection.getChildren().addAll(connectionLabel, connectionTypeBox);
        
        // NODE COLOUR Section.
        VBox colorSection = new VBox(NODE_BOX_GAP);
        Label colorLabel = new Label("NODE COLOUR");
        colorLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        nodeColorBox = new ComboBox<>();
        nodeColorBox.setPrefWidth(FIELD_WIDTH);
        nodeColorBox.getItems().addAll(
            Color.WHITE,
            Color.LIGHTGRAY,
            Color.BLUE,
            Color.GREEN,
            Color.RED
        );
        nodeColorBox.setValue(Color.web("#FFFFFF"));
        nodeColorBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                              "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                              "-fx-background-radius: 10; -fx-border-radius: 10; -fx-text-fill: white;");
        colorSection.getChildren().addAll(colorLabel, nodeColorBox);
        
        // Button container.
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonBox, new Insets(15, 0, 0, 0));
        
        createButton = new Button("Create");
        createButton.setDisable(true);
        createButton.setStyle("-fx-background-color: grey; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 14px;");
        buttonBox.getChildren().addAll(createButton, cancelButton);
        
        contentBox.getChildren().addAll(headerBox, ipSection, nameSection, deviceSection, networkSection, connectionSection, colorSection, buttonBox);
        
        ipField.textProperty().addListener((obs, oldVal, newVal) -> checkFields());
        displayNameField.textProperty().addListener((obs, oldVal, newVal) -> checkFields());
        
        createButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String name = displayNameField.getText().trim();
            DeviceType deviceType = deviceTypeBox.getValue();
            NetworkType networkType = networkTypeBox.getValue();
            ConnectionType connectionType = connectionTypeBox.getValue();
            Color color = nodeColorBox.getValue();
            
            NetworkNode newNode = new NetworkNode(ip, name, deviceType, networkType);
            newNode.setOutlineColor(String.format("#%02X%02X%02X",
                    (int)(color.getRed()*255),
                    (int)(color.getGreen()*255),
                    (int)(color.getBlue()*255)));
            newNode.setConnectionType(connectionType);
            
            NetworkMonitorApp.addNewNode(newNode);
            
            ipField.clear();
            displayNameField.clear();
            
            collapse();
        });
        
        cancelButton.setOnAction(e -> collapse());
        
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
    }
    
    private void checkFields() {
        boolean enable = !ipField.getText().trim().isEmpty() && !displayNameField.getText().trim().isEmpty();
        createButton.setDisable(!enable);
        if (enable) {
            createButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        } else {
            createButton.setStyle("-fx-background-color: grey; -fx-text-fill: white; -fx-font-size: 14px;");
        }
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
