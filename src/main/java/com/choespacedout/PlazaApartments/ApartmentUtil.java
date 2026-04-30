package com.choespacedout.PlazaApartments;

import org.bukkit.Location;

import static java.lang.Math.floor;

public class ApartmentUtil {

    private final Config storedConfig;
    private final InstanceManager instanceManager;

    public ApartmentUtil(Config newStoredConfig, InstanceManager newInstanceManager) {
        storedConfig = newStoredConfig;
        instanceManager = newInstanceManager;
    }

    public PlayerApartment apartmentFromLocation(Location location) {
        double xPos = location.getX();
        double zPos = location.getZ();

        if ((xPos < 0) || (xPos > 512)) {
            return null;
        }

        int maxCords = (storedConfig.getInstanceCount() * 1024) + 1024;

        boolean isOutsideApartment = (floor((zPos % 1024) / 512) == 1) && (zPos > 1024) && (zPos < maxCords);

        if (isOutsideApartment) {
            return null;
        }

        int apartmentID = (int) floor((zPos - 1024)/1024);

        return instanceManager.getApartment(apartmentID);

    }
}
