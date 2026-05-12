package com.chloespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class FileManager {

    private File pluginFolder;
    private ApartmentSetupCache apartmentSetupCache;
    private File apartmentsFolder;
    private File apartmentKeysFile;

    private void createMainDirectory(File newPluginFolder, Config config) throws IOException {
        pluginFolder = newPluginFolder;

        File apartmentSetupFile = new File(pluginFolder,"apartmentSetup.yml");
        apartmentSetupFile.createNewFile();

        apartmentKeysFile = new File(pluginFolder,"apartmentKeys.yml");
        apartmentKeysFile.createNewFile();

        apartmentSetupCache = new ApartmentSetupCache(apartmentSetupFile, config);

        apartmentsFolder = new File(pluginFolder,"apartments");

        apartmentsFolder.mkdir();
    }

    private void createApartmentDirectory(ApartmentSetup apartmentSetup) {

        StructureManager structureManager = Bukkit.getStructureManager();

        Region region = apartmentSetup.getRegion();
        Location regionMin = region.getMin();
        Location regionMax = region.getMax();

        String apartmentName = apartmentSetup.getName();

        File apartmentFolder = new File(pluginFolder,"apartments/" + apartmentName);
        apartmentFolder.mkdir();

        File apartmentStructureFile = new File(apartmentFolder,"defaultApartment.nbt");

        Structure apartmentStructure = structureManager.createStructure();
        apartmentStructure.fill(regionMin,regionMax,false);

        try {
            structureManager.saveStructure(apartmentStructureFile,apartmentStructure);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File userApartmentsFolder = new File(apartmentFolder,"userApartments");
        userApartmentsFolder.mkdir();
    }

    public FileManager(File pluginFolder, Config config) {

        try {
            createMainDirectory(pluginFolder,config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        apartmentSetupCache.getAllApartmentSetups().values().stream()
                .forEach(this::createApartmentDirectory);

    }

    public ApartmentSetupCache getApartmentSetupCache() {
        return apartmentSetupCache;
    }

    public File getApartmentsFolder() {
        return apartmentsFolder;
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

        YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        YamlConfiguration keys = YamlConfiguration.loadConfiguration(apartmentKeysFile);

        HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();

        for (int i = 0; i < apartmentSetups.size(); i++) {
            ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
            String apartmentName = apartmentSetup.getName();
            ConfigurationSection publicKeys = playerData.getConfigurationSection("publicKeys." + apartmentName);
            String publicKey;
            try {
                publicKey = publicKeys.getKeys(false).stream().toList().getFirst();
            } catch (Exception ignored) {
                publicKey = null;
            }

            if (publicKey == null) {
                String keyID = UUID.randomUUID().toString();
                playerData.set("publicKeys." + apartmentName + "." + keyID, 0L);
                keys.set(keyID + ".owner",playerID.toString());
                keys.set(keyID + ".apartment",apartmentName);
                keys.set(keyID + ".perms","enter");
            }

        }

        try {
            playerData.save(playerDataFile);
            keys.save(apartmentKeysFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return playerDataFile;
    }

    public File getApartmentKeysFile() {
        return apartmentKeysFile;
    }
}
