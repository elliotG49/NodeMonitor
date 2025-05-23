package org.example;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

public class ConnectionLine extends Pane {

    private final CubicCurve curve;
    private final NetworkNode from;
    private final NetworkNode to;

    private final Circle pingParticle;
    private final AnimationTimer pingTimer;
    private final Label latencyLabel; // Label to display latency

    private static final double CYCLE_DURATION = 3.0;

    private long lastTrailTime = 0;
    private static final long TRAIL_INTERVAL_NANOS = 10_000_000; // Reduced from 20_000_000
    private static final int TRAIL_COUNT = 8; // Number of trails to create
    private static final double TRAIL_BASE_RADIUS = 2.0;
    private static final double TRAIL_FADE_FACTOR = 0.8;

    private final long animationStartTime;
    private volatile boolean connected = false;

    private final PopupPanel statsPanel;
    private Color defaultColor = Color.RED;
    private Color hoveredColor;

    // Add a field to track hover state
    private boolean isHovered = false;

    private class PopupPanel extends StackPane {
        private final Label latencyLabel;
        private final Label interfaceLabel;

        public PopupPanel() {
            getStyleClass().add("connection-stats-popup");
            setVisible(false);

            latencyLabel = new Label("...");
            latencyLabel.getStyleClass().add("latency-label");
            interfaceLabel = new Label();
            interfaceLabel.getStyleClass().add("interface-label");

            VBox content = new VBox(5);
            content.setAlignment(Pos.CENTER);
            content.getChildren().addAll(latencyLabel, interfaceLabel);
            getChildren().add(content);

            // Style popup panel
            setStyle("-fx-background-color: #333333; -fx-padding: 10; " +
                     "-fx-border-color: #666666; -fx-border-width: 1; " +
                     "-fx-border-radius: 5; -fx-background-radius: 5;");
        }

        public void updateStats(String latency, String iface) {
            latencyLabel.setText(latency);
            interfaceLabel.setText(iface);
        }
    }

