package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class FilterBox extends StackPane {

    private final double FIELD_WIDTH = 200;
    private final double WIDTH = 150;
    private final double MIN_HEIGHT = 50;
    private final double EXPANDED_HEIGHT = 400;
    private final double EXPANDED_WIDTH = 250;
    private boolean expanded = false;
    
    // Styles to match NewNodeBox.
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

    // Collapsed state label.
    private Label minimizedLabel;

    // Expanded state content.
    private VBox contentBox;

    // Filtering controls.
    private TextField subnetField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ColorPicker nodeColorPicker;
    private Button applyFilterButton;
    private Button resetFilterButton;

    public FilterBox() {
        setPrefWidth(WIDTH);
        setPrefHeight(MIN_HEIGHT);
        setMinWidth(WIDTH);
        setStyle(normalStyle);

        // Add hover behavior.
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

        // Collapsed view.
        minimizedLabel = new Label("Filter Nodes");
        minimizedLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // Build expanded view.
        contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));
        contentBox.setAlignment(Pos.TOP_CENTER);

        // Subnet Section.
        VBox subnetSection = new VBox(10);
        Label subnetLabel = new Label("Subnet:");
        subnetLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        subnetField = new TextField();
        subnetField.setPrefWidth(FIELD_WIDTH);
        subnetField.setPromptText("e.g., 192.168.1.0/24");
        subnetField.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                             "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                             "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        subnetSection.getChildren().addAll(subnetLabel, subnetField);

        // Device Type Section.
        VBox deviceSection = new VBox(10);
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setValue(null); // No filter by default.
        deviceTypeBox.setPromptText("Any");
        deviceTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                               "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                               "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        deviceSection.getChildren().addAll(deviceLabel, deviceTypeBox);

        // Connection Type Section.
        VBox connectionSection = new VBox(10);
        Label connectionLabel = new Label("Connection Type:");
        connectionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(null); // No filter by default.
        connectionTypeBox.setPromptText("Any");
        connectionTypeBox.setStyle("-fx-font-size: 14px; -fx-background-color: #1b2433; " +
                                   "-fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                                   "-fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: white;");
        connectionSection.getChildren().addAll(connectionLabel, connectionTypeBox);

        // Node Colour Section.
        VBox colorSection = new VBox(10);
        Label colorLabel = new Label("Node Colour:");
        colorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        nodeColorPicker = new ColorPicker();
        nodeColorPicker.setPrefWidth(FIELD_WIDTH);
        colorSection.getChildren().addAll(colorLabel, nodeColorPicker);

        // Buttons Section.
        VBox buttonSection = new VBox(10);
        applyFilterButton = new Button("Apply Filter");
        applyFilterButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        resetFilterButton = new Button("Reset Filter");
        resetFilterButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 14px;");
        applyFilterButton.setOnAction(e -> applyFilters());
        resetFilterButton.setOnAction(e -> resetFilters());
        buttonSection.getChildren().addAll(applyFilterButton, resetFilterButton);
        buttonSection.setAlignment(Pos.CENTER);

        // Assemble all controls.
        contentBox.getChildren().addAll(subnetSection, deviceSection, connectionSection, colorSection, buttonSection);

        // Add the scene listener to anchor the bottom (similar to NewNodeBox fix).
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && getParent() != null) {
                // Bind layoutY so that the bottom is anchored: parent's height - prefHeight - margin.
                layoutYProperty().bind(((Region)getParent()).heightProperty()
                        .subtract(prefHeightProperty())
                        .subtract(15));
            }
        });
    }

    private void applyFilters() {
        // Filtering logic (this example simply prints the criteria).
        System.out.println("Applying filters:");
        System.out.println("Subnet: " + subnetField.getText().trim());
        System.out.println("Device Type: " + deviceTypeBox.getValue());
        System.out.println("Connection Type: " + connectionTypeBox.getValue());
        System.out.println("Node Colour: " + nodeColorPicker.getValue());
        // You may call a method in NetworkMonitorApp to update the nodes displayed.
    }

    private void resetFilters() {
        subnetField.clear();
        deviceTypeBox.setValue(null);
        connectionTypeBox.setValue(null);
        nodeColorPicker.setValue(Color.WHITE);
        System.out.println("Filters reset.");
        // Optionally, reapply filters to update the view.
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
    
    private void collapse() {
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

    public void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    {
        // Toggle expansion state when clicking the box (unless an inner control is the target).
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
}
