package org.example.ui.panels;

import java.net.InetAddress; // Add this import statement
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.example.app.NetworkMonitorApp;
import org.example.model.NetworkNode;
import org.example.service.TracerouteTask;
import org.example.ui.components.TracerouteHop;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class TraceroutePanel extends VBox {
    private final NetworkNode targetNode;
    private final List<TracerouteHop> hopNodes;
    private final int MAX_HOPS = 15; // Changed to 15
    private Pane tracerouteContainer;
    private ScrollPane scrollPane;
    private List<Line> connectorLines;
    private Button closeButton;
    private Label statusLabel; // New status label
    private AtomicReference<TracerouteTask> activeTaskRef = new AtomicReference<>();
    
    public TraceroutePanel(NetworkNode targetNode) {
        this.targetNode = targetNode;
        this.hopNodes = new ArrayList<>(MAX_HOPS);
        this.connectorLines = new ArrayList<>();
        
        // Main container with 10px padding
        setPadding(new Insets(5));
        setSpacing(0);
        
        // Create a container for the traceroute path
        tracerouteContainer = new Pane();
        tracerouteContainer.setPrefWidth(170); // 200px - 10px padding on each side
        
        // Wrap in scrollpane that will only show scrollbar if needed
        scrollPane = new ScrollPane(tracerouteContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("traceroute-scrollpane");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        // Create status label
        statusLabel = new Label("IN PROGRESS");
        statusLabel.getStyleClass().add("traceroute-status-label");
        
        // Create Close & Stop button
        closeButton = createCloseButton();
        
        // Add components
        getChildren().addAll(scrollPane, statusLabel, closeButton);
        
        // Set margin for status label
        VBox.setMargin(statusLabel, new Insets(10, 5, 5, 5));
        
        // Listen to scene height changes to adjust spacing
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.heightProperty().addListener((h, old, newH) -> adjustHopSpacing());
            }
        });
        
        // Add ESC key handler
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeTraceRoute();
                e.consume();
            }
        });
        
        // Initialize with empty hops - will be properly sized once we know the panel height
        initializeEmptyHops();
        
        // Add this in the constructor after setting up the scene property listener
        heightProperty().addListener((obs, oldVal, newVal) -> {
            // Only adjust if height meaningfully changes
            if (Math.abs(oldVal.doubleValue() - newVal.doubleValue()) > 5) {
                Platform.runLater(this::adjustHopSpacing);
            }
        });
    }
    
    private Button createCloseButton() {
        Button button = new Button("Close & Stop");
        button.getStyleClass().add("traceroute-close-button");
        button.setPrefHeight(40);
        button.setPrefWidth(Double.MAX_VALUE);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER);
        
        // Set margin to give some space
        VBox.setMargin(button, new Insets(5, 5, 5, 5));
        
        button.setOnAction(e -> closeTraceRoute());
        
        return button;
    }
    
    private void closeTraceRoute() {
        // Stop any active traceroute task
        TracerouteTask task = activeTaskRef.get();
        if (task != null) {
            task.cancel();
        }
        
        // Close the panel
        RightSlidePanel panel = findParentPanel();
        if (panel != null) {
            panel.hide();
        }
    }
    
    private RightSlidePanel findParentPanel() {
        javafx.scene.Node current = this;
        while (current != null) {
            if (current instanceof RightSlidePanel) {
                return (RightSlidePanel) current;
            }
            current = current.getParent();
        }
        return null;
    }
    
    private void initializeEmptyHops() {
        // Clear any existing hops and lines
        tracerouteContainer.getChildren().clear();
        hopNodes.clear();
        connectorLines.clear();
        
        // First add all connector lines (to ensure they're behind the nodes)
        for (int i = 0; i < MAX_HOPS - 1; i++) {
            // Create connector line (initially positioned, will be adjusted later)
            Line connectingLine = new Line();
            connectingLine.setStroke(Color.gray(0.5));
            connectingLine.setStrokeWidth(2);
            
            // Add line to the container and our list
            tracerouteContainer.getChildren().add(connectingLine);
            connectorLines.add(connectingLine);
        }
        
        // Then add all hop nodes on top of the lines
        for (int i = 0; i < MAX_HOPS; i++) {
            // Start hop numbering at 1, not 0
            boolean isFirst = (i == 0);
            boolean isLast = (i == MAX_HOPS - 1);
            
            // Use i+1 for hop number to start at 1
            TracerouteHop hop = new TracerouteHop(i+1, isFirst, isLast);
            hop.setLayoutX(10); // Indent from left edge
            hop.setLayoutY(i * 30); // Initial spacing
            
            // Add the hop node to the container (on top of the lines)
            tracerouteContainer.getChildren().add(hop);
            hopNodes.add(hop);
            
            // Connect the outgoing line (except for the last hop)
            if (i < MAX_HOPS - 1) {
                hop.setConnectorLine(connectorLines.get(i));
            }
        }
        
        // Set initial height to ensure all nodes are visible
        tracerouteContainer.setPrefHeight(MAX_HOPS * 30 + 20); // Add extra padding
        
        // Schedule multiple layout passes with increasing delays to ensure proper sizing
        Platform.runLater(() -> {
            tracerouteContainer.applyCss();
            tracerouteContainer.layout();
            
            // First adjustment
            adjustHopSpacing();
            
            // Second adjustment after a short delay
            PauseTransition delay = new PauseTransition(
                Duration.millis(50));
            delay.setOnFinished(e -> {
                adjustHopSpacing();
                
                // Final adjustment after everything has settled
                PauseTransition finalDelay = new PauseTransition(
                    Duration.millis(200));
                finalDelay.setOnFinished(f -> adjustHopSpacing());
                finalDelay.play();
            });
            delay.play();
        });
    }
    
    private void adjustHopSpacing() {
        if (getScene() == null) return;
        
        // Get the available height for the panel (minus the button height, status label and padding)
        double availableHeight = getHeight() - 40 - closeButton.getHeight() - statusLabel.getHeight() - 40;
        
        // Early exit if panel hasn't been properly sized yet
        if (availableHeight <= 0) {
            // Schedule another adjustment attempt
            Platform.runLater(this::adjustHopSpacing);
            return;
        }
        
        // Calculate equal spacing between nodes
        double nodeHeight = 30; // Height of each hop
        double totalNodeSpace = nodeHeight * MAX_HOPS;
        double remainingSpace = Math.max(0, availableHeight - totalNodeSpace);
        double spaceBetweenNodes = remainingSpace / (MAX_HOPS - 1);
        
        // Calculate total height needed
        double totalContentHeight = (nodeHeight * MAX_HOPS) + (spaceBetweenNodes * (MAX_HOPS - 1));
        tracerouteContainer.setPrefHeight(totalContentHeight + 20); // Add extra padding at the bottom
        
        // Position each hop with equal spacing
        for (int i = 0; i < hopNodes.size(); i++) {
            TracerouteHop hop = hopNodes.get(i);
            // Position = (node height + spacing) * index
            double yPos = (nodeHeight + spaceBetweenNodes) * i;
            hop.setLayoutY(yPos);
        }
        
        // Now adjust all the connector lines to connect the centers of the circles
        for (int i = 0; i < connectorLines.size(); i++) {
            TracerouteHop topHop = hopNodes.get(i);
            TracerouteHop bottomHop = hopNodes.get(i + 1);
            Line line = connectorLines.get(i);
            
            // Get the exact center of the circle inside the StackPane
            double circleX = topHop.getLayoutX() + CIRCLE_SIZE + 2; // Added 2px offset as requested
            
            // Calculate vertical positions of circle centers
            double topY = topHop.getLayoutY() + 15; // Center of the circle vertically (30/2)
            double bottomY = bottomHop.getLayoutY() + 15; // Center of the circle vertically (30/2)
            
            // Set line positions to connect circle centers
            // For the first line specifically, adjust to not show above the first node
            if (i == 0) {
                // Start exactly at the center of the first circle, not above it
                line.setStartY(topY);
            } else {
                line.setStartY(topY);
            }
            
            line.setStartX(circleX);
            line.setEndX(circleX);
            line.setEndY(bottomY);
        }
    }
    
    public void setTracerouteData(List<String> hopData) {
        Platform.runLater(() -> {
            // Loop through the data we have so far
            for (int i = 0; i < Math.min(hopData.size(), MAX_HOPS); i++) {
                String ipAddress = hopData.get(i);
                NetworkNode matchingNode = findMatchingNode(ipAddress);
                TracerouteHop hop = hopNodes.get(i);
                
                // Check if this is a timeout node
                boolean isTimeout = "Timeout".equals(ipAddress);
                
                // Check if this is the target node - ONLY the actual target node is green
                boolean isTargetNode = (ipAddress != null && ipAddress.equals(targetNode.getIpOrHostname()));
                
                // Activate the hop with the data
                hop.activate(ipAddress, matchingNode, isTargetNode, isTimeout);
                
                // If not the last hop, also activate the connecting line
                if (i < hopData.size() - 1 && i < MAX_HOPS - 1) {
                    // Always use white for connection lines, regardless of timeout status
                    Color lineColor = Color.WHITE;
                    
                    hop.animateConnectorLine(lineColor);
                }
            }
        });
    }
    
    public void setTracerouteTask(TracerouteTask task) {
        activeTaskRef.set(task);
        
        // Add a listener to update status when tracing is done
        task.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (task.isCompleted()) {
                Platform.runLater(() -> {
                    if (task.isTargetFound()) {
                        statusLabel.setText("COMPLETED");
                        statusLabel.getStyleClass().removeAll("traceroute-status-inprogress", "traceroute-status-failed");
                        statusLabel.getStyleClass().add("traceroute-status-completed");
                    } else if (newVal.size() >= MAX_HOPS) {
                        statusLabel.setText("FAILED: Too Many Hops");
                        statusLabel.getStyleClass().removeAll("traceroute-status-inprogress", "traceroute-status-completed");
                        statusLabel.getStyleClass().add("traceroute-status-failed");
                    }
                });
            }
        });
        
        // Initialize status as "in progress"
        statusLabel.setText("IN PROGRESS");
        statusLabel.getStyleClass().removeAll("traceroute-status-completed", "traceroute-status-failed");
        statusLabel.getStyleClass().add("traceroute-status-inprogress");
    }
    
    // Find a node in the application that matches this IP address
    private NetworkNode findMatchingNode(String ipAddress) {
        // Special handling for Timeout
        if ("Timeout".equals(ipAddress)) {
            return null;
        }
        
        try {
            // Try to resolve the IP address (important for hostname comparisons)
            String resolvedIp = ipAddress;
            try {
                resolvedIp = InetAddress.getByName(ipAddress).getHostAddress();
            } catch (Exception e) {
                // Keep original IP if resolution fails
            }
            
            // First try exact match with IP or hostname
            for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
                // Try to match by IP directly
                if (node.getIpOrHostname().equals(ipAddress)) {
                    return node;
                }
                
                // Try to match by resolved IP
                try {
                    String nodeIp = InetAddress.getByName(node.getIpOrHostname()).getHostAddress();
                    if (nodeIp.equals(resolvedIp)) {
                        return node;
                    }
                } catch (Exception e) {
                    // Skip if we can't resolve
                }
                
                // Additional check - try to match by display name
                if (node.getDisplayName().equalsIgnoreCase(ipAddress) ||
                    ipAddress.contains(node.getDisplayName()) ||
                    node.getDisplayName().contains(ipAddress)) {
                    return node;
                }
            }
        } catch (Exception e) {
            // Catch any unexpected errors in the matching process
        }
        
        // No match found
        return null;
    }
    
    // Add a constant for the circle size
    private static final double CIRCLE_SIZE = 12;
}