package cn.lunadeer.dominion.utils.VaultConnect;

import net.milkbowl.vault2.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;

public class Vault2 implements VaultInterface {

    private Economy econ = null;
    private String PluginName = null;

    @Override
    public boolean init(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
            PluginName = plugin.getName();
            return true;
        }
        return false;
    }

    @Override
    public String currencyNamePlural() {
        return econ.defaultCurrencyNamePlural(PluginName);
    }

    @Override
    public String currencyNameSingular() {
        return econ.defaultCurrencyNameSingular(PluginName);
    }

    @Override
    public void withdrawPlayer(OfflinePlayer player, double amount) {
        econ.withdraw(PluginName, player.getUniqueId(), BigDecimal.valueOf(amount));
    }

    @Override
    public void depositPlayer(OfflinePlayer player, double amount) {
        econ.deposit(PluginName, player.getUniqueId(), BigDecimal.valueOf(amount));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return econ.getBalance(PluginName, player.getUniqueId()).doubleValue();
    }
}
