package org.example;

import java.util.List;

public class FilterOptions {
    public enum FilterMode {
        SUBNET,
        COLOUR,
        CONNECTION,
        DEVICE_TYPE
    }
    
    private FilterMode filterMode;
    private List<String> selectedFilters; // For device types, these are enum names; for colours, the hex strings; etc.

    public FilterOptions(FilterMode filterMode, List<String> selectedFilters) {
        this.filterMode = filterMode;
        this.selectedFilters = selectedFilters;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public List<String> getSelectedFilters() {
        return selectedFilters;
    }
}
