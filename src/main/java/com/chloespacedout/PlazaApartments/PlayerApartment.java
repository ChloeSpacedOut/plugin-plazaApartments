package com.chloespacedout.PlazaApartments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public class PlayerApartment {
    private final int instanceID;
    private final UUID owner;
    private final ApartmentSetup apartmentType;
    private HashSet<UUID> containedPlayers = new HashSet<>();
    private HashSet<UUID> containedEntities = new HashSet<>();
    private HashSet<UUID> builders = new HashSet<>();
    private int entityCount = 0;

    public PlayerApartment(int newInstanceID, UUID newOwner, ApartmentSetup newApartmentType) {
        instanceID = newInstanceID;
        owner = newOwner;
        apartmentType = newApartmentType;
        builders.add(owner);
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
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_TELEPORT,1.0F,1.0F);
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

    public HashSet<UUID> getContainedPlayers() {
        return containedPlayers;
    }

    public void addToContainedPlayers(UUID playerID) {
        containedPlayers.add(playerID);
    }

    public void removeFromContainedPlayers(UUID playerID) {
        containedPlayers.remove(playerID);
    }

    public HashSet<UUID> getContainedEntities() {
        return containedEntities;
    }

    public void addToContainedEntities(UUID entityID) {
        containedEntities.add(entityID);
    }

    public void removeFromContainedEntities(UUID entityID) {
        containedEntities.remove(entityID);
    }

    public HashSet<UUID> getBuilders() {
        return builders;
    }

    public void addToBuilders(UUID builderID) {
        builders.add(builderID);
    }

    public void removeFromBuilders(UUID builderID) {
        builders.remove(builderID);
    }

    public int countEntity() {
        entityCount++;
        return entityCount;
    }

    public void resetEntityCount() {
        entityCount = 0;
    }

    public void warnPlayers(String message) {
        final TextComponent actionBarText = Component.text(message).color(NamedTextColor.RED);
        getContainedPlayers().stream()
                .map(Bukkit::getPlayer)
                .forEach(player -> player.sendActionBar(actionBarText));
    }

}
