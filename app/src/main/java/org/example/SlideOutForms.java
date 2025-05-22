package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class SlideOutForms {

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
}