    public ConnectionLine(NetworkNode from, NetworkNode to) {
        this.from = from;
        this.to = to;

        // Create a container pane for better event handling
        Pane curveContainer = new Pane();
        curveContainer.setPickOnBounds(false);

        curve = new CubicCurve();
        curve.setStrokeWidth(4);
        curve.setStroke(defaultColor);
        curve.setFill(Color.TRANSPARENT);
        curve.setStrokeLineCap(StrokeLineCap.ROUND);
        curve.setStrokeLineJoin(StrokeLineJoin.ROUND);

        // Add the curve directly to the container
        curveContainer.getChildren().add(curve);
        getChildren().add(curveContainer);

        pingParticle = new Circle(2, Color.WHITE);
        getChildren().add(pingParticle);

        statsPanel = new PopupPanel();
        getChildren().add(statsPanel);

        // Add latency label
        latencyLabel = new Label();
        latencyLabel.setStyle("-fx-text-fill: white; -fx-padding: 2 4; -fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold;");
        latencyLabel.setVisible(false); // Initially hidden
        getChildren().add(latencyLabel);

        // Add hover behavior to the latency label
        latencyLabel.setOnMouseEntered(e -> {
            latencyLabel.setStyle("-fx-text-fill: white; -fx-padding: 2 4; -fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold;");
        });

        latencyLabel.setOnMouseExited(e -> {
            latencyLabel.setStyle("-fx-text-fill: white; -fx-padding: 2 4; -fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold;");
        });

        animationStartTime = System.nanoTime();

        pingTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateCurve();
                updateLatencyLabelPosition();

                double elapsedSeconds = (now - animationStartTime) / 1e9;
                double normalizedTime = (elapsedSeconds % CYCLE_DURATION) / CYCLE_DURATION;
                double fraction = !connected
                        ? (normalizedTime < 0.5 ? normalizedTime * 2 : 1.0)
                        : (normalizedTime < 0.5 ? normalizedTime * 2 : 1 - ((normalizedTime - 0.5) * 2));

                double t = fraction;
                double oneMinusT = 1 - t;
                double sx = curve.getStartX();
                double sy = curve.getStartY();
                double cx1 = curve.getControlX1();
                double cy1 = curve.getControlY1();
                double cx2 = curve.getControlX2();
                double cy2 = curve.getControlY2();
                double ex = curve.getEndX();
                double ey = curve.getEndY();

                double x = Math.pow(oneMinusT, 3) * sx +
                           3 * Math.pow(oneMinusT, 2) * t * cx1 +
                           3 * oneMinusT * Math.pow(t, 2) * cx2 +
                           Math.pow(t, 3) * ex;
                double y = Math.pow(oneMinusT, 3) * sy +
                           3 * Math.pow(oneMinusT, 2) * t * cy1 +
                           3 * oneMinusT * Math.pow(t, 2) * cy2 +   
                           Math.pow(t, 3) * ey;

                pingParticle.setLayoutX(x);
                pingParticle.setLayoutY(y);

                if (now - lastTrailTime > TRAIL_INTERVAL_NANOS) {
                    spawnTrail(x, y);
                    lastTrailTime = now;
                }
            }
        };
        pingTimer.start();

        updateStatus();
    }

    private void updateCurve() {
        double sx = from.getLayoutX() + from.getWidth() / 2;
        double sy = from.getLayoutY() + from.getHeight() / 2;
        double ex = to.getLayoutX() + to.getWidth() / 2;
        double ey = to.getLayoutY() + to.getHeight() / 2;

        curve.setStartX(sx);
        curve.setStartY(sy);
        curve.setEndX(ex);
        curve.setEndY(ey);

        double dx = ex - sx;
        double dy = ey - sy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double offset = distance * 0.15;
        double ux = -dy / distance;
        double uy = dx / distance;

        double cx1 = sx + dx / 3 + ux * offset;
        double cy1 = sy + dy / 3 + uy * offset;
        double cx2 = sx + 2 * dx / 3 + ux * offset;
        double cy2 = sy + 2 * dy / 3 + uy * offset;

        curve.setControlX1(cx1);
        curve.setControlY1(cy1);
        curve.setControlX2(cx2);
        curve.setControlY2(cy2);
    }

    private void updateLatencyLabelPosition() {
        // Calculate the midpoint of the cubic curve using Bézier formula at t = 0.5
        double t = 0.5;
        double oneMinusT = 1 - t;

        double midX = Math.pow(oneMinusT, 3) * curve.getStartX() +
                      3 * Math.pow(oneMinusT, 2) * t * curve.getControlX1() +
                      3 * oneMinusT * Math.pow(t, 2) * curve.getControlX2() +
                      Math.pow(t, 3) * curve.getEndX();

        double midY = Math.pow(oneMinusT, 3) * curve.getStartY() +
                      3 * Math.pow(oneMinusT, 2) * t * curve.getControlY1() +
                      3 * oneMinusT * Math.pow(t, 2) * curve.getControlY2() +
                      Math.pow(t, 3) * curve.getEndY();

        // Ensure the label is within the bounds of the parent container
        double labelWidth = latencyLabel.getWidth();
        double labelHeight = latencyLabel.getHeight();

        // Adjust position to keep the label within bounds
        double adjustedX = Math.max(0, midX - labelWidth / 2);
        double adjustedY = Math.max(0, midY - labelHeight / 2);

        latencyLabel.setLayoutX(adjustedX);
        latencyLabel.setLayoutY(adjustedY);
    }

    private void spawnTrail(double x, double y) {
        for (int i = 0; i < TRAIL_COUNT; i++) {
            final int trailIndex = i; // Create a final copy of i for this iteration
            Circle trail = new Circle(
                TRAIL_BASE_RADIUS * (1 - (trailIndex * 0.1)), // Gradually decrease size
                Color.WHITE.deriveColor(0, 1, 1, 1 - (trailIndex * TRAIL_FADE_FACTOR / TRAIL_COUNT))
            );
            trail.setLayoutX(x);
            trail.setLayoutY(y);
            getChildren().add(0, trail); // Add at index 0 to appear behind newer trails

            // Bind the trail's position to the curve dynamically
            AnimationTimer trailUpdater = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double t = 1.0 - (trailIndex / (double) TRAIL_COUNT); // Calculate the position along the curve
                    double oneMinusT = 1 - t;

                    double sx = curve.getStartX();
                    double sy = curve.getStartY();
                    double cx1 = curve.getControlX1();
                    double cy1 = curve.getControlY1();
                    double cx2 = curve.getControlX2();
                    double cy2 = curve.getControlY2();
                    double ex = curve.getEndX();
                    double ey = curve.getEndY();

                    double newX = Math.pow(oneMinusT, 3) * sx +
                                  3 * Math.pow(oneMinusT, 2) * t * cx1 +
                                  3 * oneMinusT * Math.pow(t, 2) * cx2 +
                                  Math.pow(t, 3) * ex;
                    double newY = Math.pow(oneMinusT, 3) * sy +
                                  3 * Math.pow(oneMinusT, 2) * t * cy1 +
                                  3 * oneMinusT * Math.pow(t, 2) * cy2 +
                                  Math.pow(t, 3) * ey;

                    trail.setLayoutX(newX);
                    trail.setLayoutY(newY);
                }
            };

            trailUpdater.start();

            // Fade out the trail and stop updating after the animation
            FadeTransition ft = new FadeTransition(Duration.seconds(0.4), trail);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                getChildren().remove(trail);
                trailUpdater.stop(); // Stop updating the trail after it fades out
            });
            ft.play();
        }
    }

    // Modify setLineColor to respect hover state
    public void setLineColor(Color color) {
        if (!isHovered) {
            defaultColor = color;
            curve.setStroke(color);
        }
    }

    public void updateStatus() {
        // Special handling for virtual machines
        if (to.getDeviceType() == DeviceType.VIRTUAL_MACHINE) {
            setLineColor(Color.web("#0cad03")); // Green for virtual machines
            // Continue with the ping logic for virtual machines
        }

        // Only make the line grey if it's going TO an unmanaged switch FROM a main node
        if (to.getDeviceType() == DeviceType.UNMANAGED_SWITCH && from.isMainNode()) {
            setLineColor(Color.GRAY);
            latencyLabel.setVisible(false); // Hide latency label for unmanaged switches
            return; // Exit early
        }

        // Handle devices connected through either type of switch
        if (from.getDeviceType() == DeviceType.UNMANAGED_SWITCH || 
            from.getDeviceType() == DeviceType.MANAGED_SWITCH) {
            new Thread(() -> {
                try {
                    String ip = to.getIpOrHostname();
                    java.net.InetAddress destAddr = java.net.InetAddress.getByName(ip);

                    long start = System.currentTimeMillis();
                    boolean reachable = destAddr.isReachable(2000);
                    long elapsed = System.currentTimeMillis() - start;

                    Platform.runLater(() -> {
                        connected = reachable;
                        if (reachable) {
                            defaultColor = Color.web("#0cad03");
                            curve.setStroke(defaultColor);
                            latencyLabel.setText(elapsed + " ms");
                            latencyLabel.setVisible(true);
                        } else {
                            defaultColor = Color.RED;
                            curve.setStroke(defaultColor);
                            latencyLabel.setText("");
                            latencyLabel.setVisible(false);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        defaultColor = Color.RED;
                        curve.setStroke(defaultColor);
                        latencyLabel.setText("");
                        latencyLabel.setVisible(false);
                    });
                }
            }).start();
            return;
        }

        // For all other cases, continue with normal ping logic
        if (!from.isMainNode() && !to.isMainNode()) {
            setLineColor(Color.GREY);
            latencyLabel.setVisible(false); // Hide latency label for non-main nodes
            return;
        }

        new Thread(() -> {
            try {
                String ip = to.getIpOrHostname();
                java.net.InetAddress destAddr = java.net.InetAddress.getByName(ip);

                // 0) Find the local interface whose subnet contains destAddr
                String interfaceName = "";
                for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        if (!(ia.getAddress() instanceof Inet4Address)) continue;
                        int prefix = ia.getNetworkPrefixLength();
                        byte[] localBytes = ia.getAddress().getAddress();
                        byte[] destBytes  = destAddr.getAddress();
                        int localInt = ((localBytes[0]&0xFF)<<24)|((localBytes[1]&0xFF)<<16)
                                    |((localBytes[2]&0xFF)<<8)| (localBytes[3]&0xFF);
                        int destInt  = ((destBytes[0]&0xFF)<<24)|((destBytes[1]&0xFF)<<16)
                                    |((destBytes[2]&0xFF)<<8)| (destBytes[3]&0xFF);
                        int mask = prefix == 0 ? 0 : 0xFFFFFFFF << (32 - prefix);
                        if ((localInt & mask) == (destInt & mask)) {
                            interfaceName = ni.getName();
                            break;
                        }
                    }
                    if (!interfaceName.isEmpty()) break;
                }
                final String iface = interfaceName;

                // 1) Ping it
                long start = System.currentTimeMillis();
                boolean reachable = destAddr.isReachable(2000);
                long elapsed = System.currentTimeMillis() - start;

                Platform.runLater(() -> {
                    connected = reachable;
                    if (reachable) {
                        defaultColor = Color.web("#0cad03");
                        if (!isHovered) {
                            curve.setStroke(defaultColor);
                        }
                        statsPanel.updateStats(elapsed + " ms", iface);
                        latencyLabel.setText(elapsed + " ms");
                        latencyLabel.setVisible(true);
                    } else {
                        defaultColor = Color.RED;
                        if (!isHovered) {
                            curve.setStroke(defaultColor);
                        }
                        statsPanel.updateStats("Not Connected", "");
                        latencyLabel.setText("");
                        latencyLabel.setVisible(false);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    defaultColor = Color.RED;
                    if (!isHovered) {
                        curve.setStroke(defaultColor);
                    }
                    statsPanel.updateStats("Error", "");
                    latencyLabel.setText("");
                    latencyLabel.setVisible(false);
                    connected = false;
                });
            }
        }).start();
    }

    public NetworkNode getFrom() {
        return from;
    }

    public NetworkNode getTo() {
        return to;
    }

    public boolean fromEquals(NetworkNode node) {
        return from == node;
    }

    public boolean toEquals(NetworkNode node) {
        return to == node;
    }

    // Add this method to allow setting the connected state and updating the UI accordingly
    public void setConnected(boolean connected) {
        this.connected = connected;
        Platform.runLater(() -> {
            if (connected) {
                defaultColor = Color.web("#0cad03");
                curve.setStroke(defaultColor);
                statsPanel.updateStats("Connected", "");
                latencyLabel.setText("Connected");
                latencyLabel.setVisible(true);
            } else {
                defaultColor = Color.RED;
                curve.setStroke(defaultColor);
                statsPanel.updateStats("", "");
                latencyLabel.setText("");
                latencyLabel.setVisible(false);
            }
        });
    }
}
