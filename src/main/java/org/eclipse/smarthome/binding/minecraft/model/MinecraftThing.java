package org.eclipse.smarthome.binding.minecraft.model;

import java.util.ArrayList;

public class MinecraftThing {

    // Location basically is the unique identifier of a device, but in case we will support pistons sometime, blocks
    // could be moved and a immutable id is needed
    public String id;
    // Location basically is the unique identifier of a device, because there could only be one at a time at each
    // location
    public MinecraftThingLocation location;
    // Type describing the thing behavior
    public MinecraftThingType type;
    // Material holds the Minecraft material and is more detailed than the type
    public String material;
    // Components are the possible channels to read (and in some cases to write)
    public ArrayList<MinecraftThingComponent> components;

    /*
     * Compares two devices based on the location, because there could only be one at a time at each location
     */
    public boolean equals(MinecraftThing location) {
        return this.location.equals(location);
    }

    /*
     * Retrieve ThingComponent by ThingComponentType
     */
    public MinecraftThingComponent getComponentByType(MinecraftThingComponentType type) {
        MinecraftThingComponent component = null;
        for (MinecraftThingComponent c : components) {
            if (c.type.equals(type)) {
                component = c;
                break;
            }
        }
        return component;
    }

}