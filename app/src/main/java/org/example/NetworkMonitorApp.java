package org.example;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NetworkMonitorApp extends Application {

    // Main map pane.
    Pane spiderMapPane;
    // List of nodes.
    private List<NetworkNode> persistentNodes = new ArrayList<>();
    private static List<NetworkNode> persistentNodesStatic = new ArrayList<>();
    // List of zones.
    private List<DrawableZone> zones = new ArrayList<>();

    // Status panel components.
    private VBox statusPanel;
    private Label totalLabel, upLabel, downLabel;

    // Config directories.
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + "NetworkMonitorApp";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "nodes.json";
    private static final String ZONES_FILE = CONFIG_DIR + File.separator + "zones.json";
    private static final String WINDOW_CONFIG_FILE = CONFIG_DIR + File.separator + "window.config";

    // Static instance reference.
    private static NetworkMonitorApp instance;
    private Stage primaryStage;
    
    // Variables to store previous scene dimensions for scaling.
    private double prevSceneWidth = 0;
    private double prevSceneHeight = 0;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;

        // Set a minimum window size.
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setTitle("Testing");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/node.png")));

        // Ensure config directory exists.
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        BorderPane root = new BorderPane();
        spiderMapPane = createSpiderMapPane();
        StackPane centerStack = new StackPane();
        centerStack.getChildren().add(spiderMapPane);
        root.setCenter(centerStack);

        if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) {
            loadWindowSize();
        } else {
            primaryStage.setMaximized(true);
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        primaryStage.setTitle("Network Device Monitor");
        primaryStage.setScene(scene);
        primaryStage.show();

        prevSceneWidth = scene.getWidth();
        prevSceneHeight = scene.getHeight();

        // Add the New Node Box to the bottom left.
        NewNodeBox newNodeBox = new NewNodeBox();
        spiderMapPane.getChildren().add(newNodeBox);
        newNodeBox.setLayoutX(10);
        // (NewNodeBox anchors its Y internally via scene property listener)

        // Add the Filter Box to the right of the NewNodeBox.
        FilterBox filterBox = new FilterBox();
        spiderMapPane.getChildren().add(filterBox);
        filterBox.layoutXProperty().bind(newNodeBox.layoutXProperty().add(newNodeBox.widthProperty()).add(10));
        // (FilterBox anchors its Y internally)

        // Add the Functions Box to the right of the FilterBox.
        FunctionsBox functionsBox = new FunctionsBox();
        spiderMapPane.getChildren().add(functionsBox);
        functionsBox.layoutXProperty().bind(filterBox.layoutXProperty().add(filterBox.widthProperty()).add(10));
        // Bind its Y so that it stays anchored at the bottom.
        functionsBox.layoutYProperty().bind(spiderMapPane.heightProperty().subtract(functionsBox.prefHeightProperty()).subtract(15));

        // Collapse any expanded boxes when clicking outside.
        spiderMapPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            // Convert the click's scene coordinates to spiderMapPane coordinates.
            javafx.geometry.Point2D pt = spiderMapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            if (newNodeBox.isExpanded() && !newNodeBox.getBoundsInParent().contains(pt)) {
                newNodeBox.collapse();
            }
            if (filterBox.isExpanded() && !filterBox.getBoundsInParent().contains(pt)) {
                filterBox.collapse();
            }
            if (functionsBox.isExpanded() && !functionsBox.getBoundsInParent().contains(pt)) {
                functionsBox.collapse();
            }
        });

        Platform.runLater(() -> {
            if (!Files.exists(Paths.get(CONFIG_FILE))) {
                createDefaultMainNodes();
            } else {
                loadNodesFromFile();
            }
            loadZonesFromFile();
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double newWidth = newVal.doubleValue();
            double ratio = newWidth / prevSceneWidth;
            for (NetworkNode node : persistentNodes) {
                double newX = node.getLayoutX() * ratio;
                node.setLayoutX(newX);
                if (newX + node.getWidth() > newWidth) {
                    node.setLayoutX(newWidth - node.getWidth());
                }
            }
            prevSceneWidth = newWidth;
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            double newHeight = newVal.doubleValue();
            double ratio = newHeight / prevSceneHeight;
            for (NetworkNode node : persistentNodes) {
                double newY = node.getLayoutY() * ratio;
                node.setLayoutY(newY);
                if (newY + node.getHeight() > newHeight) {
                    node.setLayoutY(newHeight - node.getHeight());
                }
            }
            prevSceneHeight = newHeight;
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double newWidth = newVal.doubleValue();
            for (NetworkNode node : persistentNodes) {
                if (node.getLayoutX() + node.getWidth() > newWidth) {
                    node.setLayoutX(newWidth - node.getWidth());
                }
                if (node.getLayoutX() < 0) {
                    node.setLayoutX(0);
                }
            }
        });
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            double newHeight = newVal.doubleValue();
            for (NetworkNode node : persistentNodes) {
                if (node.getLayoutY() + node.getHeight() > newHeight) {
                    node.setLayoutY(newHeight - node.getHeight());
                }
                if (node.getLayoutY() < 0) {
                    node.setLayoutY(0);
                }
            }
        });

        Timeline connectionTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            for (javafx.scene.Node node : spiderMapPane.getChildren()) {
                if (node instanceof ConnectionLine) {
                    ((ConnectionLine) node).updateStatus();
                }
            }
        }));
        connectionTimeline.setCycleCount(Timeline.INDEFINITE);
        connectionTimeline.play();
    }

    public static void updateConnectionLinesVisibility() {
        instance.spiderMapPane.getChildren().forEach(child -> {
            if (child instanceof ConnectionLine) {
                ConnectionLine line = (ConnectionLine) child;
                // The connection line is visible only if both the source and target nodes are visible.
                boolean bothVisible = line.getFrom().isVisible() && line.getTo().isVisible();
                line.setVisible(bothVisible);
            }
        });
    }
    
    public static void removeNode(NetworkNode node) {
        instance.persistentNodes.remove(node);
        instance.persistentNodesStatic.remove(node);
        instance.spiderMapPane.getChildren().remove(node);
        instance.spiderMapPane.getChildren().removeIf(child ->
            child instanceof ConnectionLine &&
            (((ConnectionLine) child).getFrom() == node || ((ConnectionLine) child).getTo() == node)
        );
        instance.saveNodesToFile();
    }
    
    public static void addNewNode(NetworkNode node) {
        instance.persistentNodes.add(node);
        instance.persistentNodesStatic.add(node);
        instance.addDetailPanelHandler(node);
        instance.spiderMapPane.getChildren().add(node);
    
        if (!node.isMainNode()) {
            ConnectionLine connection;
            if (node.getConnectionType() == ConnectionType.VIRTUAL) {
                NetworkNode host = instance.getMainNodeByDisplayName("Host");
                connection = new ConnectionLine(host, node);
            } else if (node.getNetworkType() == NetworkType.INTERNAL) {
                NetworkNode gateway = instance.getMainNodeByDisplayName("Gateway");
                connection = new ConnectionLine(gateway, node);
            } else {
                NetworkNode internet = instance.getMainNodeByDisplayName("Google DNS");
                connection = new ConnectionLine(internet, node);
            }
            instance.spiderMapPane.getChildren().add(0, connection);
        }
    }
    
    private void loadWindowSize() {
        try {
            if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) {
                String json = new String(Files.readAllBytes(Paths.get(WINDOW_CONFIG_FILE)));
                Gson gson = new Gson();
                WindowConfig wc = gson.fromJson(json, WindowConfig.class);
                if (wc != null) {
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
            WindowConfig wc = new WindowConfig(primaryStage.getWidth(), primaryStage.getHeight());
            Gson gson = new Gson();
            String json = gson.toJson(wc);
            Files.write(Paths.get(WINDOW_CONFIG_FILE), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Pane createSpiderMapPane() {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: #182030;");
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

        NetworkNode gatewayNode = new NetworkNode("192.168.1.254", "Gateway", DeviceType.GATEWAY, NetworkType.INTERNAL);
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

    private void loadNodesFromFile() {
        Platform.runLater(() -> {
            try {
                String json = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                Gson gson = new Gson();
                Type listType = new TypeToken<List<NodeConfig>>() {}.getType();
                List<NodeConfig> configs = gson.fromJson(json, listType);
                double paneWidth = spiderMapPane.getWidth();
                double paneHeight = spiderMapPane.getHeight();
                if (paneWidth < 100) paneWidth = primaryStage.getScene().getWidth();
                if (paneHeight < 100) paneHeight = primaryStage.getScene().getHeight();
                for (NodeConfig config : configs) {
                    double absoluteX = config.getRelativeX() * paneWidth;
                    double absoluteY = config.getRelativeY() * paneHeight;
                    System.out.println("Loading node: relativeX=" + config.getRelativeX() +
                                       ", paneWidth=" + paneWidth + ", computed X=" + absoluteX);
                    NetworkNode node = new NetworkNode(
                            config.getIpOrHostname(),
                            config.getDisplayName(),
                            config.getDeviceType(),
                            config.getNetworkType()
                    );
                    node.setLayoutX(absoluteX);
                    node.setLayoutY(absoluteY);
                    node.setMainNode(config.isMainNode());
                    if (config.getNodeColour() != null) {
                        node.setOutlineColor(config.getNodeColour());
                    }
                    if (config.getConnectionType() != null) {
                        node.setConnectionType(config.getConnectionType());
                    }
                    addDetailPanelHandler(node);
                    persistentNodes.add(node);
                    persistentNodesStatic.add(node);
                    spiderMapPane.getChildren().add(node);

                    if (!node.isMainNode()) {
                        ConnectionLine connection;
                        if (node.getConnectionType() == ConnectionType.VIRTUAL) {
                            NetworkNode host = getMainNodeByDisplayName("Host");
                            connection = new ConnectionLine(host, node);
                        } else if (node.getNetworkType() == NetworkType.INTERNAL) {
                            NetworkNode gateway = getMainNodeByDisplayName("Gateway");
                            connection = new ConnectionLine(gateway, node);
                        } else {
                            NetworkNode internet = getMainNodeByDisplayName("Google DNS");
                            connection = new ConnectionLine(internet, node);
                        }
                        spiderMapPane.getChildren().add(0, connection);
                    }
                    node.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
                        // No additional action.
                    });
                }
                List<NetworkNode> mainNodes = new ArrayList<>();
                for (NetworkNode node : persistentNodes) {
                    if (node.isMainNode()) {
                        mainNodes.add(node);
                    }
                }
                mainNodes.sort((a, b) -> Double.compare(a.getLayoutY(), b.getLayoutY()));
                for (int i = 0; i < mainNodes.size() - 1; i++) {
                    ConnectionLine connection = new ConnectionLine(mainNodes.get(i), mainNodes.get(i + 1));
                    spiderMapPane.getChildren().add(0, connection);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void addDetailPanelHandler(NetworkNode node) {
        node.setOnMouseClicked(e -> {
            StackPane rootStack = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            rootStack.getChildren().removeIf(n -> n instanceof NodeDetailPanel);
            NodeDetailPanel detailPanel = new NodeDetailPanel(node);
            primaryStage.getScene().getStylesheets().add(getClass().getResource("/styles/nodedetails.css").toExternalForm());
            detailPanel.showPanel();
            StackPane.setAlignment(detailPanel, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(detailPanel, new Insets(20));
            rootStack.getChildren().add(detailPanel);
            javafx.event.EventHandler<MouseEvent> filter = new javafx.event.EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent ev) {
                    if (!detailPanel.getBoundsInParent().contains(ev.getX(), ev.getY())) {
                        detailPanel.hidePanel();
                        rootStack.removeEventFilter(MouseEvent.MOUSE_PRESSED, this);
                    }
                }
            };
            rootStack.addEventFilter(MouseEvent.MOUSE_PRESSED, filter);
        });
    }

    public static void updateConnectionLineForNode(NetworkNode node) {
        // Remove existing connection lines targeting this node.
        instance.spiderMapPane.getChildren().removeIf(child ->
            child instanceof ConnectionLine && (((ConnectionLine) child).getTo() == node)
        );
        
        // Create a new connection line if the node is not a main node.
        if (!node.isMainNode()) {
            ConnectionLine connection;
            if (node.getConnectionType() == ConnectionType.VIRTUAL) {
                NetworkNode host = instance.getMainNodeByDisplayName("Host");
                connection = new ConnectionLine(host, node);
            } else if (node.getNetworkType() == NetworkType.INTERNAL) {
                NetworkNode gateway = instance.getMainNodeByDisplayName("Gateway");
                connection = new ConnectionLine(gateway, node);
            } else {
                NetworkNode internet = instance.getMainNodeByDisplayName("Google DNS");
                connection = new ConnectionLine(internet, node);
            }
            instance.spiderMapPane.getChildren().add(0, connection);
        }
    }

    private void saveNodesToFile() {
        try {
            List<NodeConfig> configs = new ArrayList<>();
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth < 100) paneWidth = primaryStage.getScene().getWidth();
            if (paneHeight < 100) paneHeight = primaryStage.getScene().getHeight();
            for (NetworkNode node : persistentNodes) {
                double relativeX = node.getLayoutX() / paneWidth;
                double relativeY = node.getLayoutY() / paneHeight;
                System.out.println("Saving node: layoutX=" + node.getLayoutX() +
                                   ", paneWidth=" + paneWidth + ", relativeX=" + relativeX);
                NodeConfig config = new NodeConfig(
                        node.getIpOrHostname(),
                        node.getDisplayName(),
                        node.getDeviceType(),
                        node.getNetworkType(),
                        node.getLayoutX(),
                        node.getLayoutY(),
                        relativeX,
                        relativeY,
                        node.isMainNode(),
                        node.getOutlineColor(),
                        node.getConnectionType()
                );
                configs.add(config);
            }
            Gson gson = new Gson();
            String json = gson.toJson(configs);
            Files.write(Paths.get(CONFIG_FILE), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveZonesToFile() {
        try {
            List<ZoneConfig> zoneConfigs = new ArrayList<>();
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth < 100) paneWidth = primaryStage.getScene().getWidth();
            if (paneHeight < 100) paneHeight = primaryStage.getScene().getHeight();
            for (DrawableZone zone : zones) {
                double relativeX = zone.getLayoutX() / paneWidth;
                double relativeY = zone.getLayoutY() / paneHeight;
                ZoneConfig zc = new ZoneConfig(zone.getZoneName(), zone.getLayoutX(),
                                               zone.getLayoutY(), zone.getPrefWidth(),
                                               zone.getPrefHeight(), relativeX, relativeY);
                zoneConfigs.add(zc);
            }
            Gson gson = new Gson();
            String json = gson.toJson(zoneConfigs);
            Files.write(Paths.get(ZONES_FILE), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadZonesFromFile() {
        Platform.runLater(() -> {
            try {
                if (!Files.exists(Paths.get(ZONES_FILE))) return;
                String json = new String(Files.readAllBytes(Paths.get(ZONES_FILE)));
                Gson gson = new Gson();
                Type listType = new TypeToken<List<ZoneConfig>>() {}.getType();
                List<ZoneConfig> zoneConfigs = gson.fromJson(json, listType);
                double paneWidth = spiderMapPane.getWidth();
                double paneHeight = spiderMapPane.getHeight();
                if (paneWidth < 100) paneWidth = primaryStage.getScene().getWidth();
                if (paneHeight < 100) paneHeight = primaryStage.getScene().getHeight();
                for (ZoneConfig zc : zoneConfigs) {
                    double absoluteX = zc.getRelativeX() * paneWidth;
                    double absoluteY = zc.getRelativeY() * paneHeight;
                    DrawableZone zone = new DrawableZone(absoluteX, absoluteY, zc.getWidth(), zc.getHeight());
                    zone.setZoneName(zc.getZoneName());
                    zones.add(zone);
                    spiderMapPane.getChildren().add(zone);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        saveNodesToFile();
        saveZonesToFile();
        saveWindowSize();
        super.stop();
    }

    private NetworkNode getMainNodeByDisplayName(String name) {
        for (NetworkNode node : persistentNodes) {
            if (node.isMainNode() && node.getDisplayName().equalsIgnoreCase(name)) {
                return node;
            }
        }
        return null;
    }

    public static List<NetworkNode> getPersistentNodesStatic() {
        return persistentNodesStatic;
    }

    public static void removeZone(DrawableZone zone) {
        instance.zones.remove(zone);
        instance.saveZonesToFile();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
