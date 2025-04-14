package org.example;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode; // Ensure we have this import
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;


public class PortscanConfigPanel extends StackPane {

    // Fixed dimensions.
    private static final double PANEL_WIDTH = 250;
    private static final double PANEL_HEIGHT = 350;

    // Overall panel style.
    private final String panelStyle = "-fx-background-color: #182030; " +
                                      "-fx-border-color: #3B3B3B; " +
                                      "-fx-border-width: 1px; " +
                                      "-fx-border-radius: 10px; " +
                                      "-fx-background-radius: 10px; " +
                                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0.5, 0, 0);";

    // Style for input controls.
    private final String inputStyle = "-fx-font-size: 14px; " +
                                      "-fx-background-color: #1b2433; " +
                                      "-fx-border-color: #3B3B3B; " +
                                      "-fx-border-width: 1px; " +
                                      "-fx-background-radius: 5; " +
                                      "-fx-border-radius: 5; " +
                                      "-fx-text-fill: white;";

    // UI controls for the configuration options.
    private TextField portsField;
    private ComboBox<String> osDetectionBox;
    private ComboBox<String> bannerBox;
    private Button scanButton;
    private Button cancelButton;

    // The target node is provided via the constructor.
    private final NetworkNode targetNode;

    // Click-outside filter.
    private javafx.event.EventHandler<MouseEvent> outsideClickFilter;

    /**
     * Constructs a PortscanConfigPanel for the given target node.
     * @param targetNode The node to be scanned.
     */
    public PortscanConfigPanel(NetworkNode targetNode) {
        this.targetNode = targetNode;
        
        // Set fixed dimensions.
        setMinWidth(PANEL_WIDTH);
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setMinHeight(PANEL_HEIGHT);
        setPrefHeight(PANEL_HEIGHT);
        setMaxHeight(PANEL_HEIGHT);
        setStyle(panelStyle);
        setPadding(new Insets(15));

        // Build the UI.
        VBox contentBox = new VBox(15);
        contentBox.setFillWidth(true);

        // --- Header section ---
        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-border-color: transparent transparent #b8d4f1 transparent; " +
                           "-fx-border-width: 0 0 0.5px 0; -fx-padding: 0 0 10px 0;");
        Label titleLabel = new Label("Portscan");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        headerBox.getChildren().add(titleLabel);

        // --- Ports input ---
        VBox portsSection = new VBox(5);
        Label portsLabel = new Label("Ports to scan:");
        portsLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        portsField = new TextField();
        portsField.setPrefWidth(PANEL_WIDTH - 20);
        portsField.setStyle(inputStyle);
        portsField.setPromptText("e.g. 22,80,443 or 1-1024");
        portsSection.getChildren().addAll(portsLabel, portsField);

        // --- OS Detection option ---
        VBox osSection = new VBox(5);
        Label osLabel = new Label("OS Detection:");
        osLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        osDetectionBox = new ComboBox<>();
        osDetectionBox.setPrefWidth(PANEL_WIDTH - 20);
        osDetectionBox.setStyle(inputStyle);
        osDetectionBox.setItems(FXCollections.observableArrayList("Yes", "No"));
        osDetectionBox.setValue("No");
        osSection.getChildren().addAll(osLabel, osDetectionBox);

        // --- Banner/Services option ---
        VBox bannerSection = new VBox(5);
        Label bannerLabel = new Label("Banner / Services:");
        bannerLabel.setStyle("-fx-text-fill: #b8d4f1; -fx-font-size: 14px;");
        bannerBox = new ComboBox<>();
        bannerBox.setPrefWidth(PANEL_WIDTH - 20);
        bannerBox.setStyle(inputStyle);
        bannerBox.setItems(FXCollections.observableArrayList("Yes", "No"));
        bannerBox.setValue("No");
        bannerSection.getChildren().addAll(bannerLabel, bannerBox);

        // --- Button section ---
        HBox buttonSection = new HBox(10);
        buttonSection.setAlignment(Pos.CENTER);
        scanButton = new Button("Scan");
        scanButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 14px;");
        buttonSection.getChildren().addAll(scanButton, cancelButton);

        // Assemble UI.
        contentBox.getChildren().addAll(headerBox, portsSection, osSection, bannerSection, buttonSection);
        getChildren().add(contentBox);

        // --- Behavior to close the panel ---
        cancelButton.setOnAction(e -> hidePanel());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hidePanel();
            }
        });
        // Use null-check in the listener to avoid calling getScene() on a removed node.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                outsideClickFilter = event -> {
                    Scene s = getScene();
                    if (s != null && !this.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
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

        // --- Scan button triggers the confirmation panel and portscan task ---
        scanButton.setOnAction(e -> {
            // Capture the ports input.
            String portsInput = portsField.getText();
            if (portsInput == null || portsInput.isEmpty()) {
                // If no ports specified, animate the portsField to show an error.
                shakeNode(portsField);
                return;
            }
            List<Integer> portList = PortscanTask.parsePorts(portsInput);
            // Capture the current scene.
            final Scene currentScene = getScene();
            hidePanel();
            Platform.runLater(() -> {
                PortscanConfirmationPanel confirmationPanel = new PortscanConfirmationPanel(targetNode, confirmed -> {
                    if (confirmed) {
                        System.out.println("User confirmed portscan for: " + targetNode.getDisplayName());
                        String targetIp = (targetNode.getResolvedIp() != null && !targetNode.getResolvedIp().isEmpty())
                                ? targetNode.getResolvedIp() : targetNode.getIpOrHostname();
                        boolean bannerDetection = "Yes".equalsIgnoreCase(bannerBox.getValue());
                        PortscanTask task = new PortscanTask(targetIp, portList, bannerDetection);
                        PortscanResultsPanel resultsPanel = new PortscanResultsPanel(targetNode.getDisplayName(), task);
                        StackPane rootStack = (StackPane) ((BorderPane) currentScene.getRoot()).getCenter();
                        rootStack.getChildren().removeIf(n -> n instanceof PortscanResultsPanel);
                        rootStack.getChildren().add(resultsPanel);
                        StackPane.setAlignment(resultsPanel, Pos.TOP_LEFT);
                        StackPane.setMargin(resultsPanel, new Insets(10));
                        resultsPanel.toFront();
                        
                        task.messageProperty().addListener((obsMsg, oldMsg, newMsg) -> {
                            resultsPanel.updateMessage(newMsg);
                        });
                        task.setOnSucceeded(ev -> {
                            resultsPanel.updateResultsWithSelectiveColoring(task.getValue());
                        });
                        new Thread(task).start();
                    } else {
                        System.out.println("User cancelled portscan for: " + targetNode.getDisplayName());
                    }
                });
                StackPane rootStack = (StackPane) ((BorderPane) currentScene.getRoot()).getCenter();
                rootStack.getChildren().removeIf(n -> n instanceof PortscanConfirmationPanel);
                rootStack.getChildren().add(confirmationPanel);
                StackPane.setAlignment(confirmationPanel, Pos.CENTER);
                confirmationPanel.toFront();
            });
        });
    }

    /**
     * Fades the panel in.
     */
    public void showPanel() {
        setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.3), this);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void shakeNode(TextField node) {
        // Save the original style.
        String originalStyle = node.getStyle();
        // Temporarily add a red border.
        node.setStyle(originalStyle + "; -fx-border-color: red; -fx-border-width: 2px;");
        
        // Create a shake animation.
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6); // Shakes left and right.
        tt.setAutoReverse(true);
        tt.setOnFinished(event -> {
            node.setTranslateX(0);
            // Restore original style.
            Platform.runLater(() -> node.setStyle(originalStyle));
        });
        tt.play();
    }
    

    /**
     * Fades the panel out and removes it from the parent.
     */
    public void hidePanel() {
        Scene s = getScene();
        if (s != null && outsideClickFilter != null) {
            s.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickFilter);
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
