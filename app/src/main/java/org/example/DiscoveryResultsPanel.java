package org.example;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DiscoveryResultsPanel extends StackPane {
    private final VBox contentBox = new VBox(10);

    public DiscoveryResultsPanel() {
        setStyle("-fx-background-color: #182030; -fx-border-color: #3B3B3B; "
               + "-fx-border-width: 1px; -fx-border-radius: 10px; "
               + "-fx-background-radius: 10px; -fx-padding: 10px;");
        contentBox.setAlignment(Pos.TOP_LEFT);
        getChildren().add(contentBox);

        // Close button
        Button close = new Button("×");
        close.setOnAction(e -> ((Pane)getParent()).getChildren().remove(this));
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
