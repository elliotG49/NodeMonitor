package org.example;

import javafx.animation.FadeTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class TracerouteLine extends Pane {
    public static final double UNKNOWN_OFFSET = 100;
    private final CubicCurve curve;
    private Circle endpointMarker; // Used only for unknown hops.
    private Text ipLabel;          // For unknown hops, shows the hop IP.

    // Constructor for known hops: starting from a node (from) to a node (to).
    public TracerouteLine(NetworkNode from, NetworkNode to) {
        curve = new CubicCurve();
        curve.setStroke(Color.web("#C9EB78"));
        curve.setStrokeWidth(3);
        curve.setFill(Color.TRANSPARENT);
        getChildren().add(curve);
        updateCurve(from, to);
    }
    
    // Overloaded constructor for known hops when the starting point is provided by coordinates.
    public TracerouteLine(double startX, double startY, NetworkNode to) {
        curve = new CubicCurve();
        curve.setStroke(Color.web("#C9EB78"));
        curve.setStrokeWidth(3);
        curve.setFill(Color.TRANSPARENT);
        getChildren().add(curve);
        double ex = to.getLayoutX() + to.getWidth() / 2;
        double ey = to.getLayoutY() + to.getHeight() / 2;
        updateCurve(startX, startY, ex, ey);
    }
    
    // Constructor for unknown hops: starting from coordinates with hop IP and an index.
    public TracerouteLine(double startX, double startY, String hopIp, int hopIndex) {
        curve = new CubicCurve();
        curve.setStroke(Color.web("#C9EB78"));
        curve.setStrokeWidth(3);
        curve.setFill(Color.TRANSPARENT);
        getChildren().add(curve);
        // Always add a fixed offset to the right.
        double targetX = startX + UNKNOWN_OFFSET;
        double targetY = startY;
        updateCurve(startX, startY, targetX, targetY);
        // Create an endpoint marker.
        endpointMarker = new Circle(5, Color.web("#C9EB78"));
        endpointMarker.setLayoutX(targetX);
        endpointMarker.setLayoutY(targetY);
        getChildren().add(endpointMarker);
        // Create an IP label below the marker.
        ipLabel = new Text(hopIp);
        ipLabel.setFill(Color.WHITE);
        ipLabel.setStyle("-fx-font-size: 12px;");
        // Position label: adjust these offsets as needed.
        ipLabel.setLayoutX(targetX - 15);
        ipLabel.setLayoutY(targetY + 20);
        getChildren().add(ipLabel);
    }
    
    // For known hops using node objects.
    private void updateCurve(NetworkNode from, NetworkNode to) {
        double sx = from.getLayoutX() + from.getWidth() / 2;
        double sy = from.getLayoutY() + from.getHeight() / 2;
        double ex = to.getLayoutX() + to.getWidth() / 2;
        double ey = to.getLayoutY() + to.getHeight() / 2;
        updateCurve(sx, sy, ex, ey);
    }
    
    // General method to update curve with start and end coordinates.
    // This version computes a perpendicular offset so the line is curved.
    private void updateCurve(double sx, double sy, double ex, double ey) {
        curve.setStartX(sx);
        curve.setStartY(sy);
        curve.setEndX(ex);
        curve.setEndY(ey);
        // Compute a perpendicular offset proportional to the distance.
        double dx = ex - sx;
        double dy = ey - sy;
        double distance = Math.hypot(dx, dy);
        double offset = distance * 0.2; // adjust as desired
        // Angle of the line.
        double angle = Math.atan2(dy, dx);
        // Perpendicular angle.
        double perpAngle = angle - Math.PI / 2;
        // Set control points offset perpendicular from the midpoint.
        double midX = (sx + ex) / 2;
        double midY = (sy + ey) / 2;
        double ctrlX = midX + offset * Math.cos(perpAngle);
        double ctrlY = midY + offset * Math.sin(perpAngle);
        // For a cubic curve, we can use the same control point for both ends.
        curve.setControlX1(ctrlX);
        curve.setControlY1(ctrlY);
        curve.setControlX2(ctrlX);
        curve.setControlY2(ctrlY);
    }
    
    // Triggers a fade-out transition for this traceroute line.
    public void startFadeOut() {
        FadeTransition ft = new FadeTransition(Duration.seconds(5), this);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            if (getParent() instanceof Pane) {
                ((Pane) getParent()).getChildren().remove(this);
            }
        });
        ft.play();
    }
    
    // (Optional) Method to set the line color.
    public void setLineColor(Color color) {
        curve.setStroke(color);
        if (endpointMarker != null) {
            endpointMarker.setFill(color);
        }
        // For unknown hops, IP label remains white.
    }
}
