package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NewNodeStage extends Stage {
    private NetworkNode result = null;
    
    public NewNodeStage() {
        initStyle(StageStyle.TRANSPARENT);
        initModality(Modality.APPLICATION_MODAL);
        
        StackPane root = new StackPane();
        root.getStyleClass().add("my-dialog-root");
        root.setPadding(new Insets(10));
        root.getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField ipField = new TextField();
        ipField.setPromptText("IP/Hostname");
        TextField nameField = new TextField();
        nameField.setPromptText("Display Name");
        
        ComboBox<DeviceType> deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getItems().addAll(DeviceType.values());
        deviceTypeBox.setValue(DeviceType.COMPUTER);
        
        ComboBox<NetworkType> networkTypeBox = new ComboBox<>();
        networkTypeBox.getItems().addAll(NetworkType.values());
        networkTypeBox.setValue(NetworkType.INTERNAL);
        
        ComboBox<ConnectionType> connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.ETHERNET);
        Label connectionLabel = new Label("Connection Type:");
        
        grid.add(new Label("IP/Hostname:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Display Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Device Type:"), 0, 2);
        grid.add(deviceTypeBox, 1, 2);
        grid.add(new Label("Network Type:"), 0, 3);
        grid.add(networkTypeBox, 1, 3);
        grid.add(connectionLabel, 0, 4);
        grid.add(connectionTypeBox, 1, 4);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        buttonBox.getChildren().addAll(okButton, cancelButton);
        grid.add(buttonBox, 1, 5);
        
        root.getChildren().add(grid);
        Scene scene = new Scene(root, 400, 300, Color.TRANSPARENT);
        setScene(scene);
        
        okButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String name = nameField.getText().trim();
            DeviceType deviceType = deviceTypeBox.getValue();
            NetworkType networkType = networkTypeBox.getValue();
            NetworkNode node = new NetworkNode(ip, name, deviceType, networkType);
            node.setConnectionType(connectionTypeBox.getValue());
            result = node;
            close();
        });
        
        cancelButton.setOnAction(e -> {
            result = null;
            close();
        });
    }
    
    public NetworkNode showAndGetResult() {
        showAndWait();
        return result;
    }
}
