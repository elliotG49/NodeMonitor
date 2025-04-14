package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;


public class PortscanResultsPanel extends StackPane {

    private final VBox contentBox;
    private final Label titleLabel;
    private final Separator separator;
    private final TextFlow resultsFlow;
    private final Button closeButton;
    private final Label progressLabel;
    private final PortscanTask runningTask;
    private final Button cancelScanButton;

    public PortscanResultsPanel(String nodeDisplayName, PortscanTask runningTask) {
        this.runningTask = runningTask;
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #182030; -fx-border-color: #3B3B3B; -fx-border-width: 1px; " +
                 "-fx-border-radius: 10px; -fx-background-radius: 10px; " +
                 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0.5, 0, 0);");

        contentBox = new VBox(10);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(10));

        titleLabel = new Label(nodeDisplayName + " Portscan Results");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        separator = new Separator();
        separator.setStyle("-fx-background-color: #b8d4f1;");

        progressLabel = new Label("Port Scan on " + nodeDisplayName + " activated. ETA 0%.");
        progressLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        progressLabel.setWrapText(true);

        cancelScanButton = new Button("Cancel Scan");
        cancelScanButton.setStyle("-fx-background-color: #8b0000; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelScanButton.setOnAction(e -> {
            if (runningTask != null) {
                runningTask.cancel();
                updateMessage("Scan cancelled by user.");
                contentBox.getChildren().remove(cancelScanButton);
                hidePanel();
            }
        });

        resultsFlow = new TextFlow();
        resultsFlow.setLineSpacing(4);

        closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #317756; -fx-text-fill: white; -fx-font-size: 14px;");
        closeButton.setOnAction(e -> hidePanel());

        contentBox.getChildren().addAll(titleLabel, separator, progressLabel, cancelScanButton);
        getChildren().add(contentBox);
        StackPane.setAlignment(contentBox, Pos.TOP_LEFT);

        // ✅ Prevent the box from stretching full width/height
        contentBox.setMaxWidth(Region.USE_PREF_SIZE);
        contentBox.setMaxHeight(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
    }

    public void updateMessage(String message) {
        Platform.runLater(() -> progressLabel.setText(message));
    }

    public void updateResultsWithSelectiveColoring(String results) {
        Platform.runLater(() -> {
            resultsFlow.getChildren().clear();
            String[] lines = results.split("\n");

            if (lines.length == 0 || (lines.length == 1 && lines[0].trim().isEmpty())) {
                Text noResults = new Text("No open ports found\n");
                noResults.setStyle("-fx-fill: red; -fx-font-size: 14px;");
                resultsFlow.getChildren().add(noResults);
            } else {
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    String lower = line.toLowerCase();
                    if (lower.contains("open")) {
                        int index = lower.indexOf("open");
                        String before = line.substring(0, index);
                        String word = line.substring(index, index + 4);
                        String after = line.substring(index + 4);

                        Text tBefore = new Text(before);
                        tBefore.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                        Text tWord = new Text(word);
                        tWord.setStyle("-fx-fill: green; -fx-font-size: 14px; -fx-font-weight: bold;");
                        Text tAfter = new Text(after + "\n");
                        tAfter.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                        resultsFlow.getChildren().addAll(tBefore, tWord, tAfter);

                    } else if (lower.contains("closed")) {
                        int index = lower.indexOf("closed");
                        String before = line.substring(0, index);
                        String word = line.substring(index, index + 6);
                        String after = line.substring(index + 6);

                        Text tBefore = new Text(before);
                        tBefore.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                        Text tWord = new Text(word);
                        tWord.setStyle("-fx-fill: red; -fx-font-size: 14px; -fx-font-weight: bold;");
                        Text tAfter = new Text(after + "\n");
                        tAfter.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                        resultsFlow.getChildren().addAll(tBefore, tWord, tAfter);

                    } else {
                        Text t = new Text(line + "\n");
                        t.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                        resultsFlow.getChildren().add(t);
                    }
                }
            }

            contentBox.getChildren().remove(progressLabel);
            if (!contentBox.getChildren().contains(resultsFlow)) {
                contentBox.getChildren().add(resultsFlow);
            }
            if (!contentBox.getChildren().contains(closeButton)) {
                contentBox.getChildren().add(closeButton);
            }
            contentBox.getChildren().remove(cancelScanButton);

            contentBox.applyCss();
            contentBox.layout();

            double newHeight = contentBox.prefHeight(-1) + 20;
            Timeline timeline = new Timeline();
            KeyValue kvHeight = new KeyValue(prefHeightProperty(), newHeight);
            KeyFrame kf = new KeyFrame(Duration.seconds(0.5), kvHeight);
            timeline.getKeyFrames().add(kf);
            timeline.play();
        });
    }

    public void hidePanel() {
        Timeline ft = new Timeline(new KeyFrame(Duration.seconds(0.3), new KeyValue(opacityProperty(), 0)));
        ft.setOnFinished(e -> {
            if (getParent() != null) {
                ((StackPane) getParent()).getChildren().remove(this);
            }
        });
        ft.play();
    }
}
