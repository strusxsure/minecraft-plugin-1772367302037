package com.stormai.plugin;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SimpleLaunch extends JavaPlugin implements Listener {

    private final Map<UUID, Long> noFallDamageUntil = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SimpleLaunch has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleLaunch has been disabled!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if player moved to a new block position
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            // Check if the block below the player is a Gold Pressure Plate
            Location below = to.clone().subtract(0, 1, 0);
            if (below.getBlock().getType() == Material.GOLD_PRESSURE_PLATE) {
                launchPlayer(player, below);
            }
        }
    }

    private void launchPlayer(Player player, Location launchLocation) {
        // Calculate launch velocity: 2 blocks up, 5 blocks forward in facing direction
        float pitch = player.getLocation().getPitch();
        float yaw = player.getLocation().getYaw();

        // Convert yaw to direction vector
        double x = -Math.sin(Math.toRadians(yaw));
        double z = Math.cos(Math.toRadians(yaw));

        // Create velocity vector: upward component + forward component
        Vector velocity = new Vector(x, 0, z).multiply(5); // 5 blocks forward
        velocity.setY(2); // 2 blocks upward

        player.setVelocity(velocity);

        // Play Firework Blast sound
        player.playSound(player.getLocation(), Sound.BLOCK_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYER, 1.0f, 1.0f);

        // Spawn Cloud particles at player's feet
        player.spawnParticle(Particle.CLOUD, launchLocation, 50, 0.5, 0.5, 0.5, 0.1);

        // Prevent fall damage for 5 seconds
        long noFallDamageUntilTime = System.currentTimeMillis() + 5000;
        noFallDamageUntil.put(player.getUniqueId(), noFallDamageUntilTime);

        // Apply resistance effect to prevent fall damage (alternative method)
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 4, true, false), true);
    }

    public boolean shouldTakeFallDamage(Player player) {
        Long until = noFallDamageUntil.get(player.getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            return false;
        }
        return true;
    }
}