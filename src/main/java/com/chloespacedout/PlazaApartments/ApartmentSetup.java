package com.chloespacedout.PlazaApartments;

import org.bukkit.Location;

import java.util.List;

public class ApartmentSetup {
    private final String name;
    private final Location enterTeleport;
    private final Location exitTeleport;
    private final Region region;
    private final List<Region> noBuildRegions;


    public ApartmentSetup(String newName, Location newEnterTeleport, Location newExitTeleport, Region newRegion, List<Region> newNoBuildRegions) {
        name = newName;
        enterTeleport = newEnterTeleport;
        exitTeleport = newExitTeleport;
        region = newRegion;
        noBuildRegions = newNoBuildRegions;
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

    public Region getRegion() {
        return region;
    }

    public List<Region> getNoBuildRegions() {
        return noBuildRegions;
    }

}
