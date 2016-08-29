package org.eclipse.smarthome.binding.minecraft.model;

import java.util.ArrayList;

public class MinecraftThingList extends ArrayList<MinecraftThing> {

    private MinecraftThingList() {
    }

    public MinecraftThing findThingByLocation(MinecraftThingLocation location) {
        MinecraftThing thing = null;
        for (MinecraftThing t : this) {
            if (t.location.equals(location)) {
                thing = t;
                break;
            }
        }
        return thing;
    }

    public MinecraftThing findThingById(String id) {
        MinecraftThing thing = null;
        for (MinecraftThing t : this) {
            if (t.id.equals(id)) {
                thing = t;
                break;
            }
        }
        return thing;
    }

}