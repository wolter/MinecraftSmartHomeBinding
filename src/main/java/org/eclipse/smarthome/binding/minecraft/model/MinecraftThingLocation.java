package org.eclipse.smarthome.binding.minecraft.model;

public class MinecraftThingLocation {

    public int x;
    public int y;
    public int z;

    public MinecraftThingLocation() {
    }

    public MinecraftThingLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}