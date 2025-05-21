package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A slide‐out panel that animates in from the left and can show arbitrary content.
 */
public class SlideOutPanel extends StackPane {
    private static final double LEFT_MARGIN = 15;    // your desired gap from the very left edge
    private final double panelWidth;
    private final BorderPane container;

    public SlideOutPanel(double width) {
        this.panelWidth = width;
        getStyleClass().add("slide-panel");
        setPrefWidth(panelWidth);
        setMinWidth(panelWidth);
        setMaxWidth(panelWidth);

        container = new BorderPane();
        container.setPadding(new Insets(4, 8, 8, 8)); // Top, Right, Bottom, Left

        // Create close button with icon


        // start fully off‐screen (to the left)
        setTranslateX(-panelWidth - LEFT_MARGIN);
        getChildren().add(container);

        // hide if clicking outside
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onSceneMousePressed);
            }
            if (oldScene != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::onSceneMousePressed);
            }
        });
    }

    /** Swap in new content (center of the panel) */
    public void setContent(Node content) {
        container.setCenter(content);
        BorderPane.setAlignment(content, Pos.TOP_CENTER);
    }

    /** Slide the panel in (hiddenX → 0) */
    public void show() {
        double hiddenX = -panelWidth - LEFT_MARGIN;
        Timeline showAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(translateXProperty(), hiddenX)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(translateXProperty(), 15)
            )
        );
        showAnim.play();
    }

    /** Slide the panel out (0 → hiddenX) */
    public void hide() {
        double hiddenX = -panelWidth - LEFT_MARGIN;
        Timeline hideAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(translateXProperty(), 0)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(translateXProperty(), hiddenX)
            )
        );
        hideAnim.play();
    }

    private void onSceneMousePressed(MouseEvent event) {
        if (getTranslateX() >= 0) {
            double x = event.getSceneX(), y = event.getSceneY();
            double panelX = localToScene(0,0).getX();
            double panelY = localToScene(0,0).getY();
            if (x < panelX || x > panelX + panelWidth ||
                y < panelY || y > panelY + getHeight()) {
                hide();
            }
        }
    }
}
