package org.example;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
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

    Pane spiderMapPane;
    private List<NetworkNode> persistentNodes = new ArrayList<>();
    private static List<NetworkNode> persistentNodesStatic = new ArrayList<>();
    private List<DrawableZone> zones = new ArrayList<>();

    private VBox statusPanel;
    private Label totalLabel, upLabel, downLabel;

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + "NetworkMonitorApp";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "nodes.json";
    private static final String ZONES_FILE = CONFIG_DIR + File.separator + "zones.json";
    private static final String WINDOW_CONFIG_FILE = CONFIG_DIR + File.separator + "window.config";
    private static final double DETAIL_PANEL_WIDTH = 350;
    private NodeDetailPanel currentDetailPanel;

    private static NetworkMonitorApp instance;
    private Stage primaryStage;

    private double prevSceneWidth = 0;
    private double prevSceneHeight = 0;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;

        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setTitle("Network Device Monitor");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/node.png")));

        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) configDir.mkdirs();

        BorderPane root = new BorderPane();
        spiderMapPane = createSpiderMapPane();
        StackPane centerStack = new StackPane();
        centerStack.getChildren().add(spiderMapPane);
        root.setCenter(centerStack);

        if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) loadWindowSize();
        else primaryStage.setMaximized(true);

        Scene scene = new Scene(root);
        centerStack.setStyle("-fx-background-color: #192428;");
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/nodedetails.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        prevSceneWidth = scene.getWidth();
        prevSceneHeight = scene.getHeight();

        NewNodeBox newNodeBox = new NewNodeBox();
        scene.getStylesheets().add(getClass().getResource("/styles/newnodebox.css").toExternalForm());
        spiderMapPane.getChildren().add(newNodeBox);

        // 15px from left
        newNodeBox.setLayoutX(15);
        // 15px from bottom
        newNodeBox.layoutYProperty().bind(
            spiderMapPane.heightProperty()
                     .subtract(newNodeBox.heightProperty())
                     .subtract(15)
        );

        FilterBox filterBox = new FilterBox();
        scene.getStylesheets().add(getClass().getResource("/styles/filterbox.css").toExternalForm());
        spiderMapPane.getChildren().add(filterBox);
        filterBox.layoutXProperty().bind(newNodeBox.layoutXProperty().add(newNodeBox.widthProperty()).add(10));

        spiderMapPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            javafx.geometry.Point2D pt = spiderMapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            if (newNodeBox.isExpanded() && !newNodeBox.getBoundsInParent().contains(pt)) newNodeBox.collapse();
            if (filterBox.isExpanded() && !filterBox.getBoundsInParent().contains(pt)) filterBox.collapse();
        });

        Platform.runLater(() -> {
            if (!Files.exists(Paths.get(CONFIG_FILE))) createDefaultMainNodes();
            else loadNodesFromFile();
            loadZonesFromFile();
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
            child instanceof ConnectionLine && (((ConnectionLine) child).getFrom()==node || ((ConnectionLine) child).getTo()==node)
        );
        instance.saveNodesToFile();
    }

    public static void addNewNode(NetworkNode node) {
        instance.persistentNodes.add(node);
        persistentNodesStatic.add(node);
        instance.addDetailPanelHandler(node);
        instance.spiderMapPane.getChildren().add(node);

        if (node.getRouteSwitch()!=null && !node.getRouteSwitch().isEmpty()) {
            NetworkNode routeNode = null;
            for (NetworkNode n : persistentNodesStatic) {
                DeviceType dt = n.getDeviceType();
                if ((dt == DeviceType.SWITCH || dt==DeviceType.WIRELESS_ACCESS_POINT)
                 && n.getDisplayName().equalsIgnoreCase(node.getRouteSwitch())) {
                    routeNode = n; break;
                }
            }
            if (routeNode!=null) {
                ConnectionLine line = new ConnectionLine(routeNode,node);
                instance.spiderMapPane.getChildren().add(0,line);
                return;
            }
        }

        if (!node.isMainNode()) {
            ConnectionLine connection;
            if (node.getConnectionType()==ConnectionType.VIRTUAL) {
                NetworkNode host=instance.getMainNodeByDisplayName("Host");
                connection=new ConnectionLine(host,node);
            } else if (node.getNetworkType()==NetworkType.INTERNAL) {
                NetworkNode gw=instance.getMainNodeByDisplayName("Gateway");
                connection=new ConnectionLine(gw,node);
            } else {
                NetworkNode internet=instance.getMainNodeByDisplayName("Google DNS");
                connection=new ConnectionLine(internet,node);
            }
            instance.spiderMapPane.getChildren().add(0,connection);
        }
    }

    public static NetworkNode getUpstreamNode(NetworkNode node) {
        if (node.getNetworkType()==NetworkType.INTERNAL)
            return instance.getMainNodeByDisplayName("Gateway");
        return instance.getMainNodeByDisplayName("Google DNS");
    }

    private void loadWindowSize() {
        try {
            if (Files.exists(Paths.get(WINDOW_CONFIG_FILE))) {
                String json=new String(Files.readAllBytes(Paths.get(WINDOW_CONFIG_FILE)));
                Gson gson=new Gson();
                WindowConfig wc=gson.fromJson(json,WindowConfig.class);
                if(wc!=null) {
                    primaryStage.setX(wc.getX()); primaryStage.setY(wc.getY());
                    primaryStage.setWidth(wc.getWidth()); primaryStage.setHeight(wc.getHeight());
                }
            }
        } catch(Exception e){e.printStackTrace();}
    }

    private void saveWindowSize() {
        try {
            WindowConfig wc=new WindowConfig(primaryStage.getX(),primaryStage.getY(),
                                             primaryStage.getWidth(),primaryStage.getHeight());
            Gson gson=new Gson(); String json=gson.toJson(wc);
            Files.write(Paths.get(WINDOW_CONFIG_FILE),json.getBytes());
        } catch(Exception e){e.printStackTrace();}
    }

    private Pane createSpiderMapPane() {
        Pane pane=new Pane(); pane.getStyleClass().add("spider-map-pane"); return pane;
    }

    private void createDefaultMainNodes() {
        double centerX=primaryStage.getWidth()/2;
        double centerY=primaryStage.getHeight()/2;
        double spacing=300;

        NetworkNode hostNode=new NetworkNode("127.0.0.1","Host",DeviceType.COMPUTER,NetworkType.INTERNAL);
        hostNode.setLayoutX(centerX-hostNode.getPrefWidth()/2);
        hostNode.setLayoutY(centerY-spacing-hostNode.getPrefHeight()/2);
        hostNode.setMainNode(true);
        addDetailPanelHandler(hostNode);
        persistentNodes.add(hostNode); persistentNodesStatic.add(hostNode);
        spiderMapPane.getChildren().add(hostNode);

        String gw=NetworkUtils.getDefaultGateway(); if(gw==null) gw="192.168.0.1";
        NetworkNode gatewayNode=new NetworkNode(gw,"Gateway",DeviceType.GATEWAY,NetworkType.INTERNAL);
        gatewayNode.setLayoutX(centerX-gatewayNode.getPrefWidth()/2);
        gatewayNode.setLayoutY(centerY-gatewayNode.getPrefHeight()/2);
        gatewayNode.setMainNode(true);
        addDetailPanelHandler(gatewayNode);
        persistentNodes.add(gatewayNode); persistentNodesStatic.add(gatewayNode);
        spiderMapPane.getChildren().add(gatewayNode);

        NetworkNode internetNode=new NetworkNode("8.8.8.8","Google DNS",DeviceType.ROUTER,NetworkType.EXTERNAL);
        internetNode.setLayoutX(centerX-internetNode.getPrefWidth()/2);
        internetNode.setLayoutY(centerY+spacing-internetNode.getPrefHeight()/2);
        internetNode.setMainNode(true);
        addDetailPanelHandler(internetNode);
        persistentNodes.add(internetNode); persistentNodesStatic.add(internetNode);
        spiderMapPane.getChildren().add(internetNode);

        ConnectionLine line1=new ConnectionLine(hostNode,gatewayNode);
        ConnectionLine line2=new ConnectionLine(gatewayNode,internetNode);
        spiderMapPane.getChildren().add(0,line1);
        spiderMapPane.getChildren().add(0,line2);
    }

    private void loadNodesFromFile() {
        Platform.runLater(() -> {
            try {
                String json=new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                Gson gson=new Gson();
                Type listType=new TypeToken<List<NodeConfig>>(){}.getType();
                List<NodeConfig> configs=gson.fromJson(json,listType);
                double paneWidth=spiderMapPane.getWidth(); double paneHeight=spiderMapPane.getHeight();
                if(paneWidth<100) paneWidth=primaryStage.getScene().getWidth();
                if(paneHeight<100) paneHeight=primaryStage.getScene().getHeight();
                for(NodeConfig config:configs) {
                    double absoluteX=config.getRelativeX()*paneWidth;
                    double absoluteY=config.getRelativeY()*paneHeight;
                    NetworkNode node=new NetworkNode(
                        config.getIpOrHostname(),config.getDisplayName(),
                        config.getDeviceType(),config.getNetworkType());
                    node.setPrefSize(config.getWidth(),config.getHeight());
                    node.updateLayoutForSavedSize();
                    node.setLayoutX(absoluteX); node.setLayoutY(absoluteY);
                    node.setMainNode(config.isMainNode());
                    if(config.getConnectionType()!=null)
                        node.setConnectionType(config.getConnectionType());
                    node.setRouteSwitch(config.getRouteSwitch());
                    addDetailPanelHandler(node);
                    persistentNodes.add(node); persistentNodesStatic.add(node);
                    spiderMapPane.getChildren().add(node);

                    if(!node.isMainNode()) {
                        if(node.getRouteSwitch()!=null && !node.getRouteSwitch().isEmpty()) {
                            NetworkNode routeNode=null;
                            for(NetworkNode n:persistentNodesStatic) {
                                DeviceType dt=n.getDeviceType();
                                if((dt==DeviceType.SWITCH||dt==DeviceType.WIRELESS_ACCESS_POINT)
                                  &&n.getDisplayName().equalsIgnoreCase(node.getRouteSwitch())) {
                                    routeNode=n; break;
                                }
                            }
                            if(routeNode!=null) {
                                ConnectionLine l1=new ConnectionLine(getUpstreamNode(node),routeNode);
                                l1.setLineColor(Color.GREY);
                                ConnectionLine l2=new ConnectionLine(routeNode,node);
                                spiderMapPane.getChildren().add(0,l1);
                                spiderMapPane.getChildren().add(0,l2);
                            } else addDefaultConnectionLine(node);
                        } else addDefaultConnectionLine(node);
                    }
                    node.addEventHandler(MouseEvent.MOUSE_RELEASED,e->{});
                }

                List<NetworkNode> mainNodes=new ArrayList<>();
                for(NetworkNode node:persistentNodes)
                    if(node.isMainNode()) mainNodes.add(node);
                mainNodes.sort((a,b)->Double.compare(a.getLayoutY(),b.getLayoutY()));
                for(int i=0;i<mainNodes.size()-1;i++) {
                    ConnectionLine c=new ConnectionLine(mainNodes.get(i),mainNodes.get(i+1));
                    spiderMapPane.getChildren().add(0,c);
                }

                for(NetworkNode node:persistentNodes) {
                    if(node.getRouteSwitch()!=null && !node.getRouteSwitch().isEmpty()) {
                        updateConnectionLineForNode(node);
                    }
                }
            } catch(Exception e){e.printStackTrace();}
        });
    }

    private void addDefaultConnectionLine(NetworkNode node) {
        ConnectionLine connection;
        if(node.getConnectionType()==ConnectionType.VIRTUAL) {
            NetworkNode host=instance.getMainNodeByDisplayName("Host");
            connection=new ConnectionLine(host,node);
        } else if(node.getNetworkType()==NetworkType.INTERNAL) {
            NetworkNode gw=instance.getMainNodeByDisplayName("Gateway");
            connection=new ConnectionLine(gw,node);
        } else {
            NetworkNode internet=instance.getMainNodeByDisplayName("Google DNS");
            connection=new ConnectionLine(internet,node);
        }
        spiderMapPane.getChildren().add(0,connection);
    }

    private void addDetailPanelHandler(NetworkNode node) {
        node.setOnMouseClicked(e-> {
            if(e.getButton()==MouseButton.PRIMARY && e.getClickCount()==2)
                showDetailPanel(node);
        });
    }

    public static void updateConnectionLineForNode(NetworkNode node) {
        instance.spiderMapPane.getChildren().removeIf(child->
            child instanceof ConnectionLine && (((ConnectionLine)child).getFrom()==node||((ConnectionLine)child).getTo()==node)
        );

        if(node.getRouteSwitch()!=null && !node.getRouteSwitch().isEmpty()) {
            NetworkNode routeNode=null;
            for(NetworkNode n:persistentNodesStatic) {
                DeviceType dt=n.getDeviceType();
                if((dt==DeviceType.SWITCH||dt==DeviceType.WIRELESS_ACCESS_POINT)
                 &&n.getDisplayName().equalsIgnoreCase(node.getRouteSwitch())) {
                    routeNode=n; break;
                }
            }
            if(routeNode!=null) {
                ConnectionLine line=new ConnectionLine(routeNode,node);
                instance.spiderMapPane.getChildren().add(0,line);
                return;
            }
        }

        if(!node.isMainNode()) {
            ConnectionLine connection;
            if(node.getConnectionType()==ConnectionType.VIRTUAL) {
                NetworkNode host=instance.getMainNodeByDisplayName("Host");
                connection=new ConnectionLine(host,node);
            } else if(node.getNetworkType()==NetworkType.INTERNAL) {
                NetworkNode gw=instance.getMainNodeByDisplayName("Gateway");
                connection=new ConnectionLine(gw,node);
            } else {
                NetworkNode internet=instance.getMainNodeByDisplayName("Google DNS");
                connection=new ConnectionLine(internet,node);
            }
            instance.spiderMapPane.getChildren().add(0,connection);
        }
    }

    private void saveNodesToFile() {
        try {
            List<NodeConfig> configs=new ArrayList<>();
            double paneWidth=spiderMapPane.getWidth(), paneHeight=spiderMapPane.getHeight();
            if(paneWidth<100) paneWidth=primaryStage.getScene().getWidth();
            if(paneHeight<100) paneHeight=primaryStage.getScene().getHeight();
            for(NetworkNode node:persistentNodes) {
                double relativeX=node.getLayoutX()/paneWidth;
                double relativeY=node.getLayoutY()/paneHeight;
                NodeConfig config=new NodeConfig(
                    node.getIpOrHostname(),node.getDisplayName(),node.getDeviceType(),
                    node.getNetworkType(),node.getLayoutX(),node.getLayoutY(),
                    relativeX,relativeY,node.isMainNode(),node.getConnectionType(),
                    node.getPrefWidth(),node.getPrefHeight(),node.getRouteSwitch()
                );
                configs.add(config);
            }
            Gson gson=new Gson(); String json=gson.toJson(configs);
            Files.write(Paths.get(CONFIG_FILE),json.getBytes());
        } catch(Exception e){e.printStackTrace();}
    }

    private void saveZonesToFile() {
        try {
            List<ZoneConfig> zoneConfigs=new ArrayList<>();
            double paneWidth=spiderMapPane.getWidth(), paneHeight=spiderMapPane.getHeight();
            if(paneWidth<100) paneWidth=primaryStage.getScene().getWidth();
            if(paneHeight<100) paneHeight=primaryStage.getScene().getHeight();
            for(DrawableZone zone:zones) {
                double relativeX=zone.getLayoutX()/paneWidth;
                double relativeY=zone.getLayoutY()/paneHeight;
                ZoneConfig zc=new ZoneConfig(zone.getZoneName(),zone.getLayoutX(),zone.getLayoutY(),
                    zone.getPrefWidth(),zone.getPrefHeight(),relativeX,relativeY);
                zoneConfigs.add(zc);
            }
            Gson gson=new Gson(); String json=gson.toJson(zoneConfigs);
            Files.write(Paths.get(ZONES_FILE),json.getBytes());
        } catch(Exception e){e.printStackTrace();}
    }

    private void loadZonesFromFile() {
        Platform.runLater(() -> {
            try {
                if(!Files.exists(Paths.get(ZONES_FILE))) return;
                String json=new String(Files.readAllBytes(Paths.get(ZONES_FILE)));
                Gson gson=new Gson();
                Type listType=new TypeToken<List<ZoneConfig>>(){}.getType();
                List<ZoneConfig> zoneConfigs=gson.fromJson(json,listType);
                double paneWidth=spiderMapPane.getWidth(), paneHeight=spiderMapPane.getHeight();
                if(paneWidth<100) paneWidth=primaryStage.getScene().getWidth();
                if(paneHeight<100) paneHeight=primaryStage.getScene().getHeight();
                for(ZoneConfig zc:zoneConfigs) {
                    double absoluteX=zc.getRelativeX()*paneWidth;
                    double absoluteY=zc.getRelativeY()*paneHeight;
                    DrawableZone zone=new DrawableZone(absoluteX,absoluteY,zc.getWidth(),zc.getHeight());
                    zone.setZoneName(zc.getZoneName());
                    zones.add(zone);
                    spiderMapPane.getChildren().add(zone);
                }
            } catch(Exception e){e.printStackTrace();}
        });
    }

    @Override
    public void stop() throws Exception {
        saveNodesToFile(); saveZonesToFile(); saveWindowSize(); super.stop();
    }

    public static void main(String[] args) { launch(args); }

    private NetworkNode getMainNodeByDisplayName(String name) {
        for(NetworkNode node:persistentNodes) {
            if(node.isMainNode() && node.getDisplayName().equalsIgnoreCase(name)) return node;
        }
        return null;
    }

    public static List<NetworkNode> getPersistentNodesStatic() { return persistentNodesStatic; }

    public static void removeZone(DrawableZone zone) {
        instance.zones.remove(zone); instance.saveZonesToFile();
    }

    public void showDetailPanel(NetworkNode node) {
        StackPane rootStack=(StackPane)((BorderPane)primaryStage.getScene().getRoot()).getCenter();
        rootStack.getChildren().removeIf(n->n instanceof NodeDetailPanel);
        NodeDetailPanel panel=new NodeDetailPanel(node);
        panel.setPrefWidth(DETAIL_PANEL_WIDTH);
        panel.setMaxWidth(DETAIL_PANEL_WIDTH);
        panel.setMinWidth(DETAIL_PANEL_WIDTH);
        panel.prefHeightProperty().bind(primaryStage.getScene().heightProperty());
        panel.setTranslateX(DETAIL_PANEL_WIDTH);
        rootStack.getChildren().add(panel);
        StackPane.setAlignment(panel,Pos.TOP_RIGHT);
        panel.requestFocus();
        Platform.runLater(() -> {
            Timeline slide=new Timeline(
                new KeyFrame(Duration.millis(200),
                    new KeyValue(panel.translateXProperty(),0),
                    new KeyValue(spiderMapPane.translateXProperty(),-DETAIL_PANEL_WIDTH)
                )
            ); slide.play();
        });
        currentDetailPanel=panel;
    }

    public void hideDetailPanel() {
        if(currentDetailPanel==null) return;
        StackPane rootStack=(StackPane)((BorderPane)primaryStage.getScene().getRoot()).getCenter();
        Timeline slide=new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(currentDetailPanel.translateXProperty(),DETAIL_PANEL_WIDTH),
                new KeyValue(spiderMapPane.translateXProperty(),0)
            )
        );
        slide.setOnFinished(e->rootStack.getChildren().remove(currentDetailPanel));
        slide.play();
    }

    public static void performTraceroute(NetworkNode source) {
        Pane spiderPane=instance.spiderMapPane;
        StackPane rootStack=(StackPane)((BorderPane)instance.primaryStage.getScene().getRoot()).getCenter();
        rootStack.getChildren().removeIf(n->n instanceof TraceroutePanel);
        TraceroutePanel panel=new TraceroutePanel();
        StackPane.setAlignment(panel,Pos.TOP_LEFT); StackPane.setMargin(panel,new Insets(10));
        rootStack.getChildren().add(panel);

        NetworkNode hostFinal;
        NetworkNode tempHost=instance.getMainNodeByDisplayName("Host");
        hostFinal=tempHost==null?source:tempHost;

        class Origin { NetworkNode node; double x,y; boolean isVirtual=false; }
        Origin origin=new Origin(); origin.node=hostFinal;
        origin.x=hostFinal.getLayoutX()+hostFinal.getWidth()/2;
        origin.y=hostFinal.getLayoutY()+hostFinal.getHeight()/2;

        java.util.concurrent.atomic.AtomicInteger hopCounter=new java.util.concurrent.atomic.AtomicInteger(0);
        List<TracerouteLine> tracerouteLines=new ArrayList<>();
        String target=source.getIpOrHostname();
        TracerouteTask task=new TracerouteTask(target);

        task.setHopCallback(hop-> {
            int index=hopCounter.incrementAndGet();
            panel.addHop("Hop "+index+": "+hop);
            NetworkNode targetNode=null;
            for(NetworkNode node:getPersistentNodesStatic()) {
                String ip=(node.getResolvedIp()!=null&&!node.getResolvedIp().isEmpty())?node.getResolvedIp():node.getIpOrHostname();
                if(ip.equals(hop)) { targetNode=node; break; }
            }
            TracerouteLine tLine;
            if(targetNode!=null) {
                if(origin.isVirtual) tLine=new TracerouteLine(origin.x,origin.y,targetNode);
                else tLine=new TracerouteLine(origin.node,targetNode);
                origin.node=targetNode;
                origin.x=targetNode.getLayoutX()+targetNode.getWidth()/2;
                origin.y=targetNode.getLayoutY()+targetNode.getHeight()/2;
                origin.isVirtual=false;
            } else {
                tLine=new TracerouteLine(origin.x,origin.y,hop,index-1);
                origin.x+=TracerouteLine.UNKNOWN_OFFSET;
                origin.node=null; origin.isVirtual=true;
            }
            tLine.setLineColor(Color.web("#C9EB78")); tracerouteLines.add(tLine);
            spiderPane.getChildren().add(0,tLine);
        });

        task.setOnSucceeded(e-> {
            panel.addHop("Traceroute complete");
            javafx.animation.PauseTransition pause=new javafx.animation.PauseTransition(Duration.seconds(5));
            pause.setOnFinished(ev-> {
                tracerouteLines.forEach(TracerouteLine::startFadeOut);
                panel.startFadeOut();
            });
            pause.play();
        });
        task.setOnFailed(e-> System.err.println("Traceroute failed: "+task.getException()));
        new Thread(task).start();
    }
}
