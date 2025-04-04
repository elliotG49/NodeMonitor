package org.example;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class DrawableZone extends Pane {
    private Rectangle zoneRect;
    private HBox header;  // Header as an HBox.
    private javafx.scene.control.Label zoneNameLabel;
    private ImageView editIcon;
    
    // Move container will hold the move icon and serve as the hit area.
    private StackPane moveContainer;
    private ImageView moveIcon;
    
    // Resize container will hold the resize icon.
    private StackPane resizeContainer;
    private ImageView resizeIcon;
    
    private double dragDeltaX, dragDeltaY;
    private double resizeStartWidth, resizeStartHeight, resizeStartX, resizeStartY;
    
    private final double SNAP = 10;
    private String zoneName = "Zone";
    
    public DrawableZone(double x, double y, double width, double height) {
        // Snap the coordinates and size.
        x = Math.round(x / SNAP) * SNAP;
        y = Math.round(y / SNAP) * SNAP;
        width = Math.round(width / SNAP) * SNAP;
        height = Math.round(height / SNAP) * SNAP;
        if (width < 50) width = 50;
        if (height < 50) height = 50;
        
        setLayoutX(x);
        setLayoutY(y);
        setPrefWidth(width);
        setPrefHeight(height);
        
        // Allow clicks to pass through non-interactive areas.
        setPickOnBounds(false);
        
        // Create the dashed border with rounded corners.
        zoneRect = new Rectangle(width, height);
        zoneRect.setFill(Color.TRANSPARENT);
        zoneRect.setStroke(Color.rgb(255, 255, 255, 0.5));
        zoneRect.getStrokeDashArray().addAll(5.0, 5.0);
        zoneRect.setStrokeWidth(2);
        zoneRect.setArcWidth(15);
        zoneRect.setArcHeight(15);
        zoneRect.setMouseTransparent(true);
        getChildren().add(zoneRect);
        
        // Create header as an HBox.
        header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 5, 0, 5));
        header.setPrefHeight(20);
        header.setPrefWidth(width);
        header.setStyle("-fx-background-color: #1A2B57;");
        header.setLayoutY(-20);
        header.setPickOnBounds(false);
        
        // Create the edit icon.
        editIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/edit.png")));
        editIcon.setFitWidth(12);
        editIcon.setFitHeight(12);
        editIcon.setCursor(Cursor.HAND);
        editIcon.setOnMouseClicked(e -> {
            ContextMenu cm = new ContextMenu();
            MenuItem editNameItem = new MenuItem("Edit Name");
            MenuItem deleteZoneItem = new MenuItem("Delete Zone");
            cm.getItems().addAll(editNameItem, deleteZoneItem);
            editNameItem.setOnAction(ev -> {
                TextInputDialog dialog = new TextInputDialog(zoneName);
                dialog.setTitle("Edit Zone Name");
                dialog.setHeaderText("Enter new zone name:");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(name -> setZoneName(name));
            });
            deleteZoneItem.setOnAction(ev -> {
                if(getParent() != null) {
                    Pane parent = (Pane) getParent();
                    parent.getChildren().remove(DrawableZone.this);
                    NetworkMonitorApp.removeZone(DrawableZone.this);
                }
            });
            cm.show(editIcon, Side.RIGHT, 0, 0);
        });
        
        // Create the zone name label.
        zoneNameLabel = new javafx.scene.control.Label(zoneName);
        zoneNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        // Assemble header: edit icon then zone name.
        header.getChildren().addAll(editIcon, zoneNameLabel);
        getChildren().add(header);
        
        // Create move container (30x30) and move icon.
        moveContainer = new StackPane();
        moveContainer.setPrefSize(30, 30);
        moveContainer.setPickOnBounds(true);
        moveContainer.layoutXProperty().bind(widthProperty().subtract(30));
        moveContainer.setLayoutY(0);
        moveContainer.setCursor(Cursor.MOVE);
        moveIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/move.png")));
        moveIcon.setFitWidth(16);
        moveIcon.setFitHeight(16);
        moveIcon.setCursor(Cursor.MOVE);
        moveContainer.getChildren().add(moveIcon);
        getChildren().add(moveContainer);
        // Attach move event handlers.
        moveContainer.setOnMousePressed(e -> {
            dragDeltaX = getLayoutX() - e.getSceneX();
            dragDeltaY = getLayoutY() - e.getSceneY();
        });
        moveContainer.setOnMouseDragged(e -> {
            double newX = e.getSceneX() + dragDeltaX;
            double newY = e.getSceneY() + dragDeltaY;
            newX = Math.round(newX / SNAP) * SNAP;
            newY = Math.round(newY / SNAP) * SNAP;
            setLayoutX(newX);
            setLayoutY(newY);
        });
        
        // Create resize container (30x30) and resize icon.
        resizeContainer = new StackPane();
        resizeContainer.setPrefSize(30, 30);
        resizeContainer.setPickOnBounds(true);
        resizeContainer.layoutXProperty().bind(widthProperty().subtract(30));
        resizeContainer.layoutYProperty().bind(heightProperty().subtract(30));
        resizeContainer.setCursor(Cursor.SE_RESIZE);
        resizeIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/resize.png")));
        resizeIcon.setFitWidth(14);
        resizeIcon.setFitHeight(14);
        resizeIcon.setCursor(Cursor.SE_RESIZE);
        resizeContainer.getChildren().add(resizeIcon);
        getChildren().add(resizeContainer);
        // Attach resize event handlers.
        resizeContainer.setOnMousePressed(e -> {
            e.consume();
            resizeStartWidth = getPrefWidth();
            resizeStartHeight = getPrefHeight();
            resizeStartX = e.getSceneX();
            resizeStartY = e.getSceneY();
        });
        resizeContainer.setOnMouseDragged(e -> {
            e.consume();
            double dx = e.getSceneX() - resizeStartX;
            double dy = e.getSceneY() - resizeStartY;
            double newWidth = resizeStartWidth + dx;
            double newHeight = resizeStartHeight + dy;
            newWidth = Math.round(newWidth / SNAP) * SNAP;
            newHeight = Math.round(newHeight / SNAP) * SNAP;
            if (newWidth < 50) newWidth = 50;
            if (newHeight < 50) newHeight = 50;
            setPrefWidth(newWidth);
            setPrefHeight(newHeight);
            zoneRect.setWidth(newWidth);
            zoneRect.setHeight(newHeight);
            header.setPrefWidth(newWidth);
        });
        
        // Disable generic dragging on the zone.
        setOnMousePressed(null);
        setOnMouseDragged(null);
    }
    
    public String getZoneName() {
        return zoneName;
    }
    
    public void setZoneName(String newName) {
        this.zoneName = newName;
        zoneNameLabel.setText(newName);
    }
}
