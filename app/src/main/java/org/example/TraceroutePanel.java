package org.example;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class TraceroutePanel extends StackPane {
    private VBox contentBox;
    private Button closeButton;
    
    public TraceroutePanel() {
        // Mimic the Portscan panel design.
        setStyle("-fx-background-color: #182030; -fx-border-color: #3B3B3B; " +
                 "-fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
        setPadding(new Insets(10));
        
        contentBox = new VBox(10);
        contentBox.setAlignment(Pos.TOP_LEFT);
        
        // Title label.
        Text title = new Text("Traceroute Results");
        title.setStyle("-fx-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Separator.
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #b8d4f1;");
        
        // VBox for hop list.
        VBox hopsBox = new VBox(5);
        hopsBox.setAlignment(Pos.TOP_LEFT);
        
        // Close button.
        closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        closeButton.setOnAction(e -> startFadeOut());
        
        contentBox.getChildren().addAll(title, sep, hopsBox, closeButton);
        getChildren().add(contentBox);
        
        // Size to content.
        setMaxWidth(VBox.USE_PREF_SIZE);
        setMaxHeight(VBox.USE_PREF_SIZE);
    }
    
    // Append a hop description.
    public void addHop(String hopDescription) {
        VBox hopsBox = (VBox) contentBox.getChildren().get(2);
        Text hopText = new Text(hopDescription);
        hopText.setStyle("-fx-fill: white; -fx-font-size: 14px;");
        hopsBox.getChildren().add(hopText);
    }
    
    // Clear hop descriptions.
    public void clear() {
        VBox hopsBox = (VBox) contentBox.getChildren().get(2);
        hopsBox.getChildren().clear();
    }
    
    // Fade out the panel.
    public void startFadeOut() {
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), this);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            if (getParent() instanceof StackPane) {
                ((StackPane)getParent()).getChildren().remove(this);
            }
        });
        ft.play();
    }
}
