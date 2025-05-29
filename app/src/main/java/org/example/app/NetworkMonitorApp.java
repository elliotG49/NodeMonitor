package org.example.app;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.example.config.NodeConfig;
import org.example.config.WindowConfig;
import org.example.model.ConnectionType;
import org.example.model.DeviceType;
import org.example.model.NetworkLocation;
import org.example.model.NetworkNode;

import org.example.ui.components.ConnectionLine;

import org.example.ui.forms.SlideOutForms;
import org.example.ui.panels.NodeDetailPanel;
import org.example.ui.panels.SlideOutPanel;

import org.example.util.NetworkUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NetworkMonitorApp extends Application {
    private VBox modePanel;  // Change to class field if not already

    public Pane spiderMapPane;
    private List<NetworkNode> persistentNodes = new ArrayList<>();
    private static List<NetworkNode> persistentNodesStatic = new ArrayList<>();

    private VBox statusPanel;
    private Label totalLabel, upLabel, downLabel;
    private SlideOutPanel slidePanel;

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + "NetworkMonitorApp";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "nodes.json";
    private static final String WINDOW_CONFIG_FILE = CONFIG_DIR + File.separator + "window.config";
    private static final double DETAIL_PANEL_WIDTH = 350;
    private static final double PANEL_WIDTH = 300;

    private EventHandler<MouseEvent> panelCloseHandler;

    private static NetworkMonitorApp instance;
    private Stage primaryStage;

    private double prevSceneWidth = 0;
    private double prevSceneHeight = 0;

    private VBox filterStatusBox;

    // Add this as a class field
    private NodeDetailPanel nodeDetailPanel;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;

        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setTitle("Network Node Monitor");
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/node.png")));
        } catch (Exception e) {
            System.out.println("Failed to load icon: " + e.getMessage());
        }

        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        BorderPane root = new BorderPane();
        spiderMapPane = createSpiderMapPane();
        createFilterStatusBox();  // Add this line
        StackPane centerStack = new StackPane();
        centerStack.getChildren().add(spiderMapPane);
        root.setCenter(centerStack);

        VBox modePanel = new VBox(5);  // Change this line to use class field
        this.modePanel = modePanel;    // Store reference

        modePanel.getStyleClass().add("mode-panel");

        // Create each row
        Button addBtn      = createModeRow("/icons/plus.png",     "Add Node");
        Button discoverBtn = createModeRow("/icons/search.png",   "Discover Node");
        Button filterBtn   = createModeRow("/icons/filter.png",   "Filter Nodes");
        Button settingsBtn = createModeRow("/icons/settings.png", "Settings");  // Add this line

        // Add rows
        modePanel.getChildren().addAll(addBtn, discoverBtn, filterBtn, settingsBtn);  // Add settingsBtn here

        // Position 15px from left & bottom
        modePanel.setLayoutX(15);
        modePanel.layoutYProperty().bind(
            spiderMapPane.heightProperty()
                .subtract(modePanel.heightProperty())
                .subtract(15)
        );

        spiderMapPane.getChildren().add(modePanel);

        
        if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) {
            loadWindowSize();
        } else {
            primaryStage.setMaximized(true);
        }

        slidePanel = new SlideOutPanel(250);

        // 2) add *before* any call to show()/hide()
        spiderMapPane.getChildren().add(slidePanel);
 

        // 3) lay it out at x=15 (flush) & bind its y so its bottom is 15px above modePanel
        double HIDE_OFFSET = PANEL_WIDTH + 15;
        slidePanel.setTranslateX(-HIDE_OFFSET);
        slidePanel.layoutYProperty().bind(
            modePanel.layoutYProperty()          // y-coordinate of top of your button-stack
            .subtract(15)                      // gap you wanted
            .subtract(slidePanel.heightProperty())  // move up by its own height
        );

        // 5) wire your “Add” button:
        addBtn.setOnAction(e -> {
            slidePanel.setContent(SlideOutForms.buildAddNodeForm(slidePanel));
            slidePanel.show();
        });

        filterBtn.setOnAction(e -> {
            slidePanel.setContent(SlideOutForms.buildFilterForm(slidePanel));
            slidePanel.show();
        });

        discoverBtn.setOnAction(e -> {
            slidePanel.setContent(SlideOutForms.buildDiscoveryLoadingPanel(slidePanel));
            slidePanel.show();
        });


        Scene scene = new Scene(root);
        centerStack.setStyle("-fx-background-color: #192428;");
        scene.getStylesheets().add(getClass().getResource("/styles/variables.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        scene.getStylesheets().add(getClass().getResource("/styles/filter.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/latency-label.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/mode-panel.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/node-discovery.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/node-styling.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/slide-panel.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/node-detail-panel.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        prevSceneWidth = scene.getWidth();
        prevSceneHeight = scene.getHeight();

        Platform.runLater(() -> {
            if (!Files.exists(Paths.get(CONFIG_FILE))) {
                createDefaultMainNodes();
            } else {
                loadNodesFromFile();
            }
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double newWidth = newVal.doubleValue();
            double ratio = newWidth / prevSceneWidth;
            for (NetworkNode node : persistentNodes) {
                double newX = node.getLayoutX() * ratio;
                node.setLayoutX(newX);
                if (newX + node.getWidth() > newWidth) node.setLayoutX(newWidth - node.getWidth());
            }
            prevSceneWidth = newWidth;
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            double newHeight = newVal.doubleValue();
            double ratio = newHeight / prevSceneHeight;
            for (NetworkNode node : persistentNodes) {
                double newY = node.getLayoutY() * ratio;
                node.setLayoutY(newY);
                if (newY + node.getHeight() > newHeight) node.setLayoutY(newHeight - node.getHeight());
            }
            prevSceneHeight = newHeight;
        });

        Timeline connectionTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            for (javafx.scene.Node node : spiderMapPane.getChildren()) {
                if (node instanceof ConnectionLine) ((ConnectionLine) node).updateStatus();
            }
        }));
        connectionTimeline.setCycleCount(Timeline.INDEFINITE);
        connectionTimeline.play();

        createFilterStatusBox();

        // Add this in the start() method, after creating centerStack
        nodeDetailPanel = new NodeDetailPanel();
        centerStack.getChildren().add(nodeDetailPanel);

        // Make sure the panel stays on top and at the right edge
        StackPane.setAlignment(nodeDetailPanel, Pos.TOP_RIGHT);

        // Add in the start() method after creating the scene
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (nodeDetailPanel != null) {
                nodeDetailPanel.setPrefHeight(scene.getHeight());
            }
        });
    }

    public static void updateConnectionLinesVisibility() {
        instance.spiderMapPane.getChildren().forEach(child -> {
            if (child instanceof ConnectionLine) {
                ConnectionLine line = (ConnectionLine) child;
                boolean bothVisible = line.getFrom().isVisible() && line.getTo().isVisible();
                line.setVisible(bothVisible);
            }
        });
    }

    public static NetworkMonitorApp getInstance() { return instance; }

    public static void removeNode(NetworkNode node) {
        instance.persistentNodes.remove(node);
        persistentNodesStatic.remove(node);
        instance.spiderMapPane.getChildren().remove(node);
        instance.spiderMapPane.getChildren().removeIf(child ->
            child instanceof ConnectionLine && (((ConnectionLine) child).getFrom() == node || ((ConnectionLine) child).getTo() == node)
        );
        instance.saveNodesToFile();
    }

    public static void addNewNode(NetworkNode node) {
        instance.persistentNodes.add(node);
        persistentNodesStatic.add(node);
        node.setViewOrder(-2); // Ensures nodes are above connection lines but below labels
        instance.spiderMapPane.getChildren().add(node);

        if (!node.isMainNode()) {
            // Use updateConnectionLineForNode instead of addDefaultConnectionLine
            // This will properly handle all connection types, including route switches
            updateConnectionLineForNode(node);
        }
        
        // Add click handler for node detail panel
        node.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                instance.nodeDetailPanel.showForNode(node);
                e.consume();
            }
        });
    }

    public static NetworkNode getUpstreamNode(NetworkNode node) {
        if (node.getConnectionType() == ConnectionType.VIRTUAL)
            return instance.getMainNodeByDisplayName("Host");
        return instance.getMainNodeByDisplayName("Gateway");
    }

    private void loadWindowSize() {
        try {
            if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) {
                String json = new String(Files.readAllBytes(Paths.get(WINDOW_CONFIG_FILE)));
                Gson gson = new Gson();
                WindowConfig wc = gson.fromJson(json, WindowConfig.class);
                if (wc != null) {
                    primaryStage.setX(wc.getX());
                    primaryStage.setY(wc.getY());
                    primaryStage.setWidth(wc.getWidth());
                    primaryStage.setHeight(wc.getHeight());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveWindowSize() {
        try {
            WindowConfig wc = new WindowConfig(primaryStage.getX(), primaryStage.getY(),
                                              primaryStage.getWidth(), primaryStage.getHeight());
            Gson gson = new Gson();
            String json = gson.toJson(wc);
            Files.write(Paths.get(WINDOW_CONFIG_FILE), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Pane createSpiderMapPane() {
        Pane pane = new Pane();
        pane.getStyleClass().add("spider-map-pane");
        return pane;
    }

    private void createDefaultMainNodes() {
        double centerX = primaryStage.getWidth() / 2;
        double centerY = primaryStage.getHeight() / 2;
        double spacing = 300;

        // Update the Host node to use LOCAL network location
        NetworkNode hostNode = new NetworkNode("127.0.0.1", "Host", DeviceType.COMPUTER, NetworkLocation.LOCAL);
        hostNode.setLayoutX(centerX - hostNode.getPrefWidth() / 2);
        hostNode.setLayoutY(centerY - spacing - hostNode.getPrefHeight() / 2);
        hostNode.setMainNode(true);
        persistentNodes.add(hostNode);
        persistentNodesStatic.add(hostNode);
        spiderMapPane.getChildren().add(hostNode);

        String gw = NetworkUtils.getDefaultGateway();
        if (gw == null) gw = "192.168.0.1";
        // Update the Gateway node to use PUBLIC network location
        NetworkNode gatewayNode = new NetworkNode(gw, "Gateway", DeviceType.GATEWAY, NetworkLocation.PUBLIC);
        gatewayNode.setLayoutX(centerX - gatewayNode.getPrefWidth() / 2);
        gatewayNode.setLayoutY(centerY - gatewayNode.getPrefHeight() / 2);
        gatewayNode.setMainNode(true);
        persistentNodes.add(gatewayNode);
        persistentNodesStatic.add(gatewayNode);
        spiderMapPane.getChildren().add(gatewayNode);

        // Create single connection between host and gateway
        ConnectionLine line1 = new ConnectionLine(hostNode, gatewayNode);
        spiderMapPane.getChildren().add(0, line1);

        addNodeDetailHandlers(); // Add this line
    }

    private Button createModeRow(String iconPath, String text) {
    // Icon inside its own small square
    ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
    iv.setFitWidth(15);
    iv.setFitHeight(15);
    StackPane iconContainer = new StackPane(iv);
    iconContainer.getStyleClass().add("mode-icon-container");
    iconContainer.setPrefSize(30, 30);

    // Button with icon + text
    Button btn = new Button(text, iconContainer);
    btn.getStyleClass().add("mode-button");
    btn.setContentDisplay(ContentDisplay.LEFT);
    btn.setGraphicTextGap(10);
    btn.setMinWidth(180);         // or whatever width you like
    btn.setAlignment(Pos.CENTER_LEFT);
    return btn;
}


    private void loadNodesFromFile() {
        Platform.runLater(() -> {
            try {
                System.out.println("\n=== LOADING NODES FROM FILE ===");
                String json = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                Gson gson = new Gson();
                Type listType = new TypeToken<List<NodeConfig>>(){}.getType();
                List<NodeConfig> configs = gson.fromJson(json, listType);
                double paneWidth = spiderMapPane.getWidth();
                double paneHeight = spiderMapPane.getHeight();
                if (paneWidth < 100) paneWidth = primaryStage.getScene().getWidth();
                if (paneHeight < 100) paneHeight = primaryStage.getScene().getHeight();

                // First, create nodes without any connections
                for (NodeConfig config : configs) {
                    NetworkNode node = new NetworkNode(
                        config.getIpOrHostname(), 
                        config.getDisplayName(),
                        config.getDeviceType(), 
                        config.getNetworkLocation()
                    );
                    node.setPrefSize(config.getWidth(), config.getHeight());
                    node.updateLayoutForSavedSize();
                    node.setLayoutX(config.getRelativeX() * paneWidth);
                    node.setLayoutY(config.getRelativeY() * paneHeight);
                    node.setMainNode(config.isMainNode());
                    if (config.getConnectionType() != null)
                        node.setConnectionType(config.getConnectionType());
                        
                    // Explicitly set node ID first if available
                    if (config.getNodeId() != null) {
                        node.setNodeIdDirectly(config.getNodeId());
                    }
                    
                    // Set IDs directly instead of via names to prevent lookup issues
                    node.setRouteSwitchId(config.getRouteSwitchId());
                    node.setHostNodeId(config.getHostNodeId());
                    
                    // Set names for UI display only
                    if (config.getRouteSwitch() != null) {
                        node.setRouteSwitchWithoutIdUpdate(config.getRouteSwitch());
                    }
                    if (config.getHostNode() != null) {
                        node.setHostNodeWithoutIdUpdate(config.getHostNode());
                    }
                    

                    persistentNodes.add(node);
                    persistentNodesStatic.add(node);
                    spiderMapPane.getChildren().add(node);
                }

                // Now create connections for ALL nodes based on their routing information
                for (NetworkNode node : persistentNodes) {
                    // Check if the node has a switch routing defined
                    if (node.getRouteSwitchId() != null) {
                        // This is the key change - call updateConnectionLineForNode for ALL nodes with a routeSwitchId
                        // regardless of whether they are main nodes or not
                        updateConnectionLineForNode(node);
                    } else if (!node.isMainNode()) {
                        // Create default connection only for non-main, non-switch-routed nodes
                        addDefaultConnectionLine(node);
                    }
                }

                // Finally, connect main nodes that aren't routed through switches to each other
                List<NetworkNode> unroutedMainNodes = new ArrayList<>();
                for (NetworkNode node : persistentNodes) {
                    if (node.isMainNode() && node.getRouteSwitchId() == null) {
                        unroutedMainNodes.add(node);
                    }
                }
                
                // Connect unrouted main nodes to each other
                for (int i = 0; i < unroutedMainNodes.size(); i++) {
                    for (int j = i + 1; j < unroutedMainNodes.size(); j++) {
                        NetworkNode node1 = unroutedMainNodes.get(i);
                        NetworkNode node2 = unroutedMainNodes.get(j);
                        ConnectionLine c = new ConnectionLine(node1, node2);
                        spiderMapPane.getChildren().add(0, c);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            // After loading all nodes, print debug info
            System.out.println("\nLoaded Nodes Configuration:");
            for (NetworkNode node : persistentNodes) {
                System.out.printf("Node: %-20s | Type: %-15s | Routed via: %s%n",
                    node.getDisplayName(),
                    node.getDeviceType(),
                    (node.getRouteSwitch() != null && !node.getRouteSwitch().isEmpty()) 
                        ? node.getRouteSwitch() 
                        : "none"
                );
            }
            System.out.println("============================\n");

            addNodeDetailHandlers(); // Add this line
        });
    }

    private void addDefaultConnectionLine(NetworkNode node) {
        // Check for specific routing first - switch routing
        if (node.getRouteSwitchId() != null) {  // Using ID instead of name
            NetworkNode routeNode = getNodeById(node.getRouteSwitchId());
            if (routeNode != null) {
                ConnectionLine connection = new ConnectionLine(routeNode, node);
                if (routeNode.getDeviceType() == DeviceType.UNMANAGED_SWITCH) {
                    connection.setLineColor(Color.GREY);
                }
                connection.setViewOrder(1);
                spiderMapPane.getChildren().add(0, connection);
                return;
            }
        }
        
        // Check for VM host node routing
        if (node.getDeviceType() == DeviceType.VIRTUAL_MACHINE && node.getHostNodeId() != null) {  // Changed from getHostNode()
            NetworkNode hostNode = getNodeById(node.getHostNodeId());  // Use ID lookup
            if (hostNode != null) {
                ConnectionLine connection = new ConnectionLine(hostNode, node);
                connection.setLineColor(Color.web("#0cad03"));
                connection.setViewOrder(1);
                spiderMapPane.getChildren().add(0, connection);
                return;
            }
        }

        // Only reach here if no specific routing is set
        ConnectionLine connection;
        if (node.getConnectionType() == ConnectionType.VIRTUAL) {
            NetworkNode host = instance.getMainNodeByDisplayName("Host");
            connection = new ConnectionLine(host, node);
            if (node.getDeviceType() == DeviceType.VIRTUAL_MACHINE) {
                connection.setLineColor(Color.web("#0cad03"));
            }
        } else {
            NetworkNode gw = instance.getMainNodeByDisplayName("Gateway");
            connection = new ConnectionLine(gw, node);
        }
        connection.setViewOrder(1);
        spiderMapPane.getChildren().add(0, connection);
    }

    private void createFilterStatusBox() {
        filterStatusBox = new VBox(2);
        filterStatusBox.getStyleClass().add("filter-status-box");
        filterStatusBox.setVisible(false);
        filterStatusBox.setPrefWidth(225);
        
        // Create labels
        Label filterActiveLabel = new Label("FILTER ACTIVE");
        filterActiveLabel.getStyleClass().add("filter-status-label");
        
        Label filterTypeLabel = new Label("Device Type");
        filterTypeLabel.getStyleClass().add("filter-type-label");
        
        Label descriptionLabel = new Label();
        descriptionLabel.getStyleClass().add("filter-description");
        
        Label resetLabel = new Label("click to reset");
        resetLabel.getStyleClass().add("filter-reset-label");
        
        filterStatusBox.getChildren().addAll(
            filterActiveLabel, 
            filterTypeLabel,
            descriptionLabel, 
            resetLabel
        );

        // Add hover effect with brightness transition
        ColorAdjust brightnessAdjust = new ColorAdjust();
        brightnessAdjust.setBrightness(0);
        filterStatusBox.setEffect(brightnessAdjust);

        Timeline hoverTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(brightnessAdjust.brightnessProperty(), 0)),
            new KeyFrame(Duration.millis(300), new KeyValue(brightnessAdjust.brightnessProperty(), 0.2))
        );

        // Add scale transition for click effect
        ScaleTransition clickTransition = new ScaleTransition(Duration.millis(100), filterStatusBox);
        clickTransition.setFromX(1);
        clickTransition.setFromY(1);
        clickTransition.setToX(0.95);
        clickTransition.setToY(0.95);
        ScaleTransition releaseTransition = new ScaleTransition(Duration.millis(100), filterStatusBox);
        releaseTransition.setFromX(0.95);
        releaseTransition.setFromY(0.95);
        releaseTransition.setToX(1);
        releaseTransition.setToY(1);

        // Mouse event handlers
        filterStatusBox.setOnMouseEntered(e -> hoverTimeline.playFromStart());
        filterStatusBox.setOnMouseExited(e -> {
            hoverTimeline.setRate(-1);
            hoverTimeline.play();
        });
        
        filterStatusBox.setOnMousePressed(e -> clickTransition.play());
        filterStatusBox.setOnMouseReleased(e -> {
            releaseTransition.play();
            resetFilter();
        });
        
        filterStatusBox.setLayoutX(15);
        filterStatusBox.setLayoutY(15);
        
        spiderMapPane.getChildren().add(filterStatusBox);
    }

    public void showFilterStatus(String filterDescription) {
        // Get just the value part after any colon if present
        String displayText = filterDescription;
        if (filterDescription.contains(":")) {
            displayText = filterDescription.split(":")[1].trim();
        }
        
        Label descriptionLabel = (Label)filterStatusBox.getChildren().get(2);
        descriptionLabel.setText(displayText);
        filterStatusBox.setVisible(true);
    }

    private void resetFilter() {
        filterNodes(node -> true);
        filterStatusBox.setVisible(false);
    }

    public static void updateConnectionLineForNode(NetworkNode node) {
        // First remove any existing connection lines for this node
        instance.spiderMapPane.getChildren().removeIf(child ->
            child instanceof ConnectionLine
            && (((ConnectionLine) child).getFrom() == node
            || ((ConnectionLine) child).getTo() == node)
        );

        // First check for specific routing through switches - ONLY use ID-based lookup
        if (node.getRouteSwitchId() != null) {
            NetworkNode routeNode = instance.getNodeById(node.getRouteSwitchId());
            if (routeNode != null) {
                ConnectionLine connection = new ConnectionLine(routeNode, node);
                if (routeNode.getDeviceType() == DeviceType.UNMANAGED_SWITCH) {
                    connection.setLineColor(Color.GREY);
                }
                connection.setViewOrder(1);
                instance.spiderMapPane.getChildren().add(0, connection);
                return;
            }
        }
        // Check for VM host node routing - also ID-based
        else if (node.getDeviceType() == DeviceType.VIRTUAL_MACHINE && node.getHostNodeId() != null) {
            NetworkNode hostNode = instance.getNodeById(node.getHostNodeId());
            if (hostNode != null) {
                ConnectionLine connection = new ConnectionLine(hostNode, node);
                connection.setLineColor(Color.web("#0cad03"));
                connection.setViewOrder(1);
                instance.spiderMapPane.getChildren().add(0, connection);
                return;
            }
        }
        // If no specific routing, use default routing
        else if (!node.isMainNode()) {
            ConnectionLine connection;
            if (node.getConnectionType() == ConnectionType.VIRTUAL) {
                NetworkNode host = instance.getMainNodeByDisplayName("Host");
                connection = new ConnectionLine(host, node);
                if (node.getDeviceType() == DeviceType.VIRTUAL_MACHINE) {
                    connection.setLineColor(Color.web("#0cad03"));
                }
            } else {
                // All non-virtual nodes route through gateway
                NetworkNode gw = instance.getMainNodeByDisplayName("Gateway");
                connection = new ConnectionLine(gw, node);
            }
            connection.setViewOrder(1);
            instance.spiderMapPane.getChildren().add(0, connection);
        }
        // ADD THIS CASE: If this is a main node, we need to connect to other main nodes
        else if (node.isMainNode()) {
            // Find all main nodes
            List<NetworkNode> mainNodes = new ArrayList<>();
            for (NetworkNode otherNode : instance.persistentNodes) {
                if (otherNode.isMainNode() && otherNode != node) {
                    mainNodes.add(otherNode);
                }
            }
            
            // Connect to any main node that doesn't have specific routing
            for (NetworkNode mainNode : mainNodes) {
                // Only create connections if the other main node doesn't route through a switch
                if (mainNode.getRouteSwitchId() == null) {
                    ConnectionLine connection = new ConnectionLine(node, mainNode);
                    connection.setViewOrder(1);
                    instance.spiderMapPane.getChildren().add(0, connection);
                }
            }
        }
    }

    private void createConnectionLine(NetworkNode from, NetworkNode to) {
        // Special handling for managed switches
        if (to.getDeviceType() == DeviceType.MANAGED_SWITCH) {
            // Create standard connection line but still allow routing
            ConnectionLine line = new ConnectionLine(from, to);
            line.setViewOrder(1); // Ensures connection lines stay below nodes
            spiderMapPane.getChildren().add(line);
        }
    }

    public void saveNodesToFile() {
        try {
            List<NodeConfig> configs = new ArrayList<>();
            for (NetworkNode node : persistentNodes) {
                NodeConfig config = new NodeConfig(
                    node.getIpOrHostname(), 
                    node.getDisplayName(), 
                    node.getDeviceType(),
                    node.getNetworkLocation(), // Use NetworkLocation 
                    node.getLayoutX(), 
                    node.getLayoutY(),
                    node.getRelativeX(), 
                    node.getRelativeY(), 
                    node.isMainNode(), 
                    node.getConnectionType(),
                    node.getPrefWidth(), 
                    node.getPrefHeight()
                );
                
                // Save both IDs and display names after construction
                config.setNodeId(node.getNodeId());
                config.setRouteSwitchId(node.getRouteSwitchId());
                config.setHostNodeId(node.getHostNodeId());
                config.setRouteSwitch(node.getRouteSwitch());
                config.setHostNode(node.getHostNode());
                
                configs.add(config);
            }
            
            Gson gson = new Gson();
            String json = gson.toJson(configs);
            Files.write(Paths.get(CONFIG_FILE), json.getBytes());
            System.out.println("Network configuration saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        saveNodesToFile();
        saveWindowSize();
        super.stop();
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private NetworkNode getMainNodeByDisplayName(String name) {
        for (NetworkNode node : persistentNodes) {
            if (node.isMainNode() && node.getDisplayName().equalsIgnoreCase(name))
                return node;
        }
        return null;
    }

    public static List<NetworkNode> getPersistentNodesStatic() { return persistentNodesStatic; }

    

    

    // Add this method to NetworkMonitorApp class
    public List<NetworkNode> getRouteToNode(NetworkNode targetNode) {
        List<NetworkNode> route = new ArrayList<>();
        NetworkNode currentNode = targetNode;
        
        // Debug output to help diagnose the issue
        System.out.println("\nDEBUG - Calculating route from: " + targetNode.getDisplayName());
        
        // Prevent infinite loops - track by ID instead of display name
        Set<Long> visitedNodeIds = new HashSet<>();
        
        while (currentNode != null && !visitedNodeIds.contains(currentNode.getNodeId())) {
            route.add(currentNode);
            visitedNodeIds.add(currentNode.getNodeId());
            System.out.println("DEBUG - Added to route: " + currentNode.getDisplayName() + 
                              " (" + currentNode.getDeviceType() + ")" +
                              " - NetworkLocation: " + currentNode.getNetworkLocation());
            
            // CASE 1: Check if node is routed through a switch - use ID lookup
            if (currentNode.getRouteSwitchId() != null) {
                System.out.println("DEBUG - Node routes through switch, ID: " + currentNode.getRouteSwitchId());
                currentNode = getNodeById(currentNode.getRouteSwitchId());
            }
            // CASE 2: Check if node is a VM with a host node
            else if (currentNode.getDeviceType() == DeviceType.VIRTUAL_MACHINE && currentNode.getHostNodeId() != null) {
                System.out.println("DEBUG - VM routes through host node, ID: " + currentNode.getHostNodeId());
                currentNode = getNodeById(currentNode.getHostNodeId());
            }
            // CASE 3: For PUBLIC nodes (like your proxy servers), route directly to GATEWAY
            else if (currentNode.getNetworkLocation() == NetworkLocation.PUBLIC && !currentNode.isMainNode()) {
                System.out.println("DEBUG - PUBLIC node routes directly to Gateway");
                currentNode = findMainNodeByDeviceType(DeviceType.GATEWAY);
                if (currentNode == null) {
                    System.out.println("DEBUG - Gateway not found, stopping route");
                    break;
                }
            }
            // CASE 4: If node is REMOTE_PRIVATE, it must connect through a PUBLIC node
            else if (currentNode.getNetworkLocation() == NetworkLocation.REMOTE_PRIVATE && !currentNode.isMainNode()) {
                System.out.println("DEBUG - REMOTE_PRIVATE node routes through PUBLIC node");
                NetworkNode publicNode = findNodeByNetworkLocation(NetworkLocation.PUBLIC);
                if (publicNode != null) {
                    currentNode = publicNode;
                } else {
                    System.out.println("DEBUG - No PUBLIC node found, using any main node");
                    currentNode = findAnyMainNode();
                }
            }
            // CASE 5: Other standard routing
            else if (!currentNode.isMainNode()) {
                // Use default routing based on network location
                if (currentNode.getNetworkLocation() == NetworkLocation.LOCAL) {
                    System.out.println("DEBUG - LOCAL node routes to Gateway");
                    NetworkNode gatewayNode = findMainNodeByDeviceType(DeviceType.GATEWAY);
                    if (gatewayNode != null) {
                        currentNode = gatewayNode;
                    } else {
                        System.out.println("DEBUG - Gateway not found, using any main node");
                        currentNode = findAnyMainNode();
                    }
                } else {
                    // All other nodes route to the main gateway node
                    System.out.println("DEBUG - Default routing to Gateway");
                    currentNode = findMainNodeByDeviceType(DeviceType.GATEWAY);
                    if (currentNode == null) {
                        System.out.println("DEBUG - Gateway not found, using any main node");
                        currentNode = findAnyMainNode();
                    }
                }
            } else {
                // Reached a main node with no further routing
                System.out.println("DEBUG - Reached a main node, ending route");
                break;
            }
            
            if (currentNode == null) {
                System.out.println("DEBUG - No further routing found, ending route");
                break;
            }
        }
        
        System.out.println("DEBUG - Final route length: " + route.size() + "\n");
        return route;
    }

    // Helper method to find a node by network location
    private NetworkNode findNodeByNetworkLocation(NetworkLocation location) {
        for (NetworkNode node : persistentNodes) {
            if (node.getNetworkLocation() == location) {
                return node;
            }
        }
        return null;
    }

    // Add method to apply filter
    public void filterNodes(Predicate<NetworkNode> filter) {
        System.out.println("\n==== FILTER DEBUGGING ====");
        
        // Step 1: Find all nodes that match the filter
        Set<NetworkNode> matchingNodes = persistentNodes.stream()
            .filter(filter)
            .collect(Collectors.toSet());
        
        // Print matching nodes
        System.out.println("NODES MATCHING FILTER: " + matchingNodes.size());
        for (NetworkNode node : matchingNodes) {
            System.out.println("  - " + node.getDisplayName() + " (" + node.getDeviceType() + ")");
        }
        
        // If no nodes match, show all nodes (reset filter)
        if (matchingNodes.isEmpty()) {
            System.out.println("No nodes match filter, resetting to show all nodes");
            resetFilter();
            return;
        }
        
        // Find Gateway node - we'll always show it
        NetworkNode gatewayNode = findMainNodeByDeviceType(DeviceType.GATEWAY);
        System.out.println("Gateway node: " + (gatewayNode != null ? gatewayNode.getDisplayName() : "Not found"));
        
        // Step 2: Create a map of nodes to their routes (from node to Gateway)
        Map<NetworkNode, List<NetworkNode>> nodeRoutes = new HashMap<>();
        for (NetworkNode node : matchingNodes) {
            List<NetworkNode> routeToNode = getRouteToNode(node);
            nodeRoutes.put(node, routeToNode);
            
            // Print complete route for this node
            System.out.println("ROUTE FOR: " + node.getDisplayName());
            for (int i = 0; i < routeToNode.size(); i++) {
                NetworkNode routeNode = routeToNode.get(i);
                System.out.println("  " + i + ": " + routeNode.getDisplayName() + 
                                  " (" + routeNode.getDeviceType() + ")" +
                                  (i == 0 ? " [START]" : ""));
            }
        }
        
        // Step 3: Create a set of all nodes in any route
        Set<NetworkNode> nodesInRoutes = new HashSet<>();
        nodeRoutes.values().forEach(nodesInRoutes::addAll);
        
        // Add Gateway to routes if not already included
        if (gatewayNode != null) {
            nodesInRoutes.add(gatewayNode);
        }
        
        System.out.println("\nNODE VISIBILITY CHANGES:");
        // Step 4: Set visibility for all nodes
        Set<NetworkNode> nodesWithLoweredOpacity = new HashSet<>();
        
        for (NetworkNode node : persistentNodes) {
            boolean isGateway = (node.getDeviceType() == DeviceType.GATEWAY);
            boolean isMatching = matchingNodes.contains(node);
            boolean isInRoute = nodesInRoutes.contains(node);
            
            // Show if it's a matching node, in a route, or is Gateway
            if (isMatching || isInRoute) {
                node.setVisible(true);
                
                // Full opacity only for filtered/matched nodes
                if (isMatching) {
                    node.setOpacity(1.0);
                    System.out.println("  - " + node.getDisplayName() + ": visible, full opacity (matched filter)");
                } else {
                    // Lower opacity for route nodes (including Host if in route)
                    node.setOpacity(0.25);
                    nodesWithLoweredOpacity.add(node);
                    System.out.println("  - " + node.getDisplayName() + ": visible, lowered opacity (in route path)");
                }
            } else {
                // Hide nodes not in any matching route
                node.setVisible(false);
                System.out.println("  - " + node.getDisplayName() + ": hidden (not in any route)");
            }
        }
        
        System.out.println("\nNODES WITH LOWERED OPACITY: " + nodesWithLoweredOpacity.size());
        for (NetworkNode node : nodesWithLoweredOpacity) {
            System.out.println("  - " + node.getDisplayName() + " (" + node.getDeviceType() + ")");
        }
        
        // Step 5: Update connection lines
        int visibleLines = 0;
        int fullOpacityLines = 0;
        int loweredOpacityLines = 0;
        
        for (javafx.scene.Node child : spiderMapPane.getChildren()) {
            if (!(child instanceof ConnectionLine)) continue;
            
            ConnectionLine line = (ConnectionLine) child;
            NetworkNode from = line.getFrom();
            NetworkNode to = line.getTo();
            
            // Only show line if both endpoints are visible
            boolean visible = from.isVisible() && to.isVisible();
            line.setVisible(visible);
            
            if (visible) {
                visibleLines++;
                // Check if this is a "last connection" - direct connection to a matching node
                boolean isLastConnection = false;
                
                // If either end is a matching node, this is a last connection
                if (matchingNodes.contains(from) || matchingNodes.contains(to)) {
                    isLastConnection = true;
                } else {
                    // Check if this connection represents the last segment in any route
                    for (NetworkNode matchingNode : matchingNodes) {
                        List<NetworkNode> route = nodeRoutes.get(matchingNode);
                        if (route != null && route.size() >= 2) {
                            int nodeIndex = route.indexOf(matchingNode);
                            if (nodeIndex > 0) {
                                NetworkNode previousNode = route.get(nodeIndex - 1);
                                if ((from == matchingNode && to == previousNode) ||
                                    (from == previousNode && to == matchingNode)) {
                                    isLastConnection = true;
                                    break;
                            }
                        }
                    }
                }
            }
            
            // Set opacity - full for last connections, reduced for others
            if (isLastConnection) {
                line.setOpacity(1.0);
                fullOpacityLines++;
            } else {
                line.setOpacity(0.25);
                loweredOpacityLines++;
            }
        }
    }
    
    System.out.println("\nCONNECTION LINES:");
    System.out.println("  - Visible lines: " + visibleLines);
    System.out.println("  - Full opacity: " + fullOpacityLines);
    System.out.println("  - Lowered opacity: " + loweredOpacityLines);
    System.out.println("==== END FILTER DEBUGGING ====\n");
}
    // Add this getter method
    public VBox getModePanel() {
        return modePanel;
    }

    private NetworkNode getNodeById(Long id) {
        for (NetworkNode node : persistentNodes) {
            if (node.getNodeId() == id) {
                return node;
            }
        }
        return null;
    }

    // Add this method to NetworkMonitorApp class
    public NetworkNode getNodeByDisplayName(String displayName) {
        for (NetworkNode node : persistentNodes) {
            if (node.getDisplayName().equals(displayName)) {
                return node;
            }
        }
        return null;
    }

    // Add this method to NetworkMonitorApp class
    public static void updateConnectionLinesRecursively(NetworkNode startNode) {
        // First update this node's own connection line
        updateConnectionLineForNode(startNode);
        
        // Then find all nodes that route through this node (its children)
        long nodeId = startNode.getNodeId();
        
        // For each child, recursively update its connection lines
        for (NetworkNode child : getPersistentNodesStatic()) {
            if (Objects.equals(child.getRouteSwitchId(), nodeId) ||
                Objects.equals(child.getHostNodeId(), nodeId)) {
                // Recursively update this child and all its descendants
                updateConnectionLinesRecursively(child);
            }
        }
    }

    // Helper method to find any main node
    private NetworkNode findAnyMainNode() {
        for (NetworkNode node : persistentNodes) {
            if (node.isMainNode()) {
                return node;
            }
        }
        return null;
    }

    // Helper method to find a main node of a specific device type
    private NetworkNode findMainNodeByDeviceType(DeviceType deviceType) {
        for (NetworkNode node : persistentNodes) {
            if (node.isMainNode() && node.getDeviceType() == deviceType) {
                return node;
            }
        }
        return null;
    }

    // Add this method to the NetworkMonitorApp class
    public void addNodeDetailHandlers() {
        for (NetworkNode node : persistentNodes) {
            node.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    nodeDetailPanel.showForNode(node);
                    e.consume();
                }
            });
        }
    }
}
