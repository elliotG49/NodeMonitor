package org.example.ui.forms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.app.NetworkMonitorApp;
import org.example.config.DeviceFormConfig;
import org.example.config.DiscoveredNode;
import org.example.model.ConnectionType;
import org.example.model.DeviceField;
import org.example.model.DeviceType;
import org.example.model.NetworkLocation;
import org.example.model.NetworkNode;
import org.example.service.NetworkDiscoveryService;
import org.example.ui.panels.SlideOutPanel;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;  // Add this import
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SlideOutForms {
    // Add this at class level
    private static Timeline discoveryCountdown;

    public static Node buildAddNodeForm(SlideOutPanel slidePanel) {
        VBox form = new VBox(12);
        form.getStyleClass().add("form-container");
        form.setPadding(new Insets(4, 8, 8, 8));

        // Title
        Label title = new Label("Add Node");
        title.getStyleClass().add("title-label");
        form.setAlignment(Pos.TOP_LEFT);

        // Device Type selector (only visible initially)
        ComboBox<DeviceType> deviceBox = new ComboBox<>();
        deviceBox.setPromptText("Device Type");
        deviceBox.getItems().setAll(DeviceType.values());

        // Create all possible form fields but don't add them yet
        Map<DeviceField, Node> formFields = createFormFields();
        
        // Create Button (disabled by default)
        Button createBtn = new Button("Add");
        createBtn.getStyleClass().add("form-button");
        createBtn.setDisable(true);

        // Add button action handler
        createBtn.setOnAction(e -> {
            // Get values from form fields
            DeviceType deviceType = deviceBox.getValue();
            Map<DeviceField, String> fieldValues = new HashMap<>();
            
            // Collect all field values
            for (Map.Entry<DeviceField, Node> entry : formFields.entrySet()) {
                DeviceField field = entry.getKey();
                Node control = entry.getValue();
                
                String value = "";
                if (control instanceof TextField) {
                    value = ((TextField) control).getText();
                } else if (control instanceof ComboBox) {
                    ComboBox<?> combo = (ComboBox<?>) control;
                    value = combo.getValue() != null ? combo.getValue().toString() : "";
                }
                
                fieldValues.put(field, value);
            }

            // Create new node
            String ipHostname = fieldValues.get(DeviceField.IP_HOSTNAME);
            String displayName = fieldValues.get(DeviceField.DISPLAY_NAME);
            if (displayName == null || displayName.isEmpty()) {
                displayName = ipHostname; // Use IP/hostname if no display name provided
            }

            // Create the new node
            NetworkNode newNode = new NetworkNode(
                ipHostname,
                displayName,
                deviceType,
                NetworkLocation.valueOf(fieldValues.get(DeviceField.NETWORK_LOCATION))
            );

            // Set connection type if specified
            String connType = fieldValues.get(DeviceField.CONNECTION_TYPE);
            if (connType != null && !connType.isEmpty()) {
                newNode.setConnectionType(ConnectionType.valueOf(connType));
            }

            // Set route switch if specified
            String routeSwitch = fieldValues.get(DeviceField.NODE_ROUTING);
            if (routeSwitch != null && !routeSwitch.isEmpty()) {
                newNode.setRouteSwitch(routeSwitch);
            }

            // Set host node if virtual machine
            if (deviceType == DeviceType.VIRTUAL_MACHINE) {
                String hostNodeValue = fieldValues.get(DeviceField.HOST_NODE);
                if (hostNodeValue != null && !hostNodeValue.isEmpty()) {
                    newNode.setHostNode(hostNodeValue);
                }
            }

            // Position the new node in the center of the visible area
            javafx.scene.Node spiderMapPane = slidePanel.getParent();
            double centerX = spiderMapPane.getBoundsInLocal().getWidth() / 2;
            double centerY = spiderMapPane.getBoundsInLocal().getHeight() / 2;
            
            newNode.setLayoutX(centerX - 32.5); // Half of SQUARE_SIZE (65/2)
            newNode.setLayoutY(centerY - 32.5);

            // Add the node to the application
            NetworkMonitorApp.addNewNode(newNode);

            // Close the slide panel
            slidePanel.hide();
        });

        // Handle device type selection
        deviceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateFormForDeviceType(form, newVal, formFields, deviceBox, createBtn);
            }
        });

        // Add initial components
        form.getChildren().addAll(title, deviceBox);

        // Set consistent width for deviceBox
        deviceBox.setPrefWidth(150);
        deviceBox.setMaxWidth(150);
        deviceBox.setMinWidth(150);

        return form;
    }

    public static Node buildFilterForm(SlideOutPanel slidePanel) {
        VBox form = new VBox(12);
        form.getStyleClass().add("form-container");
        form.setPadding(new Insets(4, 8, 8, 8));

        // Title
        Label title = new Label("Filter Nodes");
        title.getStyleClass().add("title-label");
        form.setAlignment(Pos.TOP_LEFT);

        // Device Type selector
        ComboBox<DeviceType> deviceBox = new ComboBox<>();
        deviceBox.setPromptText("Select Device Type");
        deviceBox.getItems().setAll(DeviceType.values());
        deviceBox.setPrefWidth(150);
        deviceBox.setMaxWidth(150);

        // Filter Button
        Button filterBtn = new Button("Apply Filter");
        filterBtn.getStyleClass().add("form-button");
        filterBtn.setDisable(true);
        VBox.setMargin(filterBtn, new Insets(16, 0, 0, 0));

        // Enable button when device type is selected
        deviceBox.valueProperty().addListener((obs, old, newVal) -> 
            filterBtn.setDisable(newVal == null));

        // Add filter action
        filterBtn.setOnAction(e -> {
            DeviceType selectedType = deviceBox.getValue();
            if (selectedType != null) {
                NetworkMonitorApp.getInstance().filterNodes(
                    node -> node.getDeviceType() == selectedType);
                // Just pass the device type value, not "Device Type: value"
                NetworkMonitorApp.getInstance().showFilterStatus(selectedType.toString());
                slidePanel.hide();
            }
        });

        form.getChildren().addAll(title, deviceBox, filterBtn);
        return form;
    }

    public static Node buildDiscoveryLoadingPanel(SlideOutPanel slidePanel) {
        VBox panel = new VBox(6);
        panel.getStyleClass().add("form-container");
        panel.setPadding(new Insets(4, 4, 4, 4));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPrefWidth(250);  // Match results panel width

        // Labels for discovery status
        Label interfacesLabel = new Label("Checking 0 interfaces");
        interfacesLabel.getStyleClass().addAll("discovery-status-label", "discovery-status-bold");
        interfacesLabel.setPrefWidth(240);  // Allow full width for text
        
        Label nodesLabel = new Label("Discovered 0 Nodes");
        nodesLabel.getStyleClass().addAll("discovery-status-label", "discovery-status-bold");
        nodesLabel.setPrefWidth(240);  // Allow full width for text
        
        Label etaLabel = new Label("Scanning Network...");
        etaLabel.getStyleClass().add("discovery-eta-label");
        etaLabel.setPrefWidth(240);  // Allow full width for text

        // Cancel button with updated size
        Button cancelBtn = new Button("Cancel Search");
        cancelBtn.getStyleClass().add("discovery-cancel-button");
        cancelBtn.setPrefWidth(240);  // Match panel width minus padding
        cancelBtn.setPrefHeight(45);
        VBox.setMargin(cancelBtn, new Insets(0, 0, 0, 0));

        // Create VBox for status labels with tighter spacing
        VBox statusBox = new VBox(0); // Reduced from default spacing
        statusBox.getChildren().addAll(interfacesLabel, nodesLabel);

        // Add components with specific margins
        panel.getChildren().addAll(
            statusBox,
            etaLabel,
            cancelBtn
        );

        // Add spacing between sections
        VBox.setMargin(etaLabel, new Insets(7, 0, 0, 0));
        VBox.setMargin(cancelBtn, new Insets(2, 0, 0, 0));

        // Add all components
        // panel.getChildren().addAll(
        //     interfacesLabel,
        //     nodesLabel,
        //     etaLabel,
        //     cancelBtn
        // );

        // Create discovery service
        NetworkDiscoveryService discoveryService = new NetworkDiscoveryService();
        java.util.concurrent.atomic.AtomicInteger discoveredCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // Remove the fixed countdown timer and replace with progress updates
        discoveryService.startDiscovery(
            interfaceCount -> Platform.runLater(() -> 
                interfacesLabel.setText("Checking " + interfaceCount + " interfaces")),
            iface -> {},
            node -> Platform.runLater(() -> 
                nodesLabel.setText("Discovered " + discoveredCount.incrementAndGet() + " Nodes"))
        ).thenRun(() -> {
            // Switch to results panel only after discovery is complete
            Platform.runLater(() -> {
                slidePanel.setContent(buildDiscoveryResultsPanel(slidePanel, discoveryService));
            });
        });

        // Update cancel button handler
        cancelBtn.setOnAction(e -> {
            discoveryService.cancelDiscovery();
            slidePanel.hide();
        });

        return panel;
    }

    public static Node buildDiscoveryResultsPanel(SlideOutPanel slidePanel, NetworkDiscoveryService discoveryService) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("form-container");
        panel.setPadding(new Insets(4, 4, 4, 4));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPrefWidth(250);  // Increased width to accommodate wider content

        // Title
        Label title = new Label("Discovered Nodes");
        title.getStyleClass().add("title-label");
        
        // Debug title height after it's rendered
        Platform.runLater(() -> {
            System.out.println("Title Height: " + title.getHeight());
        });

        // Main content container
        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(0, 4, 0, 4));
        
        // Create scrollable container with dynamic height
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("discovery-scroll-pane");
        scrollPane.setMaxHeight(500);
        
        // More responsive height binding
        contentBox.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            Platform.runLater(() -> {
                double contentHeight = contentBox.getHeight();
                double maxHeight = 500;
                double newHeight = Math.min(contentHeight, maxHeight);
                scrollPane.setPrefHeight(newHeight);
                
                // Debug height changes
                System.out.println("Content height changed: " + contentHeight);
                System.out.println("New scroll pane height: " + newHeight);
            });
        });

        // Remove the old height listener
        // contentBox.heightProperty().addListener...

        // Get mode panel height
        VBox modePanel = NetworkMonitorApp.getInstance().getModePanel();
        Platform.runLater(() -> {
            System.out.println("Mode Panel Height: " + modePanel.getHeight());
            System.out.println("Mode Panel Y Position: " + modePanel.getLayoutY());
            
            // Calculate available space
            double availableHeight = slidePanel.getScene().getHeight() - 15 - title.getHeight() - 24 - modePanel.getHeight();
            System.out.println("Calculated Available Height: " + availableHeight);
            
            // Debug content height
            System.out.println("Content Height: " + contentBox.getHeight());
            System.out.println("ScrollPane Height: " + scrollPane.getHeight());
            System.out.println("Total Panel Height: " + panel.getHeight());
        });
        
        // Get all discovered interfaces and create sections
        discoveryService.getDiscoveredInterfaces().forEach(interfaceName -> {
            List<DiscoveredNode> nodes = discoveryService.getNodesForInterface(interfaceName);
            VBox section = createInterfaceSection(interfaceName, nodes, slidePanel);
            contentBox.getChildren().add(section);
            
            // Debug section heights
            Platform.runLater(() -> {
                System.out.println("Section Height for " + interfaceName + ": " + section.getHeight());
            });
        });

        panel.getChildren().addAll(title, scrollPane);
        System.out.println("===========================\n");
        return panel;
    }

    private static VBox createInterfaceSection(String interfaceName, List<DiscoveredNode> nodes, SlideOutPanel slidePanel) {
        VBox section = new VBox(4);
        section.getStyleClass().add("interface-section");
        section.setPadding(new Insets(8, 4, 8, 4));

        // Header with dropdown arrow
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Create VBox for the text labels
        VBox labelBox = new VBox(2);
        
        Label nameLabel = new Label(interfaceName);
        nameLabel.getStyleClass().add("interface-name-label");
        
        Label countLabel = new Label(nodes.size() + " Nodes Found");
        countLabel.getStyleClass().add("interface-count-label");
        
        labelBox.getChildren().addAll(nameLabel, countLabel);

        // Create arrow with image on the right
        ImageView arrow = new ImageView(new Image(SlideOutForms.class.getResourceAsStream("/icons/arrow-right.png")));
        arrow.setFitWidth(12);
        arrow.setFitHeight(12);
        arrow.getStyleClass().add("interface-arrow");
        
        // Add spacer to push arrow to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(labelBox, spacer, arrow);

        // Content (initially collapsed)
        VBox content = new VBox(8);
        content.getStyleClass().add("interface-content");
        content.setVisible(false);
        content.setManaged(false);

        // Add discovered nodes
        int[] nodeCounter = {1};  // Use array to allow modification in lambda
        nodes.forEach(node -> {
            VBox nodeBox = createNodeEntry(node, slidePanel, nodeCounter[0]++);
            content.getChildren().add(nodeBox);
        });

        // Toggle visibility on click with smooth rotation
        header.setOnMouseClicked(e -> {
            boolean expanding = !content.isVisible();
            arrow.setRotate(expanding ? 90 : 0);
            content.setVisible(expanding);
            content.setManaged(expanding);
            
            // Force layout update
            content.requestLayout();
            section.requestLayout();
        });

        section.getChildren().addAll(header, content);
        return section;
    }

    private static VBox createNodeEntry(DiscoveredNode node, SlideOutPanel slidePanel) {
        return createNodeEntry(node, slidePanel, 0);
    }

    private static VBox createNodeEntry(DiscoveredNode node, SlideOutPanel slidePanel, int nodeNumber) {
        VBox nodeBox = new VBox(4);
        nodeBox.getStyleClass().add("node-entry");
        nodeBox.setPadding(new Insets(8));
        nodeBox.setMaxWidth(200);

        // Add node number label
        Label numberLabel = new Label("Node " + nodeNumber);
        numberLabel.getStyleClass().add("node-number-label");

        // Basic info section
        VBox basicInfo = new VBox(4);
        basicInfo.getStyleClass().add("node-basic-info");

        // Add the number label as first child
        basicInfo.getChildren().add(numberLabel);

        // Create labels with opaque labels and normal value text
        HBox ipBox = new HBox(4);
        Label ipLabelText = new Label("IP:");
        ipLabelText.setStyle("-fx-opacity: 0.7;"); // Make label slightly opaque
        Label ipValue = new Label(node.ip);
        ipBox.getChildren().addAll(ipLabelText, ipValue);
        ipBox.getStyleClass().add("node-detail-label");

        // Hostname (always shown, with N/A if empty)
        HBox hostnameBox = new HBox(4);
        Label hostnameLabelText = new Label("Hostname:");
        hostnameLabelText.setStyle("-fx-opacity: 0.7;"); // Make label slightly opaque
        Label hostnameValue = new Label(node.hostname.isEmpty() ? "N/A" : node.hostname);
        hostnameBox.getChildren().addAll(hostnameLabelText, hostnameValue);
        hostnameBox.getStyleClass().add("node-detail-label");

        // MAC Address
        HBox macBox = new HBox(4);
        Label macLabelText = new Label("MAC:");
        macLabelText.setStyle("-fx-opacity: 0.7;"); // Make label slightly opaque
        Label macValue = new Label(node.mac);
        macBox.getChildren().addAll(macLabelText, macValue);
        macBox.getStyleClass().add("node-detail-label");

        // Add all info labels to the basic info section
        basicInfo.getChildren().addAll(ipBox, hostnameBox, macBox);

        // Device Type selector
        ComboBox<DeviceType> deviceBox = new ComboBox<>();
        deviceBox.setPromptText("Device Type");
        deviceBox.getItems().setAll(DeviceType.values());
        deviceBox.setPrefWidth(170); // Adjusted to fit the node entry width
        deviceBox.getStyleClass().add("node-device-combo");

        // Form fields container (initially empty)
        VBox formFields = new VBox(8);
        formFields.setVisible(false);
        formFields.setManaged(false);

        // Create Add button
        Button addButton = new Button("Add");
        addButton.getStyleClass().add("node-add-button");
        addButton.setPrefWidth(70);
        addButton.setDisable(true);

        // Create and store all possible form fields
        Map<DeviceField, Node> fields = createFormFields();
        
        // Pre-fill IP and MAC
        TextField ipField = (TextField) fields.get(DeviceField.IP_HOSTNAME);
        ipField.setText(node.ip);
        ipField.setEditable(false); // Make IP field read-only
        
        TextField macField = (TextField) fields.get(DeviceField.MAC_ADDRESS);
        if (macField != null) {
            macField.setText(node.mac);
            macField.setEditable(false); // Make MAC field read-only
        }

        // Device type selection handler
        deviceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                formFields.getChildren().clear();
                formFields.setVisible(true);
                formFields.setManaged(true);

                // Get required and optional fields for the selected device type
                List<DeviceField> allFields = DeviceFormConfig.getFieldsForDevice(newVal);
                List<DeviceField> requiredFields = allFields.stream()
                    .filter(field -> DeviceFormConfig.isFieldRequired(newVal, field))
                    .toList();
                List<DeviceField> optionalFields = allFields.stream()
                    .filter(field -> !DeviceFormConfig.isFieldRequired(newVal, field))
                    .toList();

                // Add Required Fields section
                if (!requiredFields.isEmpty()) {
                    Label requiredLabel = new Label("Required Fields");
                    requiredLabel.getStyleClass().add("section-label");
                    requiredLabel.setPadding(new Insets(8, 0, 4, 0));
                    formFields.getChildren().add(requiredLabel);

                    // Add required fields
                    for (DeviceField field : requiredFields) {
                        Node control = fields.get(field);
                        if (control != null) {
                            formFields.getChildren().add(control);
                            addValidationListener(control, addButton, deviceBox, fields);
                        }
                    }
                }

                // Add Optional Fields section
                if (!optionalFields.isEmpty()) {
                    Label optionalLabel = new Label("Optional Fields");
                    optionalLabel.getStyleClass().add("section-label");
                    optionalLabel.setPadding(new Insets(16, 0, 4, 0));
                    formFields.getChildren().add(optionalLabel);

                    // Add optional fields
                    for (DeviceField field : optionalFields) {
                        Node control = fields.get(field);
                        if (control != null) {
                            formFields.getChildren().add(control);
                        }
                    }
                }

                // Initial validation
                validateForm(addButton, deviceBox, fields);
            } else {
                formFields.setVisible(false);
                formFields.setManaged(false);
                addButton.setDisable(true);
            }
        });

        // Add button handler
        addButton.setOnAction(e -> {
            DeviceType deviceType = deviceBox.getValue();
            if (deviceType != null) {
                // Collect field values
                Map<String, String> values = new HashMap<>();
                fields.forEach((field, control) -> {
                    if (control instanceof TextField) {
                        values.put(field.toString(), ((TextField) control).getText().trim());
                    } else if (control instanceof ComboBox) {
                        Object value = ((ComboBox<?>) control).getValue();
                        values.put(field.toString(), value != null ? value.toString() : "");
                    }
                });

                // Create new node
                NetworkNode newNode = new NetworkNode(
                    node.ip,
                    values.get(DeviceField.DISPLAY_NAME.toString()),
                    deviceType,
                    NetworkLocation.valueOf(values.get(DeviceField.NETWORK_LOCATION.toString()))
                );

                // Set connection type and route if provided
                if (values.containsKey(DeviceField.CONNECTION_TYPE.toString())) {
                    newNode.setConnectionType(ConnectionType.valueOf(values.get(DeviceField.CONNECTION_TYPE.toString())));
                }
                if (values.containsKey(DeviceField.NODE_ROUTING.toString())) {
                    newNode.setRouteSwitch(values.get(DeviceField.NODE_ROUTING.toString()));
                }

                // Set MAC address
                newNode.setMacAddress(node.mac);

                // Set host node if virtual machine
                if (deviceType == DeviceType.VIRTUAL_MACHINE) {
                    String hostNodeValue = values.get(DeviceField.HOST_NODE.toString());
                    if (hostNodeValue != null && !hostNodeValue.isEmpty()) {
                        newNode.setHostNode(hostNodeValue);
                    }
                }

                // Add the node to the network
                NetworkMonitorApp.addNewNode(newNode);
                
                // Close the panel
                slidePanel.hide();
            }
        });

        // Add all components to the node box
        nodeBox.getChildren().addAll(
            basicInfo,
            deviceBox,
            formFields,
            addButton
        );

        return nodeBox;
    }

    private static HBox createBoldLabel(String boldText, String normalText) {
        Label bold = new Label(boldText);
        bold.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        
        Label normal = new Label(normalText);
        normal.setStyle("-fx-text-fill: white;");
        
        HBox container = new HBox();
        container.getChildren().addAll(bold, normal);
        return container;
    }

    private static Map<DeviceField, Node> createFormFields() {
        Map<DeviceField, Node> fields = new HashMap<>();

        // Create common fields
        TextField nameField = new TextField();
        nameField.setPromptText("Display Name");
        fields.put(DeviceField.DISPLAY_NAME, nameField);

        TextField ipField = new TextField();
        ipField.setPromptText("IP/Hostname");
        fields.put(DeviceField.IP_HOSTNAME, ipField);

        ComboBox<ConnectionType> connBox = new ComboBox<>();
        connBox.setPromptText("Connection Type");
        connBox.getItems().setAll(ConnectionType.values());
        fields.put(DeviceField.CONNECTION_TYPE, connBox);

        // Create route box with special population logic
        ComboBox<String> routeBox = new ComboBox<>();
        routeBox.setPromptText("Node Route");
        populateRouteBox(routeBox);
        fields.put(DeviceField.NODE_ROUTING, routeBox);

        // Create other device-specific fields
        for (DeviceField field : DeviceField.values()) {
            if (!fields.containsKey(field)) {
                if (field.getOptions() != null) {
                    // Render a ComboBox for fields with predefined options
                    ComboBox<String> comboBox = new ComboBox<>();
                    comboBox.setPromptText(field.getLabel());
                    comboBox.getItems().addAll(field.getOptions());
                    fields.put(field, comboBox);
                } else if (field.isYesNoField()) {
                    ComboBox<String> yesNo = new ComboBox<>();
                    yesNo.setPromptText(field.getLabel());
                    yesNo.getItems().addAll("Yes", "No");
                    fields.put(field, yesNo);
                } else if (field.isTextArea()) {
                    TextArea area = new TextArea();
                    area.setPromptText(field.getLabel());
                    area.setPrefRowCount(3);
                    fields.put(field, area);
                } else {
                    TextField tf = new TextField();
                    tf.setPromptText(field.getLabel());
                    fields.put(field, tf);
                }
            }
        }

        // Add Host Node dropdown for virtual machines
        ComboBox<String> hostNodeBox = new ComboBox<>();
        hostNodeBox.setPromptText("Host Node");
        List<NetworkNode> allNodes = NetworkMonitorApp.getPersistentNodesStatic();
        for (NetworkNode node : allNodes) {
            hostNodeBox.getItems().add(node.getDisplayName());
        }
        fields.put(DeviceField.HOST_NODE, hostNodeBox);

        // Add Network Location dropdown
        ComboBox<NetworkLocation> locationBox = new ComboBox<>();
        locationBox.setPromptText("Network Location");
        locationBox.getItems().setAll(NetworkLocation.values());
        fields.put(DeviceField.NETWORK_LOCATION, locationBox);

        // Set consistent width for all controls
        fields.values().forEach(control -> {
            if (control instanceof javafx.scene.control.Control) {
                ((javafx.scene.control.Control) control).setPrefWidth(220);
                ((javafx.scene.control.Control) control).setMaxWidth(220);
            }
        });

        // Add an event listener to update route options when network location changes
        locationBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (fields.containsKey(DeviceField.NODE_ROUTING)) {
                ComboBox<String> existingRouteBox = (ComboBox<String>)fields.get(DeviceField.NODE_ROUTING);
                populateRouteBox(existingRouteBox);
            }
        });

        return fields;
    }

    private static void updateFormForDeviceType(VBox form, DeviceType deviceType, 
            Map<DeviceField, Node> fields, ComboBox<DeviceType> deviceBox, Button createBtn) {
        
        // Store title before clearing
        Label title = (Label) form.getChildren().get(0);
        
        // Clear and rebuild form
        form.getChildren().clear();
        
        // Re-add title and device type selector
        form.getChildren().addAll(title, deviceBox);

        // Get required and optional fields
        List<DeviceField> allFields = DeviceFormConfig.getFieldsForDevice(deviceType);
        List<DeviceField> requiredFields = allFields.stream()
            .filter(field -> DeviceFormConfig.isFieldRequired(deviceType, field))
            .toList();
        List<DeviceField> optionalFields = allFields.stream()
            .filter(field -> !DeviceFormConfig.isFieldRequired(deviceType, field))
            .toList();

        // Add Required Fields section
        if (!requiredFields.isEmpty()) {
            Label requiredLabel = new Label("Required Fields");
            requiredLabel.getStyleClass().add("section-label");
            requiredLabel.setPadding(new Insets(8, 0, 4, 0));
            form.getChildren().add(requiredLabel);

            // Add required fields
            for (DeviceField field : requiredFields) {
                Node control = fields.get(field);
                if (control != null) {
                    form.getChildren().add(control);
                    addValidationListener(control, createBtn, deviceBox, fields);
                }
            }
        }

        // Add Optional Fields section
        if (!optionalFields.isEmpty()) {
            Label optionalLabel = new Label("Optional Fields");
            optionalLabel.getStyleClass().add("section-label");
            optionalLabel.setPadding(new Insets(16, 0, 4, 0));
            form.getChildren().add(optionalLabel);

            // Add optional fields
            for (DeviceField field : optionalFields) {
                Node control = fields.get(field);
                if (control != null) {
                    form.getChildren().add(control);
                    addValidationListener(control, createBtn, deviceBox, fields);
                }
            }
        }

        // Add create button at the bottom with some spacing
        form.getChildren().add(createBtn);
        VBox.setMargin(createBtn, new Insets(16, 0, 0, 0));
        
        // Initial validation
        validateForm(createBtn, deviceBox, fields);
    }

    // Helper method to add validation listeners
    private static void addValidationListener(Node control, Button createBtn, 
            ComboBox<DeviceType> deviceBox, Map<DeviceField, Node> fields) {
        if (control instanceof TextField) {
            ((TextField) control).textProperty().addListener((obs, old, newVal) -> 
                validateForm(createBtn, deviceBox, fields));
        } else if (control instanceof ComboBox) {
            ((ComboBox<?>) control).valueProperty().addListener((obs, old, newVal) -> 
                validateForm(createBtn, deviceBox, fields));
        }
    }

    private static void validateForm(Button createBtn, ComboBox<DeviceType> deviceBox, 
            Map<DeviceField, Node> fields) {
        
        DeviceType deviceType = deviceBox.getValue();
        if (deviceType == null) {
            createBtn.setDisable(true);
            return;
        }

        List<DeviceField> requiredFields = DeviceFormConfig.getFieldsForDevice(deviceType).stream()
            .filter(field -> DeviceFormConfig.isFieldRequired(deviceType, field))
            .toList();

        boolean isValid = requiredFields.stream().allMatch(field -> {
            Node control = fields.get(field);
            if (control instanceof TextField) {
                return !((TextField) control).getText().trim().isEmpty();
            } else if (control instanceof ComboBox) {
                return ((ComboBox<?>) control).getValue() != null;
            }
            return true;
        });

        createBtn.setDisable(!isValid);
    }

    private static void populateRouteBox(ComboBox<String> routeBox) {
        routeBox.getItems().clear();
        routeBox.getItems().add("None");
        
        // Get the selected network location (if applicable) - using final
        final NetworkLocation selectedLocation;
        
        // Check if the routeBox has a parent before trying to find the network location field
        if (routeBox.getParent() != null) {
            Node locationControl = getFormFieldByType(routeBox.getParent(), ComboBox.class, "Network Location");
            if (locationControl instanceof ComboBox<?>) {
                ComboBox<?> locationBox = (ComboBox<?>) locationControl;
                Object value = locationBox.getValue();
                if (value instanceof NetworkLocation) {
                    selectedLocation = (NetworkLocation) value;
                } else {
                    selectedLocation = null;
                }
            } else {
                selectedLocation = null;
            }
        } else {
            selectedLocation = null;
        }
        
        // The lambda expression now uses the final selectedLocation
        NetworkMonitorApp.getPersistentNodesStatic().stream()
            .filter(n -> {
                // For REMOTE_PRIVATE nodes, only PUBLIC devices can be routes
                if (selectedLocation == NetworkLocation.REMOTE_PRIVATE) {
                    return n.getNetworkLocation() == NetworkLocation.PUBLIC;
                } 
                // For other locations, any switch or AP can be a route
                else {
                    return (n.getDeviceType() == DeviceType.UNMANAGED_SWITCH || 
                           n.getDeviceType() == DeviceType.MANAGED_SWITCH || 
                           n.getDeviceType() == DeviceType.WIRELESS_ACCESS_POINT) 
                          && !n.isMainNode();
                }
            })
            .map(NetworkNode::getDisplayName)
            .forEach(routeBox.getItems()::add);
    }

    // Helper method to find form field by type and prompt
    private static Node getFormFieldByType(Parent parent, Class<?> type, String promptText) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (type.isInstance(node)) {
                if (node instanceof ComboBox && ((ComboBox<?>)node).getPromptText().contains(promptText)) {
                    return node;
                } else if (node instanceof TextField && ((TextField)node).getPromptText().contains(promptText)) {
                    return node;
                }
            }
            if (node instanceof Parent) {
                Node result = getFormFieldByType((Parent)node, type, promptText);
                if (result != null) return result;
            }
        }
        return null;
    }
}
