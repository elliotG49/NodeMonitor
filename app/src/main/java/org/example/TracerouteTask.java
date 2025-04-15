package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.concurrent.Task;

public class TracerouteTask extends Task<List<String>> {
    private final String target;
    private Consumer<String> hopCallback;  // Callback to notify each hop as soon as it’s discovered.

    public TracerouteTask(String target) {
        this.target = target;
    }
    
    // Setter for the hop callback.
    public void setHopCallback(Consumer<String> callback) {
        this.hopCallback = callback;
    }

    @Override
    protected List<String> call() throws Exception {
        List<String> hops = new ArrayList<>();
        // Windows-specific tracert command with parameters.
        String command = "tracert -4 -h 15 -w 500 " + target;
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        // Pattern to match an IPv4 address.
        Pattern ipPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        while ((line = reader.readLine()) != null) {
            // Skip header/footer lines.
            if (line.contains("Tracing route") ||
                line.contains("over a maximum") ||
                line.contains("Trace complete")) {
                continue;
            }
            updateMessage(line);
            Matcher matcher = ipPattern.matcher(line);
            if (matcher.find()) {
                String hopIp = matcher.group(1);
                hops.add(hopIp);
                if (hopCallback != null) {
                    Platform.runLater(() -> hopCallback.accept(hopIp));
                }
            }
        }
        process.waitFor();
        updateMessage("Traceroute complete");
        return hops;
    }
}
