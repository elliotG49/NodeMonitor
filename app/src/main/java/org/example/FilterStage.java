package org.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class FilterStage extends Stage {
    private FilterOptions result = null;
    
    public FilterStage(List<NetworkNode> nodes) {
        // Use transparent style so CSS controls everything.
        initStyle(StageStyle.TRANSPARENT);
        initModality(Modality.APPLICATION_MODAL);

        // Create a root pane (BorderPane) for the content.
        BorderPane root = new BorderPane();
        root.getStyleClass().add("my-dialog-root"); // Ensure your CSS defines .my-dialog-root
        root.setPadding(new Insets(10));
        // Load your dialog CSS.
        root.getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
        
        // Content
        VBox contentBox = new VBox(10);
        HBox modeBox = new HBox(10);
        Label filterByLabel = new Label("Filter by:");
        ComboBox<String> filterModeBox = new ComboBox<>();
        filterModeBox.getItems().addAll("Subnet", "Colour", "Connection", "Device");
        filterModeBox.setValue("Subnet");
        modeBox.getChildren().addAll(filterByLabel, filterModeBox);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox advancedBox = new VBox(10);
        advancedBox.setPadding(new Insets(10));
        
        // Subnet filtering.
        VBox subnetBox = new VBox(10);
        Set<String> subnetSet = new HashSet<>();
        for (NetworkNode node : nodes) {
            if (node.getNetworkType() == NetworkType.INTERNAL) {
                String ip = node.getIpOrHostname();
                if (ip != null && ip.startsWith("127.0.0")) continue;
                String subnet = getSubnet(ip);
                if (subnet != null) subnetSet.add(subnet);
                if (node.getResolvedIp() != null) {
                    subnet = getSubnet(node.getResolvedIp());
                    if (subnet != null) subnetSet.add(subnet);
                }
            }
        }
        ObservableList<CheckBox> subnetCheckBoxes = FXCollections.observableArrayList();
        for (String subnet : subnetSet) {
            CheckBox cb = new CheckBox(subnet);
            subnetBox.getChildren().add(cb);
            subnetCheckBoxes.add(cb);
        }
        
        // Colour filtering.
        VBox colourBox = new VBox(10);
        Set<String> colourSet = new HashSet<>();
        for (NetworkNode node : nodes) {
            if (!node.isMainNode()) {
                String col = node.getOutlineColor();
                if (col != null) colourSet.add(col);
            }
        }
        ObservableList<CheckBox> colourCheckBoxes = FXCollections.observableArrayList();
        for (String colour : colourSet) {
            CheckBox cb = new CheckBox();
            Region colorSwatch = new Region();
            colorSwatch.setPrefSize(20, 20);
            colorSwatch.setStyle("-fx-background-color: " + colour + "; -fx-border-color: black;");
            cb.setGraphic(colorSwatch);
            cb.setUserData(colour);
            colourBox.getChildren().add(cb);
            colourCheckBoxes.add(cb);
        }
        
        // Connection filtering.
        VBox connectionBox = new VBox(10);
        Set<String> connectionSet = new HashSet<>();
        for (NetworkNode node : nodes) {
            if (!node.isMainNode()) {
                String conn = node.getConnectionType().toString();
                connectionSet.add(conn);
            }
        }
        ObservableList<CheckBox> connectionCheckBoxes = FXCollections.observableArrayList();
        for (String conn : connectionSet) {
            CheckBox cb = new CheckBox(conn);
            Image iconImage = null;
            if ("ETHERNET".equalsIgnoreCase(conn)) {
                iconImage = new Image(getClass().getResourceAsStream("/icons/ethernet.png"));
            } else if ("WIRELESS".equalsIgnoreCase(conn)) {
                iconImage = new Image(getClass().getResourceAsStream("/icons/wireless.png"));
            }
            if (iconImage != null) {
                ImageView iv = new ImageView(iconImage);
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                cb.setGraphic(iv);
            }
            connectionBox.getChildren().add(cb);
            connectionCheckBoxes.add(cb);
        }
        
        // Device filtering.
        VBox deviceBox = new VBox(10);
        Set<String> deviceSet = new HashSet<>();
        for (NetworkNode node : nodes) {
            if (!node.isMainNode()) {
                deviceSet.add(node.getDeviceType().toString());
            }
        }
        ObservableList<CheckBox> deviceCheckBoxes = FXCollections.observableArrayList();
        for (String device : deviceSet) {
            CheckBox cb = new CheckBox();
            HBox hb = new HBox(5);
            ImageView iv = new ImageView();
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            String iconFile;
            try {
                DeviceType dt = DeviceType.valueOf(device);
                switch(dt) {
                    case COMPUTER: iconFile = "host.png"; break;
                    case LAPTOP: iconFile = "laptop.png"; break;
                    case SERVER: iconFile = "server.png"; break;
                    case ROUTER: iconFile = "internet.png"; break;
                    case GATEWAY: iconFile = "gateway.png"; break;
                    case PHONE: iconFile = "phone.png"; break;
                    case TV: iconFile = "tv.png"; break;
                    case SECURITY_CAMERA: iconFile = "security_camera.png"; break;
                    case VIRTUAL_MACHINE: iconFile = "virtual_machine.png"; break;
                    default: iconFile = "host.png";
                }
            } catch(Exception e) {
                iconFile = "host.png";
            }
            iv.setImage(new Image(getClass().getResourceAsStream("/icons/" + iconFile)));
            Label lbl = new Label(device);
            hb.getChildren().addAll(iv, lbl);
            cb.setGraphic(hb);
            deviceBox.getChildren().add(cb);
            deviceCheckBoxes.add(cb);
        }
        
        // Initially show subnet options.
        advancedBox.getChildren().add(new Label("Select Subnets:"));
        advancedBox.getChildren().add(subnetBox);
        
        // Update advanced options when filter mode changes.
        filterModeBox.setOnAction(e -> {
            String mode = filterModeBox.getValue();
            advancedBox.getChildren().clear();
            if ("Subnet".equals(mode)) {
                advancedBox.getChildren().add(new Label("Select Subnets:"));
                advancedBox.getChildren().add(subnetBox);
            } else if ("Colour".equals(mode)) {
                advancedBox.getChildren().add(new Label("Select Colours:"));
                advancedBox.getChildren().add(colourBox);
            } else if ("Connection".equals(mode)) {
                advancedBox.getChildren().add(new Label("Select Connection Types:"));
                advancedBox.getChildren().add(connectionBox);
            } else if ("Device".equals(mode)) {
                advancedBox.getChildren().add(new Label("Select Device Types:"));
                advancedBox.getChildren().add(deviceBox);
            }
        });
        
        ScrollPane scrollPane = new ScrollPane(advancedBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(300);
        
        VBox mainContent = new VBox(10, modeBox, scrollPane);
        mainContent.setPadding(new Insets(10));
        
        // OK/Cancel buttons.
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        buttonBox.getChildren().addAll(okButton, cancelButton);
        
        VBox container = new VBox(10, mainContent, buttonBox);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("my-dialog-root");
        
        root.setCenter(container);
        
        Scene scene = new Scene(root, 400, 450, Color.TRANSPARENT);
        setScene(scene);
        
        okButton.setOnAction(e -> {
            String mode = filterModeBox.getValue();
            List<String> selected = new ArrayList<>();
            if ("Subnet".equals(mode)) {
                for (CheckBox cb : subnetCheckBoxes) {
                    if (cb.isSelected()) {
                        selected.add(cb.getText());
                    }
                }
                result = new FilterOptions(FilterOptions.FilterMode.SUBNET, selected);
            } else if ("Colour".equals(mode)) {
                for (CheckBox cb : colourCheckBoxes) {
                    if (cb.isSelected()) {
                        selected.add(cb.getUserData().toString());
                    }
                }
                result = new FilterOptions(FilterOptions.FilterMode.COLOUR, selected);
            } else if ("Connection".equals(mode)) {
                for (CheckBox cb : connectionCheckBoxes) {
                    if (cb.isSelected()) {
                        selected.add(cb.getText());
                    }
                }
                result = new FilterOptions(FilterOptions.FilterMode.CONNECTION, selected);
            } else if ("Device".equals(mode)) {
                for (CheckBox cb : deviceCheckBoxes) {
                    if (cb.isSelected()) {
                        HBox hb = (HBox) cb.getGraphic();
                        Label lbl = (Label) hb.getChildren().get(1);
                        selected.add(lbl.getText());
                    }
                }
                result = new FilterOptions(FilterOptions.FilterMode.DEVICE_TYPE, selected);
            }
            close();
        });
        
        cancelButton.setOnAction(e -> {
            result = null;
            close();
        });
    }
    
    private String getSubnet(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".";
        }
        return null;
    }
    
    public FilterOptions showAndGetResult() {
        showAndWait();
        return result;
    }
}
