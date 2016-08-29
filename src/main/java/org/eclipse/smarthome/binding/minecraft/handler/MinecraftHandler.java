/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft.handler;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.*;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.binding.minecraft.model.MinecraftThing;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingCommand;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingComponent;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingComponentType;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingType;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link MinecraftHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sascha Wolter - Initial contribution
 */
public class MinecraftHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MinecraftHandler.class);

    public MinecraftHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // handle command
        switch (channelUID.getId()) {
            case CHANNEL_POWERED:
                logger.debug("Command {} on {} : {}", command.toString(), this.getThing().getUID(), CHANNEL_POWERED);
                if (command instanceof OnOffType) {
                    MinecraftThingCommand minecraftCommand = new MinecraftThingCommand();
                    minecraftCommand.id = id;
                    // minecraftCommand.location = location;
                    minecraftCommand.component = new MinecraftThingComponent();
                    minecraftCommand.component.type = MinecraftThingComponentType.POWERED;
                    minecraftCommand.component.state = command.equals(OnOffType.ON) ? true : false;
                    postState(minecraftCommand);
                } else {
                    logger.error("Unhandled command {} on {} : {}", command.toString(), this.getThing().getUID(),
                            CHANNEL_POWERED);
                }
                break;
            case CHANNEL_OPEN:
                logger.debug("Command {} on {} : {}", command.toString(), this.getThing().getUID(), CHANNEL_OPEN);
                if (command instanceof OnOffType) {
                    MinecraftThingCommand minecraftCommand = new MinecraftThingCommand();
                    minecraftCommand.id = id;
                    // minecraftCommand.location = location;
                    minecraftCommand.component = new MinecraftThingComponent();
                    minecraftCommand.component.type = MinecraftThingComponentType.OPEN;
                    minecraftCommand.component.state = command.equals(OnOffType.ON) ? true : false;
                    postState(minecraftCommand);
                } else {
                    logger.error("Unhandled command {} on {} : {}", command.toString(), this.getThing().getUID(),
                            CHANNEL_POWERED);
                }
                break;
            default:
                logger.error("Unhandled channel {} on {}", channelUID.getId(), this.getThing().getUID());
                // Note: if communication with thing fails for some reason,
                // indicate that by setting the status with detail information
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                // "Could not control device at IP address x.x.x.x");
        }
    }

    private String endpoint;
    private String id;
    private Long refreshInterval;

    @Override
    public void initialize() {

        Configuration config = getConfig();
        refreshInterval = ((BigDecimal) config.get("refresh")).longValue();
        endpoint = (String) config.get("endpoint");
        id = (String) config.get("id");

        startAutomaticRefresh();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        // updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    private void requestState() {

        String urlTemplate = "%sthings/%s";
        String urlString = String.format(urlTemplate, endpoint, id);

        try {
            // Create HTTP GET request
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // Process response
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            Gson gson = new Gson();
            MinecraftThing minecraftThing = gson.fromJson(reader, MinecraftThing.class);

            MinecraftThingComponent component;
            component = minecraftThing.getComponentByType(MinecraftThingComponentType.POWERED);
            if (component != null) {
                OnOffType state = ((Boolean) component.state) ? OnOffType.ON : OnOffType.OFF;
                if (minecraftThing.type.equals(MinecraftThingType.SWITCH)
                        || minecraftThing.type.equals(MinecraftThingType.BUTTON)) {
                    updateState(CHANNEL_POWERED, state);
                    logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWERED, state);
                } else {
                    // Tripwire or Lamp
                    updateState(CHANNEL_POWERED_READONLY, state);
                    logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWERED_READONLY, state);
                }
            }

            component = minecraftThing.getComponentByType(MinecraftThingComponentType.OPEN);
            if (component != null) {
                OnOffType state = ((Boolean) component.state) ? OnOffType.ON : OnOffType.OFF;
                updateState(CHANNEL_OPEN, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_OPEN, state);
            }

            component = minecraftThing.getComponentByType(MinecraftThingComponentType.PRESSED);
            if (component != null) {
                OnOffType state = ((Boolean) component.state) ? OnOffType.ON : OnOffType.OFF;
                updateState(CHANNEL_PRESSED, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_PRESSED, state);
            }

            component = minecraftThing.getComponentByType(MinecraftThingComponentType.HUMIDITY);
            if (component != null) {
                DecimalType state = new DecimalType((Double) component.state);
                updateState(CHANNEL_HUMIDITY, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_HUMIDITY, state);
            }

            component = minecraftThing.getComponentByType(MinecraftThingComponentType.LIGHT);
            if (component != null) {
                DecimalType state = new DecimalType((Double) component.state);
                updateState(CHANNEL_LIGHT, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_LIGHT, state);
            }

            component = minecraftThing.getComponentByType(MinecraftThingComponentType.POWER);
            if (component != null) {
                DecimalType state = new DecimalType((Double) component.state);
                updateState(CHANNEL_POWER, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWER, state);
            }

            component = minecraftThing.getComponentByType(MinecraftThingComponentType.TEMPERATURE);
            if (component != null) {
                DecimalType state = new DecimalType((Double) component.state);
                updateState(CHANNEL_TEMPERATURE, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_TEMPERATURE, state);
            }

            updateStatus(ThingStatus.ONLINE);

        } catch (Exception e) {

            logger.warn("Unable to request state: " + e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE);

        }
    }

    private ScheduledFuture<?> refreshJob;

    @Override
    public void dispose() {
        refreshJob.cancel(true);
    }

    private void startAutomaticRefresh() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                requestState();
            }
        };
        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refreshInterval, TimeUnit.SECONDS);
    }

    private void postState(MinecraftThingCommand command) {

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
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.warn("Unable to post state: " + connection.getResponseCode());
                updateStatus(ThingStatus.OFFLINE);
            }
            connection.disconnect();
        } catch (Exception e) {
            logger.warn("Unable to post state: " + e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE);
        }

    }

}
