package dev._2lstudios.elevators;

import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

import dev._2lstudios.elevators.config.MessagesConfig;
import dev._2lstudios.elevators.listeners.PlayerInteractListener;

public class Elevators extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        Server server = getServer();
        Configuration config = getConfig();
        MessagesConfig messagesConfig = new MessagesConfig(config);
        ElevatorsHandler elevatorsHandler = new ElevatorsHandler(messagesConfig);

        Elevators.instance = this;

        server.getPluginManager().registerEvents(new PlayerInteractListener(elevatorsHandler), this);
    }

    private static Elevators instance;

    public static Elevators getInstance() {
        return Elevators.instance;
    }
}