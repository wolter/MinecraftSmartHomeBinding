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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.binding.minecraft.model.MinecraftThing;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingCommand;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingList;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
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
        if (isServerAlive()) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot connect to bridge.");
        }

        startAlivePing();

        /*
         * // TODO Refactor prototypic trial of SSE
         * Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
         *
         * WebTarget target = client.target(endpoint + "events/");
         * logger.info("!!!" + endpoint + "events/");
         * EventSource eventSource = EventSource.target(target).build();
         * EventListener listener = new EventListener() {
         *
         * @Override
         * public void onEvent(InboundEvent inboundEvent) {
         * logger.info("!!!" + inboundEvent.getName() + "; " + inboundEvent.readData(String.class));
         * }
         * };
         * // here you could filter for certain messages
         * eventSource.register(listener);
         * eventSource.open();
         * // ...
         * // TODO Don't forget to close the eventSource.close();
         */

    }

    public void setStatus(ThingStatus status) {
        if (thing.getStatus() != status) {
            updateStatus(status);
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isServerAlive() {
        try (InputStreamReader reader = executeGetRequest(String.format("%shello", endpoint))) {
            setStatus(ThingStatus.ONLINE);
            return true;
        } catch (IOException e) {
            if (thing.getStatus() != ThingStatus.OFFLINE) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
        }
        return false;
    }

    public MinecraftThing requestState(String id) {
        try (InputStreamReader reader = executeGetRequest(String.format("%sthings/%s", endpoint, id))) {
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

            // Process response
            // InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            // Gson gson = new Gson();
            // MinecraftThing thing = gson.fromJson(reader, MinecraftThing.class);
            //
            // OnOffType state = ((Boolean) thing.components.get(0).state) ? OnOffType.ON : OnOffType.ON;
            // updateState(CHANNEL_POWERED, state);
            // logger.info("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWERED, state);

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

    private ScheduledFuture<?> refreshJob;

    @Override
    public void dispose() {
        refreshJob.cancel(true);
    }

    private void startAlivePing() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                isServerAlive();
            }
        };
        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.SECONDS);
    }
}
