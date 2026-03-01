package com.stormai.plugin;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class SimpleLaunch extends JavaPlugin implements Listener {

    private final Map<UUID, Long> noFallDamageUntil = new HashMap<>();
    private final Map<UUID, Long> launchCooldownUntil = new HashMap<>();
    private final Map<UUID, Boolean> launchEnabled = new HashMap<>();
    private final Map<UUID, Integer> launchHeight = new HashMap<>();
    private final Map<UUID, Integer> launchDistance = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SimpleLaunch has been enabled!");

        // Register commands
        getCommand("launch").setExecutor(new LaunchCommand());
        getCommand("launch").setTabCompleter(new LaunchTabCompleter());
        getCommand("launchreload").setExecutor(new LaunchReloadCommand());

        // Initialize default values for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            launchEnabled.put(player.getUniqueId(), true);
            launchHeight.put(player.getUniqueId(), 2);
            launchDistance.put(player.getUniqueId(), 5);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleLaunch has been disabled!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!launchEnabled.getOrDefault(player.getUniqueId(), true)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if player moved to a new block position
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            // Check if the block below the player is a Gold Pressure Plate
            Location below = to.clone().subtract(0, 1, 0);
            if (below.getBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
                launchPlayer(player, below);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            // Player started sneaking - reset fall damage timer
            Player player = event.getPlayer();
            long noFallDamageUntilTime = System.currentTimeMillis() + 5000;
            noFallDamageUntil.put(player.getUniqueId(), noFallDamageUntilTime);
        }
    }

    private void launchPlayer(Player player, Location launchLocation) {
        // Check cooldown
        long cooldownUntil = launchCooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() < cooldownUntil) {
            return;
        }

        // Calculate launch velocity
        float yaw = player.getLocation().getYaw();
        double x = -Math.sin(Math.toRadians(yaw));
        double z = Math.cos(Math.toRadians(yaw));

        int height = launchHeight.getOrDefault(player.getUniqueId(), 2);
        int distance = launchDistance.getOrDefault(player.getUniqueId(), 5);

        Vector velocity = new Vector(x, 0, z).multiply(distance);
        velocity.setY(height);

        player.setVelocity(velocity);

        // Play Firework Blast sound
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Spawn Cloud particles at player's feet
        player.spawnParticle(Particle.CLOUD, launchLocation, 50, 0.5, 0.5, 0.5, 0.1);

        // Prevent fall damage for 5 seconds
        long noFallDamageUntilTime = System.currentTimeMillis() + 5000;
        noFallDamageUntil.put(player.getUniqueId(), noFallDamageUntilTime);

        // Apply resistance effect to prevent fall damage
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, true, false), true);

        // Set cooldown (2 seconds)
        launchCooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + 2000);
    }

    public boolean shouldTakeFallDamage(Player player) {
        Long until = noFallDamageUntil.get(player.getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            return false;
        }
        return true;
    }

    private class LaunchCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            if (args.length == 0) {
                // Show help
                sender.sendMessage(ChatColor.GOLD + "SimpleLaunch Commands:");
                sender.sendMessage(ChatColor.AQUA + "/launch toggle" + ChatColor.WHITE + " - Enable/disable launch for yourself");
                sender.sendMessage(ChatColor.AQUA + "/launch height <value>" + ChatColor.WHITE + " - Set launch height (default: 2)");
                sender.sendMessage(ChatColor.AQUA + "/launch distance <value>" + ChatColor.WHITE + " - Set launch distance (default: 5)");
                sender.sendMessage(ChatColor.AQUA + "/launch status" + ChatColor.WHITE + " - Show your current settings");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "toggle":
                    boolean enabled = !launchEnabled.getOrDefault(uuid, true);
                    launchEnabled.put(uuid, enabled);
                    sender.sendMessage(ChatColor.GREEN + "Launch " + (enabled ? "enabled" : "disabled") + " for you!");
                    return true;

                case "height":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /launch height <value>");
                        return true;
                    }
                    try {
                        int height = Integer.parseInt(args[1]);
                        if (height < 1 || height > 10) {
                            sender.sendMessage(ChatColor.RED + "Height must be between 1 and 10!");
                            return true;
                        }
                        launchHeight.put(uuid, height);
                        sender.sendMessage(ChatColor.GREEN + "Launch height set to " + height + " blocks!");
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid number!");
                    }
                    return true;

                case "distance":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /launch distance <value>");
                        return true;
                    }
                    try {
                        int distance = Integer.parseInt(args[1]);
                        if (distance < 1 || distance > 20) {
                            sender.sendMessage(ChatColor.RED + "Distance must be between 1 and 20!");
                            return true;
                        }
                        launchDistance.put(uuid, distance);
                        sender.sendMessage(ChatColor.GREEN + "Launch distance set to " + distance + " blocks!");
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid number!");
                    }
                    return true;

                case "status":
                    boolean isEnabled = launchEnabled.getOrDefault(uuid, true);
                    int currentHeight = launchHeight.getOrDefault(uuid, 2);
                    int currentDistance = launchDistance.getOrDefault(uuid, 5);
                    sender.sendMessage(ChatColor.GOLD + "Your SimpleLaunch Settings:");
                    sender.sendMessage(ChatColor.AQUA + "Enabled: " + ChatColor.WHITE + (isEnabled ? "Yes" : "No"));
                    sender.sendMessage(ChatColor.AQUA + "Height: " + ChatColor.WHITE + currentHeight + " blocks");
                    sender.sendMessage(ChatColor.AQUA + "Distance: " + ChatColor.WHITE + currentDistance + " blocks");
                    return true;

                default:
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /launch for help.");
                    return true;
            }
        }
    }

    private class LaunchTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                List<String> options = Arrays.asList("toggle", "height", "distance", "status");
                return options.stream()
                        .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                        .toList();
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("height") || args[0].equalsIgnoreCase("distance")) {
                    return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");
                }
            }
            return Collections.emptyList();
        }
    }

    private class LaunchReloadCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("simplelaunch.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            // In a real plugin, you would reload configuration here
            sender.sendMessage(ChatColor.GREEN + "SimpleLaunch reloaded!");
            return true;
        }
    }
}