package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.utils.Residence.Message;
import cn.lunadeer.dominion.utils.Residence.Permission;
import cn.lunadeer.dominion.utils.Residence.Residence;
import cn.lunadeer.dominion.utils.Residence.SaveFile;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class ResMigration {
    public static class ResidenceNode {
        public UUID owner;
        public String ownerName;
        public World world;
        public String name;
        public Location loc1;
        public Location loc2;
        public Location tpLoc;
        public String joinMessage;
        public String leaveMessage;
        public List<ResidenceNode> children = new ArrayList<>();
    }

    public static List<ResidenceNode> extractFromResidence(JavaPlugin plugin) {
        List<ResidenceNode> dominions = new ArrayList<>();
        File resSave = new File(plugin.getDataFolder().getParent(), "Residence");
        resSave = new File(resSave, "Save");
        resSave = new File(resSave, "Worlds");
        if (!resSave.exists()) {
            XLogger.info("Residence Save not found, skipping migration");
            return dominions;
        }
        // list .yml files
        File[] files = resSave.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            XLogger.info("No save files found");
            return dominions;
        }
        for (File file : files) {
            try {
                dominions.addAll(processWorld(file));
            } catch (Exception e) {
                XLogger.error("Failed to process file: {0}, {1}", file.getName(), e.getMessage());
            }
        }
        XLogger.info("Extract {0} residences", dominions.size());
        return dominions;
    }

    private static ResidenceNode parseDominion(String name, World world, Residence res, SaveFile save) {
        String[] loc = res.Areas.values().toArray()[0].toString().split(":");
        if (loc.length != 6) {
            XLogger.warn("Invalid location: " + res.Areas.get("main"));
            return null;
        }
        ResidenceNode dominionNode = new ResidenceNode();
        dominionNode.owner = UUID.fromString(res.Permissions.OwnerUUID);
        dominionNode.ownerName = res.Permissions.OwnerLastKnownName;
        dominionNode.world = world;
        dominionNode.name = name;
        dominionNode.joinMessage = save.Messages.get(res.Messages).EnterMessage;
        dominionNode.leaveMessage = save.Messages.get(res.Messages).LeaveMessage;
        dominionNode.loc1 = new Location(world, Double.parseDouble(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]));
        dominionNode.loc2 = new Location(world, Double.parseDouble(loc[3]), Double.parseDouble(loc[4]), Double.parseDouble(loc[5]));
        if (res.TPLoc != null) {
            String[] tpLocStr = res.TPLoc.split(":");
            if (tpLocStr.length >= 3) {
                dominionNode.tpLoc = new Location(world, Double.parseDouble(tpLocStr[0]), Double.parseDouble(tpLocStr[1]), Double.parseDouble(tpLocStr[2]));
            }
        }
        if (res.Subzones != null) {
            for (Map.Entry<String, Residence> entry : res.Subzones.entrySet()) {
                ResidenceNode sub = parseDominion(entry.getKey(), world, entry.getValue(), save);
                if (sub != null) {
                    dominionNode.children.add(sub);
                }
            }
        }
        return dominionNode;
    }

    private static Map<String, Residence> parseResYml(Map<String, Object> zones) {
        Map<String, Residence> res = new HashMap<>();
        for (Map.Entry<String, Object> entry : zones.entrySet()) {
            Map<String, Object> zone = (Map<String, Object>) entry.getValue();
            Residence residence = new Residence();
            if (zone.containsKey("TPLoc")) {
                residence.setTPLoc((String) zone.get("TPLoc"));
            }
            residence.setMessages((int) zone.get("Messages"));
            Permission permission = new Permission();
            permission.OwnerUUID = ((Map<String, Object>) zone.get("Permissions")).get("OwnerUUID").toString();
            if (Objects.equals(permission.OwnerUUID, "00000000-0000-0000-0000-000000000000")) {
                XLogger.warn("Skipping server land residence: {0} (Because Dominion does not support that).", entry.getKey());
                continue;
            }
            permission.OwnerLastKnownName = ((Map<String, Object>) zone.get("Permissions")).get("OwnerLastKnownName").toString();
            residence.setPermissions(permission);
            residence.setAreas((Map<String, String>) zone.get("Areas"));
            if (zone.containsKey("Subzones")) {
                residence.setSubzones(parseResYml((Map<String, Object>) zone.get("Subzones")));
            }
            res.put(entry.getKey(), residence);
        }
        return res;
    }

    private static List<ResidenceNode> processWorld(File saveFile) throws Exception {
        XLogger.debug("=====================================");
        XLogger.debug("Processing file: {0}", saveFile.getName());
        String worldName = saveFile.getName().replace("res_", "").replace(".yml", "");
        World world = Dominion.instance.getServer().getWorld(worldName);
        InputStream inputStream = Files.newInputStream(saveFile.toPath());

        Map<String, Object> yaml = new Yaml().load(inputStream);

        SaveFile save = new SaveFile();

        Map<Integer, Object> Messages = (Map<Integer, Object>) yaml.get("Messages");
        Map<Integer, Message> messages = new HashMap<>();
        for (Map.Entry<Integer, Object> entry : Messages.entrySet()) {
            Map<String, String> message = (Map<String, String>) entry.getValue();
            Message msg = new Message();
            msg.EnterMessage = (String) message.get("EnterMessage")
                    .replace("%owner", "{OWNER}")
                    .replace("%residence", "{DOM}")
                    .replace("%player", "{PLAYER}");
            msg.LeaveMessage = (String) message.get("LeaveMessage")
                    .replace("%owner", "{OWNER}")
                    .replace("%residence", "{DOM}")
                    .replace("%player", "{PLAYER}");
            messages.put(entry.getKey(), msg);
        }
        save.setMessages(messages);

        Map<String, Object> Residences = (Map<String, Object>) yaml.get("Residences");
        save.Residences = parseResYml(Residences);

        inputStream.close();
        List<ResidenceNode> dominions = new ArrayList<>();
        for (Map.Entry<String, Residence> entry : save.Residences.entrySet()) {
            String name = entry.getKey();
            Residence residence = entry.getValue();
            ResidenceNode dominion = parseDominion(name, world, residence, save);
            dominions.add(dominion);
        }
        return dominions;
    }

}
