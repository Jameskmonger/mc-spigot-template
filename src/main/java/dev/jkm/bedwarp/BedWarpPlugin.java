package dev.jkm.bedwarp;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class BedWarpPlugin extends JavaPlugin {
    /**
     * Whether or not economy is enabled.
     * 
     * TODO (jkm) load this from config
     */
    public static final boolean ECONOMY_ENABLED = true;

    /**
     * The Vault economy instance.
     */
    public Economy econ = null;

    /**
     * Cooldowns for players, i.e. when they can use the command again.
     * 
     * TODO (jkm) add admin command to reset cooldown for a player
     */
    public HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        if (ECONOMY_ENABLED) {
            boolean economySetupSuccess = setupEconomy();

            if (economySetupSuccess) {
                getLogger().log(Level.INFO, "Economy is enabled.");
            } else {
                getLogger().log(Level.WARNING, "Economy is enabled, but Vault not found! Bedwarp will be free.");
            }
        } else {
            getLogger().log(Level.INFO, "Economy is disabled.");
        }

        this.getCommand("bedwarp").setExecutor(new BedWarpCommand(this));
    }

    /**
     * Load the economy from Vault.
     *
     * @return true if the economy was loaded successfully, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        econ = rsp.getProvider();
        return econ != null;
    }
}
