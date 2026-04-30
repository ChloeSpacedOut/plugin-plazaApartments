package com.choespacedout.PlazaApartments;

import org.bukkit.World;

public class Config {
    final private int instanceCount;
    final private World apartmentWorld;
    final private World mainWorld;

    Config(int newInstanceCount, World newApartmentWorld, World newMainWorld) {
        instanceCount = newInstanceCount;
        apartmentWorld = newApartmentWorld;
        mainWorld = newMainWorld;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public World getApartmentWorld() {
        return apartmentWorld;
    }

    public World getMainWorld() {
        return mainWorld;
    }
}
