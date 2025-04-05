package org.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class FilterDropdownPane extends VBox {
    private ComboBox<String> filterModeBox;
    private VBox advancedBox;
    private ObservableList<CheckBox> subnetCheckBoxes;
    private ObservableList<CheckBox> colourCheckBoxes;
    private ObservableList<CheckBox> connectionCheckBoxes;
    private ObservableList<CheckBox> deviceCheckBoxes;
    private List<NetworkNode> nodes;
    private Consumer<FilterOptions> onApply;
    private Runnable onCancel;

    public FilterDropdownPane(List<NetworkNode> nodes) {
        this.nodes = nodes;
        this.setStyle("-fx-background-color: #13213F; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 20px; -fx-background-radius: 20px;");
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // Mode selection
        HBox modeBox = new HBox(10);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        Label filterByLabel = new Label("Filter by:");
        filterByLabel.setTextFill(Color.WHITE);
        filterModeBox = new ComboBox<>();
        filterModeBox.getItems().addAll("Subnet", "Colour", "Connection", "Device");
        filterModeBox.setValue("Subnet");
        modeBox.getChildren().addAll(filterByLabel, filterModeBox);
        this.getChildren().add(modeBox);

        // Advanced options container
        advancedBox = new VBox(10);
        advancedBox.setPadding(new Insets(10));
        subnetCheckBoxes = FXCollections.observableArrayList();
        colourCheckBoxes = FXCollections.observableArrayList();
        connectionCheckBoxes = FXCollections.observableArrayList();
        deviceCheckBoxes = FXCollections.observableArrayList();
        updateAdvancedOptions();

        ScrollPane scrollPane = new ScrollPane(advancedBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(150);
        this.getChildren().add(scrollPane);

        // OK/Cancel buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        buttonBox.getChildren().addAll(okButton, cancelButton);
        this.getChildren().add(buttonBox);

        okButton.setOnAction(e -> {
            String mode = filterModeBox.getValue();
            List<String> selected = new ArrayList<>();
            if ("Subnet".equals(mode)) {
                for (CheckBox cb : subnetCheckBoxes) {
                    if (cb.isSelected()) {
                        selected.add(cb.getText());
                    }
                }
                if (onApply != null)
                    onApply.accept(new FilterOptions(FilterOptions.FilterMode.SUBNET, selected));
            } else if ("Colour".equals(mode)) {
                for (CheckBox cb : colourCheckBoxes) {
                    if (cb.isSelected()) {
                        selected.add(cb.getUserData().toString());
                    }
                }
                if (onApply != null)
                    onApply.accept(new FilterOptions(FilterOptions.FilterMode.COLOUR, selected));
            } else if ("Connection".equals(mode)) {
                for (CheckBox cb : connectionCheckBoxes) {
                    if (cb.isSelected()) {
                        selected.add(cb.getText());
                    }
                }
                if (onApply != null)
                    onApply.accept(new FilterOptions(FilterOptions.FilterMode.CONNECTION, selected));
            } else if ("Device".equals(mode)) {
                for (CheckBox cb : deviceCheckBoxes) {
                    if (cb.isSelected()) {
                        HBox hb = (HBox) cb.getGraphic();
                        Label lbl = (Label) hb.getChildren().get(0);
                        selected.add(lbl.getText());
                    }
                }
                if (onApply != null)
                    onApply.accept(new FilterOptions(FilterOptions.FilterMode.DEVICE_TYPE, selected));
            }
        });

        cancelButton.setOnAction(e -> {
            if (onCancel != null)
                onCancel.run();
        });

        filterModeBox.setOnAction(e -> updateAdvancedOptions());
    }

    private void updateAdvancedOptions() {
        String mode = filterModeBox.getValue();
        advancedBox.getChildren().clear();
        if ("Subnet".equals(mode)) {
            VBox subnetBox = new VBox(10);
            subnetCheckBoxes.clear();
            Set<String> subnetSet = new HashSet<>();
            for (NetworkNode node : nodes) {
                if (node.getNetworkType() == NetworkType.INTERNAL) {
                    String ip = node.getIpOrHostname();
                    if (ip != null && ip.startsWith("127.0.0")) continue;
                    String subnet = getSubnet(ip);
                    if (subnet != null)
                        subnetSet.add(subnet);
                    if (node.getResolvedIp() != null) {
                        subnet = getSubnet(node.getResolvedIp());
                        if (subnet != null)
                            subnetSet.add(subnet);
                    }
                }
            }
            for (String subnet : subnetSet) {
                CheckBox cb = new CheckBox(subnet);
                cb.setTextFill(Color.WHITE);
                subnetBox.getChildren().add(cb);
                subnetCheckBoxes.add(cb);
            }
            Label label = new Label("Select Subnets:");
            label.setTextFill(Color.WHITE);
            advancedBox.getChildren().addAll(label, subnetBox);
        } else if ("Colour".equals(mode)) {
            VBox colourBox = new VBox(10);
            colourCheckBoxes.clear();
            Set<String> colourSet = new HashSet<>();
            for (NetworkNode node : nodes) {
                if (!node.isMainNode()) {
                    String col = node.getOutlineColor();
                    if (col != null)
                        colourSet.add(col);
                }
            }
            for (String colour : colourSet) {
                CheckBox cb = new CheckBox();
                Region colorSwatch = new Region();
                colorSwatch.setPrefSize(20, 20);
                colorSwatch.setStyle("-fx-background-color: " + colour + "; -fx-border-color: black;");
                cb.setGraphic(colorSwatch);
                cb.setUserData(colour);
                cb.setTextFill(Color.WHITE);
                colourBox.getChildren().add(cb);
                colourCheckBoxes.add(cb);
            }
            Label label = new Label("Select Colours:");
            label.setTextFill(Color.WHITE);
            advancedBox.getChildren().addAll(label, colourBox);
        } else if ("Connection".equals(mode)) {
            VBox connectionBox = new VBox(10);
            connectionCheckBoxes.clear();
            Set<String> connectionSet = new HashSet<>();
            for (NetworkNode node : nodes) {
                if (!node.isMainNode()) {
                    String conn = node.getConnectionType().toString();
                    connectionSet.add(conn);
                }
            }
            for (String conn : connectionSet) {
                CheckBox cb = new CheckBox(conn);
                cb.setTextFill(Color.WHITE);
                connectionBox.getChildren().add(cb);
                connectionCheckBoxes.add(cb);
            }
            Label label = new Label("Select Connection Types:");
            label.setTextFill(Color.WHITE);
            advancedBox.getChildren().addAll(label, connectionBox);
        } else if ("Device".equals(mode)) {
            VBox deviceBox = new VBox(10);
            deviceCheckBoxes.clear();
            Set<String> deviceSet = new HashSet<>();
            for (NetworkNode node : nodes) {
                if (!node.isMainNode()) {
                    deviceSet.add(node.getDeviceType().toString());
                }
            }
            for (String device : deviceSet) {
                CheckBox cb = new CheckBox();
                HBox hb = new HBox(5);
                Label lbl = new Label(device);
                lbl.setTextFill(Color.WHITE);
                hb.getChildren().add(lbl);
                cb.setGraphic(hb);
                deviceBox.getChildren().add(cb);
                deviceCheckBoxes.add(cb);
            }
            Label label = new Label("Select Device Types:");
            label.setTextFill(Color.WHITE);
            advancedBox.getChildren().addAll(label, deviceBox);
        }
    }

    private String getSubnet(String ip) {
        if (ip == null)
            return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".";
        }
        return null;
    }

    public void setOnApply(Consumer<FilterOptions> onApply) {
        this.onApply = onApply;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void showDropdown() {
        if (!this.isVisible()) {
            this.setOpacity(0);
            this.setTranslateY(-10);
            this.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(300), this);
            ft.setFromValue(0);
            ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), this);
            tt.setFromY(-10);
            tt.setToY(0);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.play();
        }
    }

    public void hideDropdown() {
        if (this.isVisible()) {
            FadeTransition ft = new FadeTransition(Duration.millis(300), this);
            ft.setFromValue(1);
            ft.setToValue(0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), this);
            tt.setFromY(0);
            tt.setToY(-10);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setOnFinished(e -> this.setVisible(false));
            pt.play();
        }
    }
}
