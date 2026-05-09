package com.chloespacedout.PlazaApartments;

import org.bukkit.Location;

public class Region {
    private final String regionID;
    private final Location regionMin;
    private final Location regionMax;

    public Region(String newRegionID, Location newRegionMin, Location newRegionMax) {
        regionID = newRegionID;
        regionMin = newRegionMin;
        regionMax = newRegionMax.add(1,1,1);
    }

    public String getRegionID() {
        return regionID;
    }

    public Location getMin() {
        return regionMin;
    }

    public Location getMax() {
        return regionMax;
    }
}
