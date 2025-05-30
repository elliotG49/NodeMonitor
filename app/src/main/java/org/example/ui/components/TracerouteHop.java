package org.example.ui.components;

import org.example.model.NetworkNode;

import javafx.animation.StrokeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class TracerouteHop extends HBox {
    private static final double CIRCLE_SIZE = 12;
    private final Circle circle;
    private final Label hopNumberLabel;
    private final Label ipAddressLabel;
    private final Label latencyLabel;
    private String ipAddress;
    private int hopNumber;
    private NetworkNode matchingNode;
    private boolean isActive = false;
    private boolean isFirstOrLast = false;
    private boolean isDestination = false;
    private Line connectorLine; // Connection line to previous hop
    
    public TracerouteHop(int hopNumber, boolean isFirst, boolean isLast) {
        this.hopNumber = hopNumber;
        this.isFirstOrLast = isFirst;
        
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(5, 5, 5, 0));
        setPrefHeight(30);
        
        // Circle with initial gray state
        circle = new Circle(CIRCLE_SIZE);
        circle.setFill(Color.web("#222E3C"));
        circle.setStroke(Color.gray(0.5));
        circle.setStrokeWidth(2);
        
        // Create VBox for the text details with tighter spacing
        VBox textDetailsBox = new VBox(0); // Reduced spacing between labels to 0
        textDetailsBox.setAlignment(Pos.CENTER_LEFT);
        
        // Hop number label - white and bold, initially hidden
        hopNumberLabel = new Label("Hop " + hopNumber);
        hopNumberLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px;"
        );
        hopNumberLabel.setVisible(false); // Initially hidden until node is found
        hopNumberLabel.setManaged(false); // Don't reserve space for it
        
        // IP address label - light gray (C2C2C2) and regular weight
        ipAddressLabel = new Label("");
        ipAddressLabel.setStyle(
            "-fx-text-fill: #C2C2C2; " +
            "-fx-font-size: 11px;"
        );
        
        // Latency label - white with 50% opacity
        latencyLabel = new Label("");
        latencyLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-opacity: 0.5; " +
            "-fx-font-size: 10px;"
        );
        
        // Add labels to the VBox
        textDetailsBox.getChildren().addAll(hopNumberLabel, ipAddressLabel, latencyLabel);
        
        // Stack for positioning the circle
        StackPane circleContainer = new StackPane(circle);
        circleContainer.setMinWidth(2 * CIRCLE_SIZE + 4);
        circleContainer.setMinHeight(2 * CIRCLE_SIZE + 4);
        circleContainer.setAlignment(Pos.CENTER);
        
        // Add the components
        getChildren().addAll(circleContainer, textDetailsBox);
        
        // If this is first hop, apply special styling without glow
        if (isFirst) {
            circle.setStroke(Color.web("#00FF00"));
        }
        
        // Add hover effects
        setupHoverEffects(circleContainer, textDetailsBox);
    }
    
    public void activate(String ipAddress, NetworkNode matchingNode, boolean isTargetNode, boolean isTimeout) {
        this.ipAddress = ipAddress;
        this.matchingNode = matchingNode;
        this.isActive = true;
        this.isDestination = isTargetNode;
        
        // Only show hop number label if node is found
        hopNumberLabel.setVisible(true);
        hopNumberLabel.setManaged(true);
        
        // Update IP label text
        if (isTimeout) {
            ipAddressLabel.setText("Timeout");
            latencyLabel.setText("N/A");
        } else {
            ipAddressLabel.setText(ipAddress);
            // Set hostname/device name in latency field if we have a matching node
            if (matchingNode != null) {
                latencyLabel.setText(matchingNode.getDisplayName());
            } else {
                latencyLabel.setText("Unknown Node");
            }
        }
        
        // Determine color based on hop type
        Color strokeColor;
        
        if (isTimeout) {
            strokeColor = Color.RED;
        } else if (isTargetNode) {
            strokeColor = Color.web("#00FF00"); // Green for target node
        } else {
            strokeColor = Color.WHITE; // White for standard nodes (changed from #00EBFF)
        }
        
        // Create improved glow effect with the same color
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setColor(strokeColor);
        glow.setWidth(CIRCLE_SIZE * 2);
        glow.setHeight(CIRCLE_SIZE * 2);
        glow.setRadius(9);
        glow.setSpread(0.2);
        circle.setEffect(glow);
        
        // Animate the circle stroke
        StrokeTransition strokeTransition = new StrokeTransition(
            Duration.millis(500),
            circle,
            Color.gray(0.5),
            strokeColor
        );
        
        // Play the animation
        strokeTransition.play();
    }
    
    // Update the old activate method to use the new one
    public void activate(String ipAddress, NetworkNode matchingNode) {
        activate(ipAddress, matchingNode, false, false);
    }
    
    // Overload for backward compatibility
    public void activate(String ipAddress, NetworkNode matchingNode, boolean isTargetNode) {
        activate(ipAddress, matchingNode, isTargetNode, false);
    }
    
    public void setConnectorLine(Line line) {
        this.connectorLine = line;
    }
    
    public Line getConnectorLine() {
        return connectorLine;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getHopNumber() {
        return hopNumber;
    }
    
    public NetworkNode getMatchingNode() {
        return matchingNode;
    }
    
    public boolean isDestination() {
        return isDestination;
    }
    
    // Add a new method to animate just the connector line
    public void animateConnectorLine(Color color) {
        if (connectorLine != null) {
            StrokeTransition lineStrokeTransition = new StrokeTransition(
                Duration.millis(500),
                connectorLine,
                Color.gray(0.5), // Start with the default gray color
                color
            );
            lineStrokeTransition.play();
        }
    }
    
    // Add this new method to set up hover effects
    private void setupHoverEffects(StackPane circleContainer, VBox textDetailsBox) {
        // Create hover effect transitions for the circle container
        javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(
            Duration.millis(100), circleContainer);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);
        
        javafx.animation.ScaleTransition scaleDown = new javafx.animation.ScaleTransition(
            Duration.millis(100), circleContainer);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        // Create hover effect for the text box - brightness adjustment
        javafx.scene.effect.ColorAdjust brighten = new javafx.scene.effect.ColorAdjust();
        brighten.setBrightness(0.0); // Start with normal brightness
        textDetailsBox.setEffect(brighten);
        
        javafx.animation.Timeline brightenTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO, 
                new javafx.animation.KeyValue(brighten.brightnessProperty(), 0.0)),
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(brighten.brightnessProperty(), 0.5))
        );
        
        javafx.animation.Timeline dimTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO, 
                new javafx.animation.KeyValue(brighten.brightnessProperty(), 0.5)),
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(brighten.brightnessProperty(), 0.0))
        );
        
        // Set mouse event handlers for hover effects
        setOnMouseEntered(e -> {
            scaleUp.play();
            brightenTimeline.play();
            setCursor(javafx.scene.Cursor.HAND);
        });
        
        setOnMouseExited(e -> {
            scaleDown.play();
            dimTimeline.play();
            setCursor(javafx.scene.Cursor.DEFAULT);
        });
    }
}