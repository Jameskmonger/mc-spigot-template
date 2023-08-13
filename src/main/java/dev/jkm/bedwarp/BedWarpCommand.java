package dev.jkm.bedwarp;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BedWarpCommand implements CommandExecutor {
    /**
     * The cooldown for the command, in seconds.
     * 
     * TODO (jkm) load this from config
     */
    private static final int COOLDOWN = 15 * 60;

    /**
     * The delay before warping, in seconds.
     * 
     * TODO (jkm) load this from config
     */
    private static final int DELAY = 8;

    /**
     * The cost to warp, in dollars.
     * 
     * TODO (jkm) load this from config
     * TODO (jkm) is "dollars" correct? Some economies use other currencies.
     */
    private static final int COST = 15;

    /**
     * The radius to search for a bed, in blocks, around the player's bed spawn location.
     */
    private static final int BED_SEARCH_RADIUS = 2;

    private final BedWarpPlugin plugin;

    public BedWarpCommand(BedWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player == false) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        Location bedLocation = player.getBedSpawnLocation();

        if (bedLocation == null) {
            plugin.getLogger().info("Player " + player.getName() + " tried to warp to their bed, but they don't have one");
            player.sendMessage(ChatColor.RED + "You don't have a bed to warp to!");
            return true;
        }

        if (!isBedNearby(bedLocation, BED_SEARCH_RADIUS)) {
            plugin.getLogger().info("Player " + player.getName() + " tried to warp to their bed, but there's no bed nearby " + bedLocation.toString());
            player.sendMessage(ChatColor.RED + "Your bed is missing! You can't warp to it.");
            return true;
        }

        if (plugin.econ.getBalance(player) < COST) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to warp to your bed!");
            return true;
        }

        long secondsLeft = getCooldownSecondsRemaining(player);

        if (secondsLeft > 0) {
            player.sendMessage(ChatColor.RED + "You can't use this for another " + secondsLeft + " seconds!");
            return true;
        }

        queueWarp(player, bedLocation);

        return true;
    }

    /**
     * Queues a warp to the given bed location.
     * @param player The player to warp
     * @param bedLocation The location of the bed to warp to
     */
    private void queueWarp(Player player, Location bedLocation) {
        plugin.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        Location startLocation = player.getLocation();
        player.sendMessage(ChatColor.GREEN + "Warping to your bed in " + DELAY + " seconds, for " + ChatColor.YELLOW + "$" + COST + ChatColor.GREEN + ". Stay still!");
        
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            public void run() {
                Location currentLocation = player.getLocation();
                if (currentLocation.distance(startLocation) <= 1) {
                    plugin.econ.withdrawPlayer(player, COST);
                    player.teleport(bedLocation);
                    player.sendMessage(ChatColor.GREEN + "Successfully warped to your bed for $" + COST);
                } else {
                    player.sendMessage(ChatColor.RED + "You moved! Warp cancelled.");
                    plugin.cooldowns.remove(player.getUniqueId());
                }
            }
        }, DELAY * 20);
    }

    /**
     * Gets the number of seconds remaining in the cooldown for the given player.
     * @param player The player to check
     * @return The number of seconds remaining in the cooldown
     */
    private long getCooldownSecondsRemaining(Player player) {
        if (plugin.cooldowns.containsKey(player.getUniqueId())) {
            long secondsLeft = ((plugin.cooldowns.get(player.getUniqueId()) / 1000) + COOLDOWN)
                    - (System.currentTimeMillis() / 1000);

            return secondsLeft;
        }

        return 0;
    }

    /**
     * Checks if there is a bed nearby the given location.
     * @param location The location to check
     * @return true if there is a bed nearby, false otherwise
     */
    private boolean isBedNearby(Location location, int radius) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);

                    if (block.getType().name().contains("BED")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
