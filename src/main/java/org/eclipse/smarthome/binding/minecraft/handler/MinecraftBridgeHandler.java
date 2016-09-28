/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft.handler;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.ENDPOINT;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.binding.minecraft.model.MinecraftThing;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingCommand;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingList;
import org.eclipse.smarthome.binding.minecraft.sse.Client;
import org.eclipse.smarthome.binding.minecraft.sse.Event;
import org.eclipse.smarthome.binding.minecraft.sse.EventHandler;
import org.eclipse.smarthome.binding.minecraft.sse.MessageType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * @author sw
 *
 */
public class MinecraftBridgeHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(MinecraftThingHandler.class);

    private String endpoint = null;

    public MinecraftBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandler#handleCommand(org.eclipse.smarthome.core.thing.ChannelUID,
     * org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // nothing todo
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Minecraft bridge handler.");
        endpoint = (String) getConfig().get(ENDPOINT);
        startServerSentEventsListener();
    }

    public void setStatus(ThingStatus status) {

        if (thing.getStatus() != status) {
            updateStatus(status);
        }

    }

    public String getEndpoint() {
        return endpoint;
    }

    public MinecraftThing requestState(String id) {
        try (InputStreamReader reader = executeGetRequest(String.format("%sthings/%s", endpoint, id))) {
            setStatus(ThingStatus.ONLINE);
            Gson gson = new Gson();
            return gson.fromJson(reader, MinecraftThing.class);
        } catch (IOException e) {
            logger.error("Cannot request state for minecraft thing {}", id);
            setStatus(ThingStatus.OFFLINE);
        }

        return null;
    }

    public MinecraftThingList requestList() {
        try (InputStreamReader reader = executeGetRequest(String.format("%sthings", endpoint))) {
            setStatus(ThingStatus.ONLINE);
            Gson gson = new Gson();
            return gson.fromJson(reader, MinecraftThingList.class);
        } catch (Exception e) {
            logger.warn("Unable to request state: {}", e.getMessage());
            setStatus(ThingStatus.OFFLINE);
        }

        return null;
    }

    private InputStreamReader executeGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return new InputStreamReader(connection.getInputStream());
    }

    public void postState(MinecraftThingCommand command) {
        String urlString = endpoint + "commands/execute/";
        String json = new Gson().toJson(command);

        try {

            // Create HTTP POST request
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(json);
            out.close();

            if (connection.getResponseCode() == 200) {
                setStatus(ThingStatus.ONLINE);
            } else {
                logger.warn("Unable to post command: " + connection.getResponseCode());
                setStatus(ThingStatus.OFFLINE);
            }
            connection.disconnect();
        } catch (Exception e) {
            logger.warn("Unable to post command: " + e.getMessage(), e);
            setStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void dispose() {
        stopServerSentEventsListener();
    }

    public Thing getThingByID(String id) {
        Bridge bridge = getThing();
        List<Thing> things = bridge.getThings();
        for (Thing thing : things) {
            if (thing.getConfiguration().get("id").equals(id)) {
                return thing;
            }
        }
        return null;
    }

    private Client client;

    private void stopServerSentEventsListener() {
        if (client != null) {
            client.stop();
        }
    }

    private void startServerSentEventsListener() {

        // // Jersey based prototype replaced by low level implementation below due to performance reasons
        // Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
        //
        // WebTarget target = client.target(endpoint + "events/");
        // logger.info("!!!" + endpoint + "events/");
        // EventSource eventSource = EventSource.target(target).build();
        // EventListener listener = new EventListener() {
        //
        // @Override
        // public void onEvent(InboundEvent inboundEvent) {
        // logger.info("!!!" + inboundEvent.getName() + "; " + inboundEvent.readData(String.class));
        // }
        // };
        // // here you could filter for certain messages
        // eventSource.register(listener);
        // eventSource.open();
        // // ...
        // // TODO Don't forget to close the eventSource.close();

        // Low level implementation of SSE
        EventHandler handler = new EventHandler() {

            @Override
            public void onEvent(Event event) {
                Gson gson = new Gson();
                setStatus(ThingStatus.ONLINE);
                if (event.getName().equals(MessageType.UPDATE_THING.toString())) {
                    final MinecraftThingCommand command = gson.fromJson(event.getData(), MinecraftThingCommand.class);
                    // Handle thing update here
                    scheduler.execute(new Runnable() {
                        @Override
                        public void run() {
                            ((MinecraftThingHandler) getThingByID(command.id).getHandler())
                                    .handleMinecraftThingUpdate(command.component);
                        }
                    });
                } else if (event.getName().equals(MessageType.REMOVE_THING.toString())) {
                    final MinecraftThing minecraftThing = gson.fromJson(event.getData(), MinecraftThing.class);
                    // Handle remove here
                    scheduler.execute(new Runnable() {
                        @Override
                        public void run() {
                            ((MinecraftThingHandler) getThingByID(minecraftThing.id).getHandler()).handleRemoval();
                        }
                    });
                } else {
                    // ignore so far
                }

            }

            @Override
            public void onError(IOException error) {
                logger.error(error.getMessage());
                setStatus(ThingStatus.OFFLINE);
            }

        };

        client = new Client(handler, endpoint + "events/");
        try {
            client.start();
        } catch (IOException e) {
            setStatus(ThingStatus.OFFLINE);

            stopServerSentEventsListener();
            // retry in 5 seconds until it works
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    updateStatus(ThingStatus.INITIALIZING);
                    startServerSentEventsListener();
                }
            }, 5, TimeUnit.SECONDS);
        }
    }
}
