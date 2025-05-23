package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;  // Add this import
import javafx.geometry.Pos;
import javafx.scene.Node;
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
        deviceBox.setPromptText("Device Type *");
        deviceBox.getItems().setAll(DeviceType.values());

        // Create all possible form fields but don't add them yet
        Map<DeviceField, Node> formFields = createFormFields();
        
        // Create Button (disabled by default)
        Button createBtn = new Button("Add");
        createBtn.getStyleClass().add("form-button");
        createBtn.setDisable(true);

        // Add button action handler
        createBtn.setOnAction(e -> {
            DeviceType deviceType = deviceBox.getValue();
            if (deviceType == null) return;

            // Collect field values
            Map<String, String> values = new HashMap<>();
            formFields.forEach((field, control) -> {
                if (control instanceof TextField) {
                    values.put(field.toString(), ((TextField) control).getText().trim());
                } else if (control instanceof ComboBox) {
                    Object value = ((ComboBox<?>) control).getValue();
                    values.put(field.toString(), value != null ? value.toString() : "");
                }
            });

            // Create new node
            NetworkNode newNode = new NetworkNode(
                values.get(DeviceField.IP_HOSTNAME.toString()),
                values.get(DeviceField.DISPLAY_NAME.toString()),
                deviceType,
                NetworkType.valueOf(values.get(DeviceField.NETWORK_TYPE.toString()))
            );

            // Set connection type and route if provided
            if (values.containsKey(DeviceField.CONNECTION_TYPE.toString())) {
                newNode.setConnectionType(ConnectionType.valueOf(values.get(DeviceField.CONNECTION_TYPE.toString())));
            }
            if (values.containsKey(DeviceField.NODE_ROUTING.toString())) {
                newNode.setRouteSwitch(values.get(DeviceField.NODE_ROUTING.toString()));
            }

            // Add optional fields if they exist
            if (values.containsKey(DeviceField.MAC_ADDRESS.toString())) {
                String mac = values.get(DeviceField.MAC_ADDRESS.toString());
                if (!mac.isEmpty()) newNode.setMacAddress(mac);
            }

            // Get the selected host node
            ComboBox<String> hostNodeBox = (ComboBox<String>) formFields.get(DeviceField.HOST_NODE);
            if (hostNodeBox != null) {
                String selectedHostNode = hostNodeBox.getValue();
                if (selectedHostNode != null && !selectedHostNode.isEmpty()) {
                    newNode.setRouteSwitch(selectedHostNode); // Store the selected host node
                }
            }

            // Add the node to the network
            NetworkMonitorApp.addNewNode(newNode);

            // Close the panel
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
        deviceBox.setPrefWidth(220);
        deviceBox.setMaxWidth(220);

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
        deviceBox.setPrefWidth(220);
        deviceBox.setMaxWidth(220);

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
        section.setPadding(new Insets(8, 8, 8, 8));

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
        nodes.forEach(node -> {
            VBox nodeBox = createNodeEntry(node, slidePanel);
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
        VBox nodeBox = new VBox(4);
        nodeBox.getStyleClass().add("node-entry");
        nodeBox.setPadding(new Insets(8));
        nodeBox.setMaxWidth(200);

        // IP Address label and value
        Label ipLabel = new Label();
        ipLabel.setGraphic(createBoldLabel("IP Address: ", node.ip));
        ipLabel.getStyleClass().add("node-detail-label");
        ipLabel.setWrapText(true);

        // Hostname label and value (if exists)
        if (!node.hostname.isEmpty()) {
            Label hostnameLabel = new Label();
            hostnameLabel.setGraphic(createBoldLabel("Hostname: ", node.hostname));
            hostnameLabel.getStyleClass().add("node-detail-label");
            hostnameLabel.setWrapText(true);
            nodeBox.getChildren().add(hostnameLabel);
        }

        // MAC label and value
        Label macLabel = new Label();
        macLabel.setGraphic(createBoldLabel("MAC: ", node.mac));
        macLabel.getStyleClass().add("node-detail-label");

        Button addButton = new Button("Add");
        addButton.getStyleClass().add("node-add-button");
        addButton.setPrefWidth(60);

        nodeBox.getChildren().addAll(ipLabel, macLabel, addButton);
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
        nameField.setPromptText("Display Name *");
        fields.put(DeviceField.DISPLAY_NAME, nameField);

        TextField ipField = new TextField();
        ipField.setPromptText("IP/Hostname *");
        fields.put(DeviceField.IP_HOSTNAME, ipField);

        ComboBox<NetworkType> netBox = new ComboBox<>();
        netBox.setPromptText("Network Type *");
        netBox.getItems().setAll(NetworkType.values());
        fields.put(DeviceField.NETWORK_TYPE, netBox);

        ComboBox<ConnectionType> connBox = new ComboBox<>();
        connBox.setPromptText("Connection Type *");
        connBox.getItems().setAll(ConnectionType.values());
        fields.put(DeviceField.CONNECTION_TYPE, connBox);

        // Create route box with special population logic
        ComboBox<String> routeBox = new ComboBox<>();
        routeBox.setPromptText("Node Route *");
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

        // Set consistent width for all controls
        fields.values().forEach(control -> {
            if (control instanceof javafx.scene.control.Control) {
                ((javafx.scene.control.Control) control).setPrefWidth(220);
                ((javafx.scene.control.Control) control).setMaxWidth(220);
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
        
        NetworkMonitorApp.getPersistentNodesStatic().stream()
            .filter(n -> (n.getDeviceType() == DeviceType.UNMANAGED_SWITCH || 
                         n.getDeviceType() == DeviceType.MANAGED_SWITCH || 
                         n.getDeviceType() == DeviceType.WIRELESS_ACCESS_POINT) 
                    && !n.isMainNode())
            .map(NetworkNode::getDisplayName)
            .forEach(routeBox.getItems()::add);
    }

    // Helper method to wrap numbers with styled span
    private static String wrapWithStyle(String number) {
        return String.format("<%s>%s</>", "span class='discovery-number'", number);
    }
}
