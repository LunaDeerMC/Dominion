package cn.lunadeer.dominion.utils.VaultConnect;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Vault implements VaultInterface {

    private Economy econ = null;

    @Override
    public boolean init(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
            return econ != null && econ.isEnabled();
        }
        return false;
    }

    @Override
    public String currencyNamePlural() {
        return econ.currencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return econ.currencyNameSingular();
    }

    @Override
    public void withdrawPlayer(OfflinePlayer player, double amount) {
        checkResponse(econ.withdrawPlayer(player, amount));
    }

    @Override
    public void depositPlayer(OfflinePlayer player, double amount) {
        checkResponse(econ.depositPlayer(player, amount));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return econ.getBalance(player);
    }

    private void checkResponse(EconomyResponse response) {
        if (response == null || !response.transactionSuccess()) {
            String message = response == null ? null : response.errorMessage;
            throw new IllegalStateException(message == null || message.isBlank()
                    ? "Economy transaction failed." : message);
        }
    }
}
