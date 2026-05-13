package com.chloespacedout.PlazaApartments;

import com.chloespacedout.PlazaApartments.commands.Apartment;
import com.chloespacedout.PlazaApartments.commands.ApartmentLeave;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public final class Core extends JavaPlugin implements Listener {

    InstanceManager instanceManager;
    WeatherController weatherController;
    ApartmentSetupCache apartmentSetupCache;
    FileManager fileManager;
    Config config;


    @Override
    public void onEnable() {

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        FileConfiguration configFile = this.getConfig();
        int instanceCount = configFile.getInt("instanceCount");
        int maxEntitiesPerInstance = configFile.getInt("maxEntitiesPerInstance");
        World apartmentWorld = Bukkit.getWorld(Objects.requireNonNull(configFile.getString("apartmentWorld")));
        World mainWorld = Bukkit.getWorld(Objects.requireNonNull(configFile.getString("mainWorld")));
        config = new Config(instanceCount,maxEntitiesPerInstance,apartmentWorld,mainWorld);

        fileManager = new FileManager(this.getDataFolder(),config);

        apartmentSetupCache = fileManager.getApartmentSetupCache();

        File apartmentsFolder = fileManager.getApartmentsFolder();


        WorldGuardManager worldGuardManager = new WorldGuardManager(config,apartmentSetupCache);
        try {
            worldGuardManager.regionSetup();
        } catch (ProtectedRegion.CircularInheritanceException e) {
            throw new RuntimeException(e);
        }

        instanceManager = new InstanceManager(config,apartmentsFolder,fileManager,apartmentSetupCache,worldGuardManager);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Apartment.createCommand("apartment",this,apartmentSetupCache,instanceManager,fileManager,config),"Apartment related commands");
            commands.registrar().register(ApartmentLeave.createCommand("apartmentLeave",new ApartmentUtil(config,instanceManager)));
        });

        weatherController = new WeatherController(this, config,instanceManager);
        new PlayerHandler(this, config,instanceManager,fileManager,apartmentSetupCache,worldGuardManager);
        new EntityHandler(this,config,instanceManager);
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
            weatherController.validateTimeOfDay();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        if (player.getWorld().equals(config.getApartmentWorld())) {
            if (item != null && item.getType() == Material.ENDER_EYE) { // temp fix for ender eyes
                e.setCancelled(true);
                player.sendRichMessage("<red>You may not use ender eyes");
            }
        }

        HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();
        NamespacedKey key = new NamespacedKey(this,"keyID");

        for (int i = 0; i < apartmentSetups.size(); i++) {
            ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
            Location keyBlock = apartmentSetup.getKeyBlock();
            if (e.getClickedBlock() != null && e.getClickedBlock().getLocation().equals(keyBlock)) {

                e.setCancelled(true);

                if (!player.hasPermission("apartments.use")) continue;

                if (item == null) continue;
                if (item.getType() != Material.PLAYER_HEAD) continue;

                PersistentDataContainerView dataContainer = item.getPersistentDataContainer();
                String keyID = dataContainer.get(key, PersistentDataType.STRING);

                if (keyID == null) continue;

                File apartmentKeysFile = fileManager.getApartmentKeysFile();
                YamlConfiguration apartmentKeys = YamlConfiguration.loadConfiguration(apartmentKeysFile);

                String ownerIDString = apartmentKeys.getString(keyID + ".owner");

                if (ownerIDString == null) {
                    player.sendRichMessage("<red>This key's locks have been changed!");
                    player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,1.0F,1.0F);
                    continue;
                }

                UUID ownerID = UUID.fromString(ownerIDString);
                ApartmentSetup apartment = apartmentSetupCache.getApartmentSetup(apartmentKeys.getString(keyID + ".apartment"));
                String perms = apartmentKeys.getString(keyID + ".perms");
                String keyHolderString = apartmentKeys.getString(keyID + ".keyHolder");



                UUID keyHolder = null;
                if (keyHolderString != null) {
                    keyHolder = UUID.fromString(keyHolderString);
                }

                if (!apartmentSetup.getName().equals(apartment.getName())) {
                    player.sendRichMessage("<red>This key is not for this apartment!");
                    player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,1.0F,1.0F);
                    continue;
                }

                if (keyHolder != null && !keyHolder.equals(player.getUniqueId())) {
                    String playerName = Bukkit.getOfflinePlayer(keyHolder).getName();
                    player.sendRichMessage("<red>This key belongs to " + playerName + "!");
                    player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,1.0F,1.0F);
                    continue;
                }

                PlayerApartment playerApartment = instanceManager.getApartment(ownerID,apartmentSetup.getName());

                if (playerApartment != null) {
                    playerApartment.teleport(player);
                } else {
                    boolean hasPreparedInstance = instanceManager.prepareInstance(ownerID,apartment);
                    if (hasPreparedInstance) {
                        playerApartment = instanceManager.getApartment(ownerID,apartmentSetup.getName());
                        playerApartment.teleport(player);
                    } else {
                        player.sendRichMessage("<red>Something went wrong when loading the apartment!");
                        continue;
                    }
                }

                File playerDataFile = fileManager.getPlayerDataFile(player.getUniqueId());
                YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
                playerData.set("lastApartmentEntered.perms",perms);

                File ownerDataFile = fileManager.getPlayerDataFile(ownerID);
                YamlConfiguration ownerData = YamlConfiguration.loadConfiguration(ownerDataFile);
                Long currentTime = System.currentTimeMillis();

                if (keyHolder == null) {
                    ownerData.set("publicKeys." + apartment.getName() + "." + keyID,currentTime);
                } else {
                    ownerData.set("privateKeys." + apartment.getName() + "." + keyID,currentTime);
                }

                try {
                    playerData.save(playerDataFile);
                    ownerData.save(ownerDataFile);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            }
        }
    }

}
