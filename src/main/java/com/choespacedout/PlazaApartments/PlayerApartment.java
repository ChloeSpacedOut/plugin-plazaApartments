package com.choespacedout.PlazaApartments;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerApartment {
    private final int instanceID;
    private final UUID owner;
    private final ApartmentSetup apartmentType;
    private List<UUID> containedPlayers = new ArrayList<>();
    private List<UUID> containedEntities = new ArrayList<>();

    public PlayerApartment(int newInstanceID, UUID newOwner, ApartmentSetup newApartmentType) {
        instanceID = newInstanceID;
        owner = newOwner;
        apartmentType = newApartmentType;
    }

    public Integer getInstanceID() {
        return instanceID;
    }

    public UUID getOwner() {
        return owner;
    }

    public ApartmentSetup getApartmentSetup() {
        return apartmentType;
    }

    public void teleport(Player target) {
        Location enterTeleport = apartmentType.getEnterTeleport();
        target.teleport(enterTeleport.clone().add(0.0F,0.0F,1024.0F + (instanceID * 1024.0F)));
    }

    public void relativeTeleport(Player target) {
        Location playerLocation = target.getLocation();

        double xPos = playerLocation.getX() % 512;
        double yPos = playerLocation.getY();
        double zPos = (playerLocation.getZ() % 512) + 1024 + (instanceID * 1024);
        float yaw = playerLocation.getYaw();
        float pitch = playerLocation.getPitch();

        Location newApartmentInstanceLocation = new Location(target.getWorld(),xPos,yPos,zPos,yaw,pitch);

        target.teleport(newApartmentInstanceLocation);
    }

    public List<UUID> getContainedPlayers() {
        return containedPlayers;
    }

    public void addToContainedPlayers(UUID playerID) {
        containedPlayers.add(playerID);
    }

    public void removeFromContainedPlayers(UUID playerID) {
        containedPlayers.remove(playerID);
    }

    public List<UUID> getContainedEntities() {
        return containedEntities;
    }

    public void addToContainedEntities(UUID entityID) {
        containedEntities.add(entityID);
    }

    public void removeFromContainedEntities(UUID entityID) {
        containedEntities.remove(entityID);
    }

}
