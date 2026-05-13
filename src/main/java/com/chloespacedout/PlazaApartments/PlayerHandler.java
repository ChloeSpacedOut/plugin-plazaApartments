package com.chloespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class PlayerHandler implements Listener {

    private final Config config;
    private final InstanceManager instanceManager;
    private final FileManager fileManager;
    private final ApartmentSetupCache apartmentSetupCache;
    private final WorldGuardManager worldGuardManager;

    public PlayerHandler(Core pluginInstance, Config newConfig, InstanceManager newInstanceManager, FileManager newFileManager, ApartmentSetupCache newApartmentSetupCache, WorldGuardManager newWorldGuardManager) {
        config = newConfig;
        instanceManager = newInstanceManager;
        fileManager = newFileManager;
        apartmentSetupCache = newApartmentSetupCache;
        worldGuardManager = newWorldGuardManager;
        Bukkit.getPluginManager().registerEvents(this,pluginInstance);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) throws IOException {
        Player joinPlayer = e.getPlayer();
        if (!joinPlayer.getWorld().equals(config.getApartmentWorld())) return;

        UUID joinPlayerID = joinPlayer.getUniqueId();

        instanceManager.validateLoadedEntities(joinPlayerID);

        File playerDataFile = fileManager.getPlayerDataFile(joinPlayerID);
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
        String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");
        String lastEnteredApartmentPerms = playerData.getString("lastApartmentEntered.perms");

        if (lastEnteredApartmentOwner == null || lastEnteredApartmentType == null) {
            final Location mainWorldSpawn = config.getMainWorld().getSpawnLocation();

            if (joinPlayer.hasPermission("apartments.mannage")) return;

            joinPlayer.teleport(mainWorldSpawn);
            joinPlayer.sendRichMessage("<red>Invalid apartment location detected on join! You have been sent to spawn");
            return;
        }

        UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
        PlayerApartment playerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID,lastEnteredApartmentType);

        if (playerApartment == null) {
            ApartmentSetup apartmentSetup = apartmentSetupCache.getApartmentSetup(lastEnteredApartmentType);
            Location abortLocation = apartmentSetup.getExitTeleport();

            boolean hasEnterPerms = Objects.equals(lastEnteredApartmentPerms, "enter");
            boolean hasBuildPerms = Objects.equals(lastEnteredApartmentPerms, "build");

            if (lastEnteredApartmentOwnerID.equals(joinPlayerID) || hasEnterPerms || hasBuildPerms) {
                boolean hasPreparedInstance = instanceManager.prepareInstance(lastEnteredApartmentOwnerID,apartmentSetup);
                if (hasPreparedInstance) {
                    playerApartment = instanceManager.getApartment(joinPlayerID,apartmentSetup.getName());
                    if (hasBuildPerms) {
                        playerApartment.addToBuilders(joinPlayerID);
                        worldGuardManager.updateBuilders(playerApartment);
                    }
                    playerApartment.relativeTeleport(joinPlayer);
                } else {
                    joinPlayer.teleport(abortLocation);
                    joinPlayer.sendRichMessage("<red>Something went wrong when setting up your apartment! Please contact server staff!");
                }
            } else {
                joinPlayer.teleport(abortLocation);
                joinPlayer.sendRichMessage("<red>The apartment you were inside is now closed!");
                playerData.set("lastApartmentEntered",null);
                playerData.save(playerDataFile);
            }

            return;
        };
        if (!playerApartment.getApartmentSetup().getName().equals(lastEnteredApartmentType)) return;

        playerApartment.relativeTeleport(joinPlayer);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitPlayer = e.getPlayer();
        if (!quitPlayer.getWorld().equals(config.getApartmentWorld())) return;

        UUID quitPlayerID = quitPlayer.getUniqueId();

        File playerDataFile = fileManager.getPlayerDataFile(quitPlayerID);
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
        String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");

        if (lastEnteredApartmentOwner == null || lastEnteredApartmentType == null ) return;

        UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
        PlayerApartment playerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID,lastEnteredApartmentType);

        if (playerApartment == null) return;
        if (!playerApartment.getApartmentSetup().getName().equals(lastEnteredApartmentType)) return;

        playerApartment.removeFromContainedPlayers(quitPlayerID);
        instanceManager.updateInstance(playerApartment);

        instanceManager.validateLoadedEntities(quitPlayerID);

    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) throws IOException {
        Location teleportLocation = e.getTo();
        World teleportWorld = teleportLocation.getWorld();

        Player teleportPlayer = e.getPlayer();
        UUID teleportPlayerID = teleportPlayer.getUniqueId();

        File playerDataFile = fileManager.getPlayerDataFile(teleportPlayer.getUniqueId());
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
        String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");
        String lastEnteredApartmentPerms = playerData.getString("lastApartmentEntered.perms");

        if (!teleportWorld.equals(config.getApartmentWorld())) {
            playerData.set("lastApartmentEntered",null);
            playerData.save(playerDataFile);

            if (lastEnteredApartmentOwner == null || lastEnteredApartmentType == null) return;
            UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
            PlayerApartment playerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID,lastEnteredApartmentType);
            if (playerApartment == null) return;
            if (!playerApartment.getApartmentSetup().getName().equals(lastEnteredApartmentType)) return;
            playerApartment.removeFromContainedPlayers(teleportPlayer.getUniqueId());
            instanceManager.updateInstance(playerApartment);
            instanceManager.validateLoadedEntities(teleportPlayerID);
            return;
        }

        instanceManager.validateLoadedEntities(teleportPlayerID); // remove this

        ApartmentUtil apartmentUtil = new ApartmentUtil(config,instanceManager);
        PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(teleportLocation);
        if (playerApartment == null) {

            if (teleportPlayer.hasPermission("apartments.mannage")) return;
            e.setCancelled(true);

            return;
        }

        if (lastEnteredApartmentOwner != null && lastEnteredApartmentType != null) {
            boolean hasApartmentOwnerChanged = !UUID.fromString(lastEnteredApartmentOwner).equals(playerApartment.getOwner());
            boolean hasApartmentTypeChanged = !lastEnteredApartmentType.equals(playerApartment.getApartmentSetup().getName());

            if (hasApartmentOwnerChanged || hasApartmentTypeChanged) {
                UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
                PlayerApartment lastPlayerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID,lastEnteredApartmentType);
                if (lastPlayerApartment != null) {
                    lastPlayerApartment.removeFromContainedPlayers(teleportPlayerID);
                    instanceManager.updateInstance(lastPlayerApartment);
                    instanceManager.validateLoadedEntities(teleportPlayerID);
                }
            }
        }

        playerApartment.addToContainedPlayers(teleportPlayerID);

        boolean hasBuildPerms = Objects.equals(lastEnteredApartmentPerms, "build");

        if (hasBuildPerms) {
            playerApartment.addToBuilders(teleportPlayerID);
            worldGuardManager.updateBuilders(playerApartment);
        }

        playerData.set("lastApartmentEntered.owner",playerApartment.getOwner().toString());
        playerData.set("lastApartmentEntered.type",playerApartment.getApartmentSetup().getName());

        playerData.save(playerDataFile);

    }
}
