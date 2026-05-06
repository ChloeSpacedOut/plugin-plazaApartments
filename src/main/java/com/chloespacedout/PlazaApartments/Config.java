package com.chloespacedout.PlazaApartments;

import org.bukkit.World;

public class Config {
    final private int instanceCount;
    final private int maxEntitiesPerInstance;
    final private World apartmentWorld;
    final private World mainWorld;

    Config(int newInstanceCount, int newMaxEntitiesPerInstance, World newApartmentWorld, World newMainWorld) {
        instanceCount = newInstanceCount;
        maxEntitiesPerInstance = newMaxEntitiesPerInstance;
        apartmentWorld = newApartmentWorld;
        mainWorld = newMainWorld;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public int getMaxEntitiesPerInstance() {
        return maxEntitiesPerInstance;
    }

    public World getApartmentWorld() {
        return apartmentWorld;
    }

    public World getMainWorld() {
        return mainWorld;
    }
}
