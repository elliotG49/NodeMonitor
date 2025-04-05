package org.example;

import java.io.File;
import java.lang.reflect.Type;
import java.net.InetAddress;
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
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NetworkMonitorApp extends Application {

    // Main map pane.
    private Pane spiderMapPane;
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

    // New inline filter dropdown pane.
    private FilterDropdownPane filterDropdownPane;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;
        
        // Ensure config directory exists.
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        BorderPane root = new BorderPane();
        spiderMapPane = createSpiderMapPane();
        statusPanel = createStatusPanel();

        StackPane centerStack = new StackPane();
        centerStack.getChildren().addAll(spiderMapPane, statusPanel);

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("floating-button");
        ContextMenu addMenu = new ContextMenu();
        MenuItem addNodeItem = new MenuItem("Add Node");
        MenuItem addZoneItem = new MenuItem("Add Zone");
        addMenu.getItems().addAll(addNodeItem, addZoneItem);
        plusButton.setOnAction(e -> {
            if (!addMenu.isShowing()) {
                addMenu.show(plusButton, Side.TOP, 0, 0);
            } else {
                addMenu.hide();
            }
        });
        addNodeItem.setOnAction(e -> addNewNode());
        addZoneItem.setOnAction(e -> enterZoneDrawingMode());
        StackPane.setAlignment(plusButton, Pos.BOTTOM_CENTER);
        StackPane.setMargin(plusButton, new Insets(0, 0, 20, 0));

        // Updated Filter button setup:
        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().add("floating-button");
        StackPane.setAlignment(filterButton, Pos.TOP_RIGHT);
        StackPane.setMargin(filterButton, new Insets(20, 20, 0, 0));
        
        // Create the inline dropdown pane and add it to the center stack.
        filterDropdownPane = new FilterDropdownPane(persistentNodes);
        filterDropdownPane.setVisible(false);
        // Position the dropdown pane below the filter button.
        StackPane.setAlignment(filterDropdownPane, Pos.TOP_RIGHT);
        StackPane.setMargin(filterDropdownPane, new Insets(60, 20, 0, 0));
        
        // Set callbacks for the dropdown pane.
        filterDropdownPane.setOnApply(options -> {
            applyFilter(options);
            filterDropdownPane.hideDropdown();
        });
        filterDropdownPane.setOnCancel(() -> {
            // Clear filters when canceled.
            for (NetworkNode node : persistentNodes) {
                node.setVisible(true);
            }
            for (javafx.scene.Node n : spiderMapPane.getChildren()) {
                if (n instanceof ConnectionLine) {
                    ConnectionLine cl = (ConnectionLine) n;
                    cl.setVisible(cl.getFrom().isVisible() && cl.getTo().isVisible());
                }
            }
            filterDropdownPane.hideDropdown();
        });
        
        centerStack.getChildren().addAll(plusButton, filterButton, filterDropdownPane);

        // Toggle dropdown on filter button click.
        filterButton.setOnAction(event -> {
            if (filterDropdownPane.isVisible()) {
                filterDropdownPane.hideDropdown();
            } else {
                filterDropdownPane.showDropdown();
            }
        });

        root.setCenter(centerStack);

        // Load window size.
        if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) {
            loadWindowSize();
        } else {
            primaryStage.setWidth(1920);
            primaryStage.setHeight(1080);
        }

        // Load nodes and zones.
        if (!Files.exists(Paths.get(CONFIG_FILE))) {
            createDefaultMainNodes();
        } else {
            loadNodesFromFile();
        }
        loadZonesFromFile();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        primaryStage.setTitle("Network Device Monitor");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Hide dropdown if click outside.
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (filterDropdownPane.isVisible()) {
                Bounds dropdownBounds = filterDropdownPane.localToScene(filterDropdownPane.getBoundsInLocal());
                Bounds buttonBounds = filterButton.localToScene(filterButton.getBoundsInLocal());
                if (!dropdownBounds.contains(e.getSceneX(), e.getSceneY()) &&
                    !buttonBounds.contains(e.getSceneX(), e.getSceneY())) {
                    filterDropdownPane.hideDropdown();
                }
            }
        });

        // Ensure nodes remain in bounds on window resize.
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

        Timeline statusTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> updateStatusPanel()));
        statusTimeline.setCycleCount(Timeline.INDEFINITE);
        statusTimeline.play();

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
        pane.setStyle("-fx-background-color: #080E1B;");
        return pane;
    }

    private VBox createStatusPanel() {
        ImageView nodeIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/node.png")));
        nodeIcon.setFitWidth(16);
        nodeIcon.setFitHeight(16);
        totalLabel = new Label("0");
        totalLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        HBox totalBox = new HBox(5, nodeIcon, totalLabel);
        totalBox.setAlignment(Pos.CENTER_LEFT);

        ImageView upIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/up-arrow.png")));
        upIcon.setFitWidth(16);
        upIcon.setFitHeight(16);
        upLabel = new Label("0");
        upLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        HBox upBox = new HBox(5, upIcon, upLabel);
        upBox.setAlignment(Pos.CENTER_LEFT);

        ImageView downIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/down-arrow.png")));
        downIcon.setFitWidth(16);
        downIcon.setFitHeight(16);
        downLabel = new Label("0");
        downLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        HBox downBox = new HBox(5, downIcon, downLabel);
        downBox.setAlignment(Pos.CENTER_LEFT);

        VBox vbox = new VBox(5, totalBox, upBox, downBox);
        vbox.setPadding(new Insets(5));
        vbox.setStyle("-fx-background-color: #1A2B57; -fx-background-radius: 15; -fx-border-width: 1px; -fx-border-color: rgb(255,255,255); -fx-border-style: solid; -fx-border-radius: 15;");
        vbox.setPrefWidth(50);
        vbox.setMaxWidth(50);
        vbox.setPrefHeight(70);
        vbox.setMaxHeight(70);
        StackPane.setAlignment(vbox, Pos.TOP_LEFT);
        StackPane.setMargin(vbox, new Insets(10));
        return vbox;
    }

    private void updateStatusPanel() {
        new Thread(() -> {
            int total = persistentNodes.size();
            AtomicInteger upCount = new AtomicInteger(0);
            AtomicInteger downCount = new AtomicInteger(0);
            for (NetworkNode node : persistentNodes) {
                try {
                    String ip = node.getIpOrHostname();
                    if (!ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && node.getResolvedIp() != null) {
                        ip = node.getResolvedIp();
                    }
                    InetAddress address = InetAddress.getByName(ip);
                    boolean reachable = address.isReachable(1000);
                    if (reachable) {
                        upCount.incrementAndGet();
                    } else {
                        downCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    downCount.incrementAndGet();
                }
            }
            Platform.runLater(() -> {
                totalLabel.setText(String.valueOf(total));
                upLabel.setText(String.valueOf(upCount.get()));
                downLabel.setText(String.valueOf(downCount.get()));
            });
        }).start();
    }

    private void createDefaultMainNodes() {
        double defaultWidth = 1920;
        double defaultHeight = 1080;
        double centerX = (defaultWidth - 100) / 2;
        double centerY = defaultHeight / 2;
        double spacing = 150;

        NetworkNode hostNode = new NetworkNode("127.0.0.1", "Host", DeviceType.COMPUTER, NetworkType.INTERNAL);
        hostNode.setLayoutX(centerX);
        hostNode.setLayoutY(centerY - spacing);
        hostNode.setMainNode(true);
        setupContextMenu(hostNode);
        persistentNodes.add(hostNode);
        persistentNodesStatic.add(hostNode);
        spiderMapPane.getChildren().add(hostNode);

        NetworkNode gatewayNode = new NetworkNode("192.168.1.254", "Gateway", DeviceType.GATEWAY, NetworkType.INTERNAL);
        gatewayNode.setLayoutX(centerX);
        gatewayNode.setLayoutY(centerY);
        gatewayNode.setMainNode(true);
        setupContextMenu(gatewayNode);
        persistentNodes.add(gatewayNode);
        persistentNodesStatic.add(gatewayNode);
        spiderMapPane.getChildren().add(gatewayNode);

        NetworkNode internetNode = new NetworkNode("8.8.8.8", "Internet", DeviceType.ROUTER, NetworkType.EXTERNAL);
        internetNode.setLayoutX(centerX);
        internetNode.setLayoutY(centerY + spacing);
        internetNode.setMainNode(true);
        setupContextMenu(internetNode);
        persistentNodes.add(internetNode);
        persistentNodesStatic.add(internetNode);
        spiderMapPane.getChildren().add(internetNode);

        ConnectionLine line1 = new ConnectionLine(hostNode, gatewayNode);
        ConnectionLine line2 = new ConnectionLine(gatewayNode, internetNode);
        spiderMapPane.getChildren().add(0, line1);
        spiderMapPane.getChildren().add(0, line2);
    }

    private void addNewNode() {
        NewNodeStage newNodeStage = new NewNodeStage();
        NetworkNode newNode = newNodeStage.showAndGetResult();
        if (newNode != null) {
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth == 0) paneWidth = primaryStage.getWidth();
            if (paneHeight == 0) paneHeight = primaryStage.getHeight();
            newNode.setLayoutX(Math.random() * (paneWidth - 100) + 50);
            newNode.setLayoutY(Math.random() * (paneHeight - 100) + 50);
            spiderMapPane.getChildren().add(newNode);
            persistentNodes.add(newNode);
            persistentNodesStatic.add(newNode);
            setupContextMenu(newNode);

            newNode.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
                // No additional action required.
            });

            ConnectionLine connection;
            if (newNode.getConnectionType() == ConnectionType.VIRTUAL) {
                NetworkNode host = getMainNodeByDisplayName("Host");
                connection = new ConnectionLine(host, newNode);
            } else if (newNode.getNetworkType() == NetworkType.INTERNAL) {
                NetworkNode gateway = getMainNodeByDisplayName("Gateway");
                connection = new ConnectionLine(gateway, newNode);
            } else {
                NetworkNode internet = getMainNodeByDisplayName("Internet");
                connection = new ConnectionLine(internet, newNode);
            }
            spiderMapPane.getChildren().add(0, connection);
            saveNodesToFile();
        }
    }
    
    // --- Zone Drawing Logic ---
    private void enterZoneDrawingMode() {
        spiderMapPane.setCursor(Cursor.CROSSHAIR);
        final Rectangle tempZone = new Rectangle();
        tempZone.setStroke(Color.WHITE);
        tempZone.getStrokeDashArray().addAll(5.0, 5.0);
        tempZone.setFill(Color.TRANSPARENT);
        spiderMapPane.getChildren().add(tempZone);
        
        spiderMapPane.setOnMousePressed(me -> {
            tempZone.setX(me.getX());
            tempZone.setY(me.getY());
            tempZone.setWidth(0);
            tempZone.setHeight(0);
        });
        spiderMapPane.setOnMouseDragged(me -> {
            double width = me.getX() - tempZone.getX();
            double height = me.getY() - tempZone.getY();
            tempZone.setWidth(width);
            tempZone.setHeight(height);
        });
        spiderMapPane.setOnMouseReleased(me -> {
            double x = tempZone.getX();
            double y = tempZone.getY();
            double w = tempZone.getWidth();
            double h = tempZone.getHeight();
            x = Math.round(x / 10) * 10;
            y = Math.round(y / 10) * 10;
            w = Math.round(w / 10) * 10;
            h = Math.round(h / 10) * 10;
            if (w < 50) w = 50;
            if (h < 50) h = 50;
            
            DrawableZone zone = new DrawableZone(x, y, w, h);
            zones.add(zone);
            spiderMapPane.getChildren().add(zone);
            
            spiderMapPane.setCursor(Cursor.DEFAULT);
            spiderMapPane.setOnMousePressed(null);
            spiderMapPane.setOnMouseDragged(null);
            spiderMapPane.setOnMouseReleased(null);
            spiderMapPane.getChildren().remove(tempZone);
        });
    }
    // --- End Zone Drawing Logic ---
    
    private void applyFilter(FilterOptions options) {
        if (options == null) {
            for (NetworkNode node : persistentNodes) {
                node.setVisible(true);
            }
        } else if (options.getFilterMode() == FilterOptions.FilterMode.SUBNET) {
            List<String> subnets = options.getSelectedFilters();
            boolean clearFilter = subnets.isEmpty();
            for (NetworkNode node : persistentNodes) {
                if (node.getNetworkType() != NetworkType.INTERNAL) {
                    node.setVisible(true);
                } else if ("Host".equalsIgnoreCase(node.getDisplayName())) {
                    node.setVisible(true);
                } else if (clearFilter) {
                    node.setVisible(true);
                } else {
                    boolean match = false;
                    for (String subnet : subnets) {
                        if (node.getIpOrHostname().startsWith(subnet) ||
                            (node.getResolvedIp() != null && node.getResolvedIp().startsWith(subnet))) {
                            match = true;
                            break;
                        }
                    }
                    node.setVisible(match);
                }
            }
        } else if (options.getFilterMode() == FilterOptions.FilterMode.COLOUR) {
            List<String> colours = options.getSelectedFilters();
            boolean clearFilter = colours.isEmpty();
            for (NetworkNode node : persistentNodes) {
                if (node.getNetworkType() != NetworkType.INTERNAL) {
                    node.setVisible(true);
                } else if ("Host".equalsIgnoreCase(node.getDisplayName())) {
                    node.setVisible(true);
                } else if (clearFilter) {
                    node.setVisible(true);
                } else {
                    boolean match = false;
                    for (String col : colours) {
                        if (node.getOutlineColor().equalsIgnoreCase(col)) {
                            match = true;
                            break;
                        }
                    }
                    node.setVisible(match);
                }
            }
        } else if (options.getFilterMode() == FilterOptions.FilterMode.CONNECTION) {
            List<String> connections = options.getSelectedFilters();
            boolean clearFilter = connections.isEmpty();
            for (NetworkNode node : persistentNodes) {
                if (node.getNetworkType() != NetworkType.INTERNAL) {
                    node.setVisible(true);
                } else if ("Host".equalsIgnoreCase(node.getDisplayName())) {
                    node.setVisible(true);
                } else if (clearFilter) {
                    node.setVisible(true);
                } else {
                    boolean match = false;
                    String connType = node.getConnectionType().toString();
                    for (String conn : connections) {
                        if (connType.equalsIgnoreCase(conn)) {
                            match = true;
                            break;
                        }
                    }
                    node.setVisible(match);
                }
            }
        } else if (options.getFilterMode() == FilterOptions.FilterMode.DEVICE_TYPE) {
            List<String> deviceTypes = options.getSelectedFilters();
            boolean clearFilter = deviceTypes.isEmpty();
            for (NetworkNode node : persistentNodes) {
                if (node.getNetworkType() != NetworkType.INTERNAL) {
                    node.setVisible(true);
                } else if ("Host".equalsIgnoreCase(node.getDisplayName())) {
                    node.setVisible(true);
                } else if (clearFilter) {
                    node.setVisible(true);
                } else {
                    boolean match = false;
                    String devType = node.getDeviceType().toString();
                    for (String dt : deviceTypes) {
                        if (devType.equalsIgnoreCase(dt)) {
                            match = true;
                            break;
                        }
                    }
                    node.setVisible(match);
                }
            }
        }
        for (javafx.scene.Node n : spiderMapPane.getChildren()) {
            if (n instanceof ConnectionLine) {
                ConnectionLine cl = (ConnectionLine) n;
                cl.setVisible(cl.getFrom().isVisible() && cl.getTo().isVisible());
            }
        }
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
                if (paneWidth == 0) paneWidth = primaryStage.getWidth();
                if (paneHeight == 0) paneHeight = primaryStage.getHeight();
                for (NodeConfig config : configs) {
                    double absoluteX = config.getRelativeX() * paneWidth;
                    double absoluteY = config.getRelativeY() * paneHeight;
                    System.out.println("Loading node: relativeX=" + config.getRelativeX() + ", paneWidth=" + paneWidth + ", computed X=" + absoluteX);
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
                    setupContextMenu(node);
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
                            NetworkNode internet = getMainNodeByDisplayName("Internet");
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
    
    private void saveNodesToFile() {
        try {
            List<NodeConfig> configs = new ArrayList<>();
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth == 0) paneWidth = primaryStage.getWidth();
            if (paneHeight == 0) paneHeight = primaryStage.getHeight();
            for (NetworkNode node : persistentNodes) {
                double relativeX = node.getLayoutX() / paneWidth;
                double relativeY = node.getLayoutY() / paneHeight;
                System.out.println("Saving node: layoutX=" + node.getLayoutX() + ", paneWidth=" + paneWidth + ", relativeX=" + relativeX);
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
    
    // Zone saving and loading.
    private void saveZonesToFile() {
        try {
            List<ZoneConfig> zoneConfigs = new ArrayList<>();
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth == 0) paneWidth = primaryStage.getWidth();
            if (paneHeight == 0) paneHeight = primaryStage.getHeight();
            for (DrawableZone zone : zones) {
                double relativeX = zone.getLayoutX() / paneWidth;
                double relativeY = zone.getLayoutY() / paneHeight;
                ZoneConfig zc = new ZoneConfig(zone.getZoneName(), zone.getLayoutX(), zone.getLayoutY(), zone.getPrefWidth(), zone.getPrefHeight(), relativeX, relativeY);
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
        try {
            if (!Files.exists(Paths.get(ZONES_FILE))) return;
            String json = new String(Files.readAllBytes(Paths.get(ZONES_FILE)));
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ZoneConfig>>() {}.getType();
            List<ZoneConfig> zoneConfigs = gson.fromJson(json, listType);
            double paneWidth = spiderMapPane.getWidth();
            double paneHeight = spiderMapPane.getHeight();
            if (paneWidth == 0) paneWidth = primaryStage.getWidth();
            if (paneHeight == 0) paneHeight = primaryStage.getHeight();
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
    }
    
    public static void removeZone(DrawableZone zone) {
        instance.zones.remove(zone);
        instance.saveZonesToFile();
    }
    
    @Override
    public void stop() throws Exception {
        saveNodesToFile();
        saveZonesToFile();
        saveWindowSize();
        super.stop();
    }
    
    private void setupContextMenu(NetworkNode node) {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        if (node.isMainNode()) {
            deleteItem.setDisable(true);
            if ("Host".equalsIgnoreCase(node.getDisplayName())) {
                editItem.setDisable(true);
            }
        }
        editItem.setOnAction(e -> {
            EditNodeStage editStage = new EditNodeStage(node);
            NetworkNode updatedNode = editStage.showAndGetResult();
            if (updatedNode != null) {
                node.updateFrom(updatedNode);
            }
        });
        deleteItem.setOnAction(e -> {
            spiderMapPane.getChildren().remove(node);
            persistentNodes.remove(node);
            persistentNodesStatic.remove(node);
            removeConnectionsForNode(node);
            saveNodesToFile();
        });
        contextMenu.getItems().addAll(editItem, deleteItem);
        node.setOnContextMenuRequested(event ->
            contextMenu.show(node, event.getScreenX(), event.getScreenY())
        );
    }
    
    private void removeConnectionsForNode(NetworkNode node) {
        List<javafx.scene.Node> toRemove = new ArrayList<>();
        for (javafx.scene.Node n : spiderMapPane.getChildren()) {
            if (n instanceof ConnectionLine) {
                ConnectionLine cl = (ConnectionLine) n;
                if (cl.fromEquals(node) || cl.toEquals(node)) {
                    toRemove.add(n);
                }
            }
        }
        spiderMapPane.getChildren().removeAll(toRemove);
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
    
    public static void main(String[] args) {
        launch(args);
    }
}
