package com.choespacedout.PlazaApartments;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerApartment {
    private final int instanceID;
    private final UUID owner;
    private final String apartmentType;
    private final ApartmentSetup apartmentSetup;

    public PlayerApartment(int newInstanceID, UUID newOwner, String newApartmentType, ApartmentSetupCache apartmentSetupCache) {
        instanceID = newInstanceID;
        owner = newOwner;
        apartmentType = newApartmentType;
        apartmentSetup = apartmentSetupCache.getApartmentSetup(newApartmentType);
    }

    public Integer getInstanceID() {
        return instanceID;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getApartmentType() {
        return apartmentType;
    }

    public void teleport(Player target) {
        Location enterTeleport = apartmentSetup.getEnterTeleport();
        target.teleport(enterTeleport.clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F)));
    }

}
