package com.chloespacedout.PlazaApartments;

import com.chloespacedout.PlazaApartments.commands.Apartment;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
        int maxEntitiesPerInstance = config.getInt("maxEntitiesPerInstance");
        World apartmentWorld = Bukkit.getWorld(Objects.requireNonNull(config.getString("apartmentWorld")));
        World mainWorld = Bukkit.getWorld(Objects.requireNonNull(config.getString("mainWorld")));
        storedConfig = new Config(instanceCount,maxEntitiesPerInstance,apartmentWorld,mainWorld);

        fileUtil = new FileUtil(this.getDataFolder());

        File apartmentSetupFile = fileUtil.createApartmentSetupFile();

        if (apartmentSetupFile == null) {
            System.out.println("Could not load apartments file! PlazaApartments could not finish loading!");
            return;
        }

        apartmentSetupCache = new ApartmentSetupCache(apartmentSetupFile,storedConfig);


        WorldGuardManager worldGuardManager = new WorldGuardManager(storedConfig,apartmentSetupCache);
        try {
            worldGuardManager.regionSetup();
        } catch (ProtectedRegion.CircularInheritanceException e) {
            throw new RuntimeException(e);
        }

        apartmentsFolder = fileUtil.createApartmentFiles(apartmentSetupCache);
        if (apartmentsFolder == null) {
            System.out.println("Could not save default apartment structure! PlazaApartments could not finish loading!");
            return;
        }

        instanceManager = new InstanceManager(storedConfig,apartmentsFolder,fileUtil,apartmentSetupCache);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Apartment.createCommand("apartment",apartmentSetupCache,instanceManager),"Apartment related commands");
        });

        Bukkit.getPluginManager().registerEvents(this,this);

    }

    @Override
    public void onDisable() {
        HashMap<Integer,PlayerApartment> instances = instanceManager.getApartmentInstances();
        for (int i = 0; i < instances.size(); i++) {
            PlayerApartment instance = (PlayerApartment) instances.values().toArray()[i];
            instanceManager.closeInstance(instance);
        }
    }

    @EventHandler
    public void onServerTick(ServerTickStartEvent e) {
        int tick = e.getTickNumber();
        if (tick % 1200 == 0) {
            HashMap<Integer,PlayerApartment> apartmentInstances = instanceManager.getApartmentInstances();
            for (int i = 0; i < apartmentInstances.size(); i++) {
                PlayerApartment instance = (PlayerApartment) apartmentInstances.values().toArray()[i];
                instanceManager.saveInstance(instance);
            }

            instanceManager.validateLoadedEntities(null);

            long mainWorldTime = storedConfig.getMainWorld().getTime();
            storedConfig.getApartmentWorld().setTime(mainWorldTime);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        Location spawnLocation = e.getLocation();
        ApartmentUtil apartmentUtil = new ApartmentUtil(storedConfig,instanceManager);
        PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(spawnLocation);
        if (playerApartment != null) {
            final int entitiesInInstance = playerApartment.getContainedEntities().size();
            final int entityLimit = storedConfig.getMaxEntitiesPerInstance();

            final TextComponent actionBarText = Component.text("Entity limit of " + entityLimit + " reached!").color(NamedTextColor.RED);

            if (entitiesInInstance >= entityLimit) {
                e.setCancelled(true);
                playerApartment.getContainedPlayers().stream()
                        .map(Bukkit::getPlayer)
                        .forEach(player -> player.sendActionBar(actionBarText));
                return;
            }
            entity.addScoreboardTag("apartmentEntity+owner=" + playerApartment.getOwner());
            playerApartment.addToContainedEntities(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent e) {
        Entity entity = e.getEntity();
        Location entityLocation = entity.getLocation();
        ApartmentUtil apartmentUtil = new ApartmentUtil(storedConfig,instanceManager);
        PlayerApartment playerApartment = apartmentUtil.apartmentFromLocation(entityLocation);
        if (playerApartment != null) {
            playerApartment.removeFromContainedEntities(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (!e.getWorld().equals(storedConfig.getMainWorld())) return;

        boolean isToRain = e.toWeatherState();
        storedConfig.getApartmentWorld().setStorm(isToRain);
        if (isToRain) {
            storedConfig.getApartmentWorld().setClearWeatherDuration(0);
            storedConfig.getApartmentWorld().setWeatherDuration(999999999);
        } else {
            storedConfig.getApartmentWorld().setClearWeatherDuration(999999999);
            storedConfig.getApartmentWorld().setWeatherDuration(0);
        }
    }

    @EventHandler
    public void onWorldThunder(ThunderChangeEvent e) {
        if (!e.getWorld().equals(storedConfig.getMainWorld())) return;

        boolean isToThunder = e.toThunderState();
        storedConfig.getApartmentWorld().setThundering(isToThunder);
    }

    @EventHandler
    public void onWorldLightning(LightningStrikeEvent e) {
        if (!e.getWorld().equals(storedConfig.getMainWorld())) return;

        World apartmentWorld = storedConfig.getApartmentWorld();

        HashMap <Integer,PlayerApartment> apartmentInstances = instanceManager.getApartmentInstances();

        for (int i = 0; i < apartmentInstances.size(); i++) {

            PlayerApartment playerApartment = (PlayerApartment) apartmentInstances.values().toArray()[i];
            int instanceID = playerApartment.getInstanceID();
            Location lightningLocation = playerApartment.getApartmentSetup().getEnterTeleport().clone().add(0,128,1024 + (instanceID * 1024));
            apartmentWorld.strikeLightningEffect(lightningLocation);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player joinPlayer = e.getPlayer();
        if (!joinPlayer.getWorld().equals(storedConfig.getApartmentWorld())) return;

        UUID joinPlayerID = joinPlayer.getUniqueId();

        instanceManager.validateLoadedEntities(joinPlayerID);

        File playerDataFile = fileUtil.getPlayerDataFile(joinPlayerID);
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

        File playerDataFile = fileUtil.getPlayerDataFile(quitPlayerID);
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

        instanceManager.validateLoadedEntities(quitPlayerID);

    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) throws IOException {
        Location teleportLocation = e.getTo();
        World teleportWorld = teleportLocation.getWorld();

        Player teleportPlayer = e.getPlayer();
        UUID teleportPlayerID = teleportPlayer.getUniqueId();

        File playerDataFile = fileUtil.getPlayerDataFile(teleportPlayer.getUniqueId());
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
            instanceManager.validateLoadedEntities(teleportPlayerID);
            return;
        }

        instanceManager.validateLoadedEntities(teleportPlayerID); // remove this

        ApartmentUtil apartmentUtil = new ApartmentUtil(storedConfig,instanceManager);
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
                PlayerApartment lastPlayerApartment = instanceManager.getApartment(lastEnteredApartmentOwnerID);
                if (lastPlayerApartment != null) {
                    lastPlayerApartment.removeFromContainedPlayers(teleportPlayerID);
                    instanceManager.updateInstance(lastPlayerApartment);
                    instanceManager.validateLoadedEntities(teleportPlayerID);
                }
            }
        }

        playerApartment.addToContainedPlayers(teleportPlayerID);

        playerData.set("lastApartmentEntered.owner",playerApartment.getOwner().toString());
        playerData.set("lastApartmentEntered.type",playerApartment.getApartmentSetup().getName());

        playerData.save(playerDataFile);

    }
}
