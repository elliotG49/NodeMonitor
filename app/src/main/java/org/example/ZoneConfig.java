package org.example;

public class ZoneConfig {
    private String zoneName;
    private double layoutX;
    private double layoutY;
    private double width;
    private double height;
    private double relativeX; // New field: relative X coordinate (0.0 to 1.0)
    private double relativeY; // New field: relative Y coordinate (0.0 to 1.0)

    public ZoneConfig() {}

    public ZoneConfig(String zoneName, double layoutX, double layoutY, double width, double height, double relativeX, double relativeY) {
        this.zoneName = zoneName;
        this.layoutX = layoutX;
        this.layoutY = layoutY;
        this.width = width;
        this.height = height;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    // Getters and setters.
    public String getZoneName() {
        return zoneName;
    }
    
    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }
    
    public double getLayoutX() {
        return layoutX;
    }
    
    public void setLayoutX(double layoutX) {
        this.layoutX = layoutX;
    }
    
    public double getLayoutY() {
        return layoutY;
    }
    
    public void setLayoutY(double layoutY) {
        this.layoutY = layoutY;
    }
    
    public double getWidth() {
        return width;
    }
    
    public void setWidth(double width) {
        this.width = width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public void setHeight(double height) {
        this.height = height;
    }
    
    public double getRelativeX() {
        return relativeX;
    }
    
    public void setRelativeX(double relativeX) {
        this.relativeX = relativeX;
    }
    
    public double getRelativeY() {
        return relativeY;
    }
    
    public void setRelativeY(double relativeY) {
        this.relativeY = relativeY;
    }
}
