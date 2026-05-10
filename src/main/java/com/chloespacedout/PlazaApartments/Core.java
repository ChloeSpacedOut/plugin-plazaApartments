package com.chloespacedout.PlazaApartments;

import com.chloespacedout.PlazaApartments.commands.Apartment;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public final class Core extends JavaPlugin implements Listener {

    Config config;
    InstanceManager instanceManager;
    ApartmentSetupCache apartmentSetupCache;
    FileUtil fileUtil;
    File apartmentsFolder;
    WeatherController weatherController;


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

        fileUtil = new FileUtil(this.getDataFolder());

        File apartmentSetupFile = fileUtil.createApartmentSetupFile();

        if (apartmentSetupFile == null) {
            System.out.println("Could not load apartments file! PlazaApartments could not finish loading!");
            return;
        }

        apartmentSetupCache = new ApartmentSetupCache(apartmentSetupFile, config);


        WorldGuardManager worldGuardManager = new WorldGuardManager(config,apartmentSetupCache);
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

        instanceManager = new InstanceManager(config,apartmentsFolder,fileUtil,apartmentSetupCache,worldGuardManager);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Apartment.createCommand("apartment",apartmentSetupCache,instanceManager),"Apartment related commands");
        });

        weatherController = new WeatherController(this, config,instanceManager);
        new PlayerHandler(this, config,instanceManager,fileUtil,apartmentSetupCache);
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

}
