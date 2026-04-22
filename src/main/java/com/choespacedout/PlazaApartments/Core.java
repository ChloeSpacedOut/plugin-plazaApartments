package com.choespacedout.PlazaApartments;

import com.choespacedout.PlazaApartments.commands.Apartment;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public final class Core extends JavaPlugin {

    @Override
    public void onEnable() {

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        File apartmentSetupFile = new File(getDataFolder(),"apartmentSetup.yml");
        if (!apartmentSetupFile.exists()) {
            try {
                apartmentSetupFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Could not load apartments file! PlazaApartments could not finish loading!");
                return;
            }
        }

        ApartmentSetupCache apartmentSetupCache = new ApartmentSetupCache(apartmentSetupFile,this.getConfig());

        String pluginFolderPath = this.getDataFolder().getAbsolutePath();
        File apartmentsFolder = new File(pluginFolderPath + "/apartments");

        apartmentsFolder.mkdir();

        HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();

        StructureManager structureManager = Bukkit.getStructureManager();

        for (int i = 0; i < apartmentSetups.size(); i++) {
            ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
            Location regionMin = apartmentSetup.getRegionMin();
            Location regionMax = apartmentSetup.getRegionMax();

            String apartmentName = apartmentSetup.getName();

            File apartmentFolder = new File(pluginFolderPath + "/apartments/" + apartmentName);
            apartmentFolder.mkdir();

            File apartmentStructureFile = new File(pluginFolderPath + "/apartments/" + apartmentName + "/defaultApartment.nbt");

            Structure apartmentStructure = structureManager.createStructure();
            apartmentStructure.fill(regionMin,regionMax,false);

            try {
                structureManager.saveStructure(apartmentStructureFile,apartmentStructure);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            File userApartmentsFolder = new File(pluginFolderPath + "/apartments/" + apartmentName + "/userApartments");
            userApartmentsFolder.mkdir();

        }

        FileConfiguration config = this.getConfig();
        int instanceCount = config.getInt("instanceCount");
        InstanceManager instanceManager = new InstanceManager(apartmentSetupCache,instanceCount,pluginFolderPath + "/apartments/");

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Apartment.createCommand("apartment",apartmentSetupCache,instanceManager),"Apartment related commands");
        });
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
