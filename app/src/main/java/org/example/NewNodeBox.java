// NewNodeBox.java
package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NewNodeBox extends StackPane {
    private static final double FIELD_WIDTH = 200;
    private static final double GAP = 10;

    private final Label minimizedLabel;
    private final VBox contentBox;

    private final VBox headerSection;
    private final VBox deviceTypeSection;
    private final VBox managedTypeSection;       // NEW
    private final VBox nameSection;
    private final VBox ipSection;
    private final VBox networkSection;
    private final VBox routeSection;
    private final VBox connectionSection;
    private final VBox createButtonSection;

    private final ComboBox<DeviceType> deviceTypeBox;
    private final ComboBox<String>    managedTypeBox; // NEW
    private final TextField           nameField;
    private final TextField           ipField;
    private final ComboBox<NetworkType> networkTypeBox;
    private final ComboBox<String>      routeSwitchBox;
    private final ComboBox<ConnectionType> connectionTypeBox;
    private final Button              createButton;

    private boolean expanded = false;

    public NewNodeBox() {
        // Base styling
        getStyleClass().add("newnodebox-panel");

        // Collapsed “+” label
        minimizedLabel = new Label("+");
        minimizedLabel.getStyleClass().add("newnodebox-minimized-label");
        setAlignment(minimizedLabel, Pos.CENTER);
        getChildren().add(minimizedLabel);

        // Content container
        contentBox = new VBox(GAP);
        contentBox.getStyleClass().add("newnodebox-content-box");
        contentBox.setPadding(new Insets(10));
        contentBox.setVisible(false);
        contentBox.managedProperty().bind(contentBox.visibleProperty());
        getChildren().add(contentBox);

        // 1) Header
        Label titleLabel = new Label("Add Node");
        titleLabel.getStyleClass().add("newnodebox-title-label");
        headerSection = new VBox(titleLabel);
        headerSection.getStyleClass().add("newnodebox-header-box");

        // 2) Device Type
        Label deviceLabel = new Label("Device Type:");
        deviceLabel.getStyleClass().add("newnodebox-label");
        deviceTypeBox = new ComboBox<>();
        deviceTypeBox.getStyleClass().add("newnodebox-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        HBox deviceContainer = new HBox(deviceTypeBox);
        deviceContainer.setAlignment(Pos.CENTER);
        deviceTypeSection = new VBox(5, deviceLabel, deviceContainer);

        // 3) Managed? (ONLY for SWITCH)
        Label managedLabel = new Label("Managed:");
        managedLabel.getStyleClass().add("newnodebox-label");
        managedTypeBox = new ComboBox<>();
        managedTypeBox.getStyleClass().add("newnodebox-combobox");
        managedTypeBox.setPrefWidth(FIELD_WIDTH);
        managedTypeBox.getItems().addAll("Yes", "No");
        HBox managedContainer = new HBox(managedTypeBox);
        managedContainer.setAlignment(Pos.CENTER);
        managedTypeSection = new VBox(5, managedLabel, managedContainer);

        // 4) Display Name
        Label nameLabel = new Label("Display Name:");
        nameLabel.getStyleClass().add("newnodebox-label");
        nameField = new TextField();
        nameField.getStyleClass().add("newnodebox-textfield");
        nameField.setPromptText("eg PC‑1");
        nameField.setPrefWidth(FIELD_WIDTH);
        HBox nameContainer = new HBox(nameField);
        nameContainer.setAlignment(Pos.CENTER);
        nameSection = new VBox(5, nameLabel, nameContainer);

        // 5) Hostname/IP
        Label ipLabel = new Label("Hostname/IP:");
        ipLabel.getStyleClass().add("newnodebox-label");
        ipField = new TextField();
        ipField.getStyleClass().add("newnodebox-textfield");
        ipField.setPromptText("eg 192.168.1.1");
        ipField.setPrefWidth(FIELD_WIDTH);
        HBox ipContainer = new HBox(ipField);
        ipContainer.setAlignment(Pos.CENTER);
        ipSection = new VBox(5, ipLabel, ipContainer);

        // 6) Network Type
        Label netLabel = new Label("Network Type:");
        netLabel.getStyleClass().add("newnodebox-label");
        networkTypeBox = new ComboBox<>();
        networkTypeBox.getStyleClass().add("newnodebox-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        HBox netContainer = new HBox(networkTypeBox);
        netContainer.setAlignment(Pos.CENTER);
        networkSection = new VBox(5, netLabel, netContainer);

        // 7) Route via
        Label routeLabel = new Label("Network Route:");
        routeLabel.getStyleClass().add("newnodebox-label");
        routeSwitchBox = new ComboBox<>();
        routeSwitchBox.getStyleClass().add("newnodebox-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        HBox routeContainer = new HBox(routeSwitchBox);
        routeContainer.setAlignment(Pos.CENTER);
        routeSection = new VBox(5, routeLabel, routeContainer);

        // 8) Connection Type
        Label connLabel = new Label("Connection Type:");
        connLabel.getStyleClass().add("newnodebox-label");
        connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getStyleClass().add("newnodebox-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        HBox connContainer = new HBox(connectionTypeBox);
        connContainer.setAlignment(Pos.CENTER);
        connectionSection = new VBox(5, connLabel, connContainer);

        // 9) Create button
        createButton = new Button("Create");
        createButton.getStyleClass().add("newnodebox-create-button");
        createButton.setPrefWidth(150);
        createButtonSection = new VBox(createButton);
        createButtonSection.setAlignment(Pos.CENTER);

        // Bind managed ⇄ visible
        for (VBox sec : new VBox[]{
            headerSection, deviceTypeSection, managedTypeSection,
            nameSection, ipSection, networkSection,
            routeSection, connectionSection, createButtonSection
        }) {
            sec.managedProperty().bind(sec.visibleProperty());
        }

        // Add in order
        contentBox.getChildren().addAll(
            headerSection,
            deviceTypeSection,
            managedTypeSection,
            nameSection,
            ipSection,
            networkSection,
            routeSection,
            connectionSection,
            createButtonSection
        );

        // Initial visibilities
        headerSection.setVisible(true);
        deviceTypeSection.setVisible(true);
        managedTypeSection.setVisible(false);
        nameSection.setVisible(false);
        ipSection.setVisible(false);
        networkSection.setVisible(false);
        routeSection.setVisible(false);
        connectionSection.setVisible(false);
        createButtonSection.setVisible(false);

        // Expand/collapse toggle
        setOnMouseClicked(this::toggle);
        setOnKeyPressed(e -> {
            if (expanded && e.getCode() == KeyCode.ESCAPE) collapse();
        });

        // When DeviceType changes…
        deviceTypeBox.valueProperty().addListener((obs, oldType, newType) -> {
            // hide all details
            managedTypeSection.setVisible(false);
            nameSection.setVisible(false);
            ipSection.setVisible(false);
            networkSection.setVisible(false);
            routeSection.setVisible(false);
            connectionSection.setVisible(false);
            createButtonSection.setVisible(false);

            if (newType != null) {
                switch (newType) {
                    case COMPUTER:
                    case LAPTOP:
                    case SERVER:
                        nameSection.setVisible(true);
                        ipSection.setVisible(true);
                        networkSection.setVisible(true);
                        connectionSection.setVisible(true);
                        routeSection.setVisible(true);
                        createButtonSection.setVisible(true);
                        break;
                    case SWITCH:
                        managedTypeSection.setVisible(true);
                        managedTypeBox.setValue("Yes");
                        break;
                    case VIRTUAL_MACHINE:
                        nameSection.setVisible(true);
                        createButtonSection.setVisible(true);
                        break;
                    default:
                        nameSection.setVisible(true);
                        createButtonSection.setVisible(true);
                }
            }
            if (expanded) resizeToContent();
        });

        // When Managed = Yes/No for SWITCH…
        managedTypeBox.valueProperty().addListener((obs, o, val) -> {
            nameSection.setVisible(false);
            ipSection.setVisible(false);
            networkSection.setVisible(false);
            routeSection.setVisible(false);
            connectionSection.setVisible(false);
            createButtonSection.setVisible(false);

            if ("Yes".equals(val)) {
                nameSection.setVisible(true);
                ipSection.setVisible(true);
                networkSection.setVisible(true);
                routeSection.setVisible(true);
                createButtonSection.setVisible(true);
            } else {
                nameSection.setVisible(true);
                networkSection.setVisible(true);
                routeSection.setVisible(true);
                createButtonSection.setVisible(true);
            }
            if (expanded) resizeToContent();
        });

                createButton.setOnAction(e -> {
            // Gather inputs
            DeviceType dt = deviceTypeBox.getValue();
            String name = nameField.getText().trim();
            String ip   = ipField.getText().trim();
            NetworkType nt = networkTypeBox.getValue();
            ConnectionType ct = connectionTypeBox.getValue();
            String routeVia = routeSwitchBox.getValue();

            // Basic validation
            if (dt == null || name.isEmpty() || ip.isEmpty() || nt == null || routeVia == null) {
                return; // you could flash an error here
            }

            // Create and configure the node
            NetworkNode node = new NetworkNode(ip, name, dt, nt);
            node.setConnectionType(ct);
            node.setRouteSwitch(routeVia);

            // Add to the app
            NetworkMonitorApp.addNewNode(node);

            // Collapse the form
            collapse();
        });
        
    }

    private void resizeToContent() {
        double targetH = contentBox.prefHeight(-1);
        double targetW = contentBox.prefWidth(-1);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(prefHeightProperty(), getHeight()),
                new KeyValue(prefWidthProperty(), getWidth())
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(prefHeightProperty(), targetH),
                new KeyValue(prefWidthProperty(), targetW)
            )
        );
        tl.play();
    }

        private void updateRouteSwitchList() {
            routeSwitchBox.getItems().clear();

            // 1) Always add the main Gateway first
            String gatewayName = null;
            for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                if (n.isMainNode() && n.getDeviceType() == DeviceType.GATEWAY) {
                    gatewayName = n.getDisplayName();
                    break;
                }
            }
            if (gatewayName != null) {
                routeSwitchBox.getItems().add(gatewayName);
            }

            // 2) Then add any switches or WAPs (excluding the gateway if it was mis-typed as one)
            for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
                DeviceType dt = n.getDeviceType();
                String name = n.getDisplayName();
                if ((dt == DeviceType.SWITCH || dt == DeviceType.WIRELESS_ACCESS_POINT)
                        && !name.equals(gatewayName)) {
                    routeSwitchBox.getItems().add(name);
                }
            }

            // 3) Select the gateway by default (or first item if no gateway found)
            if (!routeSwitchBox.getItems().isEmpty()) {
                routeSwitchBox.setValue(routeSwitchBox.getItems().get(0));
            }
        }



        public void expand() {
            if (expanded) return;

            // 0) Refresh the Route-via list
            updateRouteSwitchList();

            // 1) Apply the expanded style so padding/insets are active
            getStyleClass().add("expanded");
            minimizedLabel.setVisible(false);

            // 2) Force a CSS+layout pass so we can read the new insets
            applyCss();
            layout();
            Insets pad = getPadding();

            // 3) Temporarily show the contentBox so its pref size can be measured
            contentBox.setVisible(true);
            contentBox.applyCss();
            contentBox.layout();
            double contentW = contentBox.prefWidth(-1);
            double contentH = contentBox.prefHeight(-1);

            // 4) Compute the final target panel size = content size + padding
            double targetW = contentW + pad.getLeft() + pad.getRight();
            double targetH = contentH + pad.getTop()  + pad.getBottom();

            // 5) Hide the content while we animate
            contentBox.setVisible(false);

            // 6) Match FilterBox timing: 100ms pause, then 200ms simultaneous resize
            PauseTransition pause = new PauseTransition(Duration.millis(100));
            pause.setOnFinished(e -> {
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(prefWidthProperty(),  getWidth()),
                        new KeyValue(prefHeightProperty(), getHeight())
                    ),
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(prefWidthProperty(),  targetW),
                        new KeyValue(prefHeightProperty(), targetH)
                    )
                );
                tl.setOnFinished(ev -> {
                    // 7) Finally show the inputs
                    contentBox.setVisible(true);
                });
                tl.play();
                expanded = true;
            });
            pause.play();
        }



        public void collapse() {
            if (!expanded) {
                // If it’s already collapsed, just ensure the “+” is visible
                minimizedLabel.applyCss();
                minimizedLabel.layout();
                minimizedLabel.setVisible(true);
                contentBox.setVisible(false);
                return;
            }

            // 1) Brief delay to match expand timing
            PauseTransition pause = new PauseTransition(Duration.millis(100));
            pause.setOnFinished(evt -> {
                // 2) Flip the expanded flag & style
                expanded = false;
                getStyleClass().remove("expanded");

                // 3) Hide the form inputs immediately
                contentBox.setVisible(false);

                // 4) Compute the final “minimized” size
                minimizedLabel.applyCss();
                minimizedLabel.layout();
                double targetW = minimizedLabel.prefWidth(-1);
                double targetH = minimizedLabel.prefHeight(-1);

                // 5) Animate from current size back down
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(prefWidthProperty(),  getWidth()),
                        new KeyValue(prefHeightProperty(), getHeight())
                    ),
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(prefWidthProperty(),  targetW),
                        new KeyValue(prefHeightProperty(), targetH)
                    )
                );
                tl.setOnFinished(e -> {
                    // 6) Finally show the “+”
                    minimizedLabel.setVisible(true);
                });
                tl.play();
            });
            pause.play();
        }

    public void toggle(MouseEvent e) {
        e.consume();
        if (expanded) collapse();
        else          expand();
    }

    public boolean isExpanded() {
        return expanded;
    }
}
