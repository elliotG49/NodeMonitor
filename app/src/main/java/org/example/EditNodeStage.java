package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EditNodeStage extends Stage {

    private NetworkNode result = null;  // The updated node (or null if canceled).

    public EditNodeStage(NetworkNode node) {
        // Make the stage transparent / undecorated so we can style it fully with CSS.
        initStyle(StageStyle.TRANSPARENT);

        // Create a root pane (StackPane for easy layering).
        StackPane root = new StackPane();
        root.getStyleClass().add("my-dialog-root");
        root.setPadding(new Insets(10));
        root.getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());        

        // Create the content grid (same fields as your EditNodeDialog).
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        TextField ipField = new TextField(node.getIpOrHostname());
        TextField nameField = new TextField(node.getDisplayName());
        ComboBox<DeviceType> deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setValue(node.getDeviceType());
        ComboBox<NetworkType> networkTypeBox = new ComboBox<>();
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(node.getNetworkType());

        // ColorPicker for node color
        ColorPicker colorPicker = new ColorPicker();
        try {
            colorPicker.setValue(Color.web(node.getOutlineColor()));
        } catch (Exception e) {
            colorPicker.setValue(Color.WHITE);
        }
        Label colorLabel = new Label("Node Colour:");
        if (node.isMainNode()) {
            colorPicker.setDisable(true);
        }

        // Connection type combo
        ComboBox<ConnectionType> connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(node.getConnectionType());
        Label connectionLabel = new Label("Connection Type:");
        if (node.isMainNode()) {
            connectionTypeBox.setDisable(true);
        }

        // Add all fields to the grid
        grid.add(new Label("IP/Hostname:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Display Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Device Type:"), 0, 2);
        grid.add(deviceTypeBox, 1, 2);
        grid.add(new Label("Network Type:"), 0, 3);
        grid.add(networkTypeBox, 1, 3);
        grid.add(colorLabel, 0, 4);
        grid.add(colorPicker, 1, 4);
        grid.add(connectionLabel, 0, 5);
        grid.add(connectionTypeBox, 1, 5);

        // OK / Cancel buttons
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        okButton.setOnAction(e -> {
            // Create a new updated node
            String ip = ipField.getText().trim();
            String name = nameField.getText().trim();
            DeviceType deviceType = deviceTypeBox.getValue();
            NetworkType networkType = networkTypeBox.getValue();

            NetworkNode updated = new NetworkNode(ip, name, deviceType, networkType);
            updated.setLayoutX(node.getLayoutX());
            updated.setLayoutY(node.getLayoutY());
            updated.setMainNode(node.isMainNode());
            if (!node.isMainNode()) {
                updated.setOutlineColor(toHexString(colorPicker.getValue()));
                updated.setConnectionType(connectionTypeBox.getValue());
            } else {
                updated.setOutlineColor(node.getOutlineColor());
            }

            result = updated;  // store the result
            close();           // close the stage
        });
        cancelButton.setOnAction(e -> {
            result = null;
            close();
        });

        HBox buttonBox = new HBox(10, okButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 6);

        // Add the grid to root
        root.getChildren().add(grid);

        // Create a scene with a transparent fill, so no white corners appear.
        Scene scene = new Scene(root, 400, 300, Color.TRANSPARENT);
        setScene(scene);
    }

    /**
     * Show the stage and block until it's closed.
     * Then retrieve the updated node or null if canceled.
     */
    public NetworkNode showAndGetResult() {
        showAndWait();  // blocks until stage is closed
        return result;
    }

    // Helper to convert Color to hex string.
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
}
