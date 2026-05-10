package com.chloespacedout.PlazaApartments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.HashMap;

public class WeatherController implements Listener {

    private Config config;
    private InstanceManager instanceManager;

    public WeatherController(Core pluginInstance, Config newConfig, InstanceManager newInstanceManager) {
        config = newConfig;
        instanceManager = newInstanceManager;
        Bukkit.getPluginManager().registerEvents(this,pluginInstance);
    }

    public void validateTimeOfDay() {
        long mainWorldTime = config.getMainWorld().getTime();
        config.getApartmentWorld().setTime(mainWorldTime);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (!e.getWorld().equals(config.getMainWorld())) return;

        boolean isToRain = e.toWeatherState();
        config.getApartmentWorld().setStorm(isToRain);
        if (isToRain) {
            config.getApartmentWorld().setClearWeatherDuration(0);
            config.getApartmentWorld().setWeatherDuration(999999999);
        } else {
            config.getApartmentWorld().setClearWeatherDuration(999999999);
            config.getApartmentWorld().setWeatherDuration(0);
        }
    }

    @EventHandler
    public void onWorldThunder(ThunderChangeEvent e) {
        if (!e.getWorld().equals(config.getMainWorld())) return;

        boolean isToThunder = e.toThunderState();
        config.getApartmentWorld().setThundering(isToThunder);
    }

    @EventHandler
    public void onWorldLightning(LightningStrikeEvent e) {
        if (!e.getWorld().equals(config.getMainWorld())) return;

        World apartmentWorld = config.getApartmentWorld();

        HashMap<Integer,PlayerApartment> apartmentInstances = instanceManager.getApartmentInstances();

        for (int i = 0; i < apartmentInstances.size(); i++) {

            PlayerApartment playerApartment = (PlayerApartment) apartmentInstances.values().toArray()[i];
            int instanceID = playerApartment.getInstanceID();
            Location lightningLocation = playerApartment.getApartmentSetup().getEnterTeleport().clone().add(0,128,1024 + (instanceID * 1024));
            apartmentWorld.strikeLightningEffect(lightningLocation);
        }
    }
}
