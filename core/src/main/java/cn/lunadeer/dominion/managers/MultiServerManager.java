package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.storage.repository.ServerRepository;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static cn.lunadeer.dominion.utils.Misc.formatString;

public class MultiServerManager {

    public static class MultiServerManagerText extends ConfigurationPart {
        public String getIdByNameError = "Server name ({0}) does not exist.";
        public String getNameByIdError = "Server ID ({0}) does not exist.";
        public String warnUpdateServerName = "There is already a server with ID {0} and name {1}, but the current server name is {2}. Updating the name to {2}.";
    }

    public static MultiServerManager instance;
    private final JavaPlugin plugin;
    private final Map<Integer, String> cachedDerverMap = new HashMap<>();

    public MultiServerManager(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        try {
            String name = ServerRepository.getServerName(Configuration.multiServer.serverId);
            if (name == null) {
                ServerRepository.insertServer(Configuration.multiServer.serverId, Configuration.multiServer.serverName);
            } else {
                if (!name.equals(Configuration.multiServer.serverName)) {
                    XLogger.warn(Language.multiServerManagerText.warnUpdateServerName, Configuration.multiServer.serverId, name, Configuration.multiServer.serverName);
                    ServerRepository.updateServerName(Configuration.multiServer.serverId, Configuration.multiServer.serverName);
                }
            }
            cachedDerverMap.put(Configuration.multiServer.serverId, Configuration.multiServer.serverName);
        } catch (Exception e) {
            XLogger.error(e);
        }

    }

    public void connectToServer(@NotNull Player player, @NotNull String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public String getServerName(int serverId) throws Exception {
        if (cachedDerverMap.containsKey(serverId)) {
            return cachedDerverMap.get(serverId);
        }
        String name = ServerRepository.getServerName(serverId);
        if (name == null) {
            throw new Exception(formatString(Language.multiServerManagerText.getNameByIdError, serverId));
        }
        cachedDerverMap.put(serverId, name);
        return cachedDerverMap.get(serverId);
    }

    public Integer getServerId(@NotNull String serverName) throws Exception {
        if (cachedDerverMap.containsValue(serverName)) {
            return cachedDerverMap.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(serverName))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new Exception(formatString(Language.multiServerManagerText.getIdByNameError, serverName)));
        }
        Integer id = ServerRepository.getServerId(serverName);
        if (id == null) {
            throw new Exception(formatString(Language.multiServerManagerText.getIdByNameError, serverName));
        }
        cachedDerverMap.put(id, serverName);
        return id;
    }
}
