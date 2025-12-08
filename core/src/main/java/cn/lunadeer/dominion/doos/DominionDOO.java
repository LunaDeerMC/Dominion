package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.*;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.databse.FIelds.Field;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldBoolean;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldInteger;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldString;
import cn.lunadeer.dominion.utils.databse.syntax.Delete;
import cn.lunadeer.dominion.utils.databse.syntax.Insert;
import cn.lunadeer.dominion.utils.databse.syntax.Select;
import cn.lunadeer.dominion.utils.databse.syntax.Update;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DominionDOO implements DominionDTO {

    private DominionCuboid cuboid;
    private final FieldInteger id = new FieldInteger("id");
    private final FieldString owner = new FieldString("owner");
    private final FieldString name = new FieldString("name");
    private final FieldInteger parentDomId = new FieldInteger("parent_dom_id", -1);
    private final FieldString joinMessage = new FieldString("join_message", "");
    private final FieldString leaveMessage = new FieldString("leave_message", "");
    private final Map<EnvFlag, Boolean> envFlags = new HashMap<>();
    private final Map<PriFlag, Boolean> preFlags = new HashMap<>();
    private final FieldString tp_location = new FieldString("tp_location", "default");
    private final FieldString color = new FieldString("color", "#00BFFF");
    private final FieldString world_uid = new FieldString("world_uid");
    private final FieldInteger serverId = new FieldInteger("server_id");
    private final FieldString monsterSpawnSettings = new FieldString("monster_spawn_settings", "");

    // Cache for the parsed UUID to avoid repeated string parsing
    private UUID cachedWorldUid = null;

    private static Field<?>[] fields() {
        Field<?>[] fields = new Field<?>[6 + 6 + Flags.getAllEnvFlagsEnable().size() + Flags.getAllPriFlagsEnable().size() + 5];
        fields[0] = rootDominion().cuboid.x1Field();
        fields[1] = rootDominion().cuboid.y1Field();
        fields[2] = rootDominion().cuboid.z1Field();
        fields[3] = rootDominion().cuboid.x2Field();
        fields[4] = rootDominion().cuboid.y2Field();
        fields[5] = rootDominion().cuboid.z2Field();
        fields[6] = rootDominion().id;
        fields[7] = rootDominion().owner;
        fields[8] = rootDominion().name;
        fields[9] = rootDominion().world_uid;
        fields[10] = rootDominion().parentDomId;
        fields[11] = rootDominion().joinMessage;
        fields[12] = rootDominion().leaveMessage;
        int i = 13;
        for (Flag f : Flags.getAllEnvFlagsEnable()) {
            fields[i++] = new FieldBoolean(f.getFlagName(), f.getDefaultValue());
        }
        for (Flag f : Flags.getAllPriFlagsEnable()) {
            fields[i++] = new FieldBoolean(f.getFlagName(), f.getDefaultValue());
        }
        fields[i++] = rootDominion().tp_location;
        fields[i++] = rootDominion().color;
        fields[i++] = rootDominion().serverId;
        fields[i++] = rootDominion().monsterSpawnSettings;
        return fields;
    }

    private static DominionDOO parse(Map<String, Field<?>> map) {
        DominionCuboid cuboid = new DominionCuboid(
                (int) map.get("x1").getValue(),
                (int) map.get("y1").getValue(),
                (int) map.get("z1").getValue(),
                (int) map.get("x2").getValue(),
                (int) map.get("y2").getValue(),
                (int) map.get("z2").getValue()
        );
        Map<EnvFlag, Boolean> envFlags = new HashMap<>();
        for (EnvFlag f : Flags.getAllEnvFlagsEnable()) {
            envFlags.put(f, (Boolean) map.get(f.getFlagName()).getValue());
        }
        Map<PriFlag, Boolean> preFlags = new HashMap<>();
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            preFlags.put(f, (Boolean) map.get(f.getFlagName()).getValue());
        }
        return new DominionDOO(
                (Integer) map.get("id").getValue(),
                UUID.fromString((String) map.get("owner").getValue()),
                (String) map.get("name").getValue(),
                UUID.fromString((String) map.get("world_uid").getValue()),
                cuboid,
                (Integer) map.get("parent_dom_id").getValue(),
                (String) map.get("join_message").getValue(),
                (String) map.get("leave_message").getValue(),
                envFlags,
                preFlags,
                (String) map.get("tp_location").getValue(),
                (String) map.get("color").getValue(),
                (Integer) map.get("server_id").getValue(),
                (String) map.get("monster_spawn_settings").getValue()
        );
    }

    public static List<DominionDOO> selectAll(Integer serverId) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion")
                .where("server_id = ? AND id >= 0", serverId)
                .execute();
        return res.stream().map(DominionDOO::parse).toList();
    }

    public static DominionDOO rootDominion() {
        return new DominionDOO(-1,
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "根领地", UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new DominionCuboid(-2147483648, -2147483648, -2147483648, 2147483647, 2147483647, 2147483647),
                -1,
                "null", "null",
                new HashMap<>(), new HashMap<>(),
                "default", "#00BFFF", -1, "");
    }

    public static @Nullable DominionDOO select(Integer id) throws SQLException {
        if (id == -1) {
            return rootDominion();
        }
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion")
                .where("id = ?", id)
                .execute();
        if (res.isEmpty()) return null;
        return parse(res.get(0));
    }

    public static @Nullable DominionDOO select(String name) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields()).from("dominion").where("name = ?", name).execute();
        if (res.isEmpty()) return null;
        return parse(res.get(0));
    }

    public static @NotNull DominionDOO insert(DominionDOO dominion) throws SQLException {
        Map<String, Field<?>> res = Insert.insert()
                .into("dominion")
                .values(dominion.owner,
                        dominion.name,
                        dominion.world_uid,
                        dominion.cuboid.x1Field(), dominion.cuboid.y1Field(), dominion.cuboid.z1Field(),
                        dominion.cuboid.x2Field(), dominion.cuboid.y2Field(), dominion.cuboid.z2Field(),
                        dominion.parentDomId,
                        dominion.joinMessage, dominion.leaveMessage,
                        dominion.tp_location,
                        dominion.serverId)
                .returning(fields())
                .execute();
        if (res.isEmpty()) {
            throw new SQLException("Failed to insert dominion.");
        }
        DominionDOO inserted = parse(res);
        CacheManager.instance.getCache().getDominionCache().load(inserted.getId());
        return inserted;
    }

    public static void deleteById(Integer dominion) throws SQLException {
        Delete.delete()
                .from("dominion")
                .where("id = ?", dominion)
                .execute();
        CacheManager.instance.getCache().getDominionCache().delete(dominion);
    }

    // full constructor
    private DominionDOO(Integer id, UUID owner, String name, UUID world_uid,
                        DominionCuboid cuboid,
                        Integer parentDomId,
                        String joinMessage, String leaveMessage,
                        Map<EnvFlag, Boolean> envFlags,
                        Map<PriFlag, Boolean> preFlags,
                        String tp_location,
                        String color,
                        Integer serverId,
                        String monsterSpawnSettings) {
        this.id.setValue(id);
        this.owner.setValue(owner.toString());
        this.name.setValue(name);
        this.world_uid.setValue(world_uid.toString());
        this.cuboid = cuboid;
        this.parentDomId.setValue(parentDomId);
        this.joinMessage.setValue(joinMessage);
        this.leaveMessage.setValue(leaveMessage);
        this.envFlags.putAll(envFlags);
        this.preFlags.putAll(preFlags);
        this.tp_location.setValue(tp_location);
        this.color.setValue(color);
        this.serverId.setValue(serverId);
        this.monsterSpawnSettings.setValue(monsterSpawnSettings != null ? monsterSpawnSettings : "");
    }

    // constructor for new dominion
    public DominionDOO(@NotNull UUID owner,
                       @NotNull String name,
                       @NotNull UUID world_uid,
                       @NotNull CuboidDTO cuboid,
                       @NotNull Integer parentDomId) {
        this.owner.setValue(owner.toString());
        this.name.setValue(name);
        this.world_uid.setValue(world_uid.toString());
        this.cuboid = new DominionCuboid(cuboid);
        this.parentDomId.setValue(parentDomId);
        this.joinMessage.setValue(Configuration.pluginMessage.defaultEnterMessage);
        this.leaveMessage.setValue(Configuration.pluginMessage.defaultLeaveMessage);
        this.serverId.setValue(Configuration.multiServer.serverId);
    }

    private static class DominionCuboid extends CuboidDTO {
        public DominionCuboid(int x1, int y1, int z1, int x2, int y2, int z2) {
            super(x1, y1, z1, x2, y2, z2);
        }

        public DominionCuboid(CuboidDTO superObj) {
            super(superObj.x1(), superObj.y1(), superObj.z1(), superObj.x2(), superObj.y2(), superObj.z2());
        }

        public DominionCuboid(ResultSet rs) throws SQLException {
            super(rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                    rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"));
        }

        public FieldInteger x1Field() {
            return new FieldInteger("x1", x1());
        }

        public FieldInteger y1Field() {
            return new FieldInteger("y1", y1());
        }

        public FieldInteger z1Field() {
            return new FieldInteger("z1", z1());
        }

        public FieldInteger x2Field() {
            return new FieldInteger("x2", x2());
        }

        public FieldInteger y2Field() {
            return new FieldInteger("y2", y2());
        }

        public FieldInteger z2Field() {
            return new FieldInteger("z2", z2());
        }
    }

    // getters and setters
    @Override
    public @NotNull Integer getId() {
        return id.getValue();
    }

    /**
     * 设置领地ID，该方法不会更新数据库，仅用于构造对象
     *
     * @param id 领地ID
     * @return 领地
     */
    public @NotNull DominionDOO setId(Integer id) {
        this.id.setValue(id);
        return this;
    }

    @Override
    public @NotNull UUID getOwner() {
        return UUID.fromString(owner.getValue());
    }

    @Override
    public @NotNull PlayerDTO getOwnerDTO() {
        return Objects.requireNonNull(CacheManager.instance.getPlayer(getOwner()));
    }

    @Override
    public @NotNull DominionDOO setOwner(UUID owner) throws SQLException {
        this.owner.setValue(owner.toString());
        Update.update("dominion")
                .set(this.owner)
                .where("id = ?", id.getValue())
                .execute();
        CacheManager.instance.getCache().getDominionCache().load();
        return this;
    }

    @Override
    public @NotNull DominionDOO setOwner(Player owner) throws SQLException {
        return setOwner(owner.getUniqueId());
    }

    @Override
    public @NotNull String getName() {
        return name.getValue();
    }

    @Override
    public @NotNull DominionDOO setName(String name) throws SQLException {
        String oldName = this.name.getValue();
        this.name.setValue(name);
        Update.update("dominion")
                .set(this.name)
                .where("id = ?", id.getValue())
                .execute();
        CacheManager.instance.getCache().getDominionCache().dominionNameUpdate(oldName, name, getId());
        return this;
    }

    @Override
    public @Nullable World getWorld() {
        return Dominion.instance.getServer().getWorld(getWorldUid());
    }

    @Override
    public @NotNull UUID getWorldUid() {
        // Return cached UUID if available
        if (cachedWorldUid == null) {
            cachedWorldUid = UUID.fromString(world_uid.getValue());
        }
        return cachedWorldUid;
    }

    @Override
    public @NotNull CuboidDTO getCuboid() {
        return cuboid;
    }

    @Override
    public @NotNull DominionDOO setCuboid(@NotNull CuboidDTO cuboid) throws SQLException {
        this.cuboid = new DominionCuboid(cuboid);
        Update.update("dominion")
                .set(this.cuboid.x1Field(),
                        this.cuboid.y1Field(),
                        this.cuboid.z1Field(),
                        this.cuboid.x2Field(),
                        this.cuboid.y2Field(),
                        this.cuboid.z2Field())
                .where("id = ?", id.getValue())
                .execute();
        CacheManager.instance.getCache().getDominionCache().load(getId());
        return this;
    }

    @Override
    public @NotNull Integer getParentDomId() {
        return parentDomId.getValue();
    }

    @Override
    public @NotNull String getJoinMessage() {
        return joinMessage.getValue();
    }

    @Override
    public @NotNull DominionDOO setJoinMessage(String joinMessage) throws SQLException {
        this.joinMessage.setValue(joinMessage);
        Update.update("dominion")
                .set(this.joinMessage)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public @NotNull String getLeaveMessage() {
        return leaveMessage.getValue();
    }

    @Override
    public @NotNull DominionDOO setLeaveMessage(String leaveMessage) throws SQLException {
        this.leaveMessage.setValue(leaveMessage);
        Update.update("dominion")
                .set(this.leaveMessage)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public @NotNull Map<EnvFlag, Boolean> getEnvironmentFlagValue() {
        return envFlags;
    }

    /**
     * 获取领地某个环境配置的值
     *
     * @param flag 权限
     * @return 权限值
     */
    @Override
    public boolean getEnvFlagValue(@NotNull EnvFlag flag) {
        return envFlags.getOrDefault(flag, false);
    }

    @Override
    public @NotNull Map<PriFlag, Boolean> getGuestPrivilegeFlagValue() {
        return preFlags;
    }

    /**
     * 获取领地某个访客权限的值
     *
     * @param flag 权限
     * @return 权限值
     */
    @Override
    public boolean getGuestFlagValue(@NotNull PriFlag flag) {
        if (preFlags.equals(Flags.ADMIN)) { // guest's admin flag is always false
            return false;
        }
        return preFlags.getOrDefault(flag, false);
    }

    @Override
    public @NotNull DominionDOO setEnvFlagValue(@NotNull EnvFlag flag, @NotNull Boolean value) throws SQLException {
        envFlags.put(flag, value);
        FieldBoolean flagField = new FieldBoolean(flag.getFlagName(), value);
        Update.update("dominion")
                .set(flagField)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public @NotNull DominionDOO setGuestFlagValue(@NotNull PriFlag flag, @NotNull Boolean value) throws SQLException {
        preFlags.put(flag, value);
        FieldBoolean flagField = new FieldBoolean(flag.getFlagName(), value);
        Update.update("dominion")
                .set(flagField)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }


    @Override
    public @NotNull Location getTpLocation() {
        if (Objects.equals(tp_location.getValue(), "default")) {
            return new Location(getWorld(),
                    (double) (cuboid.x1() + cuboid.x2()) / 2,
                    (double) (cuboid.y1() + cuboid.y2()) / 2,
                    (double) (cuboid.z1() + cuboid.z2()) / 2);
        } else {
            // 0:0:0
            String[] loc = tp_location.getValue().split(":");
            World w = getWorld();
            if (loc.length == 3 && w != null) {
                return new Location(w, Integer.parseInt(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2]));
            } else {
                return new Location(getWorld(),
                        (double) (cuboid.x1() + cuboid.x2()) / 2,
                        (double) (cuboid.y1() + cuboid.y2()) / 2,
                        (double) (cuboid.z1() + cuboid.z2()) / 2);
            }
        }
    }

    @Override
    public @NotNull DominionDOO setTpLocation(Location loc) throws SQLException {
        this.tp_location.setValue(loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
        Update.update("dominion")
                .set(this.tp_location)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    public @NotNull DominionDOO setColor(@NotNull Color color) throws SQLException {
        this.color.setValue(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
        Update.update("dominion")
                .set(this.color)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public List<GroupDTO> getGroups() {
        return Objects.requireNonNull(CacheManager.instance.getCache(getServerId())).getGroupCache().getDominionGroups(this);
    }

    @Override
    public List<MemberDTO> getMembers() {
        return Objects.requireNonNull(CacheManager.instance.getCache(getServerId())).getMemberCache().getDominionMembers(this);
    }

    @Override
    public Integer getServerId() {
        return serverId.getValue();
    }

    @Override
    public int getColorR() {
        return Integer.valueOf(getColor().substring(1, 3), 16);
    }

    @Override
    public int getColorG() {
        return Integer.valueOf(getColor().substring(3, 5), 16);
    }

    @Override
    public int getColorB() {
        return Integer.valueOf(getColor().substring(5, 7), 16);
    }

    @Override
    public @NotNull String getColor() {
        return color.getValue();
    }

    @Override
    public int getColorHex() {
        return (getColorR() << 16) + (getColorG() << 8) + getColorB();
    }

    @Override
    public @NotNull String getMonsterSpawnSettings() {
        return monsterSpawnSettings.getValue() != null ? monsterSpawnSettings.getValue() : "";
    }

    @Override
    public @NotNull DominionDOO setMonsterSpawnSettings(@NotNull String settings) throws SQLException {
        this.monsterSpawnSettings.setValue(settings);
        Update.update("dominion")
                .set(this.monsterSpawnSettings)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    /**
     * Delete dominion by player UUID.
     * <p>
     * THIS SHOULD ONLY BE USED TO CLEAR LEGACY DATA.
     *
     * @param playerUUID the UUID of the player to delete
     * @throws SQLException if a database access error occurs
     */
    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        Delete.delete().from("dominion").where("owner = ?", playerUUID.toString()).execute();
    }
}
