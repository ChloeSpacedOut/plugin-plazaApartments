package com.choespacedout.PlazaApartments;

import com.choespacedout.PlazaApartments.commands.Apartment;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class Core extends JavaPlugin implements Listener {

    Config storedConfig;
    InstanceManager instanceManager;
    ApartmentSetupCache apartmentSetupCache;
    FileUtil fileUtil;
    File apartmentsFolder;


    @Override
    public void onEnable() {

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        FileConfiguration config = this.getConfig();
        int instanceCount = config.getInt("instanceCount");
        World apartmentWorld = Bukkit.getWorld(Objects.requireNonNull(config.getString("apartmentWorld")));
        World mainWorld = Bukkit.getWorld(Objects.requireNonNull(config.getString("mainWorld")));
        storedConfig = new Config(instanceCount,apartmentWorld,mainWorld);

        fileUtil = new FileUtil(this.getDataFolder());

        File apartmentSetupFile = fileUtil.createApartmentSetupFile();

        if (apartmentSetupFile == null) {
            System.out.println("Could not load apartments file! PlazaApartments could not finish loading!");
            return;
        }

        apartmentSetupCache = new ApartmentSetupCache(apartmentSetupFile,storedConfig);

        apartmentsFolder = fileUtil.createApartmentFiles(apartmentSetupCache);
        if (apartmentsFolder == null) {
            System.out.println("Could not save default apartment structure! PlazaApartments could not finish loading!");
            return;
        }

        instanceManager = new InstanceManager(instanceCount,apartmentsFolder);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Apartment.createCommand("apartment",apartmentSetupCache,instanceManager),"Apartment related commands");
        });

        Bukkit.getPluginManager().registerEvents(this,this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player joinPlayer = e.getPlayer();
        if (!joinPlayer.getWorld().equals(storedConfig.getApartmentWorld())) return;

        UUID joinPlayerID = joinPlayer.getUniqueId();

        File playerDataFile = fileUtil.createPlayerDataFile(joinPlayerID);
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
        String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");

        if (lastEnteredApartmentOwner == null || lastEnteredApartmentType == null) {
            final Location mainWorldSpawn = storedConfig.getMainWorld().getSpawnLocation();

            if (joinPlayer.hasPermission("apartments.mannage")) return;

            joinPlayer.teleport(mainWorldSpawn);
            joinPlayer.sendRichMessage("<red>Invalid apartment location detected on join! You have been sent to spawn");
            return;
        }

        UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
        PlayerApartment playerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID);

        if (playerApartment == null) {
            // include checks for if you're in an apartment you're a keyholder for
            ApartmentSetup apartmentSetup = apartmentSetupCache.getApartmentSetup(lastEnteredApartmentType);
            Location abortLocation = apartmentSetup.getExitTeleport();
            if (lastEnteredApartmentOwnerID.equals(joinPlayerID)) {
                boolean hasPreparedInstance = instanceManager.prepareInstance(joinPlayerID,apartmentSetup);
                if (hasPreparedInstance) {
                    playerApartment = instanceManager.getApartment(joinPlayerID);
                    playerApartment.relativeTeleport(joinPlayer);
                } else {
                    joinPlayer.teleport(abortLocation);
                    joinPlayer.sendRichMessage("<red>Something went wrong when setting up your apartment! Please contact server staff!");
                }
            } else {
                joinPlayer.teleport(abortLocation);
                joinPlayer.sendRichMessage("<red>The apartment you were inside is now closed!");
                playerData.set("lastApartmentEntered.owner",null);
                playerData.set("lastApartmentEntered.type",null);
            }

            return;
        };
        if (!playerApartment.getApartmentSetup().getName().equals(lastEnteredApartmentType)) return;

        playerApartment.relativeTeleport(joinPlayer);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitPlayer = e.getPlayer();
        if (!quitPlayer.getWorld().equals(storedConfig.getApartmentWorld())) return;

        UUID quitPlayerID = quitPlayer.getUniqueId();

        File playerDataFile = fileUtil.createPlayerDataFile(quitPlayerID);
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
        String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");

        if (lastEnteredApartmentOwner == null || lastEnteredApartmentType == null) return;

        UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
        PlayerApartment playerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID);

        if (playerApartment == null) return;
        if (!playerApartment.getApartmentSetup().getName().equals(lastEnteredApartmentType)) return;

        playerApartment.removeFromContainedPlayers(quitPlayerID);
        instanceManager.updateInstance(playerApartment);

    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) throws IOException {
        Location teleportLocation = e.getTo();
        World teleportWorld = teleportLocation.getWorld();

        Player teleportPlayer = e.getPlayer();

        File playerDataFile = fileUtil.createPlayerDataFile(teleportPlayer.getUniqueId());
        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String lastEnteredApartmentOwner = playerData.getString("lastApartmentEntered.owner");
        String lastEnteredApartmentType = playerData.getString("lastApartmentEntered.type");

        if (!teleportWorld.equals(storedConfig.getApartmentWorld())) {
            playerData.set("lastApartmentEntered.owner",null);
            playerData.set("lastApartmentEntered.type",null);
            playerData.save(playerDataFile);

            if (lastEnteredApartmentOwner == null || lastEnteredApartmentType == null) return;
            UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
            PlayerApartment playerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID);
            if (playerApartment == null) return;
            if (!playerApartment.getApartmentSetup().getName().equals(lastEnteredApartmentType)) return;
            playerApartment.removeFromContainedPlayers(teleportPlayer.getUniqueId());
            instanceManager.updateInstance(playerApartment);
            return;
        }


        ApartmentUtil apartmentUtil = new ApartmentUtil(storedConfig,instanceManager);
        PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(teleportLocation);
        if (playerApartment == null) {

            if (teleportPlayer.hasPermission("apartments.mannage")) return;

            e.setCancelled(true);

            playerData.set("lastApartmentEntered.owner",null);
            playerData.set("lastApartmentEntered.type",null);
            playerData.save(playerDataFile);

            return;
        }

        if (lastEnteredApartmentOwner != null) {
            boolean hasApartmentOwnerChanged = !UUID.fromString(lastEnteredApartmentOwner).equals(playerApartment.getOwner());
            boolean hasApartmentTypeChanged = !lastEnteredApartmentType.equals(playerApartment.getApartmentSetup().getName());

            if (hasApartmentOwnerChanged || hasApartmentTypeChanged) {
                UUID lastEnteredApartmentOwnerID = UUID.fromString(lastEnteredApartmentOwner);
                PlayerApartment lastPlayerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID);
                if (lastPlayerApartment != null) {
                    lastPlayerApartment.removeFromContainedPlayers(teleportPlayer.getUniqueId());
                    instanceManager.updateInstance(lastPlayerApartment);
                }
            }
        }

        UUID teleportPlayerID = e.getPlayer().getUniqueId();

        playerApartment.addToContainedPlayers(teleportPlayerID);

        playerData.set("lastApartmentEntered.owner",playerApartment.getOwner().toString());
        playerData.set("lastApartmentEntered.type",playerApartment.getApartmentSetup().getName());

        playerData.save(playerDataFile);

    }
}
