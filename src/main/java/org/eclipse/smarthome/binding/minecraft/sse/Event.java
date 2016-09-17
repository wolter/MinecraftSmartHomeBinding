package org.eclipse.smarthome.binding.minecraft.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Event.class);

    private static final String DELIMITER = ":";
    private static final String EVENT_KEY = "event";
    private static final String DATA_KEY = "data";
    private final String name;

    private final String data;

    public Event(String name, String data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "SseEvent{" + "name='" + name + '\'' + ", data='" + data + '\'' + '}';
    }

    public static Event parse(String input) {
        String[] lines = input.split("\\n");
        String name = null;
        String data = null;
        for (String line : lines) {
            String[] parts = line.split(DELIMITER, 2);
            if (parts.length != 2) {
                LOGGER.warn("Received invalid SSE line, ignoring: '{}'", line);
                continue;
            }
            String key = parts[0].trim();
            String value = parts[1].trim();
            if (key.equals(EVENT_KEY)) {
                name = value;
            } else if (key.equals(DATA_KEY)) {
                data = value;
            }
        }

        return new Event(name, data);
    }
}