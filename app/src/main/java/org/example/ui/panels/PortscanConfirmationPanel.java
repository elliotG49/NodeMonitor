package org.example.ui.panels;

import java.util.function.Consumer;

import org.example.model.NetworkNode;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PortscanConfirmationPanel extends StackPane {

    private Consumer<Boolean> confirmationCallback;
    private javafx.event.EventHandler<MouseEvent> outsideClickFilter;

    /**
     * Constructs a confirmation panel.
     * @param node The NetworkNode to be scanned.
     * @param confirmationCallback A callback that receives true if the user clicks "Yes" and false if "No".
     */
    public PortscanConfirmationPanel(NetworkNode node, Consumer<Boolean> confirmationCallback) {
        this.confirmationCallback = confirmationCallback;
        String resolvedIp = (node.getResolvedIp() != null && !node.getResolvedIp().isEmpty())
                            ? node.getResolvedIp() : node.getIpOrHostname();
        String message = "Do you have permission to perform a portscan on " +
                         node.getDisplayName() + " (" + resolvedIp + ")?";

        // Build the UI layout.
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setStyle("-fx-background-color: #182030; " +
                         "-fx-border-color: #3B3B3B; " +
                         "-fx-border-width: 1px; " +
                         "-fx-border-radius: 10px; " +
                         "-fx-background-radius: 10px; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0.5, 0, 0);");

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);

        Button yesButton = new Button("Yes");
        yesButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        Button noButton = new Button("No");
        noButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 14px;");

        HBox buttonBox = new HBox(20, yesButton, noButton);
        buttonBox.setAlignment(Pos.CENTER);

        mainBox.getChildren().addAll(messageLabel, buttonBox);
        setMaxWidth(400);
        setMaxHeight(200);
        getChildren().add(mainBox);

        // Fade in the panel.
        setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.3), this);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // Handle button actions.
        yesButton.setOnAction(e -> {
            if (confirmationCallback != null) {
                confirmationCallback.accept(true);
            }
            hidePanel();
        });

        noButton.setOnAction(e -> {
            if (confirmationCallback != null) {
                confirmationCallback.accept(false);
            }
            hidePanel();
        });

        // Listen for clicks outside the panel.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                outsideClickFilter = event -> {
                    Scene currentScene = getScene();
                    if (currentScene != null && !this.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                        hidePanel();
                    }
                };
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
            }
            if (oldScene != null && outsideClickFilter != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
            }
        });

        Platform.runLater(() -> requestFocus());
    }

    /**
     * Fades out the panel and removes it from its parent.
     */
    public void hidePanel() {
        Scene currentScene = getScene();
        if (currentScene != null && outsideClickFilter != null) {
            currentScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
        }
        FadeTransition ft = new FadeTransition(Duration.seconds(0.3), this);
        ft.setFromValue(getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (getParent() != null) {
                ((StackPane)getParent()).getChildren().remove(this);
            }
        });
        ft.play();
    }
}
