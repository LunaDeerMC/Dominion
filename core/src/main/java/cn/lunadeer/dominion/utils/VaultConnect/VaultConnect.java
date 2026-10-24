package cn.lunadeer.dominion.utils.VaultConnect;

import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import static cn.lunadeer.dominion.utils.Misc.formatString;

public class VaultConnect implements Listener {

    public static class VaultConnectText extends ConfigurationPart {
        public String vaultUnavailable = "Vault not available, please install Vault or VaultUnlock to use economy features.";
        public String economyUnavailable = "Economy plugin not found, you need to install one Economy plugin to use economy features.";
        public String insufficientFunds = "Insufficient money, need {0} {1}, but only have {2} {1}.";
    }

    public static VaultConnect instance;
    private VaultInterface vaultInstance = null;
    private final JavaPlugin plugin;

    public VaultConnect(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnable(ServiceRegisterEvent event) {
    }

    private synchronized void assertEconomy() throws Exception {
        if (vaultInstance == null) {
            Plugin vaultPlugin = this.plugin.getServer().getPluginManager().getPlugin("Vault");
            if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
                throw new Exception(Language.vaultConnectText.vaultUnavailable);
            }
            // VaultUnlocked can host legacy providers without a modern provider.
            // Select by registered service, and publish only a fully initialized adapter.
            VaultInterface candidate = initializeVault(true);
            if (candidate == null) {
                candidate = initializeVault(false);
            }
            if (candidate == null) {
                throw new Exception(Language.vaultConnectText.economyUnavailable);
            }
            vaultInstance = candidate;
            XLogger.debug("Vault connected using {0}.", candidate.getClass().getSimpleName());
        }
    }

    private VaultInterface initializeVault(boolean modern) {
        String economyClass = modern
                ? "net.milkbowl.vault2.economy.Economy"
                : "net.milkbowl.vault.economy.Economy";
        try {
            // Classic Vault does not contain the VaultUnlocked API.
            Class.forName(economyClass, false, VaultConnect.class.getClassLoader());
            VaultInterface candidate = modern ? new Vault2() : new Vault();
            return candidate.init(plugin) ? candidate : null;
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (RuntimeException | LinkageError exception) {
            // Fallback is limited to initialization, never balance checks or transactions.
            XLogger.debug("Could not initialize economy service {0}: {1}", economyClass, exception.toString());
            return null;
        }
    }

    public String currencyNamePlural() throws Exception {
        assertEconomy();
        return vaultInstance.currencyNamePlural();
    }

    public String currencyNameSingular() throws Exception {
        assertEconomy();
        return vaultInstance.currencyNameSingular();
    }

    public void withdrawPlayer(Player player, double amount) throws Exception {
        assertEconomy();
        if (amount > getBalance(player)) {
            throw new Exception(formatString(Language.vaultConnectText.insufficientFunds, amount, currencyNamePlural(), getBalance(player)));
        }
        vaultInstance.withdrawPlayer(player, amount);
    }

    public void depositPlayer(Player player, double amount) throws Exception {
        assertEconomy();
        vaultInstance.depositPlayer(player, amount);
    }

    public double getBalance(Player player) throws Exception {
        assertEconomy();
        return vaultInstance.getBalance(player);
    }

}
