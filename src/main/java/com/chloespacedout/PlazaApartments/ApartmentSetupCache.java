package com.chloespacedout.PlazaApartments;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApartmentSetupCache {
    private HashMap<String, ApartmentSetup> apartmentSetupCache;

    private Location locationFromConfig(ConfigurationSection parentConfigurationSection, World world, String path) {

        ConfigurationSection configurationSection = parentConfigurationSection.getConfigurationSection(path);

        assert configurationSection != null;

        double x = configurationSection.getDouble("x");
        double y = configurationSection.getDouble("y");
        double z = configurationSection.getDouble("z");
        return new Location (world,x,y,z);
    }

    private Location applyRotationFromConfig(ConfigurationSection parentConfigurationSection, String path, Location location) {

        ConfigurationSection configurationSection = parentConfigurationSection.getConfigurationSection(path);

        assert configurationSection != null;

        float pitch = (float) configurationSection.getDouble("pitch");
        float yaw = (float) configurationSection.getDouble("yaw");
        location.setPitch(pitch);
        location.setYaw(yaw);
        return location;
    }

    private HashMap<String, ApartmentSetup> cacheFromFile(File apartmentsFile, Config config) {
        HashMap<String, ApartmentSetup> newApartmentSetupCache = new HashMap<>();
        YamlConfiguration apartmentSetupConfig = YamlConfiguration.loadConfiguration(apartmentsFile);

        World apartmentWorld = config.getApartmentWorld();
        World mainWorld = config.getMainWorld();

        List<String> apartments = apartmentSetupConfig.getKeys(false).stream().toList();

        for (String apartmentName : apartments) {
            ConfigurationSection apartmentSection = apartmentSetupConfig.getConfigurationSection(apartmentName);

            assert apartmentSection != null;

            Location enterTeleport = locationFromConfig(apartmentSection,apartmentWorld,"enterTeleport.position");
            enterTeleport = applyRotationFromConfig(apartmentSection,"enterTeleport.rotation",enterTeleport);

            Location exitTeleport = locationFromConfig(apartmentSection,mainWorld,"exitTeleport.position");
            exitTeleport = applyRotationFromConfig(apartmentSection,"exitTeleport.rotation",exitTeleport);

            Location regionMin = locationFromConfig(apartmentSection,apartmentWorld,"region.min");
            Location regionMax = locationFromConfig(apartmentSection,apartmentWorld,"region.max");
            Region region = new Region("main",regionMin,regionMax);

            ConfigurationSection noBuildRegionsSection = apartmentSection.getConfigurationSection("noBuildRegions");
            List<String> noBuildRegionIDs = noBuildRegionsSection.getKeys(false).stream().toList();

            List<Region> noBuildRegions = new ArrayList<>();

            for (String buildRegionID : noBuildRegionIDs) {

                Location noBuildRegionMin = locationFromConfig(noBuildRegionsSection,apartmentWorld,buildRegionID + ".min");
                Location noBuildRegionMax = locationFromConfig(noBuildRegionsSection,apartmentWorld,buildRegionID + ".max");
                Region noBuildRegion = new Region(buildRegionID,noBuildRegionMin,noBuildRegionMax);

                noBuildRegions.add(noBuildRegion);

            }

            Location keyBlock = locationFromConfig(apartmentSection,mainWorld,"keyBlock");

            ApartmentSetup apartmentSetup = new ApartmentSetup(apartmentName,enterTeleport,exitTeleport,region,noBuildRegions,keyBlock);
            newApartmentSetupCache.put(apartmentName,apartmentSetup);

        }
        return newApartmentSetupCache;
    }

    ApartmentSetupCache(File apartmentsFile, Config config) {
        apartmentSetupCache = cacheFromFile(apartmentsFile,config);
    }

    public ApartmentSetup getApartmentSetup(String ID) {
        return apartmentSetupCache.get(ID);
    }

    public HashMap<String,ApartmentSetup> getAllApartmentSetups() {
        return apartmentSetupCache;
    }
}
