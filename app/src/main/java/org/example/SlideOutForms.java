package org.example;

import java.util.Arrays;

import static org.example.NetworkMonitorApp.getPersistentNodesStatic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Utility class for building the slide-out panel forms outside of NetworkMonitorApp.
 */
public class SlideOutForms {

    /**
     * Builds the "Add New Node" form.
     * @param slidePanel the SlideOutPanel that will host and hide this form
     */
    public static Node buildAddNodeForm(SlideOutPanel slidePanel) {
        // container VBox with 16px spacing
        VBox form = new VBox(16); // Increased spacing between elements
        form.getStyleClass().add("form-container");
        form.setPadding(new Insets(0, 8, 8, 8)); // Add padding inside the form

        // Title with less top margin since we handled padding in container
        Label title = new Label("Add New Node");
        title.getStyleClass().add("title-label");

        ComboBox<DeviceType> deviceBox = new ComboBox<>();
        deviceBox.setPromptText("Device Type");
        deviceBox.getItems().setAll(DeviceType.values());

        // Display Name
        TextField nameField = new TextField();
        nameField.setPromptText("Display Name");

        // Hostname / IP
        TextField ipField = new TextField();
        ipField.setPromptText("IP/Hostname");

        ComboBox<NetworkType> netBox = new ComboBox<>();
        netBox.setPromptText("Network Type");
        netBox.getItems().setAll(NetworkType.values());

        ComboBox<ConnectionType> connBox = new ComboBox<>();
        connBox.setPromptText("Connection Type");
        connBox.getItems().setAll(ConnectionType.values());
        connBox.getStyleClass().add("slide-form-combobox");

        ComboBox<String> routeBox = new ComboBox<>();
        routeBox.getStyleClass().add("slide-form-combobox");
        netBox.setPromptText("Node Route");
        // Populate with Gateway first, then switches/WAPs
        getPersistentNodesStatic().stream()
            .filter(n -> n.isMainNode() && n.getDeviceType() == DeviceType.GATEWAY)
            .map(NetworkNode::getDisplayName)
            .findFirst()
            .ifPresent(routeBox.getItems()::add);
        getPersistentNodesStatic().stream()
            .filter(n -> n.getDeviceType() == DeviceType.SWITCH 
                      || n.getDeviceType() == DeviceType.WIRELESS_ACCESS_POINT)
            .map(NetworkNode::getDisplayName)
            .filter(name -> !routeBox.getItems().contains(name))
            .forEach(routeBox.getItems()::add);
        if (!routeBox.getItems().isEmpty()) {
            routeBox.setValue(routeBox.getItems().get(0));
        }

        // Create Button
        Button createBtn = new Button("Create Node");
        createBtn.getStyleClass().add("form-button");
        createBtn.setOnAction(evt -> {
            // simple validation
            if (deviceBox.getValue() == null
             || nameField.getText().trim().isEmpty()
             || ipField.getText().trim().isEmpty()
             || netBox.getValue() == null
             || routeBox.getValue() == null) {
                return;
            }
            NetworkNode node = new NetworkNode(
                ipField.getText().trim(),
                nameField.getText().trim(),
                deviceBox.getValue(),
                netBox.getValue()
            );
            node.setConnectionType(connBox.getValue());
            node.setRouteSwitch(routeBox.getValue());
            NetworkMonitorApp.addNewNode(node);
            slidePanel.hide();
        });

        // Set consistent width for all controls
        Arrays.asList(deviceBox, nameField, ipField, netBox, connBox, routeBox, createBtn)
            .forEach(control -> {
                control.setPrefWidth(150); // Slightly wider to account for padding
                control.setMaxWidth(1500);
            });

        // Assemble form
        form.getChildren().addAll(
            title,
            deviceBox,
            nameField,
            ipField,
            netBox,
            connBox,
            routeBox,
            createBtn
        );
        form.setAlignment(Pos.TOP_CENTER);

        return form;
    }

    // TODO: add buildDiscoverForm(...) and buildFilterForm(...) with the same pattern,
    //       assigning style-classes: slide-form, slide-form-title, slide-form-label,
    //       slide-form-combobox, slide-form-textfield, slide-form-create, etc.
}
