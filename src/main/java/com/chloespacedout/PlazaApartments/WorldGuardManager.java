package com.chloespacedout.PlazaApartments;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldGuardManager {
    RegionManager regions;
    Config config;
    ApartmentSetupCache apartmentSetupCache;

    WorldGuardManager(Config newConfig, ApartmentSetupCache newApartmentSetupCache) {
        config = newConfig;
        apartmentSetupCache = newApartmentSetupCache;

        WorldGuard worldGuard = WorldGuard.getInstance();
        RegionContainer container = worldGuard.getPlatform().getRegionContainer();

        World apartmentWorld = BukkitAdapter.adapt(config.getApartmentWorld());

        regions = container.get(apartmentWorld);

    }

    private BlockVector3 locationToApartmentBlockVector(Location location, int instanceID) {
        Location apartmentLocation = location.clone().add(0,0,1024 + (instanceID * 1024));
        return BlockVector3.at(apartmentLocation.getBlockX(),apartmentLocation.getBlockY(),apartmentLocation.getBlockZ());
    }

    public void regionSetup() throws ProtectedRegion.CircularInheritanceException {
        GlobalProtectedRegion apartmentTemplate = (GlobalProtectedRegion) regions.getRegion("apartment_template");
        if (apartmentTemplate == null) {
            apartmentTemplate = new GlobalProtectedRegion("apartment_template");
            regions.addRegion(apartmentTemplate);
        }

        GlobalProtectedRegion outerApartmentTemplate = (GlobalProtectedRegion) regions.getRegion("outer_apartment_template");
        if (outerApartmentTemplate == null) {
            outerApartmentTemplate = new GlobalProtectedRegion("outer_apartment_template");
            regions.addRegion(outerApartmentTemplate);
        }

        GlobalProtectedRegion noBuildTemplate = (GlobalProtectedRegion) regions.getRegion("no_build_template");
        if (noBuildTemplate == null) {
            noBuildTemplate = new GlobalProtectedRegion("no_build_template");
            regions.addRegion(noBuildTemplate);
        }

        int instanceCount = config.getInstanceCount();

        Map<String,ProtectedRegion> allRegions = regions.getRegions();
        allRegions.keySet().stream()
                .filter(string -> !string.equals("__global__") && !string.equals("apartment_template") && !string.equals("outer_apartment_template") && !string.equals("no_build_template"))
                .forEach(regions::removeRegion);

        for (int instanceID = 0; instanceID < instanceCount; instanceID++) {
            HashMap<String,ApartmentSetup> apartmentSetups = apartmentSetupCache.getAllApartmentSetups();
            for (int i = 0; i < apartmentSetups.size(); i++) {
                ApartmentSetup apartmentSetup = (ApartmentSetup) apartmentSetups.values().toArray()[i];
                String apartmentName = apartmentSetup.getName();

                String regionID = "apartment_" + apartmentName + "_" + instanceID;

                Region region = apartmentSetup.getRegion();

                BlockVector3 pos1 = locationToApartmentBlockVector(region.getMin(),instanceID);
                BlockVector3 pos2 = locationToApartmentBlockVector(region.getMax(),instanceID).add(-1,-1,-1);

                ProtectedCuboidRegion mainProtectedRegion = new ProtectedCuboidRegion(regionID,pos1,pos2);
                mainProtectedRegion.setParent(apartmentTemplate);
                regions.addRegion(mainProtectedRegion);

                ProtectedCuboidRegion outerProtectedRegion = new ProtectedCuboidRegion(regionID + "_outer",pos1.add(-1,-1,-1),pos2.add(1,1,1));

                outerProtectedRegion.setParent(outerApartmentTemplate);
                regions.addRegion(outerProtectedRegion);

                List<Region> noBuildRegions = apartmentSetup.getNoBuildRegions();

                for (Region noBuildRegion : noBuildRegions) {
                    String noBuildRegionID = regionID + "_" + noBuildRegion.getRegionID();
                    BlockVector3 noBuildPos1 = locationToApartmentBlockVector(noBuildRegion.getMin(),instanceID);
                    BlockVector3 noBuildPos2 = locationToApartmentBlockVector(noBuildRegion.getMax(),instanceID).add(-1,-1,-1);
                    ProtectedCuboidRegion noBuildProtectedRegion = new ProtectedCuboidRegion(noBuildRegionID,noBuildPos1,noBuildPos2);
                    noBuildProtectedRegion.setParent(noBuildTemplate);
                    regions.addRegion(noBuildProtectedRegion);
                }
            }
        }
    }

    public void updateBuilders(PlayerApartment playerApartment) {
        String apartmentName = playerApartment.getApartmentSetup().getName();
        int instanceID = playerApartment.getInstanceID();
        String regionID = "apartment_" + apartmentName + "_" + instanceID;
        ProtectedCuboidRegion region = (ProtectedCuboidRegion) regions.getRegion(regionID);
        DefaultDomain defaultDomain = new DefaultDomain();

        playerApartment.getBuilders().forEach(defaultDomain::addPlayer);

        assert region != null;
        region.setMembers(defaultDomain);
    }
}
