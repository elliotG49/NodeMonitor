package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class FunctionsBox extends StackPane {

    private final double MIN_WIDTH = 150;
    private final double MIN_HEIGHT = 50;
    private final double EXPANDED_WIDTH = 250;
    private final double EXPANDED_HEIGHT = 150; // Expanded height to accommodate options
    private boolean expanded = false;
    
    // Styles to match the other boxes.
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
    // Expanded state content (for function options).
    private VBox contentBox;
    
    public FunctionsBox() {
        setPrefWidth(MIN_WIDTH);
        setPrefHeight(MIN_HEIGHT);
        setMinWidth(MIN_WIDTH);
        setStyle(normalStyle);
        
        // Collapsed view: display a label.
        minimizedLabel = new Label("Functions");
        minimizedLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);
        
        // Build expanded view: a VBox containing two option buttons.
        contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));
        contentBox.setAlignment(Pos.CENTER);
        
        Button portscanButton = new Button("Portscan");
        portscanButton.setPrefWidth(100);
        portscanButton.setOnAction(e -> {
            System.out.println("Portscan selected");
            // TODO: Open portscan configuration options.
        });
        
        Button tracerouteButton = new Button("Traceroute");
        tracerouteButton.setPrefWidth(100);
        tracerouteButton.setOnAction(e -> {
            System.out.println("Traceroute selected");
            // TODO: Open traceroute configuration options.
        });
        
        contentBox.getChildren().addAll(portscanButton, tracerouteButton);
        
        // Toggle expansion on mouse click.
        setOnMouseClicked(e -> {
            toggle();
            e.consume();
        });
        
        // Allow collapsing when ESC is pressed.
        setOnKeyPressed(e -> {
            if (expanded && e.getCode() == KeyCode.ESCAPE) {
                collapse();
            }
        });
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
            requestFocus();
        });
        expandTimeline.play();
    }
    
    public void collapse() {
        expanded = false;
        getChildren().remove(contentBox);
        Timeline collapseTimeline = new Timeline();
        KeyValue kvHeight = new KeyValue(prefHeightProperty(), MIN_HEIGHT);
        KeyValue kvWidth = new KeyValue(prefWidthProperty(), MIN_WIDTH);
        KeyFrame kf = new KeyFrame(Duration.millis(200), kvHeight, kvWidth);
        collapseTimeline.getKeyFrames().add(kf);
        collapseTimeline.setOnFinished(e -> {
            if (!getChildren().contains(minimizedLabel)) {
                getChildren().add(minimizedLabel);
            }
        });
        collapseTimeline.play();
    }
    
    public void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }
    
    public boolean isExpanded() {
        return expanded;
    }
}
