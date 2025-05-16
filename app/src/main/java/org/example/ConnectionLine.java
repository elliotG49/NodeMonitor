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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.util.Duration;


public class ConnectionLine extends Pane {

    private final CubicCurve curve;
    private final StackPane latencyContainer; // Container for latency label.
    private final Label latencyLabel;
    private final Label interfaceLabel;
    private final NetworkNode from;
    private final NetworkNode to;

    private final Circle pingParticle;
    private final AnimationTimer pingTimer;
    private static final double CYCLE_DURATION = 3.0;

    private long lastTrailTime = 0;
    private static final long TRAIL_INTERVAL_NANOS = 10_000_000;

    private final long animationStartTime;
    private volatile boolean connected = false;

    public ConnectionLine(NetworkNode from, NetworkNode to) {
        this.from = from;
        this.to = to;

        curve = new CubicCurve();
        curve.setStrokeWidth(4);
        curve.setStroke(Color.RED);
        curve.setFill(Color.TRANSPARENT);

        // Create the latency label and wrap it in its container.
        latencyLabel = new Label("...");
        latencyLabel.getStyleClass().add("latency-label");
        interfaceLabel = new Label(); 
        interfaceLabel.getStyleClass().add("latency-label");  // reuse the same styling
        latencyContainer = new StackPane();
        latencyContainer.getStyleClass().add("latency-container");
        latencyContainer.getChildren().add(latencyLabel);
        // Initially hide the latency container if there's no connection.
        latencyContainer.setVisible(false);

        pingParticle = new Circle(2, Color.WHITE);

        // Add children in proper order.
        getChildren().addAll(curve, latencyContainer, pingParticle);

        animationStartTime = System.nanoTime();

        pingTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateCurve();

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

                // Reposition the latency container so that its center is at the midpoint of the curve.
                double midT = 0.5;
                double omt = 1 - midT;
                double lx = Math.pow(omt, 3) * sx +
                            3 * Math.pow(omt, 2) * midT * cx1 +
                            3 * omt * Math.pow(midT, 2) * cx2 +
                            Math.pow(midT, 3) * ex;
                double ly = Math.pow(omt, 3) * sy +
                            3 * Math.pow(omt, 2) * midT * cy1 +
                            3 * omt * Math.pow(midT, 2) * cy2 +
                            Math.pow(midT, 3) * ey;
                
                latencyContainer.autosize();
                latencyContainer.setLayoutX(lx - latencyContainer.getWidth() / 2);
                latencyContainer.setLayoutY(ly - latencyContainer.getHeight() / 2);
                
                // Bring the latency container to the front so it appears above the ping particles.
                latencyContainer.toFront();
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

    private void spawnTrail(double x, double y) {
        Circle trail = new Circle(2, Color.WHITE);
        trail.setLayoutX(x);
        trail.setLayoutY(y);
        getChildren().add(trail);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.3), trail);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> getChildren().remove(trail));
        ft.play();
    }

    public void setLineColor(Color color) {
        curve.setStroke(color);
    }
    

    public void updateStatus() {
    // If the target node is a switch, just set grey and hide the latency label.
    if (to.getDeviceType() == DeviceType.SWITCH) {
        Platform.runLater(() -> {
            curve.setStroke(Color.GREY);
            latencyLabel.setText("");
            latencyContainer.setVisible(false);
            connected = true;
        });
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
                    int mask = prefix==0 ? 0 : 0xFFFFFFFF << (32 - prefix);
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
                    curve.setStroke(Color.web("#2E8B57"));
                    latencyLabel.setText(elapsed + " ms");
                    interfaceLabel.setText(iface);

                    if (to.getNetworkType() == NetworkType.INTERNAL) {
                        ImageView icon = new ImageView();
                        if (to.getConnectionType() == ConnectionType.ETHERNET
                            || to.getConnectionType() == ConnectionType.VIRTUAL) {
                            icon.setImage(new Image(getClass().getResourceAsStream("/icons/power-cable.png")));
                        } else {
                            icon.setImage(new Image(getClass().getResourceAsStream("/icons/wireless.png")));
                        }
                        icon.setFitWidth(15);
                        icon.setFitHeight(15);

                        HBox hbox = new HBox(5, icon, latencyLabel, interfaceLabel);
                        hbox.setAlignment(Pos.CENTER);
                        latencyContainer.getChildren().setAll(hbox);

                    } else {
                        HBox hbox = new HBox(5, latencyLabel, interfaceLabel);
                        hbox.setAlignment(Pos.CENTER);
                        latencyContainer.getChildren().setAll(hbox);
                    }
                    latencyContainer.setVisible(true);

                } else {
                    curve.setStroke(Color.RED);
                    latencyLabel.setText("");
                    latencyContainer.setVisible(false);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> {
                curve.setStroke(Color.RED);
                latencyLabel.setText("Error");
                connected = false;
                latencyContainer.setVisible(false);
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
}
