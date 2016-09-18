package org.eclipse.smarthome.binding.minecraft.sse;

import java.io.IOException;

public interface EventHandler {
    void onEvent(Event event);

    void onError(IOException error);
}
