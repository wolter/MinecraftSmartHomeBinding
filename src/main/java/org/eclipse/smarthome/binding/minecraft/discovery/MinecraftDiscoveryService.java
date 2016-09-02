/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft.discovery;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.binding.minecraft.handler.MinecraftBridgeHandler;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThing;
import org.eclipse.smarthome.binding.minecraft.model.MinecraftThingList;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * @author sw
 *
 */
public class MinecraftDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(MinecraftDiscoveryService.class);

    private final static int SEARCH_TIME = 10;

    private final MinecraftBridgeHandler bridge;

    public MinecraftDiscoveryService(MinecraftBridgeHandler bridge) throws IllegalArgumentException {
        super(Sets.newHashSet(THING_TYPE_MINECRAFT_SWITCH, THING_TYPE_MINECRAFT_DOOR, THING_TYPE_MINECRAFT_PLATE,
                THING_TYPE_MINECRAFT_WEATHER_SENSOR, THING_TYPE_MINECRAFT_LAMP), SEARCH_TIME, false);
        this.bridge = bridge;
    }

    @Override
    protected void startScan() {
        doScan();
    }

    private void doScan() {
        if (bridge.getThing().getStatus() == ThingStatus.ONLINE) {
            try {
                MinecraftThingList minecraftThingList = bridge.requestList();
                if (minecraftThingList != null) {
                    for (MinecraftThing minecraftThing : minecraftThingList) {
                        onDeviceAddedInternal(minecraftThing);
                    }
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
        }
    }

    private void onDeviceAddedInternal(MinecraftThing minecraftThing) {

        Map<String, Object> properties = new HashMap<>(1);
        properties.put("refresh", new BigDecimal(4));
        properties.put("id", minecraftThing.id);
        properties.put("type", minecraftThing.type);
        properties.put("material", minecraftThing.material);

        ThingTypeUID thingTypeUID;

        switch (minecraftThing.type) {
            case DOOR:
                thingTypeUID = THING_TYPE_MINECRAFT_DOOR;
                break;
            case WEATHER_SENSOR:
                thingTypeUID = THING_TYPE_MINECRAFT_WEATHER_SENSOR;
                break;
            case SWITCH:
                thingTypeUID = THING_TYPE_MINECRAFT_SWITCH;
                break;
            case BUTTON:
                thingTypeUID = THING_TYPE_MINECRAFT_BUTTON;
                break;
            case PRESSURE_SENSOR:
                thingTypeUID = THING_TYPE_MINECRAFT_PLATE;
                break;
            case LAMP:
                thingTypeUID = THING_TYPE_MINECRAFT_LAMP;
                break;
            case TRIPWIRE:
                thingTypeUID = THING_TYPE_MINECRAFT_TRIPWIRE;
                break;
            default:
                thingTypeUID = null;
        }

        if (thingTypeUID != null) {
            ThingUID thingUID = new ThingUID(thingTypeUID, bridge.getThing().getUID(),
                    String.valueOf(minecraftThing.id));
            String label = String.format("%s %s", minecraftThing.material,
                    String.valueOf(minecraftThing.location.toString()));
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withBridge(bridge.getThing().getUID()).withProperties(properties).withLabel(label).build();
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("Discovered unsupported Minecraft Thing of type '{}' with id {}.", minecraftThing.type,
                    minecraftThing.id);
        }

    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

}
