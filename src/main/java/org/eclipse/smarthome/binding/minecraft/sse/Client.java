package org.eclipse.smarthome.binding.minecraft.sse;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private static final int BUFFER_SIZE = 256 * 1024;

    /**
     * Http client which opens the connection.
     */
    private HttpClient client;
    /**
     * True if the SSE thread should run.
     */
    private AtomicBoolean running = new AtomicBoolean();

    /**
     * Event handler for handling SSE events.
     */
    private final EventHandler eventHandler;
    /**
     * SSE thread.
     */
    private Thread thread;
    /**
     * Current request. Needed for stopping.
     */
    private final AtomicReference<HttpRequestBase> currentRequest = new AtomicReference<>();

    /**
     * SSE url.
     */
    private final String url;

    public Client(EventHandler eventHandler, String url) {
        this.eventHandler = eventHandler;
        this.url = url;
    }

    public void start() throws IOException {
        if (client != null) {
            throw new IllegalStateException("Already started");
        }

        client = new DefaultHttpClient();

        running.set(true);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.debug("SSE thread started");
                while (running.get()) {
                    HttpGet request = new HttpGet(url);
                    request.setHeader("Accept", "text/event-stream");
                    request.setHeader("Cache-Control", "no-cache");
                    // Set the current request so the stop() method can abort it
                    currentRequest.set(request);

                    try {
                        HttpResponse response = client.execute(request);
                        LOGGER.debug("SSE connection established");

                        InputStream stream = response.getEntity().getContent();
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = stream.read(buffer)) != -1) {
                            LOGGER.debug("Reveived {} bytes", read);
                            String string = new String(buffer, 0, read, "utf-8");

                            // Fire the event
                            eventHandler.onEvent(Event.parse(string));
                            if (!running.get()) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("IOException while receiving events", e);
                    }
                }
                LOGGER.debug("SSE thread stopped");
            }
        });
        thread.setDaemon(true);
        thread.setName("SSE-client-thread");
        thread.start();
    }

    public void stop() {
        running.set(false);
        thread.interrupt();
        abortCurrentRequest();
        try {
            // Wait for SSE thread to end
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
        // Shutdown client
        client.getConnectionManager().shutdown();
        client = null;
    }

    private void abortCurrentRequest() {
        HttpRequestBase request = currentRequest.get();
        if (request != null) {
            request.abort();
        }
    }

}
