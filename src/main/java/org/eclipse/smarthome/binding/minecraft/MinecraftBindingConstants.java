/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.minecraft;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MinecraftBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Sascha Wolter - Initial contribution
 */
public class MinecraftBindingConstants {

    public static final String BINDING_ID = "minecraft";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_MINECRAFT_SWITCH = new ThingTypeUID(BINDING_ID, "minecraft-switch");
    public final static ThingTypeUID THING_TYPE_MINECRAFT_BUTTON = new ThingTypeUID(BINDING_ID, "minecraft-button");
    public final static ThingTypeUID THING_TYPE_MINECRAFT_DOOR = new ThingTypeUID(BINDING_ID, "minecraft-door");
    public final static ThingTypeUID THING_TYPE_MINECRAFT_PLATE = new ThingTypeUID(BINDING_ID, "minecraft-plate");
    public final static ThingTypeUID THING_TYPE_MINECRAFT_WEATHER_SENSOR = new ThingTypeUID(BINDING_ID,
            "minecraft-weather-sensor");
    public final static ThingTypeUID THING_TYPE_MINECRAFT_LAMP = new ThingTypeUID(BINDING_ID, "minecraft-lamp");
    public final static ThingTypeUID THING_TYPE_MINECRAFT_TRIPWIRE = new ThingTypeUID(BINDING_ID, "minecraft-tripwire");
    // List of all Channel ids
    public final static String CHANNEL_POWERED = "channelPowered";
    public final static String CHANNEL_OPEN = "channelOpen";
    public final static String CHANNEL_PRESSED = "channelPressed";
    public final static String CHANNEL_POWERED_READONLY = "channelPoweredReadonly";

    public final static String CHANNEL_HUMIDITY = "channelHumidity";
    public final static String CHANNEL_LIGHT = "channelLight";
    public final static String CHANNEL_POWER = "channelPower";
    public final static String CHANNEL_TEMPERATURE = "channelTemperature";

}