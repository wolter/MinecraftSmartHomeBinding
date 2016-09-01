/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft.handler;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.ENDPOINT;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sw
 *
 */
public class MinecraftBridgeHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(MinecraftHandler.class);

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
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "Cannot connect to
        // bridge.");
        updateStatus(ThingStatus.ONLINE);
    }

    public void setStatus(ThingStatus status) {
        updateStatus(status);
    }

    public String getEndpoint() {
        return endpoint;
    }

}
