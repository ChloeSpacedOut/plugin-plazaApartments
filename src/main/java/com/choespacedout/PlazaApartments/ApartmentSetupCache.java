package com.choespacedout.PlazaApartments;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ApartmentSetupCache {
    private HashMap<String, ApartmentSetup> apartmentSetupCache;

    private List<Double> coordinateFromConfig(ConfigurationSection configurationSection) {
        double x = configurationSection.getDouble("x");
        double y = configurationSection.getDouble("y");
        double z = configurationSection.getDouble("z");
        List<Double> coordinate = new ArrayList<>();
        coordinate.add(x);
        coordinate.add(y);
        coordinate.add(z);
        return coordinate;
    }

    private List<Float> rotationFromConfig(ConfigurationSection configurationSection) {
        double pitch = configurationSection.getDouble("pitch");
        double yaw = configurationSection.getDouble("yaw");
        List<Float> rotation = new ArrayList<>();
        rotation.add((float) pitch);
        rotation.add((float) yaw);
        return rotation;
    }

    private BoundingBox areaToBoundingBox(List<Double> areaMin, List<Double> areaMax) {
        double xDistanceToCentre = (areaMax.get(0) - areaMin.get(0)) /2;
        double yDistanceToCentre = (areaMax.get(1) - areaMin.get(1)) /2;
        double zDistanceToCentre = (areaMax.get(2) - areaMin.get(2)) /2;

        BoundingBox boundingBox = new BoundingBox().expand(xDistanceToCentre, yDistanceToCentre, zDistanceToCentre);
        boundingBox.shift(areaMax.get(0) - xDistanceToCentre,areaMax.get(1) - yDistanceToCentre,areaMax.get(2) - zDistanceToCentre);
        return boundingBox;
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
            List<Double> enterTeleportPosition = coordinateFromConfig(Objects.requireNonNull(apartmentSection.getConfigurationSection("enterTeleport.position")));
            List<Float> enterTeleportRotation = rotationFromConfig(Objects.requireNonNull(apartmentSection.getConfigurationSection("enterTeleport.rotation")));
            List<Double> exitTeleportPosition = coordinateFromConfig(Objects.requireNonNull(apartmentSection.getConfigurationSection("exitTeleport.position")));
            List<Float> exitTeleportRotation = rotationFromConfig(Objects.requireNonNull(apartmentSection.getConfigurationSection("exitTeleport.rotation")));
            List<Double> regionMin = coordinateFromConfig(Objects.requireNonNull(apartmentSection.getConfigurationSection("region.min")));
            List<Double> regionMax = coordinateFromConfig(Objects.requireNonNull(apartmentSection.getConfigurationSection("region.max")));

            Location regionMinLocation = new Location (apartmentWorld,regionMin.get(0),regionMin.get(1),regionMin.get(2));
            Location regionMaxLocation = new Location (apartmentWorld,regionMax.get(0) + 1,regionMax.get(1) + 1,regionMax.get(2) + 1);
            Location enterTeleport = new Location(apartmentWorld,enterTeleportPosition.get(0),enterTeleportPosition.get(1),enterTeleportPosition.get(2),enterTeleportRotation.get(0),enterTeleportRotation.get(1));
            Location exitTeleport = new Location(mainWorld,exitTeleportPosition.get(0),exitTeleportPosition.get(1),exitTeleportPosition.get(2),exitTeleportRotation.get(0),exitTeleportRotation.get(1));
            BoundingBox region = areaToBoundingBox(regionMin,regionMax);

            ApartmentSetup apartmentSetup = new ApartmentSetup(apartmentName,enterTeleport,exitTeleport,regionMinLocation,regionMaxLocation,region);
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
