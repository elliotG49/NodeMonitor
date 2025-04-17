package org.example;

import java.util.HashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;




public class FilterBox extends StackPane {
    private final double FIELD_WIDTH     = 200;
    private final double WIDTH           = 50;
    private final double MIN_HEIGHT      = 50;
    private final double EXPANDED_WIDTH  = 250;
    private final double EXPANDED_HEIGHT = 500;
    private boolean expanded = false;

    private Label        minimizedLabel;
    private VBox         contentBox;
    private ComboBox<String> subnetComboBox;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ComboBox<String> connectionStatusBox;
    private Button       applyFilterButton;
    private Button       resetFilterButton;

    public FilterBox() {
        // root style
        getStyleClass().add("filterbox-panel");
        setPrefSize(WIDTH, MIN_HEIGHT);

        // collapsed icon
        minimizedLabel = new Label();
        minimizedLabel.getStyleClass().add("filterbox-minimized-icon");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // expanded content wrapper
        contentBox = new VBox(15);
        contentBox.getStyleClass().add("filterbox-content-box");
        contentBox.setPadding(new Insets(10));
        contentBox.setAlignment(Pos.TOP_CENTER);

        // --- Header ---
        VBox headerBox = new VBox();
        headerBox.getStyleClass().add("filterbox-header-box");
        Label titleLabel = new Label("Filter Node");
        titleLabel.getStyleClass().add("filterbox-title-label");
        headerBox.getChildren().add(titleLabel);

        // --- Subnet ---
        Label subnetLabel = new Label("Subnet:");
        subnetLabel.getStyleClass().add("filterbox-label");
        subnetComboBox = new ComboBox<>();
        subnetComboBox.getStyleClass().add("filterbox-combobox");
        subnetComboBox.setPrefWidth(FIELD_WIDTH);
        VBox subnetSection = new VBox(5, subnetLabel, subnetComboBox);

        // --- Device Type ---
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.getStyleClass().add("filterbox-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("filterbox-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        VBox deviceSection = new VBox(5, deviceLabel, deviceTypeBox);

        // --- Connection Type ---
        Label connectionLabel = new Label("Connection Type:");
        connectionLabel.getStyleClass().add("filterbox-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("filterbox-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        VBox connectionSection = new VBox(5, connectionLabel, connectionTypeBox);

        // --- Connection Status ---
        Label statusLabel = new Label("Connection Status:");
        statusLabel.getStyleClass().add("filterbox-label");
        connectionStatusBox = new ComboBox<>();
        connectionStatusBox.getStyleClass().add("filterbox-combobox");
        connectionStatusBox.setPrefWidth(FIELD_WIDTH);
        connectionStatusBox.getItems().addAll("Any", "Connected", "Disconnected");
        VBox statusSection = new VBox(5, statusLabel, connectionStatusBox);

        // --- Buttons ---
        applyFilterButton = new Button("Apply Filter");
        applyFilterButton.getStyleClass().add("filterbox-apply-button");
        resetFilterButton = new Button("Reset Filter");
        resetFilterButton.getStyleClass().add("filterbox-reset-button");
        HBox buttonSection = new HBox(10, applyFilterButton, resetFilterButton);
        buttonSection.setAlignment(Pos.CENTER);

        contentBox.getChildren().addAll(
            headerBox,
            subnetSection,
            deviceSection,
            connectionSection,
            statusSection,
            buttonSection
        );

        // keep it 15px above bottom
        sceneProperty().addListener((obs, o, n) -> {
            if (n != null && getParent() instanceof Region) {
                layoutYProperty().bind(
                    ((Region)getParent()).heightProperty()
                        .subtract(prefHeightProperty())
                        .subtract(15)
                );
            }
        });

        // toggle on click
        setOnMouseClicked(e -> toggle());

        // wire up
        applyFilterButton.setOnAction(e -> {
            applyFilters();
            NetworkMonitorApp.updateConnectionLinesVisibility();
        });
        resetFilterButton.setOnAction(e -> {
            resetFilters();
            applyFilters();
            NetworkMonitorApp.updateConnectionLinesVisibility();
        });
        // ――― Hover + Press effects ―――
        this.setOnMouseEntered(e -> {
            if (!expanded) {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
                st.setToX(1.05);
                st.setToY(1.05);
                st.play();
            }
        });
        this.setOnMouseExited(e -> {
            if (!expanded) {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            }
        });
        this.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
        });
        this.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        


        updateSubnetOptions();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void collapse() {
        if (!expanded) return;
    
        // brief delay to match the expand timing
        PauseTransition pause = new PauseTransition(Duration.millis(100));
        pause.setOnFinished(evt -> {
            expanded = false;
            getStyleClass().remove("expanded");
            getChildren().remove(contentBox);
    
            // animate back to collapsed size over 200ms
            Timeline collapseAnim = new Timeline(
                new KeyFrame(Duration.millis(200),
                    new KeyValue(prefWidthProperty(), WIDTH),
                    new KeyValue(prefHeightProperty(), MIN_HEIGHT)
                )
            );
            collapseAnim.setOnFinished(e -> {
                if (!getChildren().contains(minimizedLabel)) {
                    getChildren().add(minimizedLabel);
                }
            });
            collapseAnim.play();
        });
        pause.play();
    }
    
    private void expand() {
    if (expanded) return;

    // brief delay to match NewNodeBox timing
    PauseTransition pause = new PauseTransition(Duration.millis(100));
    pause.setOnFinished(evt -> {
        expanded = true;
        getStyleClass().add("expanded");
        getChildren().remove(minimizedLabel);

        // animate width & height over 200ms
        Timeline expandAnim = new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(prefWidthProperty(), EXPANDED_WIDTH),
                new KeyValue(prefHeightProperty(), EXPANDED_HEIGHT)
            )
        );
        expandAnim.setOnFinished(e -> {
            if (!getChildren().contains(contentBox)) {
                getChildren().add(contentBox);
            }
        });
        expandAnim.play();
    });
    pause.play();
}



    public void toggle() {
        if (expanded) collapse();
        else expand();
    }

    private void updateSubnetOptions() {
        Set<String> subnets = new HashSet<>();
        for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
            String ip = node.getResolvedIp() != null ? node.getResolvedIp() : node.getIpOrHostname();
            if ("127.0.0.1".equals(ip)) continue;
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                subnets.add(parts[0] + "." + parts[1] + "." + parts[2] + ".0/24");
            }
        }
        subnetComboBox.getItems().setAll("Any");
        subnetComboBox.getItems().addAll(subnets);
        subnetComboBox.setValue("Any");
    }

    private void applyFilters() {
        String subnetFilter = subnetComboBox.getValue();
        DeviceType dt = deviceTypeBox.getValue();
        ConnectionType ct = connectionTypeBox.getValue();
        String status = connectionStatusBox.getValue();
        for (NetworkNode node : NetworkMonitorApp.getPersistentNodesStatic()) {
            if (node.isMainNode()) {
                node.setVisible(true);
                continue;
            }
            boolean ok = true;
            if (!"Any".equals(subnetFilter)) {
                ok &= isIPInSubnet(
                    node.getResolvedIp() != null ? node.getResolvedIp() : node.getIpOrHostname(),
                    subnetFilter
                );
            }
            if (dt != null)        ok &= node.getDeviceType()    == dt;
            if (ct != null)        ok &= node.getConnectionType()== ct;
            if (!"Any".equals(status)) {
                ok &= "Connected".equals(status)
                    ? node.isConnected()
                    : !node.isConnected();
            }
            node.setVisible(ok);
        }
    }

    private void resetFilters() {
        subnetComboBox.setValue("Any");
        deviceTypeBox.setValue(null);
        connectionTypeBox.setValue(null);
        connectionStatusBox.setValue("Any");
    }

    private boolean isIPInSubnet(String ip, String subnet) {
        try {
            String[] parts   = subnet.split("/");
            int prefix       = Integer.parseInt(parts[1]);
            int ipInt        = ipToInt(ip);
            int subnetInt    = ipToInt(parts[0]);
            int mask         = prefix == 0 ? 0 : 0xFFFFFFFF << (32 - prefix);
            return (ipInt & mask) == (subnetInt & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private int ipToInt(String ip) {
        String[] p = ip.split("\\.");
        int res = 0;
        for (String part : p) {
            res = (res << 8) + Integer.parseInt(part);
        }
        return res;
    }
}
