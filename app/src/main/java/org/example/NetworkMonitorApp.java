package org.example;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import javafx.scene.input.MouseButton;
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

    Pane spiderMapPane;
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

    private NodeDetailPanel currentDetailPanel;
    private EventHandler<MouseEvent> panelCloseHandler;

    private static NetworkMonitorApp instance;
    private Stage primaryStage;

    private double prevSceneWidth = 0;
    private double prevSceneHeight = 0;

    private VBox filterStatusBox;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;

        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setTitle("Network Device Monitor");
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
        scene.getStylesheets().add(getClass().getResource("/styles/nodedetails.css").toExternalForm());

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
        instance.addDetailPanelHandler(node);
        node.setViewOrder(-2); // Ensures nodes are above connection lines but below labels
        instance.spiderMapPane.getChildren().add(node);

        if (!node.isMainNode()) {
            // Check if a host node is selected
            String hostNodeName = node.getRouteSwitch(); // Assuming HOST_NODE is stored in routeSwitch
            NetworkNode hostNode = null;
            if (hostNodeName != null && !hostNodeName.isEmpty()) {
                for (NetworkNode n : persistentNodesStatic) {
                    if (n.getDisplayName().equalsIgnoreCase(hostNodeName)) {
                        hostNode = n;
                        break;
                    }
                }
            }

            // Create connection line
            ConnectionLine connection;
            if (hostNode != null) {
                connection = new ConnectionLine(hostNode, node);
                // Ensure the line is not grey for virtual machines
                if (node.getDeviceType() == DeviceType.VIRTUAL_MACHINE) {
                    connection.setLineColor(Color.web("#0cad03")); // Set to green or another color
                }
            } else if (node.getNetworkType() == NetworkType.INTERNAL) {
                NetworkNode gw = instance.getMainNodeByDisplayName("Gateway");
                connection = new ConnectionLine(gw, node);
            } else {
                NetworkNode internet = instance.getMainNodeByDisplayName("Google DNS");
                connection = new ConnectionLine(internet, node);
            }
            connection.setViewOrder(1); // Ensures connection lines stay below nodes
            instance.spiderMapPane.getChildren().add(0, connection);
        }
    }

    public static NetworkNode getUpstreamNode(NetworkNode node) {
        if (node.getNetworkType() == NetworkType.INTERNAL)
            return instance.getMainNodeByDisplayName("Gateway");
        return instance.getMainNodeByDisplayName("Google DNS");
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

        NetworkNode hostNode = new NetworkNode("127.0.0.1", "Host", DeviceType.COMPUTER, NetworkType.INTERNAL);
        hostNode.setLayoutX(centerX - hostNode.getPrefWidth() / 2);
        hostNode.setLayoutY(centerY - spacing - hostNode.getPrefHeight() / 2);
        hostNode.setMainNode(true);
        addDetailPanelHandler(hostNode);
        persistentNodes.add(hostNode);
        persistentNodesStatic.add(hostNode);
        spiderMapPane.getChildren().add(hostNode);

        String gw = NetworkUtils.getDefaultGateway();
        if (gw == null) gw = "192.168.0.1";
        NetworkNode gatewayNode = new NetworkNode(gw, "Gateway", DeviceType.GATEWAY, NetworkType.INTERNAL);
        gatewayNode.setLayoutX(centerX - gatewayNode.getPrefWidth() / 2);
        gatewayNode.setLayoutY(centerY - gatewayNode.getPrefHeight() / 2);
        gatewayNode.setMainNode(true);
        addDetailPanelHandler(gatewayNode);
        persistentNodes.add(gatewayNode);
        persistentNodesStatic.add(gatewayNode);
        spiderMapPane.getChildren().add(gatewayNode);

        NetworkNode internetNode = new NetworkNode("8.8.8.8", "Google DNS", DeviceType.ROUTER, NetworkType.EXTERNAL);
        internetNode.setLayoutX(centerX - internetNode.getPrefWidth() / 2);
        internetNode.setLayoutY(centerY + spacing - internetNode.getPrefHeight() / 2);
        internetNode.setMainNode(true);
        addDetailPanelHandler(internetNode);
        persistentNodes.add(internetNode);
        persistentNodesStatic.add(internetNode);
        spiderMapPane.getChildren().add(internetNode);

        ConnectionLine line1 = new ConnectionLine(hostNode, gatewayNode);
        ConnectionLine line2 = new ConnectionLine(gatewayNode, internetNode);
        spiderMapPane.getChildren().add(0, line1);
        spiderMapPane.getChildren().add(0, line2);
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
                    double absoluteX = config.getRelativeX() * paneWidth;
                    double absoluteY = config.getRelativeY() * paneHeight;
                    NetworkNode node = new NetworkNode(
                        config.getIpOrHostname(), config.getDisplayName(),
                        config.getDeviceType(), config.getNetworkType());
                    node.setPrefSize(config.getWidth(), config.getHeight());
                    node.updateLayoutForSavedSize();
                    node.setLayoutX(absoluteX);
                    node.setLayoutY(absoluteY);
                    node.setMainNode(config.isMainNode());
                    if (config.getConnectionType() != null)
                        node.setConnectionType(config.getConnectionType());
                    node.setRouteSwitch(config.getRouteSwitch());
                    addDetailPanelHandler(node);
                    persistentNodes.add(node);
                    persistentNodesStatic.add(node);
                    spiderMapPane.getChildren().add(node);
                }

                // Then connect main nodes that aren't routed through switches
                List<NetworkNode> mainNodes = new ArrayList<>();
                for (NetworkNode node : persistentNodes) {
                    // Remove the routing check for main nodes - we'll handle all connections later
                    if (node.isMainNode()) {
                        mainNodes.add(node);
                    }
                }
                mainNodes.sort((a, b) -> Double.compare(a.getLayoutY(), b.getLayoutY()));

                // Connect main nodes that don't have switch routing
                for (int i = 0; i < mainNodes.size() - 1; i++) {
                    NetworkNode current = mainNodes.get(i);
                    NetworkNode next = mainNodes.get(i + 1);
                    
                    // Only create direct connections if neither node is routed through a switch
                    if ((current.getRouteSwitch() == null || current.getRouteSwitch().isEmpty()) &&
                        (next.getRouteSwitch() == null || next.getRouteSwitch().isEmpty())) {
                        ConnectionLine c = new ConnectionLine(current, next);
                        spiderMapPane.getChildren().add(0, c);
                    }
                }

                // Finally, create all switch-routed connections for ALL nodes
                for (NetworkNode node : persistentNodes) {
                    if (node.getRouteSwitch() != null && !node.getRouteSwitch().isEmpty()) {
                        updateConnectionLineForNode(node);
                    } else if (!node.isMainNode()) {
                        // Create default connection only for non-main nodes that aren't switch-routed
                        addDefaultConnectionLine(node);
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

        });
    }

    private void addDefaultConnectionLine(NetworkNode node) {
        ConnectionLine connection;
        if (node.getConnectionType() == ConnectionType.VIRTUAL) {
            NetworkNode host = instance.getMainNodeByDisplayName("Host");
            connection = new ConnectionLine(host, node);
        } else if (node.getNetworkType() == NetworkType.INTERNAL) {
            NetworkNode gw = instance.getMainNodeByDisplayName("Gateway");
            connection = new ConnectionLine(gw, node);
        } else {
            NetworkNode internet = instance.getMainNodeByDisplayName("Google DNS");
            connection = new ConnectionLine(internet, node);
        }
        connection.setViewOrder(1); // Ensures connection lines stay below nodes
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

    private void addDetailPanelHandler(NetworkNode node) {
        node.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                showDetailPanel(node);
        });
    }

    public static void updateConnectionLineForNode(NetworkNode node) {
        instance.spiderMapPane.getChildren().removeIf(child ->
            child instanceof ConnectionLine
            && (((ConnectionLine) child).getFrom() == node
            || ((ConnectionLine) child).getTo() == node)
        );

        if (node.getRouteSwitch() != null && !node.getRouteSwitch().isEmpty()) {
            NetworkNode routeNode = null;
            for (NetworkNode n : persistentNodesStatic) {
                if (n.getDisplayName().equalsIgnoreCase(node.getRouteSwitch())) {
                    routeNode = n;
                    break;
                }
            }
            if (routeNode != null) {
                ConnectionLine connection;
                if (routeNode.getDeviceType() == DeviceType.UNMANAGED_SWITCH) {
                    connection = new ConnectionLine(routeNode, node);
                    connection.setLineColor(Color.GREY);
                } else {
                    connection = new ConnectionLine(routeNode, node);
                }
                connection.setViewOrder(1);
                connection.setVisible(true); // Explicitly set visibility
                instance.spiderMapPane.getChildren().add(0, connection);
                return;
            }
        }

        if (!node.isMainNode()) {
            ConnectionLine connection;
            if (node.getConnectionType() == ConnectionType.VIRTUAL) {
                NetworkNode host = instance.getMainNodeByDisplayName("Host");
                connection = new ConnectionLine(host, node);
            } else if (node.getNetworkType() == NetworkType.INTERNAL) {
                NetworkNode gw = instance.getMainNodeByDisplayName("Gateway");
                connection = new ConnectionLine(gw, node);
            } else {
                NetworkNode internet = instance.getMainNodeByDisplayName("Google DNS");
                connection = new ConnectionLine(internet, node);
            }
            connection.setViewOrder(1); // Ensures connection lines stay below nodes
            instance.spiderMapPane.getChildren().add(0, connection);
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

    private void saveNodesToFile() {
        try {
            System.out.println("\n=== SAVING NODES TO FILE ===");
            System.out.println("Current Node Configuration:");
            for (NetworkNode node : persistentNodes) {
                System.out.printf("Node: %-20s | Type: %-15s | Routed via: %s%n",
                    node.getDisplayName(),
                    node.getDeviceType(),
                    (node.getRouteSwitch() != null && !node.getRouteSwitch().isEmpty()) 
                        ? node.getRouteSwitch() 
                        : "none"
                );
            }
            List<NodeConfig> configs = new ArrayList<>();
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth < 100) paneWidth = primaryStage.getScene().getWidth();
            if (paneHeight < 100) paneHeight = primaryStage.getScene().getHeight();
            for (NetworkNode node : persistentNodes) {
                double relativeX = node.getLayoutX() / paneWidth;
                double relativeY = node.getLayoutY() / paneHeight;
                NodeConfig config = new NodeConfig(
                    node.getIpOrHostname(), node.getDisplayName(), node.getDeviceType(),
                    node.getNetworkType(), node.getLayoutX(), node.getLayoutY(),
                    relativeX, relativeY, node.isMainNode(), node.getConnectionType(),
                    node.getPrefWidth(), node.getPrefHeight(), node.getRouteSwitch()
                );
                configs.add(config);
            }
            Gson gson = new Gson();
            String json = gson.toJson(configs);
            Files.write(Paths.get(CONFIG_FILE), json.getBytes());
            System.out.println("============================\n");
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

    public void showDetailPanel(NetworkNode node) {
        StackPane rootStack = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
        rootStack.getChildren().removeIf(n -> n instanceof NodeDetailPanel);
        
        currentDetailPanel = new NodeDetailPanel(node);
        currentDetailPanel.setPrefWidth(DETAIL_PANEL_WIDTH);
        currentDetailPanel.setMaxWidth(DETAIL_PANEL_WIDTH);
        currentDetailPanel.setMinWidth(DETAIL_PANEL_WIDTH);
        currentDetailPanel.prefHeightProperty().bind(primaryStage.getScene().heightProperty());
        currentDetailPanel.setTranslateX(DETAIL_PANEL_WIDTH);
        rootStack.getChildren().add(currentDetailPanel);
        StackPane.setAlignment(currentDetailPanel, Pos.TOP_RIGHT);
        currentDetailPanel.requestFocus();

        // Store the handler reference and use addEventFilter instead of setOnMouseClicked
        panelCloseHandler = e -> {
            if (!(e.getPickResult().getIntersectedNode() instanceof NetworkNode)) {
                hideDetailPanel();
            }
        };
        
        // Add click handler using addEventFilter to maintain other handlers
        spiderMapPane.addEventFilter(MouseEvent.MOUSE_CLICKED, panelCloseHandler);

        Timeline panelSlide = new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(currentDetailPanel.translateXProperty(), 0)
            )
        );
        panelSlide.play();
    }

    public void hideDetailPanel() {
        if (currentDetailPanel == null) return;
        
        StackPane rootStack = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
        
        // Remove only the panel close handler using removeEventFilter
        if (panelCloseHandler != null) {
            spiderMapPane.removeEventFilter(MouseEvent.MOUSE_CLICKED, panelCloseHandler);
            panelCloseHandler = null;
        }

        Timeline slide = new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(currentDetailPanel.translateXProperty(), DETAIL_PANEL_WIDTH)
            )
        );
        slide.setOnFinished(e -> {
            rootStack.getChildren().remove(currentDetailPanel);
            currentDetailPanel = null;
        });
        slide.play();
    }
    

    public static void performTraceroute(NetworkNode source) {
        Pane spiderPane = instance.spiderMapPane;
        StackPane rootStack = (StackPane) ((BorderPane) instance.primaryStage.getScene().getRoot()).getCenter();
        rootStack.getChildren().removeIf(n -> n instanceof TraceroutePanel);
        TraceroutePanel panel = new TraceroutePanel();
        StackPane.setAlignment(panel, Pos.TOP_LEFT);
        StackPane.setMargin(panel, new Insets(10));
        rootStack.getChildren().add(panel);

        NetworkNode hostFinal;
        NetworkNode tempHost = instance.getMainNodeByDisplayName("Host");
        hostFinal = (tempHost == null) ? source : tempHost;

        class Origin { 
            NetworkNode node; 
            double x, y; 
            boolean isVirtual = false; 
        }
        
        Origin origin = new Origin();
        origin.node = hostFinal;
        origin.x = hostFinal.getLayoutX() + hostFinal.getWidth() / 2;
        origin.y = hostFinal.getLayoutY() + hostFinal.getHeight() / 2;

        java.util.concurrent.atomic.AtomicInteger hopCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        List<TracerouteLine> tracerouteLines = new ArrayList<>();
        String target = source.getIpOrHostname();
        TracerouteTask task = new TracerouteTask(target);

        task.setHopCallback(hop -> {
            int index = hopCounter.incrementAndGet();
            panel.addHop("Hop " + index + ": " + hop);
            NetworkNode targetNode = null;
            
            for (NetworkNode node : getPersistentNodesStatic()) {
                String ip = (node.getResolvedIp() != null && !node.getResolvedIp().isEmpty())
                    ? node.getResolvedIp() : node.getIpOrHostname();
                if (ip.equals(hop)) {
                    targetNode = node;
                    break;
                }
            }

            TracerouteLine tLine;
            if (targetNode != null) {
                tLine = origin.isVirtual
                    ? new TracerouteLine(origin.x, origin.y, targetNode)
                    : new TracerouteLine(origin.node, targetNode);
                origin.node = targetNode;
                origin.x = targetNode.getLayoutX() + targetNode.getWidth() / 2;
                origin.y = targetNode.getLayoutY() + targetNode.getHeight() / 2;
                origin.isVirtual = false;
            } else {
                tLine = new TracerouteLine(origin.x, origin.y, hop, index - 1);
                origin.x += TracerouteLine.UNKNOWN_OFFSET;
                origin.node = null;
                origin.isVirtual = true;
            }

            tLine.setLineColor(Color.web("#C9EB78"));
            tracerouteLines.add(tLine);
            spiderPane.getChildren().add(0, tLine);
        });

        task.setOnSucceeded(e -> {
            panel.addHop("Traceroute complete");
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(5));
            pause.setOnFinished(ev -> {
                tracerouteLines.forEach(TracerouteLine::startFadeOut);
                panel.startFadeOut();
            });
            pause.play();
        });

        task.setOnFailed(e -> System.err.println("Traceroute failed: " + task.getException()));
        new Thread(task).start();
    }

    // Add this method to NetworkMonitorApp class
    public List<NetworkNode> getRouteToNode(NetworkNode targetNode) {
        List<NetworkNode> route = new ArrayList<>();
        NetworkNode currentNode = targetNode;
        
        // Prevent infinite loops
        Set<String> visitedNodes = new HashSet<>();
        
        while (currentNode != null && !visitedNodes.contains(currentNode.getDisplayName())) {
            route.add(currentNode);
            visitedNodes.add(currentNode.getDisplayName());
            
            // Check if node is routed through a switch
            if (currentNode.getRouteSwitch() != null && !currentNode.getRouteSwitch().isEmpty()) {
                currentNode = getNodeByDisplayName(currentNode.getRouteSwitch());
            }
            // If not routed through switch, check default routing
            else if (!currentNode.isMainNode()) {
                if (currentNode.getConnectionType() == ConnectionType.VIRTUAL) {
                    currentNode = getMainNodeByDisplayName("Host");
                } else if (currentNode.getNetworkType() == NetworkType.INTERNAL) {
                    currentNode = getMainNodeByDisplayName("Gateway");
                } else {
                    currentNode = getMainNodeByDisplayName("Google DNS");
                }
            } else {
                // Reached a main node with no further routing
                break;
            }
        }
        
        return route;
    }

    // Add helper method to find any node by display name
    private NetworkNode getNodeByDisplayName(String name) {
        for (NetworkNode node : persistentNodes) {
            if (node.getDisplayName().equalsIgnoreCase(name))
                return node;
        }
        return null;
    }

    // Add method to apply filter
    public void filterNodes(Predicate<NetworkNode> filter) {
        // Find all nodes that match the filter
        Set<NetworkNode> matchingNodes = persistentNodes.stream()
            .filter(filter)
            .collect(Collectors.toSet());
        
        // Get all route nodes for matching nodes
        Set<NetworkNode> routeNodes = new HashSet<>();
        for (NetworkNode node : matchingNodes) {
            routeNodes.addAll(getRouteToNode(node));
        }
        
        // Apply visual changes to all nodes
        for (NetworkNode node : persistentNodes) {
            if (matchingNodes.contains(node)) {
                // Node matches filter - full opacity
                node.setOpacity(1.0);
                node.setVisible(true);
            } else if (routeNodes.contains(node)) {
                // Node is part of a route - reduced opacity
                node.setOpacity(0.25);
                node.setVisible(true);
            } else {
                // Node doesn't match and isn't in a route - hide it
                node.setVisible(false);
            }
        }
        
        // Update connection lines
        for (javafx.scene.Node child : spiderMapPane.getChildren()) {
            if (child instanceof ConnectionLine) {
                ConnectionLine line = (ConnectionLine) child;
                NetworkNode from = line.getFrom();
                NetworkNode to = line.getTo();
                
                boolean visible = from.isVisible() && to.isVisible();
                line.setVisible(visible);
                
                if (visible) {
                    if (matchingNodes.contains(from) || matchingNodes.contains(to)) {
                        // If either node is a filtered node, connection should be at full opacity
                        line.setOpacity(1.0);
                    } else {
                        // Both nodes are route nodes, so connection should be faded
                        line.setOpacity(0.25);
                    }
                }
            }
        }
    }

    // Add this getter method
    public VBox getModePanel() {
        return modePanel;
    }
}
