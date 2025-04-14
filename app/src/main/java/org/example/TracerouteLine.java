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
    private Circle endpointMarker; // Used for unknown hops.
    private Text ipLabel;          // Displays the hop IP for unknown hops.

    // Constructor for known hops (connecting two nodes).
    public TracerouteLine(NetworkNode from, NetworkNode to) {
        curve = new CubicCurve();
        curve.setStroke(Color.YELLOW);
        curve.setStrokeWidth(3);
        curve.setFill(Color.TRANSPARENT);
        getChildren().add(curve);
        updateCurve(from, to);
    }

    // Constructor for unknown hops (using a fixed rightward offset).
    public TracerouteLine(double startX, double startY, String hopIp, int hopIndex) {
        curve = new CubicCurve();
        curve.setStroke(Color.YELLOW);
        curve.setStrokeWidth(3);
        curve.setFill(Color.TRANSPARENT);
        getChildren().add(curve);
        // Always add a fixed offset to the right.
        double targetX = startX + UNKNOWN_OFFSET;
        double targetY = startY;
        updateCurve(startX, startY, targetX, targetY);
        // Create endpoint marker (a small circle).
        endpointMarker = new Circle(5, Color.YELLOW);
        endpointMarker.setLayoutX(targetX);
        endpointMarker.setLayoutY(targetY);
        getChildren().add(endpointMarker);
        // Create an IP label for the unknown hop.
        ipLabel = new Text(hopIp);
        ipLabel.setFill(Color.YELLOW);
        ipLabel.setStyle("-fx-font-size: 12px;");
        // Position the label slightly below the marker.
        ipLabel.setLayoutX(targetX - 15); // Adjust offset as needed.
        ipLabel.setLayoutY(targetY + 20);
        getChildren().add(ipLabel);
    }

    // Update the curve given two nodes.
    private void updateCurve(NetworkNode from, NetworkNode to) {
        double sx = from.getLayoutX() + from.getWidth() / 2;
        double sy = from.getLayoutY() + from.getHeight() / 2;
        double ex = to.getLayoutX() + to.getWidth() / 2;
        double ey = to.getLayoutY() + to.getHeight() / 2;
        updateCurve(sx, sy, ex, ey);
    }

    // Update the curve using explicit coordinates.
    private void updateCurve(double sx, double sy, double ex, double ey) {
        curve.setStartX(sx);
        curve.setStartY(sy);
        curve.setEndX(ex);
        curve.setEndY(ey);
        // Set control points to create a smooth curve.
        double ctrlOffset = 50;
        curve.setControlX1(sx);
        curve.setControlY1(sy - ctrlOffset);
        curve.setControlX2(ex);
        curve.setControlY2(ey - ctrlOffset);
    }

    // Set the line's color (and update the endpoint marker and IP label if present).
    public void setLineColor(Color color) {
        curve.setStroke(color);
        if (endpointMarker != null) {
            endpointMarker.setFill(color);
        }
        if (ipLabel != null) {
            ipLabel.setFill(color);
        }
    }

    // This method triggers a fade-out transition.
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
}
