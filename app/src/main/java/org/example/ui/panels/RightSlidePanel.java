package org.example.ui.panels;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class RightSlidePanel extends StackPane {
    private static final double RIGHT_MARGIN = 15;  // Gap from the right edge of the screen
    private final BorderPane contentPane;
    private final Timeline slideInTimeline;
    private final Timeline slideOutTimeline;
    private final double panelWidth;
    private boolean isShowing = false;
    
    public RightSlidePanel(double width) {
        // Set width to 200px as requested
        this.panelWidth = 200;
        
        // Set dimensions
        setPrefWidth(this.panelWidth); 
        setMaxWidth(this.panelWidth); 
        setMaxHeight(Double.MAX_VALUE);
        
        // Style the panel with dark background and rounded left corners
        Rectangle background = new Rectangle();
        background.setArcWidth(20);
        background.setArcHeight(20);
        background.setFill(Color.web("#1a273b"));
        
        // Make background resize with the panel
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty().subtract(30)); // Subtract 30 for top/bottom gaps
        
        // Create content pane
        contentPane = new BorderPane();
        contentPane.setPadding(new Insets(15));
        
        // Add components to the main pane
        getChildren().addAll(background, contentPane);
        
        // Set initial state - hidden off screen to the right
        // Position is now "panel width + margin" - this ensures it starts hidden
        setTranslateX(panelWidth + RIGHT_MARGIN);
        
        // Create animations
        slideInTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(translateXProperty(), panelWidth + RIGHT_MARGIN, Interpolator.EASE_OUT)
            ),
            new KeyFrame(Duration.millis(200), 
                new KeyValue(translateXProperty(), 0, Interpolator.EASE_OUT) // Changed to 0 to add margin
            )
        );
        
        slideOutTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(translateXProperty(), 0, Interpolator.EASE_IN) // Changed to 0 to match in animation
            ),
            new KeyFrame(Duration.millis(200), 
                new KeyValue(translateXProperty(), panelWidth + RIGHT_MARGIN, Interpolator.EASE_IN)
            )
        );
        
        // Add to layout but start hidden
        setVisible(false);
        
        // Position panel from the top of the parent with 15px margin
        setLayoutY(15); // 15px from top
        
        // Add right margin of 15px by setting negative translateX
        // This shifts the panel left to 15px from the right edge
        translateXProperty().addListener((obs, old, newVal) -> {
            if (newVal.doubleValue() == 0) { // When fully visible
                setTranslateX(-RIGHT_MARGIN); // Add 15px right margin
            }
        });
        
        // Add ESC key handler to close the panel
        setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && isShowing) {
                hide();
                e.consume();
            }
        });
    }
    
    public void show() {
        if (!isShowing) {
            setVisible(true);
            slideInTimeline.play();
            isShowing = true;
            
            // Request focus to enable keyboard events (ESC key)
            Platform.runLater(this::requestFocus);
        }
    }
    
    public void hide() {
        if (isShowing) {
            slideOutTimeline.setOnFinished(e -> {
                setVisible(false);
                // Clear the content when hidden to ensure fresh start next time
                contentPane.setCenter(null);
            });
            slideOutTimeline.play();
            isShowing = false;
        }
    }
    
    public void setContent(Node content) {
        contentPane.setCenter(content);
    }
    
    public boolean isShowing() {
        return isShowing;
    }
}