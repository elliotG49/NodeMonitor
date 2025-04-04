package org.example;

public class WindowConfig {
    private double width;
    private double height;

    public WindowConfig() {
        // Default constructor for Gson
    }

    public WindowConfig(double width, double height) {
        this.width = width;
        this.height = height;
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
}
