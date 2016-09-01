/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft.internal;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.minecraft.discovery.MinecraftDiscoveryService;
import org.eclipse.smarthome.binding.minecraft.handler.MinecraftBridgeHandler;
import org.eclipse.smarthome.binding.minecraft.handler.MinecraftHandler;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.Sets;

/**
 * The {@link MinecraftHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sascha Wolter - Initial contribution
 */
public class MinecraftHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet(THING_TYPE_MINECRAFT_SWITCH,
            THING_TYPE_MINECRAFT_DOOR, THING_TYPE_MINECRAFT_PLATE, THING_TYPE_MINECRAFT_WEATHER_SENSOR,
            THING_TYPE_MINECRAFT_LAMP);

    private final static Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES_UIDS = Collections.singleton(THING_TYPE_BRIDGE);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return Sets.union(SUPPORTED_THING_TYPES_UIDS, SUPPORTED_BRIDGE_TYPES_UIDS).contains(thingTypeUID);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        if (SUPPORTED_BRIDGE_TYPES_UIDS.contains(thingTypeUID)) {
            ThingUID minecraftBridgeUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, minecraftBridgeUID, null);
        }
        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            ThingUID minecraftThingUID = getMinecraftThingUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the hue binding.");
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_BRIDGE_TYPES_UIDS.contains(thingTypeUID)) {
            MinecraftBridgeHandler bridgeHandler = new MinecraftBridgeHandler((Bridge) thing);
            registerDiscoveryService(bridgeHandler);
            return bridgeHandler;
        }
        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            MinecraftHandler thingHandler = new MinecraftHandler(thing);
            return thingHandler;
        }
        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the Minecraft SmartHome binding.");
    }

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private synchronized void registerDiscoveryService(MinecraftBridgeHandler bridgeHandler) {
        MinecraftDiscoveryService discoveryService = new MinecraftDiscoveryService(bridgeHandler);
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof MinecraftBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                MinecraftDiscoveryService service = (MinecraftDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
            String endpoint = (String) configuration.get(ENDPOINT);
            String id = endpoint.replace("http[s]://", "").replace("/rest/", "").replaceAll("\\.", "_").replaceAll(":",
                    "__");
            thingUID = new ThingUID(thingTypeUID, id);
        }
        return thingUID;
    }

    private ThingUID getMinecraftThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String id = (String) configuration.get("id");

        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, id, bridgeUID.getId());
        }
        return thingUID;
    }

}