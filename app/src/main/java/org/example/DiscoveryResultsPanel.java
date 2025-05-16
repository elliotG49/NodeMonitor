package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.List;

public class DiscoveryResultsPanel extends StackPane {
    private final VBox contentBox = new VBox(10);

    public DiscoveryResultsPanel() {
        // ── Panel styling ──
        setStyle(
            "-fx-background-color: #182030; " +
            "-fx-border-color: #3B3B3B; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px;"
        );
        setPadding(new Insets(10));

        // ── Make the panel shrink to its content, not fill the screen ──
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        // ── Configure contentBox ──
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(10));
        contentBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().add(contentBox);
        StackPane.setAlignment(contentBox, Pos.TOP_LEFT);

        // ── Close button ──
        Button close = new Button("×");
        close.setOnAction(e -> ((Pane) getParent()).getChildren().remove(this));
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        getChildren().add(close);
    }

    /** Populate the list once discovery completes */
    public void setDevices(List<DiscoveredNode> devices) {
        contentBox.getChildren().clear();
        for (DiscoveredNode dn : devices) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(
                new Label(dn.ip),
                new Label(dn.mac),
                new Label(dn.hostname),
                new Label(dn.iface)
            );
            Button add = new Button("+");
            add.setOnAction(e -> {
                NetworkNode node = new NetworkNode(
                    dn.ip,
                    dn.hostname.isEmpty()? dn.ip : dn.hostname,
                    DeviceType.COMPUTER,
                    dn.ip.startsWith("192.168.") ? NetworkType.INTERNAL : NetworkType.EXTERNAL
                );
                node.setConnectionType(ConnectionType.ETHERNET);
                node.setRouteSwitch("Gateway");
                NetworkMonitorApp.addNewNode(node);
                add.setDisable(true);
            });
            row.getChildren().add(add);
            contentBox.getChildren().add(row);
        }
    }
}
