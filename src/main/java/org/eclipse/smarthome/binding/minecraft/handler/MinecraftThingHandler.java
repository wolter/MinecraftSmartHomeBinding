/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft.handler;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.*;

import java.math.BigDecimal;
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
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MinecraftThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sascha Wolter - Initial contribution
 */
public class MinecraftThingHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MinecraftThingHandler.class);

    private MinecraftBridgeHandler bridgeHandler;

    public MinecraftThingHandler(Thing thing) {
        super(thing);
    }

    private synchronized MinecraftBridgeHandler getBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof MinecraftBridgeHandler) {
                this.bridgeHandler = (MinecraftBridgeHandler) handler;
            } else {
                return null;
            }
        }
        return this.bridgeHandler;
    }

    public void setStatus(ThingStatus status) {
        if (thing.getStatus() != status) {
            updateStatus(status);
        }
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

    private Long refreshInterval;

    private String id;

    public String getId() {
        return id;
    }

    @Override
    public void initialize() {

        if (refreshJob != null) {
            refreshJob.cancel(true);
        }

        Configuration config = getConfig();
        refreshInterval = ((BigDecimal) config.get("refresh")).longValue();
        id = (String) config.get("id");

        if (id == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Id is empty");
        } else if (refreshInterval == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Refresh interval is empty");
        } else {
            ThingStatus bridgeStatus = (getBridge() == null) ? null : getBridge().getStatus();
            if (getBridgeHandler() != null) {
                if (bridgeStatus == ThingStatus.ONLINE) {
                    updateStatus(ThingStatus.ONLINE);
                    startAutomaticRefresh();
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            startAutomaticRefresh();
        } else {
            if (refreshJob != null) {
                refreshJob.cancel(true);
            }
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    private void requestState() {
        MinecraftBridgeHandler bridgeHandler = getBridgeHandler();

        if (bridgeHandler == null) {
            return;
        }

        MinecraftThing minecraftThing = bridgeHandler.requestState(id);

        if (minecraftThing != null) {
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

            setStatus(ThingStatus.ONLINE);

        } else {
            setStatus(ThingStatus.OFFLINE);
        }
    }

    private void postState(MinecraftThingCommand command) {
        if (getBridgeHandler() != null) {
            bridgeHandler.postState(command);
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

    public void handleMinecraftThingCommand(MinecraftThingCommand command) {

        setStatus(ThingStatus.ONLINE);

        MinecraftThingComponent component = command.component;
        ThingTypeUID thingTypeUid = getThing().getThingTypeUID();

        if (component.type == MinecraftThingComponentType.POWERED) {
            OnOffType state = ((Boolean) component.state) ? OnOffType.ON : OnOffType.OFF;
            if (thingTypeUid.getId().equals(THING_TYPE_MINECRAFT_SWITCH.getId())
                    || thingTypeUid.getId().equals(THING_TYPE_MINECRAFT_BUTTON.getId())) {
                updateState(CHANNEL_POWERED, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWERED, state);
            } else {
                // Tripwire or Lamp
                updateState(CHANNEL_POWERED_READONLY, state);
                logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWERED_READONLY, state);
            }
            return;
        }

        if (component.type == MinecraftThingComponentType.OPEN) {
            OnOffType state = ((Boolean) component.state) ? OnOffType.ON : OnOffType.OFF;
            updateState(CHANNEL_OPEN, state);
            logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_OPEN, state);
            return;
        }

        if (component.type == MinecraftThingComponentType.PRESSED) {
            OnOffType state = ((Boolean) component.state) ? OnOffType.ON : OnOffType.OFF;
            updateState(CHANNEL_PRESSED, state);
            logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_PRESSED, state);
            return;
        }

        if (component.type == MinecraftThingComponentType.HUMIDITY) {
            DecimalType state = new DecimalType((Double) component.state);
            updateState(CHANNEL_HUMIDITY, state);
            logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_HUMIDITY, state);
            return;
        }

        if (component.type == MinecraftThingComponentType.LIGHT) {
            DecimalType state = new DecimalType((Double) component.state);
            updateState(CHANNEL_LIGHT, state);
            logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_LIGHT, state);
            return;
        }

        if (component.type == MinecraftThingComponentType.POWER) {
            DecimalType state = new DecimalType((Double) component.state);
            updateState(CHANNEL_POWER, state);
            logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_POWER, state);
            return;
        }

        if (component.type == MinecraftThingComponentType.TEMPERATURE) {
            DecimalType state = new DecimalType((Double) component.state);
            updateState(CHANNEL_TEMPERATURE, state);
            logger.debug("Update of {} : {} to {}", this.getThing().getUID(), CHANNEL_TEMPERATURE, state);
            return;
        }

    }

}