package com.chloespacedout.PlazaApartments;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class ApartmentSetup {
    private final String name;
    private final Location enterTeleport;
    private final Location exitTeleport;
    private final Location regionMin;
    private final Location regionMax;
    private final BoundingBox region;


    public ApartmentSetup(String newName, Location newEnterTeleport, Location newExitTeleport, Location newRegionMin, Location newRegionMax, BoundingBox newRegion) {
        name = newName;
        enterTeleport = newEnterTeleport;
        exitTeleport = newExitTeleport;
        regionMin = newRegionMin;
        regionMax = newRegionMax;
        region = newRegion;
    }

    public String getName() {
        return name;
    }

    public Location getEnterTeleport() {
        return enterTeleport;
    }

    public Location getExitTeleport() {
        return exitTeleport;
    }

    public BoundingBox getRegion() {
        return region;
    }

    public Location getRegionMin() {
        return regionMin;
    }

    public Location getRegionMax() {
        return regionMax;
    }

}
