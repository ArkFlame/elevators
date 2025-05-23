package com.arkflame.elevators;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Elevators extends JavaPlugin implements Listener {

    private Map<UUID, Long> cooldowns = new HashMap<>();
    private FileConfiguration config;

    // Configuration values
    private long cooldownTime;
    private String upMessage;
    private String downMessage;
    private String cooldownMessage;
    private String noElevatorMessage;
    private String unsafeLocationMessage;
    private int maxSearchHeight;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        loadConfig();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Elevators plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Elevators plugin has been disabled!");
    }

    private void loadConfig() {
        config = getConfig();

        // Load configuration values with defaults
        cooldownTime = config.getLong("cooldown-seconds", 3) * 1000L;
        upMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.going-up", "&aGoing up!"));
        downMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.going-down", "&aGoing down!"));
        cooldownMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.cooldown", "&cPlease wait before using the elevator again!"));
        noElevatorMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.no-elevator", "&cNo elevator found in that direction!"));
        unsafeLocationMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.unsafe-location", "&cUnsafe location! Cannot teleport."));
        maxSearchHeight = config.getInt("max-search-height", 128);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !isSign(block)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();

        // Check if it's an elevator sign
        boolean isUpElevator = false;
        boolean isDownElevator = false;

        for (String line : lines) {
            if (line.toLowerCase().contains("[up]")) {
                isUpElevator = true;
                break;
            } else if (line.toLowerCase().contains("[down]")) {
                isDownElevator = true;
                break;
            }
        }

        if (!isUpElevator && !isDownElevator) {
            return;
        }

        Player player = event.getPlayer();

        // Check cooldown
        if (isOnCooldown(player)) {
            player.sendMessage(cooldownMessage);
            return;
        }

        // Set cooldown
        setCooldown(player);

        // Search for elevator asynchronously
        final boolean searchUp = isUpElevator;
        final Location signLocation = block.getLocation();

        new BukkitRunnable() {
            @Override
            public void run() {
                Location targetLocation = searchForElevator(signLocation, searchUp);

                // Teleport synchronously
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (targetLocation != null && isSafeLocation(targetLocation)) {
                            // Adjust target location to be safe for player
                            player.teleport(targetLocation);
                            player.sendMessage(searchUp ? upMessage : downMessage);
                        } else if (targetLocation != null) {
                            player.sendMessage(unsafeLocationMessage);
                        } else {
                            player.sendMessage(noElevatorMessage);
                        }
                    }
                }.runTask(Elevators.this);
            }
        }.runTaskAsynchronously(this);
    }

    private boolean isSign(Block block) {
        Material type = block.getType();
        return type.name().equals("SIGN") || type.name().contains("SIGN");
    }

    private boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }

        long lastUse = cooldowns.get(playerId);
        return System.currentTimeMillis() - lastUse < cooldownTime;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private Location searchForElevator(Location startLocation, boolean searchUp) {
        int startY = startLocation.getBlockY();
        int increment = searchUp ? 1 : -1;
        int maxY = searchUp ? Math.min(startY + maxSearchHeight, 256) : Math.max(startY - maxSearchHeight, 0);

        for (int y = startY + increment; searchUp ? y <= maxY : y >= maxY; y += increment) {
            Location checkLocation = new Location(
                    startLocation.getWorld(),
                    startLocation.getBlockX(),
                    y,
                    startLocation.getBlockZ());

            Block block = checkLocation.getBlock();
            if (isSign(block)) {
                Sign sign = (Sign) block.getState();
                String[] lines = sign.getLines();

                String targetText = searchUp ? "[down]" : "[up]";
                for (String line : lines) {
                    if (line.toLowerCase().contains(targetText)) {
                        if (isSafeLocation(checkLocation)) {
                            return checkLocation
                                    .add(0.5, 0, 0.5);
                        } else {
                            return checkLocation
                                    .add(0, -1, 0)
                                    .add(0.5, 0, 0.5);
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        Location feet = location;
        Location head = location.clone().add(0, 1, 0);
        Location surface = location.clone().add(0, -1, 0);

        // Check if both blocks above the sign are air or passable
        return isPassable(feet.getBlock()) && isPassable(head.getBlock()) && !isPassable(surface.getBlock());
    }

    private boolean isPassable(Block block) {
        Material type = block.getType();
        return type == Material.AIR ||
                type.name().equals("CAVE_AIR") ||
                type.name().equals("VOID_AIR") ||
                type.name().contains("SIGN") ||
                !type.isSolid();
    }

    // Command to reload config
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender,
            org.bukkit.command.Command command,
            String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("elevators")) {
            return false;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("elevators.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to reload the config!");
                return true;
            }

            reloadConfig();
            loadConfig();
            sender.sendMessage(ChatColor.GREEN + "Elevators config reloaded!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Elevators v" + getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Usage: /elevators reload");
        return true;
    }
}