package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NodeDetailPanel extends BorderPane {
    private static final double FIELD_WIDTH = 200;
    private static final double PANEL_WIDTH = 350;

    private TextField ipField;
    private ComboBox<DeviceType> deviceTypeBox;
    private ComboBox<NetworkType> networkTypeBox;
    private ComboBox<ConnectionType> connectionTypeBox;
    private ComboBox<String> routeSwitchBox;
    private Label macValueLabel;
    private Label uptimeLabel;
    private Button updateButton;
    private Label subnetValueLabel;
    private TextField nameEditField;
    private Label    nameLabel;


    private VBox topContent;
    private VBox fieldsBox;
    private Separator headerDivider;
    private BorderPane bottomContainer;

    private final NetworkNode node;

    public NodeDetailPanel(NetworkNode node) {
        this.node = node;
        createUI();
        populateFields();
        setupUpdateButtonHandler(); // Call the new method instead of setupListeners()

        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setMinWidth(PANEL_WIDTH);
        setPadding(new Insets(10));
        getStyleClass().add("nodedetail-panel");

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                NetworkMonitorApp.getInstance().hideDetailPanel();
            }
        });
    }

    // Add this new method with a single, clean implementation
    private void setupUpdateButtonHandler() {
        // Add listeners to enable the update button when fields change
        ipField.textProperty().addListener((o, oldV, newV) -> enableUpdate());
        deviceTypeBox.valueProperty().addListener((o, oldV, newV) -> enableUpdate());
        networkTypeBox.valueProperty().addListener((o, oldV, newV) -> enableUpdate());
        connectionTypeBox.valueProperty().addListener((o, oldV, newV) -> enableUpdate());
        routeSwitchBox.valueProperty().addListener((o, oldV, newV) -> enableUpdate());
        
        // Single update button action handler
        updateButton.setOnAction(e -> {
            // 1) Read and process the IP field
            String ipInput = ipField.getText().trim();
            String ipPart;
            if (ipInput.contains("/")) {
                String[] parts = ipInput.split("/", 2);
                ipPart = parts[0].trim();
            } else {
                ipPart = ipInput;
            }
            node.setIpOrHostname(ipPart);
            
            // 2) Handle display name
            String newDisplayName = nameEditField.isVisible() 
                ? nameEditField.getText().trim()
                : nameLabel.getText().trim();
            
            node.setDisplayName(newDisplayName);
            
            // 3) Update device properties
            node.setDeviceType(deviceTypeBox.getValue());
            node.setNetworkType(networkTypeBox.getValue());
            node.setConnectionType(connectionTypeBox.getValue());
            
            // 4) Handle routing through switch
            String routeSwitchName = routeSwitchBox.getValue();
            if (routeSwitchName != null && !routeSwitchName.equals("None")) {
                NetworkNode routeNode = NetworkMonitorApp.getInstance().getNodeByDisplayName(routeSwitchName);
                if (routeNode != null) {
                    node.setRouteSwitch(routeSwitchName);
                    node.setRouteSwitchId(routeNode.getNodeId());
                }
            } else {
                node.setRouteSwitch("");
                node.setRouteSwitchId(null);
            }
            
            // 5) Update UI state
            updateButton.setDisable(true);
            
            // 6) Update this node and its descendants' connection lines
            long thisId = node.getNodeId();
            System.out.println("\n==== Updating node " + node.getDisplayName() + " (ID: " + thisId + ") ====");
            
            // Use recursively to update this node and all its descendants
            NetworkMonitorApp.updateConnectionLinesRecursively(node);
            
            // 7) Store changes to disk
            NetworkMonitorApp.getInstance().saveNodesToFile();
            
            System.out.println("Update complete for " + node.getDisplayName());
            System.out.println("=============================================\n");
        });
    }

    private void createUI() {
        // ── Top content container ──
        topContent = new VBox(0);
        topContent.setAlignment(Pos.TOP_LEFT);
    
        // --- Display Name Header ---
        Label sectionLabel = new Label("Display Name");
        sectionLabel.getStyleClass().add("nodedetail-label");
    
        // 1) Label (default view)
        nameLabel = new Label(node.getDisplayName());
        nameLabel.getStyleClass().add("nodedetail-title-label");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    
        // 2) TextField (editable, hidden until edit-click)
        nameEditField = new TextField(node.getDisplayName());
        nameEditField.getStyleClass().add("nodedetail-namefield");
        nameEditField.setVisible(false);
        nameEditField.setMaxWidth(Double.MAX_VALUE);
        nameEditField.setBackground(Background.EMPTY);
        nameEditField.setBorder(Border.EMPTY);
    
        // 3) Raw edit icon
        ImageView editIcon = new ImageView(new Image(
            getClass().getResourceAsStream("/icons/edit.png")
        ));
        editIcon.setFitWidth(20);
        editIcon.setFitHeight(20);

        // 4) Wrap it for padding + hover
        StackPane editContainer = new StackPane(editIcon);
        editContainer.setPadding(new Insets(6));                          // larger hitbox
        editContainer.getStyleClass().add("edit-icon-container");
        editContainer.setCursor(Cursor.HAND);

        // start transparent
        editContainer.setBackground(new Background(new BackgroundFill(
            Color.TRANSPARENT, new CornerRadii(8), Insets.EMPTY
        )));

        // **only** this one call wires up the background‑color tween:
        applyBackgroundHover(editContainer);

        // 5) click → edit mode
        editContainer.setOnMouseClicked(e -> {
            nameEditField.setText(nameLabel.getText());
            nameLabel.setVisible(false);
            nameEditField.setVisible(true);
            nameEditField.requestFocus();
            nameEditField.selectAll();
            enableUpdate();
        });
    
        // 5) Put label+field into a single growing region
        StackPane namePane = new StackPane(nameLabel, nameEditField);
        namePane.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(namePane, Priority.ALWAYS);
    
        // 6) HBox: [ namePane | editContainer ]
        HBox nameRow = new HBox(2, namePane, editContainer);
        nameRow.setAlignment(Pos.CENTER_LEFT);
    
        VBox headerBox = new VBox(5, sectionLabel, nameRow);
        headerBox.setAlignment(Pos.TOP_LEFT);
        topContent.getChildren().add(headerBox);
    
        // --- Separator under header ---
        Region hairline_header = new Region();
        hairline_header.setMinHeight(0.5);
        hairline_header.setMaxHeight(0.5);
        hairline_header.setMaxWidth(Double.MAX_VALUE);  // allow full‑width stretch
        hairline_header.setBackground(new Background(new BackgroundFill(
            Color.web("#CCCCCC"),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));

        VBox.setMargin(hairline_header, new Insets(5, 0, 10, 0));
        topContent.getChildren().add(hairline_header);
    
        // --- The rest of your fields (IP, deviceType, etc.) ──
        fieldsBox = new VBox(20);
        fieldsBox.setAlignment(Pos.TOP_LEFT);
    
        // IP / Hostname
        Label ipLabel = new Label("IP/Hostname:");
        ipLabel.getStyleClass().add("nodedetail-label");
        ipField = new TextField();
        applyHoverTransition(ipField);
        ipField.setPrefWidth(FIELD_WIDTH);
        ipField.getStyleClass().add("nodedetail-textfield");
        VBox ipContainer = new VBox(5, ipLabel, ipField);
    
        // Device Type
        Label deviceTypeLabel = new Label("Device Type:");
        deviceTypeLabel.getStyleClass().add("nodedetail-label");
        deviceTypeBox = new ComboBox<>();
        applyHoverTransition(deviceTypeBox);
        deviceTypeBox.getStyleClass().add("nodedetail-combobox");
        deviceTypeBox.setPrefWidth(FIELD_WIDTH);
        deviceTypeBox.getItems().addAll(DeviceType.values());
        VBox deviceContainer = new VBox(5, deviceTypeLabel, deviceTypeBox);
    
        // Network Type
        Label networkTypeLabel = new Label("Network Type:");
        networkTypeLabel.getStyleClass().add("nodedetail-label");
        networkTypeBox = new ComboBox<>();
        applyHoverTransition(networkTypeBox);
        networkTypeBox.getStyleClass().add("nodedetail-combobox");
        networkTypeBox.setPrefWidth(FIELD_WIDTH);
        networkTypeBox.getItems().addAll(NetworkType.values());
        VBox networkContainer = new VBox(5, networkTypeLabel, networkTypeBox);
    
        // Connection Type
        Label connectionTypeLabel = new Label("Connection Type:");
        connectionTypeLabel.getStyleClass().add("nodedetail-label");
        connectionTypeBox = new ComboBox<>();
        applyHoverTransition(connectionTypeBox);
        connectionTypeBox.getStyleClass().add("nodedetail-combobox");
        connectionTypeBox.setPrefWidth(FIELD_WIDTH);
        connectionTypeBox.getItems().addAll(ConnectionType.values());
        VBox connectionContainer = new VBox(5, connectionTypeLabel, connectionTypeBox);
    
        // Route via Switch
        Label routeSwitchLabel = new Label("Route via Switch:");
        routeSwitchLabel.getStyleClass().add("nodedetail-label");
        routeSwitchBox = new ComboBox<>();
        applyHoverTransition(routeSwitchBox);
        routeSwitchBox.getStyleClass().add("nodedetail-combobox");
        routeSwitchBox.setPrefWidth(FIELD_WIDTH);
        updateRouteSwitchList();
        VBox routeContainer = new VBox(5, routeSwitchLabel, routeSwitchBox);
    
        Region hairline_editable = new Region();
        hairline_editable.setMinHeight(0.5);
        hairline_editable.setMaxHeight(0.5);
        hairline_editable.setMaxWidth(Double.MAX_VALUE);  // allow full‑width stretch
        hairline_editable.setBackground(new Background(new BackgroundFill(
            Color.web("#CCCCCC"),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        

        // MAC Address
        Label macLabel = new Label("MAC Address:");
        macLabel.getStyleClass().add("nodedetail-label");
        macValueLabel = new Label();
        macValueLabel.getStyleClass().add("nodedetail-value-label");
        VBox macContainer = new VBox(5, macLabel, macValueLabel);
    
        // Uptime
        Label uptimeStaticLabel = new Label("Uptime:");
        uptimeStaticLabel.getStyleClass().add("nodedetail-label");
        uptimeLabel = new Label();
        uptimeLabel.getStyleClass().add("nodedetail-value-label");
        VBox uptimeContainer = new VBox(5, uptimeStaticLabel, uptimeLabel);

        // … then add it into your fieldsBox right after uptimeContainer
    
        fieldsBox.getChildren().addAll(
            ipContainer, deviceContainer, networkContainer,
            connectionContainer, routeContainer,
            hairline_editable, macContainer, uptimeContainer
        );
    
        topContent.getChildren().add(fieldsBox);
        setCenter(topContent);
    
        // --- Bottom Buttons ---
        bottomContainer = new BorderPane();
        bottomContainer.setPadding(new Insets(10));
    
        updateButton = new Button("Update");
        updateButton.setDisable(true);
        updateButton.getStyleClass().add("nodedetail-updatebutton");
    
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("nodedetail-binbutton");
        deleteButton.setOnAction(e -> {
            NetworkMonitorApp.removeNode(node);
            NetworkMonitorApp.getInstance().hideDetailPanel();
        });
    
        bottomContainer.setCenter(updateButton);
        bottomContainer.setRight(deleteButton);
        setBottom(bottomContainer);
    
        // --- Persist changes on Update ---
        updateButton.setOnAction(e -> {
            // IP/Hostname
            String ipInput = ipField.getText();
            if (ipInput.contains("/")) {
                node.setIpOrHostname(ipInput.split("/", 2)[1].trim());
            } else {
                node.setIpOrHostname(ipInput);
            }

            // Display Name - capture new name
            String newDisplayName = nameEditField.isVisible()
                ? nameEditField.getText().trim()
                : nameLabel.getText().trim();

            // **Update the node’s own display name**
            node.setDisplayName(newDisplayName);

            // ——— ID-based propagation to all children ———
            long thisId = node.getNodeId();
            List<NetworkNode> children = NetworkMonitorApp.getPersistentNodesStatic().stream()
                .filter(n -> Objects.equals(n.getRouteSwitchId(), thisId)
                        || Objects.equals(n.getHostNodeId(),    thisId))
                .collect(Collectors.toList());

                

            for (NetworkNode child : children) {
                if (Objects.equals(child.getRouteSwitchId(), thisId)) {
                    child.setRouteSwitch(newDisplayName);
                }
                if (Objects.equals(child.getHostNodeId(), thisId)) {
                    child.setHostNode(newDisplayName);
                }
                // Redraw their connection line
                NetworkMonitorApp.updateConnectionLineForNode(child);
                System.out.println("Child " + child.getDisplayName()
                + " → routeSwitchId=" + child.getRouteSwitchId());
            }

            // Other fields
            node.setDeviceType(deviceTypeBox.getValue());
            node.setNetworkType(networkTypeBox.getValue());
            node.setConnectionType(connectionTypeBox.getValue());

            // Handle route switch
            String routeSwitchName = routeSwitchBox.getValue();
            if (routeSwitchName != null && !routeSwitchName.equals("None")) {
                NetworkNode routeNode = NetworkMonitorApp.getInstance().getNodeByDisplayName(routeSwitchName);
                if (routeNode != null) {
                    node.setRouteSwitch(routeSwitchName);
                    node.setRouteSwitchId(routeNode.getNodeId());
                }
            } else {
                node.setRouteSwitch("");
                node.setRouteSwitchId(null);
                
            }

            updateButton.setDisable(true);

            // Update this node’s own connection line
            NetworkMonitorApp.updateConnectionLinesRecursively(node);

            // If it's a switch/host, also refresh anyone who routes through it
            boolean isRouteProvider =
                node.getDeviceType() == DeviceType.UNMANAGED_SWITCH ||
                node.getDeviceType() == DeviceType.MANAGED_SWITCH   ||
                node.getDeviceType() == DeviceType.WIRELESS_ACCESS_POINT
                || node.getDeviceType() == DeviceType.COMPUTER; // host

            if (isRouteProvider) {
                System.out.println("\n==== DEBUG: Updating children of " + node.getDisplayName() + " (ID: " + thisId + ") ====");
                int childCount = 0;
                
                for (NetworkNode otherNode : NetworkMonitorApp.getPersistentNodesStatic()) {
                    if (Objects.equals(otherNode.getRouteSwitchId(), thisId)
                    || Objects.equals(otherNode.getHostNodeId(), thisId)) {
                        childCount++;
                        System.out.println("Child #" + childCount + ": " + otherNode.getDisplayName() + 
                                        " (ID: " + otherNode.getNodeId() + ")");
                        System.out.println("  - routeSwitchId: " + otherNode.getRouteSwitchId() + 
                                        ", routeSwitch name: " + otherNode.getRouteSwitch());
                        System.out.println("  - hostNodeId: " + otherNode.getHostNodeId() + 
                                        ", hostNode name: " + otherNode.getHostNode());
                        
                        // Update connection line for this child
                        System.out.println("  - Updating connection line for child...");
                        NetworkMonitorApp.updateConnectionLineForNode(otherNode);
                    }
                }
                
                if (childCount == 0) {
                    System.out.println("No children found that route through this node.");
                } else {
                    System.out.println("Updated " + childCount + " children nodes.");
                }
                System.out.println("=============================================\n");
            }

            // Persist immediately
            NetworkMonitorApp.getInstance().saveNodesToFile();
        });
    }


    private void populateFields() {
        ipField.setText(node.getResolvedIp() != null
            ? node.getResolvedIp() + "/" + node.getIpOrHostname()
            : node.getIpOrHostname());
        deviceTypeBox.setValue(node.getDeviceType());
        networkTypeBox.setValue(node.getNetworkType());
        connectionTypeBox.setValue(node.getConnectionType());
        
        // Fix the NullPointerException by checking for null
        String routeSwitch = node.getRouteSwitch();
        routeSwitchBox.setValue(
            routeSwitch == null || routeSwitch.isEmpty() ? "None" : routeSwitch
        );
        
        updateMacAddress();
        long uptimeSeconds = (System.currentTimeMillis() - node.getStartTime()) / 1000;
        uptimeLabel.setText(uptimeSeconds + " s");
    }

    // helper to animate bg‐color on hover
    private void applyBackgroundHover(Region region) {
        Color start = Color.TRANSPARENT;
        Color hover = Color.web("#38444A");   // slightly lighter panel‑bg
        DoubleProperty t = new SimpleDoubleProperty(0);
        t.addListener((obs, o, n) -> {
            Color c = start.interpolate(hover, n.doubleValue());
            region.setBackground(new Background(
            new BackgroundFill(c, new CornerRadii(8), Insets.EMPTY)
            ));
        });

        Timeline fadeIn = new Timeline(
        new KeyFrame(Duration.ZERO,      new KeyValue(t, 0)),
        new KeyFrame(Duration.millis(200), new KeyValue(t, 1))
        );
        fadeIn.setDelay(Duration.millis(100));  // 100ms delay

        Timeline fadeOut = new Timeline(
        new KeyFrame(Duration.ZERO,      new KeyValue(t, 1)),
        new KeyFrame(Duration.millis(200), new KeyValue(t, 0))
        );

        region.setOnMouseEntered(e -> { fadeOut.stop(); fadeIn.playFromStart(); });
        region.setOnMouseExited(e  -> { fadeIn.stop(); fadeOut.playFromStart(); });
    }


    private void updateRouteSwitchList() {
        routeSwitchBox.getItems().clear();
        routeSwitchBox.getItems().add("None");
        for (NetworkNode n : NetworkMonitorApp.getPersistentNodesStatic()) {
            DeviceType dt = n.getDeviceType();
            if ((dt == DeviceType.UNMANAGED_SWITCH || 
                 dt == DeviceType.MANAGED_SWITCH || 
                 dt == DeviceType.WIRELESS_ACCESS_POINT) 
                && !n.isMainNode()) {
                routeSwitchBox.getItems().add(n.getDisplayName());
            }
        }
    }

    private void enableUpdate() {
        updateButton.setDisable(false);
    }

    private void updateMacAddress() {
        new Thread(() -> {
            String mac = "N/A";
            if (node.getNetworkType() == NetworkType.INTERNAL) {
                String ip = node.getResolvedIp() != null ? node.getResolvedIp() : node.getIpOrHostname();
                mac = getMacAddressForIP(ip);
            }
            final String macFinal = mac;
            Platform.runLater(() -> macValueLabel.setText(macFinal));
        }).start();
    }

    private void applyHoverTransition(Region node) {
        Color bgStart = Color.web("#2E383C");
        Color bgHover = Color.web("#38444A");
        Color borderStart = Color.web("#979797");
        Color borderHover = Color.WHITE;
    
        DoubleProperty t = new SimpleDoubleProperty(0);
        t.addListener((obs, oldV, newV) -> {
            double v = newV.doubleValue();
            Color bg = bgStart.interpolate(bgHover, v);
            Color border = borderStart.interpolate(borderHover, v);
    
            node.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(10), Insets.EMPTY)));
            node.setBorder(new Border(new BorderStroke(border,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
    
            if (node instanceof ComboBox<?>) {
                // Handle selected cell
                Region displayNode = (Region) node.lookup(".list-cell");
                if (displayNode != null) {
                    displayNode.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(10), Insets.EMPTY)));
                    displayNode.setBorder(Border.EMPTY);
                }
    
                // Handle prompt-text (no selection)
                TextField promptText = (TextField) node.lookup(".text-input");
                if (promptText != null) {
                    promptText.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(10), Insets.EMPTY)));
                    promptText.setBorder(Border.EMPTY);
                    promptText.setStyle("-fx-text-fill: #979797; -fx-font-weight: 600; -fx-background-color: transparent;");
                }
    
                // Remove arrow background
                Region arrow = (Region) node.lookup(".arrow-button");
                if (arrow != null) {
                    arrow.setBackground(Background.EMPTY);
                }
            } else if (node instanceof TextField) {
                ((TextField) node).setBackground(new Background(new BackgroundFill(bg, new CornerRadii(10), Insets.EMPTY)));
            }
        });
    
        Timeline hoverIn = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(t, 0)),
            new KeyFrame(Duration.millis(200), new KeyValue(t, 1))
        );
        Timeline hoverOut = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(t, 1)),
            new KeyFrame(Duration.millis(200), new KeyValue(t, 0))
        );
    
        node.setOnMouseEntered(e -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        });
        node.setOnMouseExited(e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });
    }
    
    
    

    private String getMacAddressForIP(String ip) {
        try {
            Process p = Runtime.getRuntime().exec("arp -a");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains(ip)) {
                    String[] t = line.trim().split("\\s+");
                    if (t.length >= 2) return t[1].replace('-', ':');
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "N/A";
    }
}
