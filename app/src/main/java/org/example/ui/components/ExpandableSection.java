package org.example.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ExpandableSection extends VBox {
    private final VBox contentContainer;
    private final ImageView arrow;
    private final HBox headerBox;
    private boolean expanded;
    
    private static final Image ARROW_DOWN = new Image(
            ExpandableSection.class.getResourceAsStream("/icons/dropdown.png"));
    private static final Image ARROW_UP = new Image(
            ExpandableSection.class.getResourceAsStream("/icons/dropdown.png"));
    
    public ExpandableSection(String title, boolean expandedByDefault) {
        setSpacing(8);
        
        // Header with title and arrow
        headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        // Create header text
        Text headerText = new Text(title);
        headerText.getStyleClass().add("section-header");
        headerBox.setPadding(new Insets(0, 10, 0, 0));
        headerText.setFill(Color.WHITE);
        
        // Create arrow icon
        arrow = new ImageView(expandedByDefault ? ARROW_UP : ARROW_DOWN);
        arrow.setFitHeight(16);
        arrow.setFitWidth(16);
        
        // Add a spacer that will push the arrow to the right
        Region spacer = new Region();
        spacer.setMinWidth(1);
        headerBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        
        
        headerBox.getChildren().addAll(headerText, spacer, arrow);
        headerBox.getStyleClass().add("expandable-section-header");
        headerBox.setCursor(javafx.scene.Cursor.HAND);

        
        
        // Content container that will be shown/hidden
        contentContainer = new VBox(4);
        contentContainer.getStyleClass().add("expandable-section-content");
        
        // Add both to the main container
        getChildren().addAll(headerBox, contentContainer);
        
        // Set initial expanded state
        expanded = expandedByDefault;
        if (!expanded) {
            contentContainer.setVisible(false);
            contentContainer.setManaged(false);
        }
        
        // Add click handler to the header
        headerBox.setOnMouseClicked(e -> toggleExpanded());
    }
    
    public void addContent(Node... nodes) {
        contentContainer.getChildren().addAll(nodes);
    }
    
    public void clearContent() {
        contentContainer.getChildren().clear();
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            toggleExpanded();
        }
    }
    
    private void toggleExpanded() {
        expanded = !expanded;
        
        // Update the arrow
        arrow.setImage(expanded ? ARROW_UP : ARROW_DOWN);
        
        // Show/hide the content
        contentContainer.setVisible(expanded);
        contentContainer.setManaged(expanded);
    }
}