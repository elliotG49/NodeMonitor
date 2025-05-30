package org.example.ui.panels;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import org.example.app.NetworkMonitorApp;
import org.example.config.NodeDetailFieldConfig;
import org.example.config.NodeDetailPanelConfig;
import org.example.model.ConnectionType;
import org.example.model.DeviceField;
import org.example.model.DeviceType;
import org.example.model.FieldSection;
import org.example.model.NetworkLocation;
import org.example.model.NetworkNode;
import org.example.ui.components.ExpandableSection;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Panel that displays details about a selected network node.
 * Slides in from the right side of the application when a node is clicked.
 */
public class NodeDetailPanel extends VBox {
    private static final double PANEL_WIDTH = 250;
    private final Timeline showTimeline;
    private final Timeline hideTimeline;
    private NetworkNode currentNode;
    private Text statusValue;
    private Text nodeNameText; // New field for node name
    
    // Add a field to store the contentBox
    private VBox contentBox;
    
    private NetworkNode previousNode = null; // Add this field to track the previously shown node
    
    public NodeDetailPanel() {
        // Create a content VBox to hold all panel contents
        contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20, 10, 15, 10));
        contentBox.getStyleClass().add("node-detail-content");
        
        // Create a ScrollPane to make the content scrollable
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("detail-scrollpane");
        
        // Basic panel setup
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH); // Force exact width
        getStyleClass().add("node-detail-panel");
        setStyle("-fx-background-color: -panels-bg-color;");
        
        // Set up animations
        showTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(translateXProperty(), PANEL_WIDTH)), // Use PANEL_WIDTH directly here
            new KeyFrame(Duration.millis(250), 
                new KeyValue(translateXProperty(), 0))
        );
        
        hideTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(translateXProperty(), 0)),
            new KeyFrame(Duration.millis(250), 
                new KeyValue(translateXProperty(), PANEL_WIDTH)) // Use PANEL_WIDTH directly here
        );
        
        // Initially hidden - move the panel completely off-screen
        setTranslateX(PANEL_WIDTH); // Use PANEL_WIDTH directly here
        
        // Add the ScrollPane to the main panel
        getChildren().add(scrollPane);
        
        // Make sure the panel takes full height
        heightProperty().addListener((obs, oldVal, newVal) -> {
            if (getScene() != null) {
                setPrefHeight(getScene().getHeight());
            }
        });
        
        // Initialize ESC key handler
        initializeEscHandler();
    }
    
    /**
     * Display the panel with details for the given node
     */
    public void showForNode(NetworkNode node) {
        
        // Set new node as current
        this.currentNode = node;
        this.previousNode = node;
        // Remove highlight call
        // node.setHighlighted(true);
        
        // Check for connections to this node
        checkConnectionStatus(node);
        
        // Show detailed fields for this node
        showNodeDetails(node);
        
        // Make sure we're taking full height of the scene
        if (getScene() != null) {
            setPrefHeight(getScene().getHeight());
        }
        
        // Play the show animation
        showTimeline.play();
        
        // Request focus to enable keyboard events (ESC key)
        requestFocus();
    }
    
    /**
     * Check connections related to this node to determine if it's online
     */
    private void checkConnectionStatus(NetworkNode node) {
        // Show "N/A" status for specific node types that shouldn't be pinged
        if (node.getNetworkLocation() == NetworkLocation.REMOTE_PRIVATE || 
            node.getDeviceType() == DeviceType.UNMANAGED_SWITCH) {
            Platform.runLater(() -> {
                // Display N/A for these special cases
                statusValue.setText("N/A");
                statusValue.setFill(Color.GRAY); // Gray color for N/A status
            });
            return;
        }
        
        // Set initial status to UNKNOWN while checking
        Platform.runLater(() -> {
            statusValue.setText("CHECKING...");
            statusValue.setFill(Color.YELLOW);
        });
        
        // Force the node to ping and update its status
        new Thread(() -> {
            try {
                // Get the IP or hostname
                String ipOrHostname = node.getIpOrHostname();
                
                // Ping the node directly
                java.net.InetAddress destAddr = java.net.InetAddress.getByName(ipOrHostname);
                boolean reachable = destAddr.isReachable(1000);
                
                // Update the node status
                node.setConnected(reachable);
                
                // Then update the UI from the JavaFX thread
                Platform.runLater(() -> {
                    updateConnectionStatus(reachable);
                });
            } catch (Exception e) {
                // If ping fails, mark as offline
                Platform.runLater(() -> {
                    updateConnectionStatus(false);
                });
            }
        }).start();
    }
    
    /**
     * Update the connection status display
     */
    private void updateConnectionStatus(boolean isConnected) {
        // If current node is a special case, don't update the status
        if (currentNode != null && 
            (currentNode.getNetworkLocation() == NetworkLocation.REMOTE_PRIVATE || 
             currentNode.getDeviceType() == DeviceType.UNMANAGED_SWITCH)) {
            return;
        }
        
        if (isConnected) {
            statusValue.setText("ONLINE");
            statusValue.setFill(Color.web("#00FF88")); // Bright green for online
        } else {
            statusValue.setText("OFFLINE");
            statusValue.setFill(Color.web("#ff3b30")); // Bright red for offline
        }
    }
    
    /**
     * Hide the panel
     */
    public void hide() {
        // Remove highlight code
        // if (currentNode != null) {
        //     currentNode.setHighlighted(false);
        // }
        
        hideTimeline.play();
    }
    
    /**
     * Get the currently displayed node
     */
    public NetworkNode getCurrentNode() {
        return currentNode;
    }
    
    private void showNodeDetails(NetworkNode node) {
        // Clear any existing detail fields
        contentBox.getChildren().clear();
        
        // Create a VBox for the status section
        VBox statusSection = new VBox(8);
        statusSection.getStyleClass().add("detail-section");
        
        // Create an HBox to hold status label and value on the same line
        javafx.scene.layout.HBox statusRow = new javafx.scene.layout.HBox(10); // 10px spacing between label and value
        statusRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Add "Status" label (now in white)
        Text statusHeader = new Text("STATUS:");
        statusHeader.getStyleClass().add("status-label");
        statusHeader.setFill(Color.WHITE);
        statusRow.getChildren().add(statusHeader);
        
        // Status value
        statusValue = new Text("UNKNOWN");
        statusValue.getStyleClass().add("nodedetail-status-value");
        statusValue.setFill(Color.WHITE);
        statusRow.getChildren().add(statusValue);
        
        // Add the status row to the section
        statusSection.getChildren().add(statusRow);
        
        // Add components to the panel
        contentBox.getChildren().add(statusSection);
        
        DeviceType deviceType = node.getDeviceType();
        
        // Get sections for this device type from config
        List<FieldSection> sections = NodeDetailPanelConfig.getSectionsForDevice(deviceType);
        
        // Create each section
        for (FieldSection section : sections) {
            // Create an expandable section
            // Only expand BASIC_INFO by default
            boolean expandByDefault = section == FieldSection.NODE_BASIC_INFORMATION;
            
            ExpandableSection sectionBox = new ExpandableSection(
                formatSectionName(section.name()), 
                expandByDefault
            );
            
            // Get fields for this section
            List<NodeDetailFieldConfig> fields = NodeDetailPanelConfig.getFieldsForSection(deviceType, section);
            
            // Add each field to the section
            for (NodeDetailFieldConfig fieldConfig : fields) {
                // Get the field value from the node
                String value = getNodeFieldValue(node, fieldConfig.getField());
                
                // Create the field container
                VBox fieldBox = createDetailField(
                    fieldConfig.getLabel(), 
                    value, 
                    fieldConfig.isEditable()
                );
                
                // Add it to the section
                sectionBox.addContent(fieldBox);
            }
            
            // Only add the section if it has content
            if (sectionBox.getChildren().size() > 0) {
                contentBox.getChildren().add(sectionBox);
            }
        }
        
        // Add the bottom button bar
        contentBox.getChildren().add(createBottomButtonBar());
    }

    // Create a detail field (separating this from the addDetailField method)
    private VBox createDetailField(String labelText, String value, boolean editable) {
        VBox fieldBox = new VBox(5);
        fieldBox.getStyleClass().add("detail-field");
        
        // Label
        Text label = new Text(labelText);
        label.getStyleClass().add("nodedetail-label");
        label.setFill(Color.web("#88c5cc"));
        label.setStyle("-fx-font-size: 14px;");
        fieldBox.getChildren().add(label);
        
        // Create either an editable control or a text display
        if (editable) {
            // Find the DeviceField enum based on the label
            DeviceField fieldType = null;
            for (NodeDetailFieldConfig config : NodeDetailPanelConfig.getFieldsForDevice(currentNode.getDeviceType())) {
                if (config.getLabel().equals(labelText)) {
                    fieldType = config.getField();
                    break;
                }
            }
            
            // If this is an enum field, create a ComboBox
            if (fieldType != null && (fieldType == DeviceField.CONNECTION_TYPE || 
                                     fieldType == DeviceField.NETWORK_LOCATION || 
                                     fieldType == DeviceField.NODE_ROUTING ||
                                     fieldType == DeviceField.HOST_NODE ||  // Add HOST_NODE here
                                     fieldType.getOptions() != null)) {
                
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.getStyleClass().add("nodedetail-combobox");
                comboBox.setPrefWidth(200);
                comboBox.setMaxWidth(200);
                
                // Add options based on the field type
                if (fieldType == DeviceField.CONNECTION_TYPE) {
                    for (ConnectionType type : ConnectionType.values()) {
                        comboBox.getItems().add(type.toString());
                    }
                } else if (fieldType == DeviceField.NETWORK_LOCATION) {
                    for (NetworkLocation location : NetworkLocation.values()) {
                        comboBox.getItems().add(location.toString());
                    }
                } else if (fieldType == DeviceField.NODE_ROUTING) {
                    // Special case for Node Routing - populate with nodes that can be routes
                    populateNodeRoutingOptions(comboBox, currentNode);
                } else if (fieldType == DeviceField.HOST_NODE) {
                    // Special case for Host Node - populate with valid host nodes
                    populateHostNodeOptions(comboBox, currentNode);
                } else if (fieldType.getOptions() != null) {
                    comboBox.getItems().addAll(fieldType.getOptions());
                }
                
                // Set the current value
                comboBox.setValue(value);
                
                // Save changes when a new value is selected
                comboBox.setOnAction(e -> {
                    saveFieldChange(currentNode, labelText, comboBox.getValue());
                });
                
                fieldBox.getChildren().add(comboBox);
            } else {
                // Create editable text field for other editable fields
                TextField textField = new TextField(value);
                textField.getStyleClass().add("nodedetail-textfield");
                textField.setPrefWidth(200);  // Match the width used in add node form
                textField.setMaxWidth(200);   // Match the width used in add node form
                
                // Save changes when focus is lost
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        saveFieldChange(currentNode, labelText, textField.getText());
                    }
                });
                
                // Also save on Enter key
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        saveFieldChange(currentNode, labelText, textField.getText());
                        textField.getParent().requestFocus(); // Remove focus from the text field
                    }
                });
                
                fieldBox.getChildren().add(textField);
            }
        } else {
            // Non-editable text
            Text valueText = new Text(value);
            valueText.getStyleClass().add("nodedetail-value-label"); // Match the style from your screenshot
            valueText.setFill(Color.WHITE);
            valueText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            fieldBox.getChildren().add(valueText);
        }
        
        // Add spacing
        VBox.setMargin(fieldBox, new Insets(0, 0, 12, 0));
        return fieldBox;
    }

    /**
     * Saves a changed field value back to the node
     */
    private void saveFieldChange(NetworkNode node, String fieldLabel, String newValue) {
        if (node == null) return;
        
        // Map field label back to field type
        DeviceType deviceType = node.getDeviceType();
        DeviceField fieldType = null;
        
        // Find the field type based on label
        for (NodeDetailFieldConfig config : NodeDetailPanelConfig.getFieldsForDevice(deviceType)) {
            if (config.getLabel().equals(fieldLabel)) {
                fieldType = config.getField();
                break;
            }
        }
        
        if (fieldType == null) return;
        
        // Update the appropriate field in the node
        switch (fieldType) {
            case DISPLAY_NAME:
                node.setDisplayName(newValue);
                break;
            case IP_HOSTNAME:
                node.setIpOrHostname(newValue);
                break;
            case NODE_ROUTING:
                // Handle "Direct" as null or find the node ID based on display name
                if ("Direct".equals(newValue)) {
                    node.setRouteSwitch(null);
                    node.setRouteSwitchId(null); // Make sure ID is also null
                } else {
                    // Find the node ID of the selected node by display name
                    for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                        if (n.getDisplayName().equals(newValue)) {
                            // Store both the display name and ID
                            node.setRouteSwitch(newValue); // Store display name instead of ID
                            node.setRouteSwitchId(n.getNodeId()); // Set ID correctly
                            
                            // Remove the immediate update - will be done when Update button is clicked
                            // NetworkMonitorApp.updateConnectionLineForNode(node);
                            break;
                        }
                    }
                }
                break;
            case CONNECTION_TYPE:
                try {
                    ConnectionType connType = ConnectionType.valueOf(newValue);
                    node.setConnectionType(connType);
                } catch (IllegalArgumentException e) {
                    // Invalid connection type - revert the field
                    showNodeDetails(node); // Refresh the panel
                }
                break;
            case NETWORK_LOCATION:
                try {
                    NetworkLocation netLoc = NetworkLocation.valueOf(newValue);
                    node.setNetworkLocation(netLoc);
                } catch (IllegalArgumentException e) {
                    // Invalid network location - revert the field
                    showNodeDetails(node); // Refresh the panel
                }
                break;
            case HOST_NODE:
                // Handle "None" as null or find the node ID based on display name
                if ("None".equals(newValue)) {
                    node.setHostNode(null);
                    node.setHostNodeId(null);
                } else {
                    // Find the node ID of the selected host by display name
                    for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                        if (n.getDisplayName().equals(newValue)) {
                            node.setHostNode(newValue);
                            node.setHostNodeId(n.getNodeId());
                            break;
                        }
                    }
                }
                break;
            // Add more fields as needed
            default:
                break;
        }
        
        // Enable the Update button
        for (javafx.scene.Node child : contentBox.getChildren()) {
            if (child instanceof BorderPane) {
                BorderPane buttonBar = (BorderPane) child;
                if (buttonBar.getCenter() instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox buttonBox = (javafx.scene.layout.HBox) buttonBar.getCenter();
                    for (javafx.scene.Node buttonNode : buttonBox.getChildren()) {
                        if (buttonNode instanceof Button && ((Button) buttonNode).getText().equals("Update")) {
                            Button updateButton = (Button) buttonNode;
                            updateButton.setDisable(false); // Enable update button
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Populates a ComboBox with the available node routing options
     * based on the current network topology
     */
    private void populateNodeRoutingOptions(ComboBox<String> routeBox, NetworkNode currentNode) {
        routeBox.getItems().clear();
        routeBox.getItems().add("Direct"); // Default option for no routing
        
        NetworkLocation nodeLocation = currentNode.getNetworkLocation();
        
        // Use static list instead of direct field access
        for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
            // Don't include the current node as a route option
            if (n.getNodeId() == currentNode.getNodeId()) {
                continue;
            }
            
            boolean shouldAdd = false;
            
            // For REMOTE_PRIVATE nodes, only PUBLIC devices can be routes
            if (nodeLocation == NetworkLocation.REMOTE_PRIVATE) {
                shouldAdd = n.getNetworkLocation() == NetworkLocation.PUBLIC;
            } 
            // For other locations, only switches or APs can be routes
            else {
                shouldAdd = (n.getDeviceType() == DeviceType.UNMANAGED_SWITCH || 
                           n.getDeviceType() == DeviceType.MANAGED_SWITCH || 
                           n.getDeviceType() == DeviceType.WIRELESS_ACCESS_POINT) 
                          && !n.isMainNode();
            }
            
            if (shouldAdd) {
                routeBox.getItems().add(n.getDisplayName());
            }
        }
    }
    
    /**
     * Populates a ComboBox with the available host node options
     * for the HOST_NODE field
     */
    private void populateHostNodeOptions(ComboBox<String> hostNodeBox, NetworkNode currentNode) {
        hostNodeBox.getItems().clear();
        
        // Add "None" option for no host
        hostNodeBox.getItems().add("None");
        
        // Only show other nodes as options
        for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
            // Don't include the current node as a host option
            if (n.getNodeId() == currentNode.getNodeId()) {
                continue;
            }

            if (n.getDeviceType() == DeviceType.COMPUTER) {
                hostNodeBox.getItems().add(n.getDisplayName());
            }
            
            // For REMOTE_PRIVATE nodes, only PUBLIC devices can be hosts
            if (currentNode.getNetworkLocation() == NetworkLocation.REMOTE_PRIVATE) {
                if (n.getNetworkLocation() == NetworkLocation.PUBLIC) {
                    hostNodeBox.getItems().add(n.getDisplayName());
                }
            } 
            // For other locations, only devices that are not main nodes
            else {
                if (!n.isMainNode()) {
                    hostNodeBox.getItems().add(n.getDisplayName());
                }
            }
        }
    }
    
    /**
     * Queries the system ARP table to find the MAC address for a given IP
     * @param ip The IP address to look up
     * @return The MAC address in format xx:xx:xx:xx:xx:xx or "N/A" if not found
     */
    private String getMacAddressForIP(String ip) {
        try {
            // Execute the arp command to get the ARP table
            Process p = Runtime.getRuntime().exec("arp -a");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            // Search for the line containing the IP address
            while ((line = r.readLine()) != null) {
                if (line.contains(ip)) {
                    // Parse the line to extract the MAC address
                    String[] t = line.trim().split("\\s+");
                    if (t.length >= 2) {
                        // Format the MAC address with colons
                        return t[1].replace('-', ':');
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "N/A";
    }

        private String getNodeFieldValue(NetworkNode node, DeviceField field) {
        if (node == null) return "N/A";
        
        switch (field) {
            case DISPLAY_NAME:
                return node.getDisplayName();
                
            case DEVICE_TYPE:
                return node.getDeviceType().toString();
                
            case IP_HOSTNAME:
                return node.getIpOrHostname();
                
            case MAC_ADDRESS:
                // Try to get MAC from node or look it up
                String mac = node.getMacAddress();
                if (mac == null || mac.isEmpty()) {
                    // Try to look up MAC address for the IP
                    return getMacAddressForIP(node.getIpOrHostname());
                }
                return mac;
                
            case CONNECTION_TYPE:
                return node.getConnectionType().toString();
                
            case NETWORK_LOCATION:
                return node.getNetworkLocation().toString();
                
            case NODE_ROUTING:
                // Get the route switch by name (which we now store directly)
                String routeSwitch = node.getRouteSwitch();
                
                // If it's null or empty, return "Direct"
                if (routeSwitch == null || routeSwitch.isEmpty()) {
                    return "Direct";
                }
                
                // If the stored value looks like a numeric ID (old data)
                if (routeSwitch.matches("\\d+")) {
                    // Look up the display name from the ID
                    Long routeSwitchId = Long.parseLong(routeSwitch);
                    for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                        if (n.getNodeId() == routeSwitchId) {
                            return n.getDisplayName();
                        }
                    }
                }
                
                // Otherwise return the display name we stored
                return routeSwitch;
            case TOTAL_CONNECTIONS:
                // Get the number of connections for this node
                long nodeId = node.getNodeId();
                int connectionCount = 0;
                for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                    if (Objects.equals(n.getRouteSwitchId(), nodeId) ||
                        Objects.equals(n.getHostNodeId(), nodeId)) {
                        connectionCount++;
                    }
                }
                return String.valueOf(connectionCount);
                
            case ONLINE_CONNECTIONS:
                // Count only online connections
                long nodeId2 = node.getNodeId();
                int onlineCount = 0;
                for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                    if ((Objects.equals(n.getRouteSwitchId(), nodeId2) ||
                        Objects.equals(n.getHostNodeId(), nodeId2)) &&
                        n.isConnected()) {
                        onlineCount++;
                    }
                }
                return String.valueOf(onlineCount);
                
            case HOST_NODE:
                // Show the host node name if applicable
                Long hostId = node.getHostNodeId();
                if (hostId != null) {
                    for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                        if (n.getNodeId() == hostId) {
                            return n.getDisplayName();
                        }
                    }
                }
                return "None";
                
            // Add other fields as needed
            default:
                return "N/A";
        }
    }
    
    /**
     * Format a section name from ENUM_VALUE to "Enum Value"
     */
    private String formatSectionName(String enumName) {
        // Replace underscores with spaces
        String spaced = enumName.replace('_', ' ');
        
        // Convert to title case (capitalize first letter of each word)
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : spaced.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                result.append(c);
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }

    // Add these methods to NodeDetailPanel class
    private BorderPane createBottomButtonBar() {
        BorderPane buttonBar = new BorderPane();
        buttonBar.setPadding(new Insets(15, 10, 10, 10));
        
        // Create Update button
        Button updateButton = createActionButton("Update", "#4DD0AC", "#40B393");
        updateButton.getStyleClass().add("nodedetail-updatebutton");
        
        // Create Delete button
        Button deleteButton = createActionButton("Delete", "#F16A6A", "#D45C5C");
        deleteButton.getStyleClass().add("nodedetail-deletebutton");
        
        // Set up button handlers
        setupButtonHandlers(updateButton, deleteButton);
        
        // Create an HBox to center the buttons with some spacing between them
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10); // 10px spacing
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBox.getChildren().addAll(updateButton, deleteButton);
        
        // Place the HBox in the center of the BorderPane
        buttonBar.setCenter(buttonBox);
        
        return buttonBar;
    }

    private Button createActionButton(String text, String baseColor, String hoverColor) {
        Button button = new Button(text);
        button.setPrefHeight(36);
        button.setPrefWidth(90);
        button.setStyle(
            "-fx-background-color: " + baseColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 5;" +
            "-fx-cursor: hand;"
        );
        
        // Add hover effect
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
            
            button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);" +
                "-fx-cursor: hand;"
            );
        });
        
        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
            
            button.setStyle(
                "-fx-background-color: " + baseColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;"
            );
        });
        
        return button;
    }

    // Add this to NodeDetailPanel class, right after the createBottomButtonBar method
    private void setupButtonHandlers(Button updateButton, Button deleteButton) {
        // Update button handler
        updateButton.setOnAction(e -> {
            if (currentNode == null) return;
            
            System.out.println("\n==== Updating node " + currentNode.getDisplayName() + " (ID: " + currentNode.getNodeId() + ") ====");
            
            // Use the recursive update to refresh all connections for this node and its descendants
            NetworkMonitorApp.updateConnectionLinesRecursively(currentNode);
            
            // Save updated nodes to file
            NetworkMonitorApp.getInstance().saveNodesToFile();
            
            System.out.println("Update complete for " + currentNode.getDisplayName());
            System.out.println("=============================================\n");
            
            // Hide the panel after update
            hide();
        });
        
        // Simplified delete button handler - no confirmation dialog
        deleteButton.setOnAction(e -> {
            if (currentNode == null) return;
            
            System.out.println("\n==== Deleting node " + currentNode.getDisplayName() + " (ID: " + currentNode.getNodeId() + ") ====");
            
            // Direct delete without confirmation
            performNodeDelete();
        });
    }

    private void performNodeDelete() {
        // Remove the node
        NetworkMonitorApp.removeNode(currentNode);
        
        // Save the updated configuration
        NetworkMonitorApp.getInstance().saveNodesToFile();
        
        System.out.println("Node deleted: " + currentNode.getDisplayName());
        System.out.println("=============================================\n");
        
        // Hide the panel
        hide();
        
        // Reset current node reference
        currentNode = null;
    }

    /**
     * Check if the panel is currently showing
     * @return true if the panel is visible and showing
     */
    public boolean isShowing() {
        // The panel is showing if it's visible and fully slid in (translateX = 0)
        return isVisible() && getTranslateX() == 0;
    }

    private void initializeEscHandler() {
        // Add ESC key handler to close the panel, but only when visible
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && isShowing()) {
                hide();
                e.consume();
            }
        });
    }
}