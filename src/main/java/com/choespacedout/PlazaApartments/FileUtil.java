package com.choespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class FileUtil {

    private File pluginFolder;

    public FileUtil(File newPluginFolder) {
        pluginFolder = newPluginFolder;
    }

    public File createApartmentFiles(ApartmentSetupCache apartmentSetupCache) {

        File apartmentsFolder = new File(pluginFolder,"apartments");

        apartmentsFolder.mkdir();

        HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();

        StructureManager structureManager = Bukkit.getStructureManager();

        for (int i = 0; i < apartmentSetups.size(); i++) {
            ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
            Location regionMin = apartmentSetup.getRegionMin();
            Location regionMax = apartmentSetup.getRegionMax();

            String apartmentName = apartmentSetup.getName();

            File apartmentFolder = new File(pluginFolder,"apartments/" + apartmentName);
            apartmentFolder.mkdir();

            File apartmentStructureFile = new File(apartmentFolder,"defaultApartment.nbt");

            Structure apartmentStructure = structureManager.createStructure();
            apartmentStructure.fill(regionMin,regionMax,false);

            try {
                structureManager.saveStructure(apartmentStructureFile,apartmentStructure);
            } catch (IOException e) {
                return null;
            }

            File userApartmentsFolder = new File(apartmentFolder,"userApartments");
            userApartmentsFolder.mkdir();

        }
        return apartmentsFolder;
    }

    public File createApartmentSetupFile() {
        File apartmentSetupFile = new File(pluginFolder,"apartmentSetup.yml");

        try {
            apartmentSetupFile.createNewFile();
        } catch (IOException e) {
            return null;
        }
        return apartmentSetupFile;
    }

    public File getPlayerDataFile(UUID playerID) {
        File playerDataFolder = new File(pluginFolder,"playerData");
        playerDataFolder.mkdir();

        File playerDataFile = new File(playerDataFolder,playerID + ".yml");

        try {
            playerDataFile.createNewFile();
        } catch (IOException e) {
            return null;
        }

        return playerDataFile;
    }

}
